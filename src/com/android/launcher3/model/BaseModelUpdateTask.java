/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Context;
import android.os.UserHandle;
import android.util.LongSparseArray;
import android.util.Pair;

import com.android.launcher3.AllAppsList;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherModel.CallbackTask;
import com.android.launcher3.LauncherModel.Callbacks;
import com.android.launcher3.LauncherModel.ModelUpdateTask;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.GridOccupancy;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.MultiHashMap;
import com.android.mxlibrary.util.XLog;
import com.android.launcher3.widget.WidgetListRowEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Extension of {@link ModelUpdateTask} with some utility methods
 */
public abstract class BaseModelUpdateTask implements ModelUpdateTask {

    private static final boolean DEBUG_TASKS = true;
    private static final String TAG = "BaseModelUpdateTask";

    private LauncherAppState mApp;
    private LauncherModel mModel;
    private BgDataModel mDataModel;
    private AllAppsList mAllAppsList;
    private Executor mUiExecutor;
    private boolean mForceExecute = false;

    public void init(LauncherAppState app, LauncherModel model,
                     BgDataModel dataModel, AllAppsList allAppsList, Executor uiExecutor) {
        mApp = app;
        mModel = model;
        mDataModel = dataModel;
        mAllAppsList = allAppsList;
        mUiExecutor = uiExecutor;
    }

    @Override
    public final void run() {
        if (!mForceExecute) {
            if (!mModel.isModelLoaded()) {
                if (DEBUG_TASKS) {
                    XLog.e(XLog.getTag(), XLog.TAG_GU + "Ignoring model task since loader is pending=" + this);
                }
                // Loader has not yet run.
                return;
            }
        }
        execute(mApp, mDataModel, mAllAppsList);
    }

    /**
     * Execute the actual task. Called on the worker thread.
     */
    public abstract void execute(
            LauncherAppState app, BgDataModel dataModel, AllAppsList apps);

    /**
     * Schedules a {@param task} to be executed on the current callbacks.
     */
    public final void scheduleCallbackTask(final CallbackTask task) {
        final Callbacks callbacks = mModel.getCallback();
        mUiExecutor.execute(() -> {
            Callbacks cb = mModel.getCallback();
            if (callbacks == cb && cb != null) {
                task.execute(callbacks);
            }
        });
    }

    public ModelWriter getModelWriter() {
        // Updates from model task, do not deal with icon position in hotseat. Also no need to
        // verify changes as the ModelTasks always push the changes to callbacks
        return mModel.getWriter(false /* hasVerticalHotseat */, false /* verifyChanges */);
    }


    public void bindUpdatedShortcuts(
            final ArrayList<ShortcutInfo> updatedShortcuts, final UserHandle user) {
        if (!updatedShortcuts.isEmpty()) {
            scheduleCallbackTask(new CallbackTask() {
                @Override
                public void execute(Callbacks callbacks) {
                    callbacks.bindShortcutsChanged(updatedShortcuts, user);
                }
            });
        }
    }

    public void bindDeepShortcuts(BgDataModel dataModel) {
        final MultiHashMap<ComponentKey, String> shortcutMapCopy = dataModel.deepShortcutMap.clone();
        scheduleCallbackTask(new CallbackTask() {
            @Override
            public void execute(Callbacks callbacks) {
                callbacks.bindDeepShortcutMap(shortcutMapCopy);
            }
        });
    }

    public void bindUpdatedWidgets(BgDataModel dataModel) {
        final ArrayList<WidgetListRowEntry> widgets =
                dataModel.widgetsModel.getWidgetsList(mApp.getContext());
        scheduleCallbackTask(new CallbackTask() {
            @Override
            public void execute(Callbacks callbacks) {
                callbacks.bindAllWidgets(widgets);
            }
        });
    }

    public void deleteAndBindComponentsRemoved(final ItemInfoMatcher matcher) {
        getModelWriter().deleteItemsFromDatabase(matcher);

        // Call the components-removed callback
        scheduleCallbackTask(new CallbackTask() {
            @Override
            public void execute(Callbacks callbacks) {
                callbacks.bindWorkspaceComponentsRemoved(matcher);
            }
        });
    }

    /**
     * Find a position on the screen for the given size or adds a new screen.
     *
     * @return screenId and the coordinates for the item.
     */
    protected Pair<Long, int[]> findSpaceForItem(
            LauncherAppState app, BgDataModel dataModel,
            ArrayList<Long> workspaceScreens,
            ArrayList<Long> addedWorkspaceScreensFinal,
            int spanX, int spanY) {
        LongSparseArray<ArrayList<ItemInfo>> screenItems = new LongSparseArray<>();

        // Use sBgItemsIdMap as all the items are already loaded.
        synchronized (dataModel) {
            for (ItemInfo info : dataModel.itemsIdMap) {
                if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    ArrayList<ItemInfo> items = screenItems.get(info.screenId);
                    if (items == null) {
                        items = new ArrayList<>();
                        screenItems.put(info.screenId, items);
                    }
                    items.add(info);
                }
            }
        }

        // Find appropriate space for the item.
        long screenId = 0;
        int[] cordinates = new int[2];
        boolean found = false;

        int screenCount = workspaceScreens.size();
        // First check the preferred screen.
        int preferredScreenIndex = workspaceScreens.isEmpty() ? 0 : 1;
        if (preferredScreenIndex < screenCount) {
            screenId = workspaceScreens.get(preferredScreenIndex);
            found = findNextAvailableIconSpaceInScreen(
                    app, screenItems.get(screenId), cordinates, spanX, spanY);
        }

        if (!found) {
            // Search on any of the screens starting from the first screen.
            for (int screen = 1; screen < screenCount; screen++) {
                screenId = workspaceScreens.get(screen);
                if (findNextAvailableIconSpaceInScreen(
                        app, screenItems.get(screenId), cordinates, spanX, spanY)) {
                    // We found a space for it
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            // Still no position found. Add a new screen to the end.
            screenId = LauncherSettings.Settings.call(app.getContext().getContentResolver(),
                    LauncherSettings.Settings.METHOD_NEW_SCREEN_ID)
                    .getLong(LauncherSettings.Settings.EXTRA_VALUE);

            // Save the screen id for binding in the workspace
            workspaceScreens.add(screenId);
            addedWorkspaceScreensFinal.add(screenId);

            // If we still can't find an empty space, then God help us all!!!
            if (!findNextAvailableIconSpaceInScreen(
                    app, screenItems.get(screenId), cordinates, spanX, spanY)) {
                throw new RuntimeException("Can't find space to add the item");
            }
        }
        return Pair.create(screenId, cordinates);
    }

    private boolean findNextAvailableIconSpaceInScreen(
            LauncherAppState app, ArrayList<ItemInfo> occupiedPos,
            int[] xy, int spanX, int spanY) {
        InvariantDeviceProfile profile = app.getInvariantDeviceProfile();

        GridOccupancy occupied = new GridOccupancy(profile.numColumns, profile.numRows);
        if (occupiedPos != null) {
            for (ItemInfo r : occupiedPos) {
                occupied.markCells(r, true);
            }
        }
        return occupied.findVacantCell(xy, spanX, spanY);
    }

    protected void filterAddedItemsFinal(LauncherAppState app, BgDataModel dataModel, AllAppsList apps,
                                         ArrayList<Long> workspaceScreens, List<ItemInfo> filteredItems,
                                         ArrayList<Long> addedWorkspaceScreensFinal, ArrayList<ItemInfo> addedItemsFinal) {
        for (ItemInfo item : filteredItems) {
            // Find appropriate space for the item.
            Pair<Long, int[]> coords = findSpaceForItem(app, dataModel, workspaceScreens,
                    addedWorkspaceScreensFinal, item.spanX, item.spanY);
            long screenId = coords.first;
            int[] cordinates = coords.second;

            ItemInfo itemInfo;
            if (item instanceof ShortcutInfo || item instanceof FolderInfo ||
                    item instanceof LauncherAppWidgetInfo) {
                itemInfo = item;
            } else {
                throw new RuntimeException("Unexpected info type");
            }

            // Add the shortcut to the db
            getModelWriter().addItemToDatabase(itemInfo,
                    LauncherSettings.Favorites.CONTAINER_DESKTOP, screenId,
                    cordinates[0], cordinates[1]);

            // Save the ShortcutInfo for binding in the workspace
            addedItemsFinal.add(itemInfo);
        }
    }

    protected void bindAddedItemsFinal(ArrayList<Long> addedWorkspaceScreensFinal,
                                       List<ItemInfo> addedItemsFinal) {
        if (!addedItemsFinal.isEmpty()) {
            scheduleCallbackTask(new CallbackTask() {
                @Override
                public void execute(Callbacks callbacks) {
                    final ArrayList<ItemInfo> addAnimated = new ArrayList<>();
                    final ArrayList<ItemInfo> addNotAnimated = new ArrayList<>();
                    if (!addedItemsFinal.isEmpty()) {
                        ItemInfo info = addedItemsFinal.get(addedItemsFinal.size() - 1);
                        long lastScreenId = info.screenId;
                        for (ItemInfo i : addedItemsFinal) {
                            if (i.screenId == lastScreenId) {
                                addAnimated.add(i);
                            } else {
                                addNotAnimated.add(i);
                            }
                        }
                    }
                    callbacks.bindAppsAdded(addedWorkspaceScreensFinal,
                            addNotAnimated, addAnimated);
                    mAllAppsList.clearNoPositionList();
                }
            });
        }
    }

    protected void updateScreens(Context context, ArrayList<Long> workspaceScreens) {
        LauncherModel.updateWorkspaceScreenOrder(context, workspaceScreens);
    }

    public void setForceExecute(boolean forceExecute) {
        this.mForceExecute = forceExecute;
    }
}
