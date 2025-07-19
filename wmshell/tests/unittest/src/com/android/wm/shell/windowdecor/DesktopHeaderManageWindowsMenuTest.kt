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
package com.android.wm.shell.windowdecor

import android.app.ActivityManager.RunningTaskInfo
import android.app.WindowConfiguration.ACTIVITY_TYPE_STANDARD
import android.app.WindowConfiguration.WINDOWING_MODE_FREEFORM
import android.platform.test.annotations.EnableFlags
import android.testing.AndroidTestingRunner
import android.testing.TestableLooper
import android.view.SurfaceControl
import android.window.TaskSnapshot
import androidx.test.filters.SmallTest
import com.android.window.flags.Flags
import com.android.wm.shell.MockToken
import com.android.wm.shell.ShellTestCase
import com.android.wm.shell.TestRunningTaskInfoBuilder
import com.android.wm.shell.TestShellExecutor
import com.android.wm.shell.desktopmode.DesktopUserRepositories
import com.android.wm.shell.sysui.ShellInit
import com.android.wm.shell.windowdecor.additionalviewcontainer.AdditionalSystemViewContainer
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

/**
 * Tests for [DesktopHeaderManageWindowsMenu].
 *
 * Build/Install/Run:
 * atest WMShellUnitTests:DesktopHeaderManageWindowsMenuTest
 */
@SmallTest
@TestableLooper.RunWithLooper
@RunWith(AndroidTestingRunner::class)
class DesktopHeaderManageWindowsMenuTest : ShellTestCase() {

    private lateinit var userRepositories: DesktopUserRepositories
    private lateinit var menu: DesktopHeaderManageWindowsMenu

    @Before
    fun setUp() {
        userRepositories = DesktopUserRepositories(
            context = context,
            shellInit = ShellInit(TestShellExecutor()),
            shellController = mock(),
            persistentRepository = mock(),
            repositoryInitializer = mock(),
            mainCoroutineScope = mock(),
            userManager = mock(),
        )
    }

    @After
    fun tearDown() {
        menu.animateClose()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_FULLY_IMMERSIVE_IN_DESKTOP)
    fun testShow_forImmersiveTask_usesSystemViewContainer() {
        val task = createFreeformTask()
        userRepositories.getProfile(DEFAULT_USER_ID).setTaskInFullImmersiveState(
            displayId = task.displayId,
            taskId = task.taskId,
            immersive = true
        )
        menu = createMenu(task)

        assertThat(menu.menuViewContainer).isInstanceOf(AdditionalSystemViewContainer::class.java)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_FULLY_IMMERSIVE_IN_DESKTOP)
    fun testShow_nullSnapshotDoesNotCauseNPE() {
        val task = createFreeformTask()
        val snapshotList = listOf(Pair(/* index = */ 1, /* snapshot = */ null))
        // Set as immersive so that menu is created as system view container (simpler of the
        // options)
        userRepositories.getProfile(DEFAULT_USER_ID).setTaskInFullImmersiveState(
            displayId = task.displayId,
            taskId = task.taskId,
            immersive = true
        )
        try {
            menu = createMenu(task, snapshotList)
        } catch (e: NullPointerException) {
            fail("Null snapshot should not have thrown null pointer exception")
        }
    }

    private fun createMenu(
        task: RunningTaskInfo,
        snapshotList: List<Pair<Int, TaskSnapshot?>> = emptyList()
    ) = DesktopHeaderManageWindowsMenu(
        callerTaskInfo = task,
        x = 0,
        y = 0,
        displayController = mock(),
        rootTdaOrganizer = mock(),
        context = context,
        desktopUserRepositories = userRepositories,
        surfaceControlBuilderSupplier = { SurfaceControl.Builder() },
        surfaceControlTransactionSupplier = { SurfaceControl.Transaction() },
        snapshotList = snapshotList,
        onIconClickListener = {},
        onOutsideClickListener = {},
    )

    private fun createFreeformTask(): RunningTaskInfo = TestRunningTaskInfoBuilder()
        .setToken(MockToken().token())
        .setActivityType(ACTIVITY_TYPE_STANDARD)
        .setWindowingMode(WINDOWING_MODE_FREEFORM)
        .setUserId(DEFAULT_USER_ID)
        .build()

    private companion object {
        const val DEFAULT_USER_ID = 10
    }
}
