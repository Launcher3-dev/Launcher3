/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.wm.shell.splitscreen
import android.app.ActivityManager
import android.hardware.display.DisplayManager
import android.view.SurfaceControl
import com.android.internal.protolog.ProtoLog
import com.android.wm.shell.common.split.SplitLayout
import com.android.wm.shell.protolog.ShellProtoLogGroup

/**
 * Helper class for managing split-screen functionality across multiple displays.
 */
class SplitMultiDisplayHelper(private val displayManager: DisplayManager) {

    /**
     * A map that stores the [SplitTaskHierarchy] associated with each display ID.
     * The keys are display IDs (integers), and the values are [SplitTaskHierarchy] objects,
     * which encapsulate the information needed to manage split-screen tasks on that display.
     */
    private val displayTaskMap: MutableMap<Int, SplitTaskHierarchy> = mutableMapOf()

    /**
     * SplitTaskHierarchy is a class that encapsulates the components required
     * for managing split-screen functionality on a specific display.
     */
    data class SplitTaskHierarchy(
        var rootTaskInfo: ActivityManager.RunningTaskInfo? = null,
        var mainStage: StageTaskListener? = null,
        var sideStage: StageTaskListener? = null,
        var rootTaskLeash: SurfaceControl? = null,
        var splitLayout: SplitLayout? = null
    )

    /**
     * Returns a list of all currently connected display IDs.
     *
     * @return An ArrayList of display IDs.
     */
    fun getDisplayIds(): ArrayList<Int> {
        val displayIds = ArrayList<Int>()
        displayManager.displays?.forEach { display ->
            displayIds.add(display.displayId)
        }
        return displayIds
    }

    /**
     * Swaps the [SplitTaskHierarchy] objects associated with two different display IDs.
     *
     * @param firstDisplayId  The ID of the first display.
     * @param secondDisplayId The ID of the second display.
     */
    fun swapDisplayTaskHierarchy(firstDisplayId: Int, secondDisplayId: Int) {
        if (!displayTaskMap.containsKey(firstDisplayId) || !displayTaskMap.containsKey(secondDisplayId)) {
            ProtoLog.w(
                ShellProtoLogGroup.WM_SHELL_SPLIT_SCREEN,
                "Attempted to swap task hierarchies for invalid display IDs: %d, %d",
                firstDisplayId,
                secondDisplayId
            )
            return
        }

        if (firstDisplayId == secondDisplayId) {
            return
        }

        val firstHierarchy = displayTaskMap[firstDisplayId]
        val secondHierarchy = displayTaskMap[secondDisplayId]

        displayTaskMap[firstDisplayId] = checkNotNull(secondHierarchy)
        displayTaskMap[secondDisplayId] = checkNotNull(firstHierarchy)
    }

    /**
     * Gets the root task info for the given display ID.
     *
     * @param displayId The ID of the display.
     * @return The root task info, or null if not found.
     */
    fun getDisplayRootTaskInfo(displayId: Int): ActivityManager.RunningTaskInfo? {
        return displayTaskMap[displayId]?.rootTaskInfo
    }

    /**
     * Sets the root task info for the given display ID.
     *
     * @param displayId    The ID of the display.
     * @param rootTaskInfo The root task info to set.
     */
    fun setDisplayRootTaskInfo(
        displayId: Int,
        rootTaskInfo: ActivityManager.RunningTaskInfo?
    ) {
        val hierarchy = displayTaskMap.computeIfAbsent(displayId) { SplitTaskHierarchy() }
        hierarchy.rootTaskInfo = rootTaskInfo
    }
}