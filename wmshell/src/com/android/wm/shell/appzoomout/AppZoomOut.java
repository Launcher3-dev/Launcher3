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

package com.android.wm.shell.appzoomout;

import com.android.wm.shell.shared.annotations.ExternalThread;

/**
 * Interface to engage with the app zoom out feature.
 */
@ExternalThread
public interface AppZoomOut {

    /**
     * Called when the zoom out progress is updated, which is used to scale down the current app
     * surface from fullscreen to the max pushback level we want to apply. {@param progress} ranges
     * between [0,1], 0 when fullscreen, 1 when it's at the max pushback level.
     */
    void setProgress(float progress);
}
