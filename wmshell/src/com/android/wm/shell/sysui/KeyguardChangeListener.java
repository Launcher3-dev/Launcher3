/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.wm.shell.sysui;

/**
 * Callbacks for when the keyguard changes.
 */
public interface KeyguardChangeListener {
    /**
     * Called when the keyguard is showing (and if so, whether it is occluded).
     */
    default void onKeyguardVisibilityChanged(boolean visible, boolean occluded,
            boolean animatingDismiss) {}

    /**
     * Called when the keyguard dismiss animation has finished.
     *
     * TODO(b/206741900) deprecate this path once we're able to animate the PiP window as part of
     * keyguard dismiss animation.
     */
    default void onKeyguardDismissAnimationFinished() {}
}
