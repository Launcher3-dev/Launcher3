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

import static com.android.launcher3.Flags.enableSmartspaceRemovalToggle;
import static com.android.launcher3.GridType.GRID_TYPE_NON_ONE_GRID;
import static com.android.launcher3.GridType.GRID_TYPE_ONE_GRID;
import static com.android.launcher3.InvariantDeviceProfile.TYPE_TABLET;
import static com.android.launcher3.LauncherSettings.Favorites.TABLE_NAME;
import static com.android.launcher3.LauncherSettings.Favorites.TMP_TABLE;
import static com.android.launcher3.Utilities.SHOULD_SHOW_FIRST_PAGE_WIDGET;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_ROW_SHIFT_GRID_MIGRATION;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_ROW_SHIFT_ONE_GRID_MIGRATION;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_STANDARD_GRID_MIGRATION;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_STANDARD_ONE_GRID_MIGRATION;
import static com.android.launcher3.model.LoaderTask.SMARTSPACE_ON_HOME_SCREEN;
import static com.android.launcher3.provider.LauncherDbUtils.copyTable;
import static com.android.launcher3.provider.LauncherDbUtils.dropTable;
import static com.android.launcher3.provider.LauncherDbUtils.shiftWorkspaceByXCells;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.android.launcher3.Flags;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.Utilities;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.logging.StatsLogManager;
import com.android.launcher3.provider.LauncherDbUtils.SQLiteTransaction;
import com.android.launcher3.util.GridOccupancy;
import com.android.launcher3.util.IntArray;
import com.android.launcher3.widget.LauncherAppWidgetProviderInfo;
import com.android.launcher3.widget.WidgetManagerHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class takes care of shrinking the workspace (by maximum of one row and one column), as a
 * result of restoring from a larger device or device density change.
 */
public class GridSizeMigrationDBController {

    private static final String TAG = "GridSizeMigrationDBController";
    private static final boolean DEBUG = true;

    private GridSizeMigrationDBController() {
        // Util class should not be instantiated
    }

    /**
     * Check given a new IDP, if migration is necessary.
     */
    public static boolean needsToMigrate(Context context, InvariantDeviceProfile idp) {
        return needsToMigrate(new DeviceGridState(context), new DeviceGridState(idp));
    }

    static boolean needsToMigrate(
            DeviceGridState srcDeviceState, DeviceGridState destDeviceState) {
        boolean needsToMigrate = !destDeviceState.isCompatible(srcDeviceState);
        if (needsToMigrate) {
            Log.i(TAG, "Migration is needed. destDeviceState: " + destDeviceState
                    + ", srcDeviceState: " + srcDeviceState);
        } else {
            Log.i(TAG, "Migration is not needed. destDeviceState: " + destDeviceState
                    + ", srcDeviceState: " + srcDeviceState);
        }
        return needsToMigrate;
    }

    /**
     * @return all the workspace and hotseat entries in the db.
     */
    @VisibleForTesting
    public static List<DbEntry> readAllEntries(SQLiteDatabase db, String tableName,
            Context context) {
        DbReader dbReader = new DbReader(db, tableName, context);
        List<DbEntry> result = dbReader.loadAllWorkspaceEntries();
        result.addAll(dbReader.loadHotseatEntries());
        return result;
    }

    /**
     * When migrating the grid, we copy the table
     * {@link LauncherSettings.Favorites#TABLE_NAME} from {@code source} into
     * {@link LauncherSettings.Favorites#TMP_TABLE}, run the grid size migration algorithm
     * to migrate the later to the former, and load the workspace from the default
     * {@link LauncherSettings.Favorites#TABLE_NAME}.
     *
     * @return false if the migration failed.
     */
    public static boolean migrateGridIfNeeded(
            @NonNull Context context,
            @NonNull DeviceGridState srcDeviceState,
            @NonNull DeviceGridState destDeviceState,
            @NonNull DatabaseHelper target,
            @NonNull SQLiteDatabase source,
            boolean isDestNewDb,
            ModelDelegate modelDelegate) {

        if (!needsToMigrate(srcDeviceState, destDeviceState)) {
            return true;
        }

        StatsLogManager statsLogManager = StatsLogManager.newInstance(context);

        boolean shouldMigrateToStrictlyTallerGrid = (Flags.oneGridSpecs() || isDestNewDb)
                && srcDeviceState.getColumns().equals(destDeviceState.getColumns())
                && srcDeviceState.getRows() < destDeviceState.getRows();
        if (shouldMigrateToStrictlyTallerGrid) {
            copyTable(source, TABLE_NAME, target.getWritableDatabase(), TABLE_NAME, context);
        } else {
            copyTable(source, TABLE_NAME, target.getWritableDatabase(), TMP_TABLE, context);
        }

        long migrationStartTime = System.currentTimeMillis();
        try (SQLiteTransaction t = new SQLiteTransaction(target.getWritableDatabase())) {

            if (shouldMigrateToStrictlyTallerGrid) {
                // We want to add the extra row(s) to the top of the screen, so we shift the grid
                // down.
                if (Flags.oneGridSpecs()) {
                    shiftWorkspaceByXCells(
                            target.getWritableDatabase(),
                            (destDeviceState.getRows() - srcDeviceState.getRows()),
                            TABLE_NAME);
                }

                // Save current configuration, so that the migration does not run again.
                destDeviceState.writeToPrefs(context);
                t.commit();
                if (isOneGridMigration(srcDeviceState, destDeviceState)) {
                    statsLogManager.logger().log(LAUNCHER_ROW_SHIFT_ONE_GRID_MIGRATION);
                }
                statsLogManager.logger().log(LAUNCHER_ROW_SHIFT_GRID_MIGRATION);
                return true;
            }

            DbReader srcReader = new DbReader(t.getDb(), TMP_TABLE, context);
            DbReader destReader = new DbReader(t.getDb(), TABLE_NAME, context);

            Point targetSize = new Point(destDeviceState.getColumns(), destDeviceState.getRows());
            migrate(target, srcReader, destReader, srcDeviceState.getNumHotseat(),
                    destDeviceState.getNumHotseat(), targetSize, srcDeviceState, destDeviceState);
            dropTable(t.getDb(), TMP_TABLE);
            t.commit();
            if (isOneGridMigration(srcDeviceState, destDeviceState)) {
                statsLogManager.logger().log(LAUNCHER_STANDARD_ONE_GRID_MIGRATION);
            }
            statsLogManager.logger().log(LAUNCHER_STANDARD_GRID_MIGRATION);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error during grid migration", e);
            return false;
        } finally {
            Log.v(TAG, "Workspace migration completed in "
                    + (System.currentTimeMillis() - migrationStartTime));

            // Save current configuration, so that the migration does not run again.
            destDeviceState.writeToPrefs(context);
            // Notify if we've migrated successfully
            modelDelegate.gridMigrationComplete(srcDeviceState, destDeviceState);
        }
    }

    public static boolean migrate(
            @NonNull DatabaseHelper helper,
            @NonNull final DbReader srcReader, @NonNull final DbReader destReader,
            final int srcHotseatSize, final int destHotseatSize, @NonNull final Point targetSize,
            @NonNull final DeviceGridState srcDeviceState,
            @NonNull final DeviceGridState destDeviceState) {

        final List<DbEntry> srcHotseatItems = srcReader.loadHotseatEntries();
        final List<DbEntry> srcWorkspaceItems = srcReader.loadAllWorkspaceEntries();
        final List<DbEntry> dstHotseatItems = destReader.loadHotseatEntries();
        // We want to filter out the hotseat items that are placed beyond the size of the source
        // grid as we always want to keep those extra items from the destination grid.
        List<DbEntry> filteredDstHotseatItems = dstHotseatItems;
        if (srcHotseatSize < destHotseatSize) {
            filteredDstHotseatItems = filteredDstHotseatItems.stream()
                    .filter(entry -> entry.screenId < srcHotseatSize)
                    .collect(Collectors.toList());
        }
        final List<DbEntry> dstWorkspaceItems = destReader.loadAllWorkspaceEntries();
        final List<DbEntry> hotseatToBeAdded = new ArrayList<>(1);
        final List<DbEntry> workspaceToBeAdded = new ArrayList<>(1);
        final IntArray toBeRemoved = new IntArray();

        calcDiff(srcHotseatItems, filteredDstHotseatItems, hotseatToBeAdded, toBeRemoved);
        calcDiff(srcWorkspaceItems, dstWorkspaceItems, workspaceToBeAdded, toBeRemoved);

        final int trgX = targetSize.x;
        final int trgY = targetSize.y;

        if (DEBUG) {
            Log.d(TAG, "Start migration:"
                    + "\n Source Device:"
                    + srcWorkspaceItems.stream().map(DbEntry::toString).collect(
                    Collectors.joining(",\n", "[", "]"))
                    + "\n Target Device:"
                    + dstWorkspaceItems.stream().map(DbEntry::toString).collect(
                    Collectors.joining(",\n", "[", "]"))
                    + "\n Removing Items:"
                    + dstWorkspaceItems.stream().filter(entry ->
                    toBeRemoved.contains(entry.id)).map(DbEntry::toString).collect(
                    Collectors.joining(",\n", "[", "]"))
                    + "\n Adding Workspace Items:"
                    + workspaceToBeAdded.stream().map(DbEntry::toString).collect(
                    Collectors.joining(",\n", "[", "]"))
                    + "\n Adding Hotseat Items:"
                    + hotseatToBeAdded.stream().map(DbEntry::toString).collect(
                    Collectors.joining(",\n", "[", "]"))
            );
        }
        if (!toBeRemoved.isEmpty()) {
            removeEntryFromDb(destReader.mDb, destReader.mTableName, toBeRemoved);
        }
        if (hotseatToBeAdded.isEmpty() && workspaceToBeAdded.isEmpty()) {
            return false;
        }

        // Sort the items by the reading order.
        Collections.sort(hotseatToBeAdded);
        Collections.sort(workspaceToBeAdded);

        List<DbEntry> remainingDstHotseatItems = destReader.loadHotseatEntries();
        List<DbEntry> remainingDstWorkspaceItems = destReader.loadAllWorkspaceEntries();
        List<Integer> idsInUse = remainingDstHotseatItems.stream()
                .map(entry -> entry.id)
                .collect(Collectors.toList());
        idsInUse.addAll(remainingDstWorkspaceItems.stream()
                .map(entry -> entry.id)
                .collect(Collectors.toList()));


        // Migrate hotseat
        solveHotseatPlacement(helper, destHotseatSize,
                srcReader, destReader, remainingDstHotseatItems, hotseatToBeAdded, idsInUse);

        // Migrate workspace.
        // First we create a collection of the screens
        List<Integer> screens = new ArrayList<>();
        for (int screenId = 0; screenId <= destReader.mLastScreenId; screenId++) {
            screens.add(screenId);
        }

        // Then we place the items on the screens
        for (int screenId : screens) {
            if (DEBUG) {
                Log.d(TAG, "Migrating " + screenId);
            }
            solveGridPlacement(helper, srcReader,
                    destReader, screenId, trgX, trgY, workspaceToBeAdded, idsInUse);
            if (workspaceToBeAdded.isEmpty()) {
                break;
            }
        }

        // In case the new grid is smaller, there might be some leftover items that don't fit on
        // any of the screens, in this case we add them to new screens until all of them are placed.
        int screenId = destReader.mLastScreenId + 1;
        while (!workspaceToBeAdded.isEmpty()) {
            solveGridPlacement(helper, srcReader, destReader, screenId, trgX, trgY,
                    workspaceToBeAdded,
                    srcWorkspaceItems.stream().map(entry -> entry.id).collect(Collectors.toList()));
            screenId++;
        }

        return true;
    }

    protected static boolean isOneGridMigration(DeviceGridState srcDeviceState,
            DeviceGridState destDeviceState) {
        return srcDeviceState.getDeviceType() != TYPE_TABLET
                && srcDeviceState.getGridType() == GRID_TYPE_NON_ONE_GRID
                && destDeviceState.getGridType() == GRID_TYPE_ONE_GRID;
    }
    /**
     * Calculate the differences between {@code src} (denoted by A) and {@code dest}
     * (denoted by B).
     * All DbEntry in A - B will be added to {@code toBeAdded}
     * All DbEntry.id in B - A will be added to {@code toBeRemoved}
     */
    private static void calcDiff(@NonNull final List<DbEntry> src,
            @NonNull final List<DbEntry> dest, @NonNull final List<DbEntry> toBeAdded,
            @NonNull final IntArray toBeRemoved) {
        HashMap<DbEntry, Integer> entryCountDiff = new HashMap<>();
        src.forEach(entry ->
                entryCountDiff.put(entry, entryCountDiff.getOrDefault(entry, 0) + 1));
        dest.forEach(entry ->
                entryCountDiff.put(entry, entryCountDiff.getOrDefault(entry, 0) - 1));

        src.forEach(entry -> {
            if (entryCountDiff.get(entry) > 0) {
                toBeAdded.add(entry);
                entryCountDiff.put(entry, entryCountDiff.get(entry) - 1);
            }
        });

        dest.forEach(entry -> {
            if (entryCountDiff.get(entry) < 0) {
                toBeRemoved.add(entry.id);
                if (entry.itemType == LauncherSettings.Favorites.ITEM_TYPE_FOLDER) {
                    entry.mFolderItems.values().forEach(ids -> ids.forEach(toBeRemoved::add));
                }
                entryCountDiff.put(entry, entryCountDiff.get(entry) + 1);
            }
        });
    }

    static void insertEntryInDb(DatabaseHelper helper, DbEntry entry,
            String srcTableName, String destTableName, List<Integer> idsInUse) {
        int id = copyEntryAndUpdate(helper, entry, srcTableName, destTableName, idsInUse);
        if (entry.itemType == LauncherSettings.Favorites.ITEM_TYPE_FOLDER
                || entry.itemType == LauncherSettings.Favorites.ITEM_TYPE_APP_PAIR) {
            for (Set<Integer> itemIds : entry.mFolderItems.values()) {
                for (int itemId : itemIds) {
                    copyEntryAndUpdate(helper, itemId, id, srcTableName, destTableName, idsInUse);
                }
            }
        }
    }

    private static int copyEntryAndUpdate(DatabaseHelper helper,
            DbEntry entry, String srcTableName, String destTableName, List<Integer> idsInUse) {
        return copyEntryAndUpdate(
                helper, entry, -1, -1, srcTableName, destTableName, idsInUse);
    }

    private static int copyEntryAndUpdate(DatabaseHelper helper, int id,
            int folderId, String srcTableName, String destTableName, List<Integer> idsInUse) {
        return copyEntryAndUpdate(
                helper, null, id, folderId, srcTableName, destTableName, idsInUse);
    }

    private static int copyEntryAndUpdate(DatabaseHelper helper, DbEntry entry, int id,
            int folderId, String srcTableName, String destTableName, List<Integer> idsInUse) {
        int newId = -1;
        Cursor c = helper.getWritableDatabase().query(srcTableName, null,
                LauncherSettings.Favorites._ID + " = '" + (entry != null ? entry.id : id) + "'",
                null, null, null, null);
        while (c.moveToNext()) {
            ContentValues values = new ContentValues();
            DatabaseUtils.cursorRowToContentValues(c, values);
            if (entry != null) {
                entry.updateContentValues(values);
            } else {
                values.put(LauncherSettings.Favorites.CONTAINER, folderId);
            }
            do {
                newId = helper.generateNewItemId();
            } while (idsInUse.contains(newId));
            values.put(LauncherSettings.Favorites._ID, newId);
            helper.getWritableDatabase().insert(destTableName, null, values);
        }
        c.close();
        return newId;
    }

    static void removeEntryFromDb(SQLiteDatabase db, String tableName, IntArray entryIds) {
        db.delete(tableName,
                Utilities.createDbSelectionQuery(LauncherSettings.Favorites._ID, entryIds), null);
    }

    private static void solveGridPlacement(@NonNull final DatabaseHelper helper,
            @NonNull final DbReader srcReader, @NonNull final DbReader destReader,
            final int screenId, final int trgX, final int trgY,
            @NonNull final List<DbEntry> sortedItemsToPlace, List<Integer> idsInUse) {
        final GridOccupancy occupied = new GridOccupancy(trgX, trgY);
        final Point trg = new Point(trgX, trgY);
        final Point next = new Point(0, screenId == 0
                && (FeatureFlags.QSB_ON_FIRST_SCREEN
                && (!enableSmartspaceRemovalToggle() || LauncherPrefs.getPrefs(destReader.mContext)
                .getBoolean(SMARTSPACE_ON_HOME_SCREEN, true))
                && !SHOULD_SHOW_FIRST_PAGE_WIDGET)
                ? 1 /* smartspace */ : 0);
        List<DbEntry> existedEntries = destReader.mWorkspaceEntriesByScreenId.get(screenId);
        if (existedEntries != null) {
            for (DbEntry entry : existedEntries) {
                occupied.markCells(entry, true);
            }
        }
        Iterator<DbEntry> iterator = sortedItemsToPlace.iterator();
        while (iterator.hasNext()) {
            final DbEntry entry = iterator.next();
            if (entry.minSpanX > trgX || entry.minSpanY > trgY) {
                iterator.remove();
                continue;
            }
            if (findPlacementForEntry(entry, next, trg, occupied, screenId)) {
                insertEntryInDb(
                        helper, entry, srcReader.mTableName, destReader.mTableName, idsInUse);
                iterator.remove();
            }
        }
    }

    /**
     * Search for the next possible placement of an icon. (mNextStartX, mNextStartY) serves as
     * a memoization of last placement, we can start our search for next placement from there
     * to speed up the search.
     */
    private static boolean findPlacementForEntry(@NonNull final DbEntry entry,
            @NonNull final Point next, @NonNull final Point trg,
            @NonNull final GridOccupancy occupied, final int screenId) {
        for (int y = next.y; y < trg.y; y++) {
            for (int x = next.x; x < trg.x; x++) {
                boolean fits = occupied.isRegionVacant(x, y, entry.spanX, entry.spanY);
                boolean minFits = occupied.isRegionVacant(x, y, entry.minSpanX,
                        entry.minSpanY);
                if (minFits) {
                    entry.spanX = entry.minSpanX;
                    entry.spanY = entry.minSpanY;
                }
                if (fits || minFits) {
                    entry.screenId = screenId;
                    entry.cellX = x;
                    entry.cellY = y;
                    occupied.markCells(entry, true);
                    next.set(x + entry.spanX, y);
                    return true;
                }
            }
            next.set(0, next.y);
        }
        return false;
    }

    private static void solveHotseatPlacement(
            @NonNull final DatabaseHelper helper,
            final int dstHotseatSize,
            @NonNull final DbReader srcReader, @NonNull final DbReader destReader,
            @NonNull final List<DbEntry> placedHotseatItems,
            @NonNull final List<DbEntry> itemsToPlace, List<Integer> idsInUse) {

        final boolean[] occupied = new boolean[dstHotseatSize];
        for (DbEntry entry : placedHotseatItems) {
            occupied[entry.screenId] = true;
        }

        for (int i = 0; i < occupied.length; i++) {
            if (!occupied[i] && !itemsToPlace.isEmpty()) {
                DbEntry entry = itemsToPlace.remove(0);
                entry.screenId = i;
                // These values does not affect the item position, but we should set them
                // to something other than -1.
                entry.cellX = i;
                entry.cellY = 0;
                insertEntryInDb(
                        helper, entry, srcReader.mTableName, destReader.mTableName, idsInUse);
                occupied[entry.screenId] = true;
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public static class DbReader {

        final SQLiteDatabase mDb;
        final String mTableName;
        final Context mContext;
        int mLastScreenId = -1;

        Map<Integer, List<DbEntry>> mWorkspaceEntriesByScreenId =
                new ArrayMap<>();

        public DbReader(SQLiteDatabase db, String tableName, Context context) {
            mDb = db;
            mTableName = tableName;
            mContext = context;
        }

        protected List<DbEntry> loadHotseatEntries() {
            final List<DbEntry> hotseatEntries = new ArrayList<>();
            Cursor c = queryWorkspace(
                    new String[]{
                            LauncherSettings.Favorites._ID,                  // 0
                            LauncherSettings.Favorites.ITEM_TYPE,            // 1
                            LauncherSettings.Favorites.INTENT,               // 2
                            LauncherSettings.Favorites.SCREEN},              // 3
                    LauncherSettings.Favorites.CONTAINER + " = "
                            + LauncherSettings.Favorites.CONTAINER_HOTSEAT);

            final int indexId = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
            final int indexItemType = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
            final int indexIntent = c.getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);
            final int indexScreen = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);

            IntArray entriesToRemove = new IntArray();
            while (c.moveToNext()) {
                DbEntry entry = new DbEntry();
                entry.id = c.getInt(indexId);
                entry.itemType = c.getInt(indexItemType);
                entry.screenId = c.getInt(indexScreen);

                try {
                    // calculate weight
                    switch (entry.itemType) {
                        case LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT:
                        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION: {
                            entry.mIntent = c.getString(indexIntent);
                            break;
                        }
                        case LauncherSettings.Favorites.ITEM_TYPE_FOLDER: {
                            int total = getFolderItemsCount(entry);
                            if (total == 0) {
                                throw new Exception("Folder is empty");
                            }
                            break;
                        }
                        case LauncherSettings.Favorites.ITEM_TYPE_APP_PAIR: {
                            int total = getFolderItemsCount(entry);
                            if (total != 2) {
                                throw new Exception("App pair contains fewer or more than 2 items");
                            }
                            break;
                        }
                        default:
                            throw new Exception("Invalid item type");
                    }
                } catch (Exception e) {
                    if (DEBUG) {
                        Log.d(TAG, "Removing item " + entry.id, e);
                    }
                    entriesToRemove.add(entry.id);
                    continue;
                }
                hotseatEntries.add(entry);
            }
            removeEntryFromDb(mDb, mTableName, entriesToRemove);
            c.close();
            return hotseatEntries;
        }

        protected List<DbEntry> loadAllWorkspaceEntries() {
            mWorkspaceEntriesByScreenId.clear();
            final List<DbEntry> workspaceEntries = new ArrayList<>();
            Cursor c = queryWorkspace(
                    new String[]{
                            LauncherSettings.Favorites._ID,                  // 0
                            LauncherSettings.Favorites.ITEM_TYPE,            // 1
                            LauncherSettings.Favorites.SCREEN,               // 2
                            LauncherSettings.Favorites.CELLX,                // 3
                            LauncherSettings.Favorites.CELLY,                // 4
                            LauncherSettings.Favorites.SPANX,                // 5
                            LauncherSettings.Favorites.SPANY,                // 6
                            LauncherSettings.Favorites.INTENT,               // 7
                            LauncherSettings.Favorites.APPWIDGET_PROVIDER,   // 8
                            LauncherSettings.Favorites.APPWIDGET_ID},        // 9
                    LauncherSettings.Favorites.CONTAINER + " = "
                            + LauncherSettings.Favorites.CONTAINER_DESKTOP);
            final int indexId = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
            final int indexItemType = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
            final int indexScreen = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
            final int indexCellX = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
            final int indexCellY = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
            final int indexSpanX = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANX);
            final int indexSpanY = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANY);
            final int indexIntent = c.getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);
            final int indexAppWidgetProvider = c.getColumnIndexOrThrow(
                    LauncherSettings.Favorites.APPWIDGET_PROVIDER);
            final int indexAppWidgetId = c.getColumnIndexOrThrow(
                    LauncherSettings.Favorites.APPWIDGET_ID);

            IntArray entriesToRemove = new IntArray();
            WidgetManagerHelper widgetManagerHelper = new WidgetManagerHelper(mContext);
            while (c.moveToNext()) {
                DbEntry entry = new DbEntry();
                entry.id = c.getInt(indexId);
                entry.itemType = c.getInt(indexItemType);
                entry.screenId = c.getInt(indexScreen);
                mLastScreenId = Math.max(mLastScreenId, entry.screenId);
                entry.cellX = c.getInt(indexCellX);
                entry.cellY = c.getInt(indexCellY);
                entry.spanX = c.getInt(indexSpanX);
                entry.spanY = c.getInt(indexSpanY);

                try {
                    // calculate weight
                    switch (entry.itemType) {
                        case LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT:
                        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION: {
                            entry.mIntent = c.getString(indexIntent);
                            break;
                        }
                        case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET: {
                            entry.mProvider = c.getString(indexAppWidgetProvider);
                            entry.appWidgetId = c.getInt(indexAppWidgetId);
                            ComponentName cn = ComponentName.unflattenFromString(entry.mProvider);

                            LauncherAppWidgetProviderInfo pInfo = widgetManagerHelper
                                    .getLauncherAppWidgetInfo(entry.appWidgetId, cn);
                            Point spans = null;
                            if (pInfo != null) {
                                spans = pInfo.getMinSpans();
                            }
                            if (spans != null) {
                                entry.minSpanX = spans.x > 0 ? spans.x : entry.spanX;
                                entry.minSpanY = spans.y > 0 ? spans.y : entry.spanY;
                            } else {
                                // Assume that the widget be resized down to 2x2
                                entry.minSpanX = entry.minSpanY = 2;
                            }

                            break;
                        }
                        case LauncherSettings.Favorites.ITEM_TYPE_FOLDER: {
                            int total = getFolderItemsCount(entry);
                            if (total == 0) {
                                throw new Exception("Folder is empty");
                            }
                            break;
                        }
                        case LauncherSettings.Favorites.ITEM_TYPE_APP_PAIR: {
                            int total = getFolderItemsCount(entry);
                            if (total != 2) {
                                throw new Exception("App pair contains fewer or more than 2 items");
                            }
                            break;
                        }
                        default:
                            throw new Exception("Invalid item type");
                    }
                } catch (Exception e) {
                    if (DEBUG) {
                        Log.d(TAG, "Removing item " + entry.id, e);
                    }
                    entriesToRemove.add(entry.id);
                    continue;
                }
                workspaceEntries.add(entry);
                if (!mWorkspaceEntriesByScreenId.containsKey(entry.screenId)) {
                    mWorkspaceEntriesByScreenId.put(entry.screenId, new ArrayList<>());
                }
                mWorkspaceEntriesByScreenId.get(entry.screenId).add(entry);
            }
            removeEntryFromDb(mDb, mTableName, entriesToRemove);
            c.close();
            return workspaceEntries;
        }

        private int getFolderItemsCount(DbEntry entry) {
            Cursor c = queryWorkspace(
                    new String[]{LauncherSettings.Favorites._ID, LauncherSettings.Favorites.INTENT},
                    LauncherSettings.Favorites.CONTAINER + " = " + entry.id);

            int total = 0;
            while (c.moveToNext()) {
                try {
                    int id = c.getInt(0);
                    String intent = c.getString(1);
                    total++;
                    if (!entry.mFolderItems.containsKey(intent)) {
                        entry.mFolderItems.put(intent, new HashSet<>());
                    }
                    entry.mFolderItems.get(intent).add(id);
                } catch (Exception e) {
                    removeEntryFromDb(mDb, mTableName, IntArray.wrap(c.getInt(0)));
                }
            }
            c.close();
            return total;
        }

        private Cursor queryWorkspace(String[] columns, String where) {
            return mDb.query(mTableName, columns, where, null, null, null, null);
        }
    }
}
