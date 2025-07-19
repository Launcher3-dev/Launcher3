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

package com.android.wm.shell.windowdecor.tiling

import android.app.ActivityManager.RunningTaskInfo
import android.graphics.Rect
import com.android.wm.shell.desktopmode.DesktopTasksController.SnapPosition

/** Interface for handling snap to half screen events. */
interface SnapEventHandler {
    /** Snaps an app to half the screen for tiling. */
    fun snapToHalfScreen(
        taskInfo: RunningTaskInfo,
        currentDragBounds: Rect,
        position: SnapPosition,
    ): Boolean

    /** Removes a task from tiling if it's tiled, for example on task exiting. */
    fun removeTaskIfTiled(displayId: Int, taskId: Int)

    /** Notifies the tiling handler of user switch. */
    fun onUserChange()

    /** Notifies the tiling handler of overview animation state change. */
    fun onOverviewAnimationStateChange(running: Boolean)

    /** If a task is tiled, delegate moving to front to tiling infrastructure. */
    fun moveTaskToFrontIfTiled(taskInfo: RunningTaskInfo): Boolean

    /**
     * Returns the bounds of a task tiled on the left on the specified display, defaults to default
     * snapping bounds if no task is tiled.
     */
    fun getLeftSnapBoundsIfTiled(displayId: Int): Rect

    /**
     * Returns the bounds of a task tiled on the right on the specified display, defaults to default
     * snapping bounds if no task is tiled.
     */
    fun getRightSnapBoundsIfTiled(displayId: Int): Rect
}
