/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.model;

import static android.text.format.DateUtils.DAY_IN_MILLIS;
import static android.text.format.DateUtils.formatElapsedTime;

import static com.android.launcher3.EncryptionType.ENCRYPTED;
import static com.android.launcher3.LauncherPrefs.nonRestorableItem;
import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION;
import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_PREDICTION;
import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_WIDGETS_PREDICTION;
import static com.android.launcher3.LauncherSettings.Favorites.DESKTOP_ICON_FLAG;
import static com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
import static com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT;
import static com.android.launcher3.hybridhotseat.HotseatPredictionModel.convertDataModelToAppTargetBundle;
import static com.android.launcher3.icons.cache.CacheLookupFlag.DEFAULT_LOOKUP_FLAG;
import static com.android.launcher3.model.PredictionHelper.getAppTargetFromItemInfo;
import static com.android.launcher3.model.PredictionHelper.wrapAppTargetWithItemLocation;
import static com.android.launcher3.util.Executors.MODEL_EXECUTOR;

import static java.util.stream.Collectors.toCollection;

import android.app.StatsManager;
import android.app.prediction.AppPredictionContext;
import android.app.prediction.AppPredictionManager;
import android.app.prediction.AppPredictor;
import android.app.prediction.AppTarget;
import android.app.prediction.AppTargetEvent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.StatsEvent;

import androidx.annotation.AnyThread;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.android.launcher3.ConstantItem;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.dagger.ApplicationContext;
import com.android.launcher3.icons.cache.CacheLookupFlag;
import com.android.launcher3.logger.LauncherAtom;
import com.android.launcher3.logging.InstanceId;
import com.android.launcher3.logging.InstanceIdSequence;
import com.android.launcher3.model.BgDataModel.FixedContainerItems;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.model.data.CollectionInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.pm.UserCache;
import com.android.launcher3.shortcuts.ShortcutKey;
import com.android.launcher3.util.ApiWrapper;
import com.android.launcher3.util.Executors;
import com.android.launcher3.util.IntSparseArrayMap;
import com.android.launcher3.util.PackageManagerHelper;
import com.android.launcher3.util.PersistedItemArray;
import com.android.quickstep.logging.SettingsChangeLogger;
import com.android.quickstep.logging.StatsLogCompatManager;
import com.android.quickstep.util.ContextualSearchStateManager;
import com.android.systemui.shared.system.SysUiStatsLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Model delegate which loads prediction items
 */
public class QuickstepModelDelegate extends ModelDelegate {

    private static final String BUNDLE_KEY_ADDED_APP_WIDGETS = "added_app_widgets";
    private static final int NUM_OF_RECOMMENDED_WIDGETS_PREDICATION = 20;

    private static final boolean IS_DEBUG = false;
    private static final String TAG = "QuickstepModelDelegate";

    private static final ConstantItem<Long> LAST_SNAPSHOT_TIME_MILLIS =
            nonRestorableItem("LAST_SNAPSHOT_TIME_MILLIS", 0L, ENCRYPTED);

    @VisibleForTesting
    final PredictorState mAllAppsState = new PredictorState(
            CONTAINER_PREDICTION, "all_apps_predictions", DEFAULT_LOOKUP_FLAG);
    @VisibleForTesting
    final PredictorState mHotseatState = new PredictorState(
            CONTAINER_HOTSEAT_PREDICTION, "hotseat_predictions", DESKTOP_ICON_FLAG);
    @VisibleForTesting
    final PredictorState mWidgetsRecommendationState = new PredictorState(
            CONTAINER_WIDGETS_PREDICTION, "widgets_prediction", DESKTOP_ICON_FLAG);

    private final InvariantDeviceProfile mIDP;
    private final PackageManagerHelper mPmHelper;
    private final AppEventProducer mAppEventProducer;

    private final StatsManager mStatsManager;

    protected boolean mActive = false;

    @Inject
    public QuickstepModelDelegate(@ApplicationContext Context context,
            InvariantDeviceProfile idp,
            PackageManagerHelper pmHelper,
            @Nullable @Named("ICONS_DB") String dbFileName) {
        super(context);
        mIDP = idp;
        mPmHelper = pmHelper;

        mAppEventProducer = new AppEventProducer(context, this::onAppTargetEvent);
        StatsLogCompatManager.LOGS_CONSUMER.add(mAppEventProducer);

        // Only register for launcher snapshot logging if this is the primary ModelDelegate
        // instance, as there will be additional instances that may be destroyed at any time.
        mStatsManager = TextUtils.isEmpty(dbFileName)
                ? null : context.getSystemService(StatsManager.class);
    }

    @CallSuper
    @Override
    public void loadAndBindWorkspaceItems(@NonNull UserManagerState ums,
            @NonNull BgDataModel.Callbacks[] callbacks,
            @NonNull Map<ShortcutKey, ShortcutInfo> pinnedShortcuts) {
        loadAndBindItems(ums, pinnedShortcuts, callbacks, mIDP.numDatabaseHotseatIcons,
                mHotseatState);
    }

    @CallSuper
    @Override
    public void loadAndBindAllAppsItems(@NonNull UserManagerState ums,
            @NonNull BgDataModel.Callbacks[] callbacks,
            @NonNull Map<ShortcutKey, ShortcutInfo> pinnedShortcuts) {
        loadAndBindItems(ums, pinnedShortcuts, callbacks, mIDP.numDatabaseAllAppsColumns,
                mAllAppsState);
    }

    @WorkerThread
    private void loadAndBindItems(@NonNull UserManagerState ums,
            @NonNull Map<ShortcutKey, ShortcutInfo> pinnedShortcuts,
            @NonNull BgDataModel.Callbacks[] callbacks,
            int numColumns, @NonNull PredictorState state) {
        // TODO: Implement caching and preloading

        WorkspaceItemFactory factory =
                new WorkspaceItemFactory(mContext, ums, mPmHelper, pinnedShortcuts, numColumns,
                        state.containerId, state.lookupFlag);
        FixedContainerItems fci = new FixedContainerItems(state.containerId,
                state.storage.read(mContext, factory, ums.allUsers::get));
        mDataModel.extraItems.put(state.containerId, fci);
    }

    @CallSuper
    @Override
    public void loadAndBindOtherItems(@NonNull BgDataModel.Callbacks[] callbacks) {
        FixedContainerItems widgetPredictionFCI = new FixedContainerItems(
                mWidgetsRecommendationState.containerId, new ArrayList<>());

        // Widgets prediction isn't used frequently. And thus, it is not persisted on disk.
        mDataModel.extraItems.put(mWidgetsRecommendationState.containerId, widgetPredictionFCI);

        bindPredictionItems(callbacks, widgetPredictionFCI);
        loadStringCache(mDataModel.stringCache);
    }

    @AnyThread
    private void bindPredictionItems(@NonNull BgDataModel.Callbacks[] callbacks,
            @NonNull FixedContainerItems fci) {
        Executors.MAIN_EXECUTOR.execute(() -> {
            for (BgDataModel.Callbacks c : callbacks) {
                c.bindExtraContainerItems(fci);
            }
        });
    }

    @Override
    @WorkerThread
    public void bindAllModelExtras(@NonNull BgDataModel.Callbacks[] callbacks) {
        Iterable<FixedContainerItems> containerItems;
        synchronized (mDataModel.extraItems) {
            containerItems = mDataModel.extraItems.clone();
        }
        Executors.MAIN_EXECUTOR.execute(() -> {
            for (BgDataModel.Callbacks c : callbacks) {
                for (FixedContainerItems fci : containerItems) {
                    c.bindExtraContainerItems(fci);
                }
            }
        });
    }

    public void markActive() {
        super.markActive();
        mActive = true;
    }

    @WorkerThread
    @Override
    public void workspaceLoadComplete() {
        super.workspaceLoadComplete();
        // Initialize ContextualSearchStateManager.
        ContextualSearchStateManager.INSTANCE.get(mContext);
        recreatePredictors();
    }

    @Override
    @WorkerThread
    public void modelLoadComplete() {
        super.modelLoadComplete();

        // Log snapshot of the model
        LauncherPrefs prefs = LauncherPrefs.get(mContext);
        long lastSnapshotTimeMillis = prefs.get(LAST_SNAPSHOT_TIME_MILLIS);
        // Log snapshot only if previous snapshot was older than a day
        long now = System.currentTimeMillis();
        if (now - lastSnapshotTimeMillis < DAY_IN_MILLIS) {
            if (IS_DEBUG) {
                String elapsedTime = formatElapsedTime((now - lastSnapshotTimeMillis) / 1000);
                Log.d(TAG, String.format(
                        "Skipped snapshot logging since previous snapshot was %s old.",
                        elapsedTime));
            }
        } else {
            IntSparseArrayMap<ItemInfo> itemsIdMap;
            synchronized (mDataModel) {
                itemsIdMap = mDataModel.itemsIdMap.clone();
            }
            InstanceId instanceId = new InstanceIdSequence().newInstanceId();
            for (ItemInfo info : itemsIdMap) {
                CollectionInfo parent = getContainer(info, itemsIdMap);
                StatsLogCompatManager.writeSnapshot(info.buildProto(parent, mContext), instanceId);
            }
            additionalSnapshotEvents(instanceId);
            prefs.put(LAST_SNAPSHOT_TIME_MILLIS, now);
        }

        registerSnapshotLoggingCallback();
    }

    protected void additionalSnapshotEvents(InstanceId snapshotInstanceId){}

    /**
     * Registers a callback to log launcher workspace layout using Statsd pulled atom.
     */
    private void registerSnapshotLoggingCallback() {
        if (mStatsManager == null) {
            Log.d(TAG, "Skipping snapshot logging");
        }

        try {
            mStatsManager.setPullAtomCallback(
                    SysUiStatsLog.LAUNCHER_LAYOUT_SNAPSHOT,
                    null /* PullAtomMetadata */,
                    MODEL_EXECUTOR,
                    (i, eventList) -> {
                        InstanceId instanceId = new InstanceIdSequence().newInstanceId();
                        IntSparseArrayMap<ItemInfo> itemsIdMap;
                        synchronized (mDataModel) {
                            itemsIdMap = mDataModel.itemsIdMap.clone();
                        }

                        for (ItemInfo info : itemsIdMap) {
                            CollectionInfo parent = getContainer(info, itemsIdMap);
                            LauncherAtom.ItemInfo itemInfo = info.buildProto(parent, mContext);
                            Log.d(TAG, itemInfo.toString());
                            StatsEvent statsEvent = StatsLogCompatManager.buildStatsEvent(itemInfo,
                                    instanceId);
                            eventList.add(statsEvent);
                        }
                        Log.d(TAG,
                                String.format(
                                        "Successfully logged %d workspace items with instanceId=%d",
                                        itemsIdMap.size(), instanceId.getId()));
                        additionalSnapshotEvents(instanceId);
                        SettingsChangeLogger.INSTANCE.get(mContext).logSnapshot(instanceId);
                        return StatsManager.PULL_SUCCESS;
                    }
            );
            Log.d(TAG, "Successfully registered for launcher snapshot logging!");
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to register launcher snapshot logging callback with StatsManager",
                    e);
        }
    }

    private static CollectionInfo getContainer(
            ItemInfo info, IntSparseArrayMap<ItemInfo> itemsIdMap) {
        if (info.container > 0) {
            ItemInfo containerInfo = itemsIdMap.get(info.container);

            if (!(containerInfo instanceof CollectionInfo)) {
                Log.e(TAG, String.format(
                        "Item info: %s found with invalid container: %s",
                        info,
                        containerInfo));
            }
            // Allow crash to help debug b/173838775
            return (CollectionInfo) containerInfo;
        }
        return null;
    }

    @Override
    public void validateData() {
        super.validateData();
        if (mAllAppsState.predictor != null) {
            mAllAppsState.predictor.requestPredictionUpdate();
        }
        if (mWidgetsRecommendationState.predictor != null) {
            mWidgetsRecommendationState.predictor.requestPredictionUpdate();
        }
    }

    @WorkerThread
    @Override
    public void destroy() {
        super.destroy();
        mActive = false;
        StatsLogCompatManager.LOGS_CONSUMER.remove(mAppEventProducer);
        if (mStatsManager != null) {
            try {
                mStatsManager.clearPullAtomCallback(SysUiStatsLog.LAUNCHER_LAYOUT_SNAPSHOT);
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to unregister snapshot logging callback with StatsManager", e);
            }
        }
        destroyPredictors();
    }

    private void destroyPredictors() {
        mAllAppsState.destroyPredictor();
        mHotseatState.destroyPredictor();
        mWidgetsRecommendationState.destroyPredictor();
    }

    @WorkerThread
    private void recreatePredictors() {
        destroyPredictors();
        if (!mActive) {
            return;
        }
        AppPredictionManager apm = mContext.getSystemService(AppPredictionManager.class);
        if (apm == null) {
            return;
        }

        registerPredictor(mAllAppsState, apm.createAppPredictionSession(
                new AppPredictionContext.Builder(mContext)
                        .setUiSurface("home")
                        .setPredictedTargetCount(mIDP.numDatabaseAllAppsColumns)
                        .build()));

        // TODO: get bundle
        registerHotseatPredictor(apm, mContext);

        registerWidgetsPredictor(apm.createAppPredictionSession(
                new AppPredictionContext.Builder(mContext)
                        .setUiSurface("widgets")
                        .setExtras(getBundleForWidgetsOnWorkspace(mContext, mDataModel))
                        .setPredictedTargetCount(NUM_OF_RECOMMENDED_WIDGETS_PREDICATION)
                        .build()));
    }

    @WorkerThread
    private void recreateHotseatPredictor() {
        mHotseatState.destroyPredictor();
        if (!mActive) {
            return;
        }
        AppPredictionManager apm = mContext.getSystemService(AppPredictionManager.class);
        if (apm == null) {
            return;
        }
        registerHotseatPredictor(apm, mContext);
    }

    private void registerHotseatPredictor(AppPredictionManager apm, Context context) {
        registerPredictor(mHotseatState, apm.createAppPredictionSession(
                new AppPredictionContext.Builder(context)
                        .setUiSurface("hotseat")
                        .setPredictedTargetCount(mIDP.numDatabaseHotseatIcons)
                        .setExtras(convertDataModelToAppTargetBundle(context, mDataModel))
                        .build()));
    }

    private void registerPredictor(PredictorState state, AppPredictor predictor) {
        state.setTargets(Collections.emptyList());
        state.predictor = predictor;
        state.predictor.registerPredictionUpdates(
                MODEL_EXECUTOR, t -> handleUpdate(state, t));
        state.predictor.requestPredictionUpdate();
    }

    private void handleUpdate(PredictorState state, List<AppTarget> targets) {
        if (state.setTargets(targets)) {
            // No diff, skip
            return;
        }
        mModel.enqueueModelUpdateTask(new PredictionUpdateTask(state, targets));
    }

    private void registerWidgetsPredictor(AppPredictor predictor) {
        mWidgetsRecommendationState.predictor = predictor;
        mWidgetsRecommendationState.predictor.registerPredictionUpdates(
                MODEL_EXECUTOR, targets -> {
                    if (mWidgetsRecommendationState.setTargets(targets)) {
                        // No diff, skip
                        return;
                    }
                    mModel.enqueueModelUpdateTask(
                            new WidgetsPredictionUpdateTask(mWidgetsRecommendationState, targets));
                });
        mWidgetsRecommendationState.predictor.requestPredictionUpdate();
    }

    @VisibleForTesting
    void onAppTargetEvent(AppTargetEvent event, int client) {
        PredictorState state;
        switch(client) {
            case CONTAINER_PREDICTION:
                state = mAllAppsState;
                break;
            case CONTAINER_WIDGETS_PREDICTION:
                state = mWidgetsRecommendationState;
                break;
            case CONTAINER_HOTSEAT_PREDICTION:
            default:
                state = mHotseatState;
                break;
        }
        if (state.predictor != null) {
            state.predictor.notifyAppTargetEvent(event);
            Log.d(TAG, "notifyAppTargetEvent action=" + event.getAction()
                    + " launchLocation=" + event.getLaunchLocation());
            if (state == mHotseatState
                    && (event.getAction() == AppTargetEvent.ACTION_PIN
                            || event.getAction() == AppTargetEvent.ACTION_UNPIN)) {
                // Recreate hot seat predictor when we need to query for hot seat due to pin or
                // unpin app icons.
                recreateHotseatPredictor();
            }
        }
    }

    private Bundle getBundleForWidgetsOnWorkspace(Context context, BgDataModel dataModel) {
        Bundle bundle = new Bundle();
        ArrayList<AppTargetEvent> widgetEvents =
                dataModel.itemsIdMap.stream()
                        .filter(PredictionHelper::isTrackedForWidgetPrediction)
                        .map(item -> {
                            AppTarget target = getAppTargetFromItemInfo(context, item);
                            if (target == null) return null;
                            return wrapAppTargetWithItemLocation(
                                    target, AppTargetEvent.ACTION_PIN, item);
                        })
                        .filter(Objects::nonNull)
                        .collect(toCollection(ArrayList::new));
        bundle.putParcelableArrayList(BUNDLE_KEY_ADDED_APP_WIDGETS, widgetEvents);
        return bundle;
    }

    static class PredictorState {

        public final int containerId;
        public final PersistedItemArray<ItemInfo> storage;
        public AppPredictor predictor;
        public CacheLookupFlag lookupFlag;

        private List<AppTarget> mLastTargets;

        PredictorState(int containerId, String storageName, CacheLookupFlag lookupFlag) {
            this.containerId = containerId;
            storage = new PersistedItemArray<>(storageName);
            mLastTargets = Collections.emptyList();
            this.lookupFlag = lookupFlag;
        }

        public void destroyPredictor() {
            if (predictor != null) {
                predictor.destroy();
                predictor = null;
            }
        }

        /**
         * Sets the new targets and returns true if it was the same as before.
         */
        boolean setTargets(List<AppTarget> newTargets) {
            List<AppTarget> oldTargets = mLastTargets;
            mLastTargets = newTargets;

            int size = oldTargets.size();
            return size == newTargets.size() && IntStream.range(0, size)
                    .allMatch(i -> areAppTargetsSame(oldTargets.get(i), newTargets.get(i)));
        }
    }

    /**
     * Compares two targets for the properties which we care about
     */
    private static boolean areAppTargetsSame(AppTarget t1, AppTarget t2) {
        if (!Objects.equals(t1.getPackageName(), t2.getPackageName())
                || !Objects.equals(t1.getUser(), t2.getUser())
                || !Objects.equals(t1.getClassName(), t2.getClassName())) {
            return false;
        }

        ShortcutInfo s1 = t1.getShortcutInfo();
        ShortcutInfo s2 = t2.getShortcutInfo();
        if (s1 != null) {
            if (s2 == null || !Objects.equals(s1.getId(), s2.getId())) {
                return false;
            }
        } else if (s2 != null) {
            return false;
        }
        return true;
    }

    private static class WorkspaceItemFactory implements PersistedItemArray.ItemFactory<ItemInfo> {

        private final Context mContext;
        private final UserManagerState mUMS;
        private final PackageManagerHelper mPmHelper;
        private final Map<ShortcutKey, ShortcutInfo> mPinnedShortcuts;
        private final int mMaxCount;
        private final int mContainer;
        private final CacheLookupFlag mLookupFlag;

        private int mReadCount = 0;

        protected WorkspaceItemFactory(
                Context context, UserManagerState ums,
                PackageManagerHelper pmHelper, Map<ShortcutKey, ShortcutInfo> pinnedShortcuts,
                int maxCount, int container, CacheLookupFlag lookupFlag) {
            mContext = context;
            mUMS = ums;
            mPmHelper = pmHelper;
            mPinnedShortcuts = pinnedShortcuts;
            mMaxCount = maxCount;
            mContainer = container;
            mLookupFlag = lookupFlag;
        }

        @Nullable
        @Override
        public ItemInfo createInfo(int itemType, UserHandle user, Intent intent) {
            if (mReadCount >= mMaxCount) {
                return null;
            }
            switch (itemType) {
                case ITEM_TYPE_APPLICATION: {
                    LauncherActivityInfo lai = mContext
                            .getSystemService(LauncherApps.class)
                            .resolveActivity(intent, user);
                    if (lai == null) {
                        return null;
                    }
                    AppInfo info = new AppInfo(
                            lai,
                            UserCache.INSTANCE.get(mContext).getUserInfo(user),
                            ApiWrapper.INSTANCE.get(mContext),
                            mPmHelper,
                            mUMS.isUserQuiet(user));
                    info.container = mContainer;
                    LauncherAppState.getInstance(mContext).getIconCache()
                            .getTitleAndIcon(info, lai, mLookupFlag);
                    mReadCount++;
                    return info.makeWorkspaceItem(mContext);
                }
                case ITEM_TYPE_DEEP_SHORTCUT: {
                    ShortcutKey key = ShortcutKey.fromIntent(intent, user);
                    if (key == null) {
                        return null;
                    }
                    ShortcutInfo si = mPinnedShortcuts.get(key);
                    if (si == null) {
                        return null;
                    }
                    WorkspaceItemInfo wii = new WorkspaceItemInfo(si, mContext);
                    wii.container = mContainer;
                    LauncherAppState.getInstance(mContext).getIconCache().getShortcutIcon(wii, si);
                    mReadCount++;
                    return wii;
                }
            }
            return null;
        }
    }
}
