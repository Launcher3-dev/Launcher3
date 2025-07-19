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
package com.android.wm.shell.common.pip

import android.app.WindowConfiguration.WINDOWING_MODE_FREEFORM
import android.app.WindowConfiguration.WINDOWING_MODE_UNDEFINED
import android.window.DesktopExperienceFlags
import android.window.DesktopModeFlags
import com.android.wm.shell.Flags
import com.android.wm.shell.RootTaskDisplayAreaOrganizer
import com.android.wm.shell.desktopmode.DesktopUserRepositories
import com.android.wm.shell.desktopmode.DragToDesktopTransitionHandler
import java.util.Optional

/** Helper class for PiP on Desktop Mode. */
class PipDesktopState(
    private val pipDisplayLayoutState: PipDisplayLayoutState,
    private val desktopUserRepositoriesOptional: Optional<DesktopUserRepositories>,
    private val dragToDesktopTransitionHandlerOptional: Optional<DragToDesktopTransitionHandler>,
    private val rootTaskDisplayAreaOrganizer: RootTaskDisplayAreaOrganizer
) {
    /**
     * Returns whether PiP in Desktop Windowing is enabled by checking the following:
     * - PiP in Desktop Windowing flag is enabled
     * - DesktopUserRepositories is present
     * - DragToDesktopTransitionHandler is present
     */
    fun isDesktopWindowingPipEnabled(): Boolean =
        DesktopModeFlags.ENABLE_DESKTOP_WINDOWING_PIP.isTrue &&
                desktopUserRepositoriesOptional.isPresent &&
                dragToDesktopTransitionHandlerOptional.isPresent

    /**
     * Returns whether PiP in Connected Displays is enabled by checking the following:
     * - PiP in Connected Displays flag is enabled
     * - PiP2 flag is enabled
     */
    fun isConnectedDisplaysPipEnabled(): Boolean =
        DesktopExperienceFlags.ENABLE_CONNECTED_DISPLAYS_PIP.isTrue && Flags.enablePip2()

    /** Returns whether the display with the PiP task is in freeform windowing mode. */
    private fun isDisplayInFreeform(): Boolean {
        val tdaInfo = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(
            pipDisplayLayoutState.displayId
        )

        return tdaInfo?.configuration?.windowConfiguration?.windowingMode == WINDOWING_MODE_FREEFORM
    }

    /** Returns whether PiP is active in a display that is in active Desktop Mode session. */
    fun isPipInDesktopMode(): Boolean {
        if (!isDesktopWindowingPipEnabled()) {
            return false
        }

        val displayId = pipDisplayLayoutState.displayId
        return desktopUserRepositoriesOptional.get().current.isAnyDeskActive(displayId)
    }

    /** Returns the windowing mode to restore to when resizing out of PIP direction. */
    // TODO(b/403345629): Update this for Multi-Desktop.
    fun getOutPipWindowingMode(): Int {
        // If we are exiting PiP while the device is in Desktop mode, the task should expand to
        // freeform windowing mode.
        // 1) If the display windowing mode is freeform, set windowing mode to UNDEFINED so it will
        //    resolve the windowing mode to the display's windowing mode.
        // 2) If the display windowing mode is not FREEFORM, set windowing mode to FREEFORM.
        if (isPipInDesktopMode()) {
            return if (isDisplayInFreeform()) {
                WINDOWING_MODE_UNDEFINED
            } else {
                WINDOWING_MODE_FREEFORM
            }
        }

        // By default, or if the task is going to fullscreen, reset the windowing mode to undefined.
        return WINDOWING_MODE_UNDEFINED
    }

    /** Returns whether there is a drag-to-desktop transition in progress. */
    fun isDragToDesktopInProgress(): Boolean =
        isDesktopWindowingPipEnabled() && dragToDesktopTransitionHandlerOptional.get().inProgress
}
