/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.view.SurfaceControl
import com.android.window.flags.Flags
import com.android.wm.shell.common.DisplayImeController
import com.android.wm.shell.common.DisplayImeController.ImePositionProcessor.IME_ANIMATION_DEFAULT
import com.android.wm.shell.sysui.ShellInit

/** Handles the interactions between IME and desktop tasks */
class DesktopImeHandler(
    private val displayImeController: DisplayImeController,
    shellInit: ShellInit,
) : DisplayImeController.ImePositionProcessor {

    init {
        shellInit.addInitCallback(::onInit, this)
    }

    private fun onInit() {
        if (Flags.enableDesktopImeBugfix()) {
            displayImeController.addPositionProcessor(this)
        }
    }

    override fun onImeStartPositioning(
        displayId: Int,
        hiddenTop: Int,
        shownTop: Int,
        showing: Boolean,
        isFloating: Boolean,
        t: SurfaceControl.Transaction?,
    ): Int {
        return IME_ANIMATION_DEFAULT
    }
}
