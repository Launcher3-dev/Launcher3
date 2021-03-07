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
package com.android.launcher3.logging;

import android.util.ArrayMap;
import android.util.SparseArray;
import android.view.View;

import com.android.launcher3.ButtonDropTarget;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.userevent.LauncherLogExtensions.TargetExtension;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.userevent.nano.LauncherLogProto.Action;
import com.android.launcher3.userevent.nano.LauncherLogProto.ItemType;
import com.android.launcher3.userevent.nano.LauncherLogProto.LauncherEvent;
import com.android.launcher3.userevent.nano.LauncherLogProto.Target;
import com.android.launcher3.util.InstantAppResolver;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Helper methods for logging.
 */
public class LoggerUtils {
    private static final ArrayMap<Class, SparseArray<String>> sNameCache = new ArrayMap<>();
    private static final String UNKNOWN = "UNKNOWN";
    private static final int DEFAULT_PREDICTED_RANK = 10000;
    private static final String DELIMITER_DOT = "\\.";

    public static String getFieldName(int value, Class c) {
        SparseArray<String> cache;
        synchronized (sNameCache) {
            cache = sNameCache.get(c);
            if (cache == null) {
                cache = new SparseArray<>();
                for (Field f : c.getDeclaredFields()) {
                    if (f.getType() == int.class && Modifier.isStatic(f.getModifiers())) {
                        try {
                            f.setAccessible(true);
                            cache.put(f.getInt(null), f.getName());
                        } catch (IllegalAccessException e) {
                            // Ignore
                        }
                    }
                }
                sNameCache.put(c, cache);
            }
        }
        String result = cache.get(value);
        return result != null ? result : UNKNOWN;
    }

    public static Target.Builder newItemTarget(Target.Type type) {
        Target.Builder builder = new Target.Builder();
        builder.setType(type);
        return builder;
    }

    public static Target newItemTarget(int itemType) {
        Target.Builder t = newItemTarget(Target.Type.ITEM);
        t.setItemType(ItemType.forNumber(itemType));
        return t.build();
    }

    public static Target newItemTarget(View v, InstantAppResolver instantAppResolver) {
        return (v != null) && (v.getTag() instanceof ItemInfo)
                ? newItemTarget((ItemInfo) v.getTag(), instantAppResolver)
                : newTargetBuilder(Target.Type.ITEM).build();
    }

    public static Target newItemTarget(ItemInfo info, InstantAppResolver instantAppResolver) {
        Target.Builder t = newTargetBuilder(Target.Type.ITEM);
        switch (info.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                t.setItemType((instantAppResolver != null && info instanceof AppInfo
                        && instantAppResolver.isInstantApp(((AppInfo) info)))
                        ? ItemType.WEB_APP
                        : ItemType.APP_ICON);
                t.setPredictedRank(DEFAULT_PREDICTED_RANK);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                t.setItemType(ItemType.SHORTCUT);
                t.setPredictedRank(DEFAULT_PREDICTED_RANK);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                t.setItemType(ItemType.FOLDER_ICON);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                t.setItemType(ItemType.WIDGET);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT:
                t.setItemType(ItemType.DEEPSHORTCUT);
                t.setPredictedRank(DEFAULT_PREDICTED_RANK);
                break;
        }
        return t.build();
    }

    public static Target newDropTarget(View v) {
        if (!(v instanceof ButtonDropTarget)) {
            return newTargetBuilder(Target.Type.CONTAINER).build();
        }
        if (v instanceof ButtonDropTarget) {
            return ((ButtonDropTarget) v).getDropTargetForLogging();
        }
        return newTargetBuilder(Target.Type.CONTROL).build();
    }

    public static Target newTarget(int targetType, TargetExtension extension) {
        Target.Builder t = newTargetBuilder(targetType);
        t.setExtension(extension);
        return t.build();
    }

    public static Target.Builder newTargetBuilder(int targetType) {
        Target.Builder builder = new Target.Builder();
        builder.setType(Target.Type.forNumber(targetType));
        return builder;
    }

    public static Target.Builder newTargetBuilder(Target.Type targetType) {
        Target.Builder builder = new Target.Builder();
        builder.setType(targetType);
        return builder;
    }

    public static Target newTarget(int targetType) {
        Target.Builder t = newTargetBuilder(targetType);
        return t.build();
    }

    public static Target newTarget(Target.Type targetType) {
        Target.Builder t = newTargetBuilder(targetType);
        return t.build();
    }

    public static Target newControlTarget(int controlType) {
        Target.Builder t = newTargetBuilder(Target.Type.CONTROL);
        t.setControlType(LauncherLogProto.ControlType.forNumber(controlType));
        return t.build();
    }

    public static Target newContainerTarget(int containerType) {
        Target.Builder t = Target.newBuilder();
        t.setType(Target.Type.CONTAINER);
        t.setContainerType(LauncherLogProto.ContainerType.forNumber(containerType));
        return t.build();
    }

    public static Target newContainerTarget(int containerType, int pageIndex) {
        Target.Builder builder = Target.newBuilder();
        builder.setType(Target.Type.CONTAINER);
        builder.setContainerType(LauncherLogProto.ContainerType.forNumber(containerType));
        builder.setPageIndex(pageIndex);
        return builder.build();
    }

    public static Action.Builder newActionBuilder(int type) {
        Action.Builder a = Action.newBuilder();
        a.setType(Action.Type.forNumber(type));
        return a;
    }

    public static Action.Builder newActionBuilder(Action.Type type) {
        Action.Builder a = Action.newBuilder();
        a.setType(type);
        return a;
    }

    public static Action newAction(int type) {
        Action.Builder a = newActionBuilder(type);
        return a.build();
    }

    public static Action newCommandAction(int command) {
        Action.Builder a = newActionBuilder(Action.Type.COMMAND);
        a.setCommand(Action.Command.forNumber(command));
        return a.build();
    }
    public static Action newCommandAction(Action.Command command) {
        Action.Builder a = newActionBuilder(Action.Type.COMMAND);
        a.setCommand(command);
        return a.build();
    }

    public static Action newTouchAction(Action.Touch touch) {
        Action.Builder a = newActionBuilder(Action.Type.TOUCH);
        a.setTouch(touch);
        return a.build();
    }

    public static Action newTouchAction(int touch) {
        Action.Builder a = newActionBuilder(Action.Type.TOUCH);
        a.setTouch(Action.Touch.forNumber(touch));
        return a.build();
    }

    public static LauncherEvent.Builder newLauncherEventBuilder(Action action, Target... srcTargets) {
        LauncherEvent.Builder event = LauncherEvent.newBuilder();
        event.addAllSrcTarget(Arrays.asList(srcTargets));
        event.setAction(action);
        return event;
    }

    public static LauncherEvent newLauncherEvent(Action action, Target... srcTargets) {
        LauncherEvent.Builder event = LauncherEvent.newBuilder();
        event.addAllSrcTarget(Arrays.asList(srcTargets));
        event.setAction(action);
        return event.build();
    }

    /**
     * Creates LauncherEvent using Action and ArrayList of Targets
     */
    public static LauncherEvent newLauncherEvent(Action action, ArrayList<Target> targets) {
        Target[] targetsArray = new Target[targets.size()];
        targets.toArray(targetsArray);
        return newLauncherEvent(action, targetsArray);
    }

    /**
     * String conversion for only the helpful parts of {@link Object#toString()} method
     * @param stringToExtract "foo.bar.baz.MyObject@1234"
     * @return "MyObject@1234"
     */
    public static String extractObjectNameAndAddress(String stringToExtract) {
        String[] superStringParts = stringToExtract.split(DELIMITER_DOT);
        if (superStringParts.length == 0) {
            return "";
        }
        return superStringParts[superStringParts.length - 1];
    }
}
