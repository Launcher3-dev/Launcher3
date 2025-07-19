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
package com.android.wm.shell.common.pip

import android.app.ActivityManager
import android.app.WindowConfiguration.WINDOWING_MODE_FREEFORM
import android.app.WindowConfiguration.WINDOWING_MODE_FULLSCREEN
import android.app.WindowConfiguration.WINDOWING_MODE_UNDEFINED
import android.platform.test.annotations.EnableFlags
import android.testing.AndroidTestingRunner
import android.testing.TestableLooper.RunWithLooper
import android.window.DisplayAreaInfo
import android.window.WindowContainerToken
import androidx.test.filters.SmallTest
import com.android.window.flags.Flags.FLAG_ENABLE_CONNECTED_DISPLAYS_PIP
import com.android.window.flags.Flags.FLAG_ENABLE_DESKTOP_WINDOWING_PIP
import com.android.wm.shell.Flags.FLAG_ENABLE_PIP2
import com.android.wm.shell.RootTaskDisplayAreaOrganizer
import com.android.wm.shell.ShellTestCase
import com.android.wm.shell.desktopmode.DesktopRepository
import com.android.wm.shell.desktopmode.DesktopUserRepositories
import com.android.wm.shell.desktopmode.DragToDesktopTransitionHandler
import com.google.common.truth.Truth.assertThat
import java.util.Optional
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit test against [PipDesktopState].
 */
@SmallTest
@RunWithLooper
@RunWith(AndroidTestingRunner::class)
@EnableFlags(FLAG_ENABLE_DESKTOP_WINDOWING_PIP)
class PipDesktopStateTest : ShellTestCase() {
    private val mockPipDisplayLayoutState = mock<PipDisplayLayoutState>()
    private val mockDesktopUserRepositories = mock<DesktopUserRepositories>()
    private val mockDesktopRepository = mock<DesktopRepository>()
    private val mockDragToDesktopTransitionHandler = mock<DragToDesktopTransitionHandler>()
    private val mockRootTaskDisplayAreaOrganizer = mock<RootTaskDisplayAreaOrganizer>()
    private val mockTaskInfo = mock<ActivityManager.RunningTaskInfo>()
    private lateinit var defaultTda: DisplayAreaInfo
    private lateinit var pipDesktopState: PipDesktopState

    @Before
    fun setUp() {
        whenever(mockDesktopUserRepositories.current).thenReturn(mockDesktopRepository)
        whenever(mockTaskInfo.getDisplayId()).thenReturn(DISPLAY_ID)
        whenever(mockPipDisplayLayoutState.displayId).thenReturn(DISPLAY_ID)

        defaultTda = DisplayAreaInfo(mock<WindowContainerToken>(), DISPLAY_ID, /* featureId = */ 0)
        whenever(mockRootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DISPLAY_ID)).thenReturn(
            defaultTda
        )

        pipDesktopState =
            PipDesktopState(
                mockPipDisplayLayoutState,
                Optional.of(mockDesktopUserRepositories),
                Optional.of(mockDragToDesktopTransitionHandler),
                mockRootTaskDisplayAreaOrganizer
            )
    }

    @Test
    fun isDesktopWindowingPipEnabled_returnsTrue() {
        assertThat(pipDesktopState.isDesktopWindowingPipEnabled()).isTrue()
    }

    @Test
    @EnableFlags(
        FLAG_ENABLE_CONNECTED_DISPLAYS_PIP,
        FLAG_ENABLE_PIP2
    )
    fun isConnectedDisplaysPipEnabled_returnsTrue() {
        assertThat(pipDesktopState.isConnectedDisplaysPipEnabled()).isTrue()
    }

    @Test
    fun isPipInDesktopMode_anyDeskActive_returnsTrue() {
        whenever(mockDesktopRepository.isAnyDeskActive(DISPLAY_ID)).thenReturn(true)

        assertThat(pipDesktopState.isPipInDesktopMode()).isTrue()
    }

    @Test
    fun isPipInDesktopMode_noDeskActive_returnsFalse() {
        whenever(mockDesktopRepository.isAnyDeskActive(DISPLAY_ID)).thenReturn(false)

        assertThat(pipDesktopState.isPipInDesktopMode()).isFalse()
    }

    @Test
    fun outPipWindowingMode_exitToDesktop_displayFreeform_returnsUndefined() {
        whenever(mockDesktopRepository.isAnyDeskActive(DISPLAY_ID)).thenReturn(true)
        setDisplayWindowingMode(WINDOWING_MODE_FREEFORM)

        assertThat(pipDesktopState.getOutPipWindowingMode()).isEqualTo(WINDOWING_MODE_UNDEFINED)
    }

    @Test
    fun outPipWindowingMode_exitToDesktop_displayFullscreen_returnsFreeform() {
        whenever(mockDesktopRepository.isAnyDeskActive(DISPLAY_ID)).thenReturn(true)
        setDisplayWindowingMode(WINDOWING_MODE_FULLSCREEN)

        assertThat(pipDesktopState.getOutPipWindowingMode()).isEqualTo(WINDOWING_MODE_FREEFORM)
    }

    @Test
    fun outPipWindowingMode_exitToFullscreen_displayFullscreen_returnsUndefined() {
        setDisplayWindowingMode(WINDOWING_MODE_FULLSCREEN)

        assertThat(pipDesktopState.getOutPipWindowingMode()).isEqualTo(WINDOWING_MODE_UNDEFINED)
    }

    @Test
    fun isDragToDesktopInProgress_inProgress_returnsTrue() {
        whenever(mockDragToDesktopTransitionHandler.inProgress).thenReturn(true)

        assertThat(pipDesktopState.isDragToDesktopInProgress()).isTrue()
    }

    @Test
    fun isDragToDesktopInProgress_notInProgress_returnsFalse() {
        whenever(mockDragToDesktopTransitionHandler.inProgress).thenReturn(false)

        assertThat(pipDesktopState.isDragToDesktopInProgress()).isFalse()
    }

    private fun setDisplayWindowingMode(windowingMode: Int) {
        defaultTda.configuration.windowConfiguration.windowingMode = windowingMode
    }

    companion object {
        private const val DISPLAY_ID = 1
    }
}
