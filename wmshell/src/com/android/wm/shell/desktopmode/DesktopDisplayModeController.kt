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

package com.android.wm.shell.desktopmode

import android.app.WindowConfiguration.ACTIVITY_TYPE_STANDARD
import android.app.WindowConfiguration.WINDOWING_MODE_FREEFORM
import android.app.WindowConfiguration.WINDOWING_MODE_FULLSCREEN
import android.app.WindowConfiguration.WINDOWING_MODE_UNDEFINED
import android.app.WindowConfiguration.windowingModeToString
import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.provider.Settings
import android.provider.Settings.Global.DEVELOPMENT_FORCE_DESKTOP_MODE_ON_EXTERNAL_DISPLAYS
import android.view.Display.DEFAULT_DISPLAY
import android.view.IWindowManager
import android.view.InputDevice
import android.view.WindowManager.TRANSIT_CHANGE
import android.window.DesktopExperienceFlags
import android.window.WindowContainerTransaction
import com.android.internal.annotations.VisibleForTesting
import com.android.internal.protolog.ProtoLog
import com.android.wm.shell.RootTaskDisplayAreaOrganizer
import com.android.wm.shell.ShellTaskOrganizer
import com.android.wm.shell.common.DisplayController
import com.android.wm.shell.desktopmode.desktopwallpaperactivity.DesktopWallpaperActivityTokenProvider
import com.android.wm.shell.protolog.ShellProtoLogGroup.WM_SHELL_DESKTOP_MODE
import com.android.wm.shell.shared.annotations.ShellMainThread
import com.android.wm.shell.shared.desktopmode.DesktopModeStatus
import com.android.wm.shell.transition.Transitions

/** Controls the display windowing mode in desktop mode */
class DesktopDisplayModeController(
    private val context: Context,
    private val transitions: Transitions,
    private val rootTaskDisplayAreaOrganizer: RootTaskDisplayAreaOrganizer,
    private val windowManager: IWindowManager,
    private val shellTaskOrganizer: ShellTaskOrganizer,
    private val desktopWallpaperActivityTokenProvider: DesktopWallpaperActivityTokenProvider,
    private val inputManager: InputManager,
    private val displayController: DisplayController,
    @ShellMainThread private val mainHandler: Handler,
) {

    private val inputDeviceListener =
        object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                updateDefaultDisplayWindowingMode()
            }

            override fun onInputDeviceChanged(deviceId: Int) {
                updateDefaultDisplayWindowingMode()
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                updateDefaultDisplayWindowingMode()
            }
        }

    init {
        if (DesktopExperienceFlags.FORM_FACTOR_BASED_DESKTOP_FIRST_SWITCH.isTrue) {
            inputManager.registerInputDeviceListener(inputDeviceListener, mainHandler)
        }
    }

    fun updateExternalDisplayWindowingMode(displayId: Int) {
        if (!DesktopExperienceFlags.ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT.isTrue) return

        val desktopModeSupported =
            displayController.getDisplay(displayId)?.let { display ->
                DesktopModeStatus.isDesktopModeSupportedOnDisplay(context, display)
            } ?: false
        if (!desktopModeSupported) return

        // An external display should always be a freeform display when desktop mode is enabled.
        updateDisplayWindowingMode(displayId, WINDOWING_MODE_FREEFORM)
    }

    fun updateDefaultDisplayWindowingMode() {
        if (!DesktopExperienceFlags.ENABLE_DISPLAY_WINDOWING_MODE_SWITCHING.isTrue) return

        updateDisplayWindowingMode(DEFAULT_DISPLAY, getTargetWindowingModeForDefaultDisplay())
    }

    private fun updateDisplayWindowingMode(displayId: Int, targetDisplayWindowingMode: Int) {
        val tdaInfo =
            requireNotNull(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(displayId)) {
                "DisplayAreaInfo of display#$displayId must be non-null."
            }
        val currentDisplayWindowingMode = tdaInfo.configuration.windowConfiguration.windowingMode
        if (currentDisplayWindowingMode == targetDisplayWindowingMode) {
            // Already in the target mode.
            return
        }

        logV(
            "Changing display#%d's windowing mode from %s to %s",
            displayId,
            windowingModeToString(currentDisplayWindowingMode),
            windowingModeToString(targetDisplayWindowingMode),
        )

        val wct = WindowContainerTransaction()
        wct.setWindowingMode(tdaInfo.token, targetDisplayWindowingMode)
        shellTaskOrganizer
            .getRunningTasks(displayId)
            .filter { it.activityType == ACTIVITY_TYPE_STANDARD }
            .forEach {
                // TODO: b/391965153 - Reconsider the logic under multi-desk window hierarchy
                when (it.windowingMode) {
                    currentDisplayWindowingMode -> {
                        wct.setWindowingMode(it.token, currentDisplayWindowingMode)
                    }
                    targetDisplayWindowingMode -> {
                        wct.setWindowingMode(it.token, WINDOWING_MODE_UNDEFINED)
                    }
                }
            }
        // The override windowing mode of DesktopWallpaper can be UNDEFINED on fullscreen-display
        // right after the first launch while its resolved windowing mode is FULLSCREEN. We here
        // it has the FULLSCREEN override windowing mode.
        desktopWallpaperActivityTokenProvider.getToken(displayId)?.let { token ->
            wct.setWindowingMode(token, WINDOWING_MODE_FULLSCREEN)
        }
        transitions.startTransition(TRANSIT_CHANGE, wct, /* handler= */ null)
    }

    // Do not directly use this method to check the state of desktop-first mode. Check the display
    // windowing mode instead.
    private fun canDesktopFirstModeBeEnabledOnDefaultDisplay(): Boolean {
        if (isDefaultDisplayDesktopEligible()) {
            if (isExtendedDisplayEnabled() && hasExternalDisplay()) {
                return true
            }
            if (DesktopExperienceFlags.FORM_FACTOR_BASED_DESKTOP_FIRST_SWITCH.isTrue) {
                if (hasAnyTouchpadDevice() && hasAnyPhysicalKeyboardDevice()) {
                    return true
                }
            }
        }
        return false
    }

    @VisibleForTesting
    fun getTargetWindowingModeForDefaultDisplay(): Int {
        if (canDesktopFirstModeBeEnabledOnDefaultDisplay()) {
            return WINDOWING_MODE_FREEFORM
        }

        return if (DesktopExperienceFlags.FORM_FACTOR_BASED_DESKTOP_FIRST_SWITCH.isTrue) {
            WINDOWING_MODE_FULLSCREEN
        } else {
            // If form factor-based desktop first switch is disabled, use the default display
            // windowing mode here to keep the freeform mode for some form factors (e.g.,
            // FEATURE_PC).
            windowManager.getWindowingMode(DEFAULT_DISPLAY)
        }
    }

    private fun isExtendedDisplayEnabled(): Boolean {
        if (DesktopExperienceFlags.ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT.isTrue) {
            return rootTaskDisplayAreaOrganizer
                .getDisplayIds()
                .filter { it != DEFAULT_DISPLAY }
                .any { displayId ->
                    displayController.getDisplay(displayId)?.let { display ->
                        DesktopModeStatus.isDesktopModeSupportedOnDisplay(context, display)
                    } ?: false
                }
        }

        return 0 !=
            Settings.Global.getInt(
                context.contentResolver,
                DEVELOPMENT_FORCE_DESKTOP_MODE_ON_EXTERNAL_DISPLAYS,
                0,
            )
    }

    private fun hasExternalDisplay() =
        rootTaskDisplayAreaOrganizer.getDisplayIds().any { it != DEFAULT_DISPLAY }

    private fun hasAnyTouchpadDevice() =
        inputManager.inputDeviceIds.any { deviceId ->
            inputManager.getInputDevice(deviceId)?.let { device ->
                device.supportsSource(InputDevice.SOURCE_TOUCHPAD) && device.isEnabled()
            } ?: false
        }

    private fun hasAnyPhysicalKeyboardDevice() =
        inputManager.inputDeviceIds.any { deviceId ->
            inputManager.getInputDevice(deviceId)?.let { device ->
                !device.isVirtual() && device.isFullKeyboard() && device.isEnabled()
            } ?: false
        }

    private fun isDefaultDisplayDesktopEligible(): Boolean {
        val display =
            requireNotNull(displayController.getDisplay(DEFAULT_DISPLAY)) {
                "Display object of DEFAULT_DISPLAY must be non-null."
            }
        return DesktopModeStatus.isDesktopModeSupportedOnDisplay(context, display)
    }

    private fun logV(msg: String, vararg arguments: Any?) {
        ProtoLog.v(WM_SHELL_DESKTOP_MODE, "%s: $msg", TAG, *arguments)
    }

    companion object {
        private const val TAG = "DesktopDisplayModeController"
    }
}
