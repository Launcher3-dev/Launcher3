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
package com.android.launcher3.taskbar.growth;

/**
 * Constants for registering Growth framework.
 */
public final class GrowthConstants {
    /**
     * For Taskbar broadcast intent filter.
     */
    public static final String BROADCAST_SHOW_NUDGE =
            "com.android.launcher3.growth.BROADCAST_SHOW_NUDGE";

    /**
     * For filtering package of broadcast intent received.
     */
    public static final String GROWTH_NUDGE_PERMISSION =
            "com.android.growth.permission.GROWTH_NUDGE_PERMISSION"
                    + " android:protectionLevel=\"signature|preinstalled\"";

    private GrowthConstants() {}
}
