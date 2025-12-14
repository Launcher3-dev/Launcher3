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

package com.android.launcher3.helper

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val NATIVE_DPAD_DOWN = NativeKeyEvent(NativeKeyEvent.ACTION_DOWN, NativeKeyEvent.KEYCODE_DPAD_DOWN)
val NATIVE_TAB = NativeKeyEvent(NativeKeyEvent.ACTION_DOWN, NativeKeyEvent.KEYCODE_TAB)
val NavigateDownKeyEvent = KeyEvent(NATIVE_DPAD_DOWN)
val NavigateDownTabEvent = KeyEvent(NATIVE_TAB)

fun hasMinTouchArea(minWidth: Dp = 48.dp, minHeight: Dp = 48.dp): SemanticsMatcher =
    SemanticsMatcher("touch area is smaller than $minWidth x $minHeight") { node ->
        val density: Density = node.root?.density ?: error("root node not available!")
        with(density) {
            val rect = node.touchBoundsInRoot
            val size = node.size
            (rect.width.toDp() >= minWidth && rect.height.toDp() >= minHeight) ||
                (size.width.toDp() >= minWidth && size.height.toDp() >= minHeight)
        }
    }
