/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.launcher3.ui.widget;

import static com.android.launcher3.WorkspaceLayoutManager.FIRST_SCREEN_ID;
import static com.android.launcher3.model.data.LauncherAppWidgetInfo.FLAG_ID_NOT_VALID;
import static com.android.launcher3.model.data.LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY;
import static com.android.launcher3.model.data.LauncherAppWidgetInfo.FLAG_RESTORE_STARTED;
import static com.android.launcher3.provider.LauncherDbUtils.itemIdMatch;
import static com.android.launcher3.util.Executors.MODEL_EXECUTOR;
import static com.android.launcher3.util.TestUtil.getOnUiThread;
import static com.android.launcher3.util.Wait.atMost;
import static com.android.launcher3.util.WidgetUtils.createWidgetInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageInstaller.SessionParams;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.RemoteViews;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.celllayout.FavoriteItemsTransaction;
import com.android.launcher3.model.data.LauncherAppWidgetInfo;
import com.android.launcher3.pm.InstallSessionHelper;
import com.android.launcher3.ui.TestViewHelpers;
import com.android.launcher3.util.BaseLauncherActivityTest;
import com.android.launcher3.util.rule.ShellCommandRule;
import com.android.launcher3.widget.LauncherAppWidgetHostView;
import com.android.launcher3.widget.LauncherAppWidgetProviderInfo;
import com.android.launcher3.widget.PendingAppWidgetHostView;
import com.android.launcher3.widget.WidgetManagerHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Tests for bind widget flow.
 *
 * Note running these tests will clear the workspace on the device.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class BindWidgetTest extends BaseLauncherActivityTest<Launcher> {

    @Rule
    public ShellCommandRule mGrantWidgetRule = ShellCommandRule.grantWidgetBind();

    // Objects created during test, which should be cleaned up in the end.
    private Cursor mCursor;
    // App install session id.
    private int mSessionId = -1;

    private LauncherModel mModel;

    @Before
    public void setUp() throws Exception {
        mModel = LauncherAppState.getInstance(targetContext()).getModel();
    }

    @After
    public void tearDown() {
        if (mCursor != null) {
            mCursor.close();
        }

        if (mSessionId > -1) {
            targetContext().getPackageManager().getPackageInstaller().abandonSession(mSessionId);
        }
    }

    @Test
    public void testBindNormalWidget_withConfig() {
        LauncherAppWidgetProviderInfo info = addWidgetToScreen(true, true, i -> { });
        verifyWidgetPresent(info);
    }

    @Test
    public void testBindNormalWidget_withoutConfig() {
        LauncherAppWidgetProviderInfo info = addWidgetToScreen(false, true, i -> { });
        verifyWidgetPresent(info);
    }

    @Test
    public void testUnboundWidget_removed() {
        LauncherAppWidgetProviderInfo info = addWidgetToScreen(false, false,
                item -> item.appWidgetId = -33);

        // Item deleted from db
        mCursor = queryItem();
        assertEquals(0, mCursor.getCount());

        // The view does not exist
        verifyItemEventuallyNull("Widget exists", widgetProvider(info));
    }

    @Test
    public void testPendingWidget_autoRestored() {
        // A non-restored widget with no config screen gets restored automatically.
        // Do not bind the widget
        LauncherAppWidgetProviderInfo info = addWidgetToScreen(false, false,
                item -> item.restoreStatus = FLAG_ID_NOT_VALID);
        verifyWidgetPresent(info);
    }

    @Test
    public void testPendingWidget_withConfigScreen() {
        // A non-restored widget with config screen get bound and shows a 'Click to setup' UI.
        // Do not bind the widget
        LauncherAppWidgetProviderInfo info = addWidgetToScreen(true, false,
                item -> item.restoreStatus = FLAG_ID_NOT_VALID);
        verifyPendingWidgetPresent();

        mCursor = queryItem();
        mCursor.moveToNext();

        // Widget has a valid Id now.
        assertEquals(0, mCursor.getInt(mCursor.getColumnIndex(LauncherSettings.Favorites.RESTORED))
                & FLAG_ID_NOT_VALID);
        assertNotNull(AppWidgetManager.getInstance(targetContext())
                .getAppWidgetInfo(mCursor.getInt(mCursor.getColumnIndex(
                        LauncherSettings.Favorites.APPWIDGET_ID))));

        // send OPTION_APPWIDGET_RESTORE_COMPLETED
        int appWidgetId = mCursor.getInt(
                mCursor.getColumnIndex(LauncherSettings.Favorites.APPWIDGET_ID));
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(targetContext());

        Bundle b = new Bundle();
        b.putBoolean(WidgetManagerHelper.WIDGET_OPTION_RESTORE_COMPLETED, true);
        RemoteViews remoteViews = new RemoteViews(
                targetContext().getPackageName(), R.layout.appwidget_not_ready);
        appWidgetManager.updateAppWidgetOptions(appWidgetId, b);
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

        // verify changes are reflected
        waitForLauncherCondition("App widget options did not update",
                l -> appWidgetManager.getAppWidgetOptions(appWidgetId).getBoolean(
                        WidgetManagerHelper.WIDGET_OPTION_RESTORE_COMPLETED));
        executeOnLauncher(l -> l.getAppWidgetHolder().startListening());
        verifyWidgetPresent(info);
        verifyItemEventuallyNull("Pending widget exists", pendingWidgetProvider());
    }

    @Test
    public void testPendingWidget_notRestored_removed() {
        addPendingItemToScreen(getInvalidWidgetInfo(), FLAG_ID_NOT_VALID | FLAG_PROVIDER_NOT_READY);

        verifyItemEventuallyNull("Pending widget exists", pendingWidgetProvider());
        // Item deleted from db
        mCursor = queryItem();
        assertEquals(0, mCursor.getCount());
    }

    @Test
    public void testPendingWidget_notRestored_brokenInstall() {
        // A widget which is was being installed once, even if its not being
        // installed at the moment is not removed.
        addPendingItemToScreen(getInvalidWidgetInfo(),
                FLAG_ID_NOT_VALID | FLAG_RESTORE_STARTED | FLAG_PROVIDER_NOT_READY);
        verifyPendingWidgetPresent();

        // Verify item still exists in db
        mCursor = queryItem();
        assertEquals(1, mCursor.getCount());

        // Widget still has an invalid id.
        mCursor.moveToNext();
        assertEquals(FLAG_ID_NOT_VALID,
                mCursor.getInt(mCursor.getColumnIndex(LauncherSettings.Favorites.RESTORED))
                        & FLAG_ID_NOT_VALID);
    }

    @Test
    public void testPendingWidget_notRestored_activeInstall() throws Exception {
        // A widget which is being installed is not removed
        LauncherAppWidgetInfo item = getInvalidWidgetInfo();

        // Create an active installer session
        SessionParams params = new SessionParams(SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(item.providerName.getPackageName());
        PackageInstaller installer = targetContext().getPackageManager().getPackageInstaller();
        mSessionId = installer.createSession(params);

        addPendingItemToScreen(item, FLAG_ID_NOT_VALID | FLAG_PROVIDER_NOT_READY);
        verifyPendingWidgetPresent();

        // Verify item still exists in db
        mCursor = queryItem();
        assertEquals(1, mCursor.getCount());

        // Widget still has an invalid id.
        mCursor.moveToNext();
        assertEquals(FLAG_ID_NOT_VALID,
                mCursor.getInt(mCursor.getColumnIndex(LauncherSettings.Favorites.RESTORED))
                        & FLAG_ID_NOT_VALID);
    }

    private void verifyWidgetPresent(LauncherAppWidgetProviderInfo info) {
        getOnceNotNull("Widget is not present", widgetProvider(info));
    }

    private void verifyPendingWidgetPresent() {
        getOnceNotNull("Widget is not present", pendingWidgetProvider());
    }

    private Function<Launcher, Object> pendingWidgetProvider() {
        return l -> l.getWorkspace().getFirstMatch(
                (item, view) -> view instanceof PendingAppWidgetHostView);
    }

    private Function<Launcher, Object> widgetProvider(LauncherAppWidgetProviderInfo info) {
        return l -> l.getWorkspace().getFirstMatch((item, view) ->
                view instanceof LauncherAppWidgetHostView
                        && TextUtils.equals(info.label, view.getContentDescription()));
    }

    private void verifyItemEventuallyNull(String message, Function<Launcher, Object> provider) {
        atMost(message, () -> getFromLauncher(provider) == null);
    }

    private void addPendingItemToScreen(LauncherAppWidgetInfo item, int restoreStatus) {
        item.restoreStatus = restoreStatus;
        item.screenId = FIRST_SCREEN_ID;
        new FavoriteItemsTransaction(targetContext()).addItem(() -> item).commit();
        loadLauncherSync();
    }

    private LauncherAppWidgetProviderInfo addWidgetToScreen(boolean hasConfigureScreen,
            boolean bindWidget, Consumer<LauncherAppWidgetInfo> itemOverride) {
        LauncherAppWidgetProviderInfo info = TestViewHelpers.findWidgetProvider(hasConfigureScreen);
        new FavoriteItemsTransaction(targetContext())
                .addItem(() -> {
                    LauncherAppWidgetInfo item =
                            createWidgetInfo(info, targetContext(), bindWidget);
                    item.screenId = FIRST_SCREEN_ID;
                    itemOverride.accept(item);
                    return item;
                }).commit();
        loadLauncherSync();
        return info;
    }

    /**
     * Returns a LauncherAppWidgetInfo with package name which is not present on the device
     */
    private LauncherAppWidgetInfo getInvalidWidgetInfo() {
        String invalidPackage = "com.invalidpackage";
        int count = 0;
        String pkg = invalidPackage;

        Set<String> activePackage = getOnUiThread(() -> {
            Set<String> packages = new HashSet<>();
            InstallSessionHelper.INSTANCE.get(targetContext()).getActiveSessions()
                    .keySet().forEach(packageUserKey -> packages.add(packageUserKey.mPackageName));
            return packages;
        });
        while (true) {
            try {
                targetContext().getPackageManager().getPackageInfo(
                        pkg, PackageManager.GET_UNINSTALLED_PACKAGES);
            } catch (Exception e) {
                if (!activePackage.contains(pkg)) {
                    break;
                }
            }
            pkg = invalidPackage + count;
            count++;
        }
        LauncherAppWidgetInfo item = new LauncherAppWidgetInfo(10,
                new ComponentName(pkg, "com.test.widgetprovider"));
        item.spanX = 2;
        item.spanY = 2;
        item.minSpanX = 2;
        item.minSpanY = 2;
        item.cellX = 0;
        item.cellY = 1;
        item.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
        return item;
    }

    private Cursor queryItem() {
        try {
            return MODEL_EXECUTOR.submit(() ->
                mModel.getModelDbController().query(
                        null, itemIdMatch(0), null, null)).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
