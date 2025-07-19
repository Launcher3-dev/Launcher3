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

package com.android.wm.shell.scenarios

import android.app.Instrumentation
import android.tools.NavBar
import android.tools.Rotation
import android.tools.device.apphelpers.StandardAppHelper
import android.tools.traces.parsers.WindowManagerStateHelper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.android.launcher3.tapl.LauncherInstrumentation
import com.android.wm.shell.Utils
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/** Base test class for window drag CUJ. */
@Ignore("Base Test Class")
abstract class DragAppWindowScenarioTestBase {

    val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    val tapl = LauncherInstrumentation()
    val wmHelper = WindowManagerStateHelper(instrumentation)
    val device = UiDevice.getInstance(instrumentation)

    @Rule
    @JvmField
    val testSetupRule = Utils.testSetupRule(NavBar.MODE_GESTURAL, Rotation.ROTATION_0)

    @Test abstract fun dragAppWindow()

    /** Return the top-center coordinate of the app header as the start coordinate. */
    fun getWindowDragStartCoordinate(appHelper: StandardAppHelper): Pair<Int, Int> {
        val windowRect = wmHelper.getWindowRegion(appHelper).bounds
        // Set start x-coordinate as center of app header.
        val startX = windowRect.centerX()
        val startY = windowRect.top
        return Pair(startX, startY)
    }
}
