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

package com.android.wm.shell.flicker

import android.tools.Rotation.ROTATION_0
import android.tools.flicker.FlickerConfig
import android.tools.flicker.annotation.ExpectedScenarios
import android.tools.flicker.annotation.FlickerConfigProvider
import android.tools.flicker.config.FlickerConfig
import android.tools.flicker.config.FlickerServiceConfig
import android.tools.flicker.junit.FlickerServiceJUnit4ClassRunner
import com.android.server.wm.flicker.helpers.DesktopModeAppHelper.MaximizeDesktopAppTrigger
import com.android.wm.shell.flicker.DesktopModeFlickerScenarios.Companion.MAXIMIZE_APP
import com.android.wm.shell.scenarios.MaximizeAppWindow
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Maximize app window by tapping on the maximize button within the app header maximize menu.
 *
 * Assert that the app window keeps the same increases in size, filling the vertical and horizontal
 * stable display bounds.
 */
@RunWith(FlickerServiceJUnit4ClassRunner::class)
class MaximizeAppViaHeaderMenuPortrait : MaximizeAppWindow(
    rotation = ROTATION_0,
    trigger = MaximizeDesktopAppTrigger.MAXIMIZE_BUTTON_IN_MENU
) {
    @ExpectedScenarios(["MAXIMIZE_APP"])
    @Test
    override fun maximizeAppWindow() = super.maximizeAppWindow()

    companion object {
        @JvmStatic
        @FlickerConfigProvider
        fun flickerConfigProvider(): FlickerConfig =
            FlickerConfig().use(FlickerServiceConfig.DEFAULT).use(MAXIMIZE_APP)
    }
}