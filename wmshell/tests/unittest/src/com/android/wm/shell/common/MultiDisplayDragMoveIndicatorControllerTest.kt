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
package com.android.wm.shell.common

import android.app.ActivityManager.RunningTaskInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.RectF
import android.testing.TestableResources
import android.view.Display
import android.view.SurfaceControl
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.android.wm.shell.RootTaskDisplayAreaOrganizer
import com.android.wm.shell.ShellTestCase
import com.android.wm.shell.TestShellExecutor
import java.util.function.Supplier
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever

/**
 * Tests for [MultiDisplayDragMoveIndicatorController].
 *
 * Build/Install/Run: atest WMShellUnitTests:MultiDisplayDragMoveIndicatorControllerTest
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class MultiDisplayDragMoveIndicatorControllerTest : ShellTestCase() {
    private val displayController = mock<DisplayController>()
    private val rootTaskDisplayAreaOrganizer = mock<RootTaskDisplayAreaOrganizer>()
    private val indicatorSurfaceFactory = mock<MultiDisplayDragMoveIndicatorSurface.Factory>()
    private val indicatorSurface0 = mock<MultiDisplayDragMoveIndicatorSurface>()
    private val indicatorSurface1 = mock<MultiDisplayDragMoveIndicatorSurface>()
    private val transaction = mock<SurfaceControl.Transaction>()
    private val transactionSupplier = mock<Supplier<SurfaceControl.Transaction>>()
    private val taskInfo = mock<RunningTaskInfo>()
    private val display0 = mock<Display>()
    private val display1 = mock<Display>()

    private lateinit var resources: TestableResources
    private val executor = TestShellExecutor()

    private lateinit var controller: MultiDisplayDragMoveIndicatorController

    @Before
    fun setUp() {
        resources = mContext.getOrCreateTestableResources()
        val resourceConfiguration = Configuration()
        resourceConfiguration.uiMode = 0
        resources.overrideConfiguration(resourceConfiguration)

        controller =
            MultiDisplayDragMoveIndicatorController(
                displayController,
                rootTaskDisplayAreaOrganizer,
                indicatorSurfaceFactory,
                executor,
            )

        val spyDisplayLayout0 =
            MultiDisplayTestUtil.createSpyDisplayLayout(
                MultiDisplayTestUtil.DISPLAY_GLOBAL_BOUNDS_0,
                MultiDisplayTestUtil.DISPLAY_DPI_0,
                resources.resources,
            )
        val spyDisplayLayout1 =
            MultiDisplayTestUtil.createSpyDisplayLayout(
                MultiDisplayTestUtil.DISPLAY_GLOBAL_BOUNDS_1,
                MultiDisplayTestUtil.DISPLAY_DPI_1,
                resources.resources,
            )

        taskInfo.taskId = TASK_ID
        whenever(displayController.getDisplayLayout(0)).thenReturn(spyDisplayLayout0)
        whenever(displayController.getDisplayLayout(1)).thenReturn(spyDisplayLayout1)
        whenever(displayController.getDisplay(0)).thenReturn(display0)
        whenever(displayController.getDisplay(1)).thenReturn(display1)
        whenever(indicatorSurfaceFactory.create(taskInfo, display0)).thenReturn(indicatorSurface0)
        whenever(indicatorSurfaceFactory.create(taskInfo, display1)).thenReturn(indicatorSurface1)
        whenever(transactionSupplier.get()).thenReturn(transaction)
    }

    @Test
    fun onDrag_boundsNotIntersectWithDisplay_noIndicator() {
        controller.onDragMove(
            RectF(2000f, 2000f, 2100f, 2200f), // not intersect with any display
            startDisplayId = 0,
            taskInfo,
            displayIds = setOf(0, 1),
        ) { transaction }
        executor.flushAll()

        verify(indicatorSurfaceFactory, never()).create(any(), any())
    }

    @Test
    fun onDrag_boundsIntersectWithStartDisplay_noIndicator() {
        controller.onDragMove(
            RectF(100f, 100f, 200f, 200f), // intersect with display 0
            startDisplayId = 0,
            taskInfo,
            displayIds = setOf(0, 1),
        ) { transaction }
        executor.flushAll()

        verify(indicatorSurfaceFactory, never()).create(any(), any())
    }

    @Test
    fun onDrag_boundsIntersectWithNonStartDisplay_showAndDisposeIndicator() {
        controller.onDragMove(
            RectF(100f, -100f, 200f, 200f), // intersect with display 0 and 1
            startDisplayId = 0,
            taskInfo,
            displayIds = setOf(0, 1),
        ) { transaction }
        executor.flushAll()

        verify(indicatorSurfaceFactory, times(1)).create(taskInfo, display1)
        verify(indicatorSurface1, times(1))
            .show(transaction, taskInfo, rootTaskDisplayAreaOrganizer, 1, Rect(0, 1800, 200, 2400))

        controller.onDragMove(
            RectF(2000f, 2000f, 2100f, 2200f), // not intersect with display 1
            startDisplayId = 0,
            taskInfo,
            displayIds = setOf(0, 1)
        ) { transaction }
        while (executor.callbacks.isNotEmpty()) {
            executor.flushAll()
        }

        verify(indicatorSurface1, times(1))
            .relayout(any(), eq(transaction), shouldBeVisible = eq(false))

        controller.onDragEnd(TASK_ID, { transaction })
        while (executor.callbacks.isNotEmpty()) {
            executor.flushAll()
        }

        verify(indicatorSurface1, times(1)).disposeSurface(transaction)
    }

    companion object {
        private const val TASK_ID = 10
    }
}
