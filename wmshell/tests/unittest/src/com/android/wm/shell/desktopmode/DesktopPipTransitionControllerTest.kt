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

package com.android.wm.shell.desktopmode

import android.os.Binder
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.FlagsParameterization
import android.view.Display.DEFAULT_DISPLAY
import android.window.WindowContainerTransaction
import androidx.test.filters.SmallTest
import com.android.window.flags.Flags
import com.android.window.flags.Flags.FLAG_ENABLE_DESKTOP_WINDOWING_PIP
import com.android.window.flags.Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND
import com.android.wm.shell.ShellTestCase
import com.android.wm.shell.common.pip.PipDesktopState
import com.android.wm.shell.desktopmode.DesktopTestHelpers.createFreeformTask
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import platform.test.runner.parameterized.ParameterizedAndroidJunit4
import platform.test.runner.parameterized.Parameters

/**
 * Tests for [DesktopPipTransitionController].
 *
 * Build/Install/Run: atest WMShellUnitTests:DesktopPipTransitionControllerTest
 */
@SmallTest
@RunWith(ParameterizedAndroidJunit4::class)
@EnableFlags(FLAG_ENABLE_DESKTOP_WINDOWING_PIP)
class DesktopPipTransitionControllerTest(flags: FlagsParameterization) : ShellTestCase() {
    private val mockDesktopTasksController = mock<DesktopTasksController>()
    private val mockDesktopUserRepositories = mock<DesktopUserRepositories>()
    private val mockDesktopRepository = mock<DesktopRepository>()
    private val mockPipDesktopState = mock<PipDesktopState>()

    private lateinit var controller: DesktopPipTransitionController

    private val transition = Binder()
    private val wct = WindowContainerTransaction()
    private val taskInfo = createFreeformTask()

    init {
        mSetFlagsRule.setFlagsParameterization(flags)
    }

    @Before
    fun setUp() {
        whenever(mockPipDesktopState.isDesktopWindowingPipEnabled()).thenReturn(true)
        whenever(mockDesktopUserRepositories.getProfile(anyInt())).thenReturn(mockDesktopRepository)
        whenever(mockDesktopRepository.isAnyDeskActive(anyInt())).thenReturn(true)
        whenever(mockDesktopRepository.getActiveDeskId(anyInt())).thenReturn(DESK_ID)

        controller =
            DesktopPipTransitionController(
                mockDesktopTasksController,
                mockDesktopUserRepositories,
                mockPipDesktopState,
            )
    }

    @Test
    fun handlePipTransition_noDeskActive_doesntPerformDesktopExitCleanup() {
        whenever(mockDesktopRepository.isAnyDeskActive(eq(taskInfo.displayId))).thenReturn(false)

        controller.handlePipTransition(wct, transition, taskInfo)

        verifyPerformDesktopExitCleanupAfterPip(isCalled = false)
    }

    @Test
    fun handlePipTransition_notLastTask_doesntPerformDesktopExitCleanup() {
        whenever(
                mockDesktopRepository.isOnlyVisibleNonClosingTaskInDesk(
                    taskId = eq(taskInfo.taskId),
                    deskId = eq(DESK_ID),
                    displayId = eq(taskInfo.displayId),
                )
            )
            .thenReturn(false)

        controller.handlePipTransition(wct, transition, taskInfo)

        verifyPerformDesktopExitCleanupAfterPip(isCalled = false)
    }

    @Test
    @EnableFlags(FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handlePipTransition_noActiveDeskId_multiDesk_doesntPerformDesktopExitCleanup() {
        whenever(mockDesktopRepository.getActiveDeskId(eq(taskInfo.displayId))).thenReturn(null)

        controller.handlePipTransition(wct, transition, taskInfo)

        verifyPerformDesktopExitCleanupAfterPip(isCalled = false)
    }

    @Test
    fun handlePipTransition_isLastTask_performDesktopExitCleanup() {
        whenever(
                mockDesktopRepository.isOnlyVisibleNonClosingTaskInDesk(
                    taskId = eq(taskInfo.taskId),
                    deskId = eq(DESK_ID),
                    displayId = eq(taskInfo.displayId),
                )
            )
            .thenReturn(true)

        controller.handlePipTransition(wct, transition, taskInfo)

        verifyPerformDesktopExitCleanupAfterPip(isCalled = true)
    }

    private fun verifyPerformDesktopExitCleanupAfterPip(isCalled: Boolean) {
        if (isCalled) {
            verify(mockDesktopTasksController)
                .performDesktopExitCleanUp(
                    wct = wct,
                    deskId = DESK_ID,
                    displayId = DEFAULT_DISPLAY,
                    willExitDesktop = true,
                )
        } else {
            verify(mockDesktopTasksController, never())
                .performDesktopExitCleanUp(
                    any(),
                    anyInt(),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                    anyBoolean(),
                )
        }
    }

    private companion object {
        const val DESK_ID = 1

        @JvmStatic
        @Parameters(name = "{0}")
        fun getParams(): List<FlagsParameterization> =
            FlagsParameterization.allCombinationsOf(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    }
}
