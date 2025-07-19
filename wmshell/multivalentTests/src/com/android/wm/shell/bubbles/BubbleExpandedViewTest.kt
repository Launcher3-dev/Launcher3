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

package com.android.wm.shell.bubbles

import android.content.ComponentName
import android.content.Context
import android.platform.test.flag.junit.FlagsParameterization
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import com.android.wm.shell.Flags
import com.android.wm.shell.taskview.TaskView
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import platform.test.runner.parameterized.ParameterizedAndroidJunit4
import platform.test.runner.parameterized.Parameters

/** Tests for [BubbleExpandedView] */
@SmallTest
@RunWith(ParameterizedAndroidJunit4::class)
class BubbleExpandedViewTest(flags: FlagsParameterization) {

    @get:Rule
    val setFlagsRule = SetFlagsRule(flags)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val componentName = ComponentName(context, "TestClass")

    @Test
    fun getTaskId_onTaskCreated_returnsCorrectTaskId() {
        val bubbleTaskView = BubbleTaskView(mock<TaskView>(), directExecutor())
        val expandedView = BubbleExpandedView(context).apply {
            initialize(
                mock<BubbleExpandedViewManager>(),
                mock<BubbleStackView>(),
                mock<BubblePositioner>(),
                false /* isOverflow */,
                bubbleTaskView,
            )
            setAnimating(true) // Skips setContentVisibility for testing.
        }

        bubbleTaskView.listener.onTaskCreated(123, componentName)

        assertThat(expandedView.getTaskId()).isEqualTo(123)
    }

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun getParams() = FlagsParameterization.allCombinationsOf(
            Flags.FLAG_ENABLE_BUBBLE_TASK_VIEW_LISTENER,
        )
    }
}
