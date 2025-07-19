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

package com.android.wm.shell.windowdecor.common

import android.app.ActivityManager
import android.app.WindowConfiguration
import android.content.Context
import android.view.WindowManager
import android.window.DesktopExperienceFlags.ENABLE_BUG_FIXES_FOR_SECONDARY_DISPLAY
import com.android.wm.shell.common.DisplayController
import com.android.wm.shell.desktopmode.DesktopWallpaperActivity.Companion.isWallpaperTask
import com.android.wm.shell.shared.bubbles.BubbleAnythingFlagHelper
import com.android.wm.shell.shared.desktopmode.DesktopModeCompatPolicy
import com.android.wm.shell.shared.desktopmode.DesktopModeStatus
import com.android.wm.shell.splitscreen.SplitScreenController

/**
 * Resolves whether, given a task and its associated display that it is currently on, to show the
 * app handle/header or not.
 */
class AppHandleAndHeaderVisibilityHelper (
    private val context: Context,
    private val displayController: DisplayController,
    private val desktopModeCompatPolicy: DesktopModeCompatPolicy
) {
    var splitScreenController: SplitScreenController? = null

    /**
     * Returns, given a task's attribute and its display attribute, whether the app
     * handle/header should show or not for this task.
     */
    fun shouldShowAppHandleOrHeader(taskInfo: ActivityManager.RunningTaskInfo): Boolean {
        if (!ENABLE_BUG_FIXES_FOR_SECONDARY_DISPLAY.isTrue) {
            return allowedForTask(taskInfo)
        }
        return allowedForTask(taskInfo) && allowedForDisplay(taskInfo.displayId)
    }

    private fun allowedForTask(taskInfo: ActivityManager.RunningTaskInfo): Boolean {
        // TODO (b/382023296): Remove once we no longer rely on
        //  Flags.enableBugFixesForSecondaryDisplay as it is taken care of in #allowedForDisplay
        val display = displayController.getDisplay(taskInfo.displayId)
        if (display == null) {
            // If DisplayController doesn't have it tracked, it could be a private/managed display.
            return false
        }
        if (taskInfo.windowingMode == WindowConfiguration.WINDOWING_MODE_FREEFORM) return true
        if (splitScreenController?.isTaskRootOrStageRoot(taskInfo.taskId) == true) {
            return false
        }

        if (desktopModeCompatPolicy.isTopActivityExemptFromDesktopWindowing(taskInfo)) {
            return false
        }

        // TODO (b/382023296): Remove once we no longer rely on
        //  Flags.enableBugFixesForSecondaryDisplay as it is taken care of in #allowedForDisplay
        val isOnLargeScreen =
            display.minSizeDimensionDp >= WindowManager.LARGE_SCREEN_SMALLEST_SCREEN_WIDTH_DP
        if (!DesktopModeStatus.canEnterDesktopMode(context)
            && DesktopModeStatus.overridesShowAppHandle(context)
            && !isOnLargeScreen
        ) {
            // Devices with multiple screens may enable the app handle but it should not show on
            // small screens
            return false
        }
        if (BubbleAnythingFlagHelper.enableBubbleToFullscreen()
            && !DesktopModeStatus.isDesktopModeSupportedOnDisplay(context, display)
        ) {
            // TODO(b/388853233): enable handles for split tasks once drag to bubble is enabled
            if (taskInfo.windowingMode != WindowConfiguration.WINDOWING_MODE_FULLSCREEN) {
                return false
            }
        }
        return DesktopModeStatus.canEnterDesktopModeOrShowAppHandle(context)
                && !isWallpaperTask(taskInfo)
                && taskInfo.windowingMode != WindowConfiguration.WINDOWING_MODE_PINNED
                && taskInfo.activityType == WindowConfiguration.ACTIVITY_TYPE_STANDARD
                && !taskInfo.configuration.windowConfiguration.isAlwaysOnTop
    }

    private fun allowedForDisplay(displayId: Int): Boolean {
        // If DisplayController doesn't have it tracked, it could be a private/managed display.
        val display = displayController.getDisplay(displayId)
        if (display == null) return false

        if (DesktopModeStatus.isDesktopModeSupportedOnDisplay(context, display)) {
            return true
        }
        // If on default display and on Large Screen (unfolded), show app handle
        return DesktopModeStatus.overridesShowAppHandle(context)
                && display.minSizeDimensionDp >= WindowManager.LARGE_SCREEN_SMALLEST_SCREEN_WIDTH_DP
    }
}