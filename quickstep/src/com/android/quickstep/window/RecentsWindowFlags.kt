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

package com.android.quickstep.window

import android.window.DesktopExperienceFlags.DesktopExperienceFlag
import com.android.launcher3.Flags

object RecentsWindowFlags {
    @JvmField
    val enableLauncherOverviewInWindow: DesktopExperienceFlag =
        DesktopExperienceFlag(
            Flags::enableLauncherOverviewInWindow,
            false,
            Flags.FLAG_ENABLE_LAUNCHER_OVERVIEW_IN_WINDOW,
        )

    @JvmField
    val enableFallbackOverviewInWindow: DesktopExperienceFlag =
        DesktopExperienceFlag(
            Flags::enableFallbackOverviewInWindow,
            false,
            Flags.FLAG_ENABLE_FALLBACK_OVERVIEW_IN_WINDOW,
        )

    @JvmField
    val enableOverviewOnConnectedDisplays: DesktopExperienceFlag =
        DesktopExperienceFlag(
            Flags::enableOverviewOnConnectedDisplays,
            true,
            Flags.FLAG_ENABLE_OVERVIEW_ON_CONNECTED_DISPLAYS,
        )

    @JvmStatic
    val enableOverviewInWindow
        get() =
            enableLauncherOverviewInWindow.isTrue ||
                enableFallbackOverviewInWindow.isTrue ||
                enableOverviewOnConnectedDisplays.isTrue

    @JvmStatic fun enableOverviewOnConnectedDisplays() = enableOverviewOnConnectedDisplays.isTrue
}
