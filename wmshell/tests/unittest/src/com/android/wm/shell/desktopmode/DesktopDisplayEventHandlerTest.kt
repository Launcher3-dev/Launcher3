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

package com.android.wm.shell.desktopmode

import android.platform.test.annotations.EnableFlags
import android.testing.AndroidTestingRunner
import android.view.Display.DEFAULT_DISPLAY
import androidx.test.filters.SmallTest
import com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession
import com.android.dx.mockito.inline.extended.ExtendedMockito.never
import com.android.dx.mockito.inline.extended.StaticMockitoSession
import com.android.server.display.feature.flags.Flags as DisplayFlags
import com.android.window.flags.Flags
import com.android.wm.shell.RootTaskDisplayAreaOrganizer
import com.android.wm.shell.ShellTestCase
import com.android.wm.shell.common.DisplayController
import com.android.wm.shell.common.DisplayController.OnDisplaysChangedListener
import com.android.wm.shell.common.ShellExecutor
import com.android.wm.shell.desktopmode.persistence.DesktopRepositoryInitializer
import com.android.wm.shell.shared.desktopmode.DesktopModeStatus
import com.android.wm.shell.sysui.ShellController
import com.android.wm.shell.sysui.ShellInit
import com.android.wm.shell.sysui.UserChangeListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

/**
 * Test class for [DesktopDisplayEventHandler]
 *
 * Usage: atest WMShellUnitTests:DesktopDisplayEventHandlerTest
 */
@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(AndroidTestingRunner::class)
class DesktopDisplayEventHandlerTest : ShellTestCase() {
    @Mock lateinit var testExecutor: ShellExecutor
    @Mock lateinit var displayController: DisplayController
    @Mock private lateinit var mockShellController: ShellController
    @Mock private lateinit var mockRootTaskDisplayAreaOrganizer: RootTaskDisplayAreaOrganizer
    @Mock private lateinit var mockDesktopUserRepositories: DesktopUserRepositories
    @Mock private lateinit var mockDesktopRepository: DesktopRepository
    @Mock private lateinit var mockDesktopTasksController: DesktopTasksController
    @Mock private lateinit var desktopDisplayModeController: DesktopDisplayModeController
    private val desktopRepositoryInitializer = FakeDesktopRepositoryInitializer()
    private val testScope = TestScope()

    private lateinit var mockitoSession: StaticMockitoSession
    private lateinit var shellInit: ShellInit
    private lateinit var handler: DesktopDisplayEventHandler

    private val onDisplaysChangedListenerCaptor = argumentCaptor<OnDisplaysChangedListener>()
    private val externalDisplayId = 100

    @Before
    fun setUp() {
        mockitoSession =
            mockitoSession()
                .strictness(Strictness.LENIENT)
                .spyStatic(DesktopModeStatus::class.java)
                .startMocking()

        shellInit = spy(ShellInit(testExecutor))
        whenever(mockDesktopUserRepositories.current).thenReturn(mockDesktopRepository)
        handler =
            DesktopDisplayEventHandler(
                context,
                shellInit,
                testScope.backgroundScope,
                mockShellController,
                displayController,
                mockRootTaskDisplayAreaOrganizer,
                desktopRepositoryInitializer,
                mockDesktopUserRepositories,
                mockDesktopTasksController,
                desktopDisplayModeController,
            )
        shellInit.init()
        verify(displayController)
            .addDisplayWindowListener(onDisplaysChangedListenerCaptor.capture())
    }

    @After
    fun tearDown() {
        testScope.cancel()
        mockitoSession.finishMocking()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun testDisplayAdded_supportsDesks_desktopRepositoryInitialized_createsDesk() =
        testScope.runTest {
            whenever(DesktopModeStatus.canEnterDesktopMode(context)).thenReturn(true)

            onDisplaysChangedListenerCaptor.lastValue.onDisplayAdded(DEFAULT_DISPLAY)
            desktopRepositoryInitializer.initialize(mockDesktopUserRepositories)
            runCurrent()

            verify(mockDesktopTasksController).createDesk(DEFAULT_DISPLAY)
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun testDisplayAdded_supportsDesks_desktopRepositoryNotInitialized_doesNotCreateDesk() =
        testScope.runTest {
            whenever(DesktopModeStatus.canEnterDesktopMode(context)).thenReturn(true)

            onDisplaysChangedListenerCaptor.lastValue.onDisplayAdded(DEFAULT_DISPLAY)
            runCurrent()

            verify(mockDesktopTasksController, never()).createDesk(DEFAULT_DISPLAY)
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun testDisplayAdded_supportsDesks_desktopRepositoryInitializedTwice_createsDeskOnce() =
        testScope.runTest {
            whenever(DesktopModeStatus.canEnterDesktopMode(context)).thenReturn(true)

            onDisplaysChangedListenerCaptor.lastValue.onDisplayAdded(DEFAULT_DISPLAY)
            desktopRepositoryInitializer.initialize(mockDesktopUserRepositories)
            desktopRepositoryInitializer.initialize(mockDesktopUserRepositories)
            runCurrent()

            verify(mockDesktopTasksController, times(1)).createDesk(DEFAULT_DISPLAY)
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun testDisplayAdded_supportsDesks_desktopRepositoryInitialized_deskExists_doesNotCreateDesk() =
        testScope.runTest {
            whenever(DesktopModeStatus.canEnterDesktopMode(context)).thenReturn(true)
            whenever(mockDesktopRepository.getNumberOfDesks(DEFAULT_DISPLAY)).thenReturn(1)

            onDisplaysChangedListenerCaptor.lastValue.onDisplayAdded(DEFAULT_DISPLAY)
            desktopRepositoryInitializer.initialize(mockDesktopUserRepositories)
            runCurrent()

            verify(mockDesktopTasksController, never()).createDesk(DEFAULT_DISPLAY)
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun testDisplayAdded_cannotEnterDesktopMode_doesNotCreateDesk() =
        testScope.runTest {
            whenever(DesktopModeStatus.canEnterDesktopMode(context)).thenReturn(false)
            desktopRepositoryInitializer.initialize(mockDesktopUserRepositories)

            onDisplaysChangedListenerCaptor.lastValue.onDisplayAdded(DEFAULT_DISPLAY)
            runCurrent()

            verify(mockDesktopTasksController, never()).createDesk(DEFAULT_DISPLAY)
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun testDeskRemoved_noDesksRemain_createsDesk() =
        testScope.runTest {
            whenever(DesktopModeStatus.canEnterDesktopMode(context)).thenReturn(true)
            whenever(mockDesktopRepository.getNumberOfDesks(DEFAULT_DISPLAY)).thenReturn(0)
            desktopRepositoryInitializer.initialize(mockDesktopUserRepositories)

            handler.onDeskRemoved(DEFAULT_DISPLAY, deskId = 1)
            runCurrent()

            verify(mockDesktopTasksController).createDesk(DEFAULT_DISPLAY)
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun testDeskRemoved_desksRemain_doesNotCreateDesk() =
        testScope.runTest {
            whenever(DesktopModeStatus.canEnterDesktopMode(context)).thenReturn(true)
            whenever(mockDesktopRepository.getNumberOfDesks(DEFAULT_DISPLAY)).thenReturn(1)
            desktopRepositoryInitializer.initialize(mockDesktopUserRepositories)

            handler.onDeskRemoved(DEFAULT_DISPLAY, deskId = 1)
            runCurrent()

            verify(mockDesktopTasksController, never()).createDesk(DEFAULT_DISPLAY)
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun testUserChanged_createsDeskWhenNeeded() =
        testScope.runTest {
            val userId = 11
            whenever(DesktopModeStatus.canEnterDesktopMode(context)).thenReturn(true)
            val userChangeListenerCaptor = argumentCaptor<UserChangeListener>()
            verify(mockShellController).addUserChangeListener(userChangeListenerCaptor.capture())
            val mockRepository = mock<DesktopRepository>()
            whenever(mockDesktopUserRepositories.getProfile(userId)).thenReturn(mockRepository)
            whenever(mockRepository.getNumberOfDesks(displayId = 2)).thenReturn(0)
            whenever(mockRepository.getNumberOfDesks(displayId = 3)).thenReturn(0)
            whenever(mockRepository.getNumberOfDesks(displayId = 4)).thenReturn(1)
            whenever(mockRootTaskDisplayAreaOrganizer.displayIds).thenReturn(intArrayOf(2, 3, 4))
            desktopRepositoryInitializer.initialize(mockDesktopUserRepositories)
            handler.onDisplayAdded(displayId = 2)
            handler.onDisplayAdded(displayId = 3)
            handler.onDisplayAdded(displayId = 4)
            runCurrent()

            clearInvocations(mockDesktopTasksController)
            userChangeListenerCaptor.lastValue.onUserChanged(userId, context)
            runCurrent()

            verify(mockDesktopTasksController).createDesk(displayId = 2)
            verify(mockDesktopTasksController).createDesk(displayId = 3)
            verify(mockDesktopTasksController, never()).createDesk(displayId = 4)
        }

    @Test
    fun testConnectExternalDisplay() {
        onDisplaysChangedListenerCaptor.lastValue.onDisplayAdded(externalDisplayId)
        verify(desktopDisplayModeController).updateExternalDisplayWindowingMode(externalDisplayId)
        verify(desktopDisplayModeController).updateDefaultDisplayWindowingMode()
    }

    @Test
    fun testDisconnectExternalDisplay() {
        onDisplaysChangedListenerCaptor.lastValue.onDisplayRemoved(externalDisplayId)
        verify(desktopDisplayModeController).updateDefaultDisplayWindowingMode()
    }

    @Test
    @EnableFlags(DisplayFlags.FLAG_ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT)
    fun testDesktopModeEligibleChanged() {
        onDisplaysChangedListenerCaptor.lastValue.onDesktopModeEligibleChanged(externalDisplayId)
        verify(desktopDisplayModeController).updateExternalDisplayWindowingMode(externalDisplayId)
        verify(desktopDisplayModeController).updateDefaultDisplayWindowingMode()
    }

    private class FakeDesktopRepositoryInitializer : DesktopRepositoryInitializer {
        override var deskRecreationFactory: DesktopRepositoryInitializer.DeskRecreationFactory =
            DesktopRepositoryInitializer.DeskRecreationFactory { _, _, deskId -> deskId }

        override val isInitialized: MutableStateFlow<Boolean> = MutableStateFlow(false)

        override fun initialize(userRepositories: DesktopUserRepositories) {
            isInitialized.value = true
        }
    }
}
