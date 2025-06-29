/*
 *  Copyright (C) 2023 The Android Open Source Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.android.quickstep.util

import android.app.WindowConfiguration.WINDOWING_MODE_FULLSCREEN
import android.app.WindowConfiguration.WINDOWING_MODE_MULTI_WINDOW
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.ContextThemeWrapper
import android.view.SurfaceControl.Transaction
import android.view.View
import android.window.TransitionInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.launcher3.apppairs.AppPairIcon
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.statehandlers.DepthController
import com.android.launcher3.statemanager.StateManager
import com.android.launcher3.taskbar.TaskbarActivityContext
import com.android.launcher3.util.SplitConfigurationOptions
import com.android.quickstep.views.GroupedTaskView
import com.android.quickstep.views.IconView
import com.android.quickstep.views.TaskContainer
import com.android.quickstep.views.TaskView
import com.android.systemui.shared.recents.model.Task
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SplitAnimationControllerTest {

    private val taskId = 9
    private val taskId2 = 10

    private val mockSplitSelectStateController: SplitSelectStateController = mock()
    // TaskView
    private val mockTaskView: TaskView = mock()
    private val mockSnapshotView: View = mock()
    private val mockBitmap: Bitmap = mock()
    private val mockIconView: IconView = mock()
    private val mockTaskViewDrawable: Drawable = mock()
    // GroupedTaskView
    private val mockGroupedTaskView: GroupedTaskView = mock()
    private val mockTask: Task = mock()
    private val mockTaskKey: Task.TaskKey = mock()
    private val mockTaskContainer: TaskContainer = mock()
    // AppPairIcon
    private val mockAppPairIcon: AppPairIcon = mock()
    private val mockContextThemeWrapper: ContextThemeWrapper = mock()
    private val mockTaskbarActivityContext: TaskbarActivityContext = mock()

    // SplitSelectSource
    private val splitSelectSource: SplitConfigurationOptions.SplitSelectSource = mock()
    private val mockSplitSourceDrawable: Drawable = mock()
    private val mockSplitSourceView: View = mock()
    private val mockItemInfo: ItemInfo = mock()

    private val stateManager: StateManager<*, *> = mock()
    private val depthController: DepthController = mock()
    private val transitionInfo: TransitionInfo = mock()
    private val transaction: Transaction = mock()

    private lateinit var splitAnimationController: SplitAnimationController

    @Before
    fun setup() {
        whenever(mockTaskContainer.snapshotView).thenReturn(mockSnapshotView)
        whenever(mockTaskContainer.thumbnail).thenReturn(mockBitmap)
        whenever(mockTaskContainer.iconView).thenReturn(mockIconView)
        whenever(mockTaskContainer.task).thenReturn(mockTask)
        whenever(mockIconView.drawable).thenReturn(mockTaskViewDrawable)
        whenever(mockTaskView.taskContainers).thenReturn(List(1) { mockTaskContainer })
        whenever(mockTaskView.firstTaskContainer).thenReturn(mockTaskContainer)

        whenever(splitSelectSource.drawable).thenReturn(mockSplitSourceDrawable)
        whenever(splitSelectSource.view).thenReturn(mockSplitSourceView)
        whenever(splitSelectSource.itemInfo).thenReturn(mockItemInfo)

        splitAnimationController = SplitAnimationController(mockSplitSelectStateController)
    }

    @Test
    fun getFirstAnimInitViews_nullTaskViewIcon_useSplitSourceIcon() {
        // Hit fullscreen task dismissal state
        whenever(mockSplitSelectStateController.isAnimateCurrentTaskDismissal).thenReturn(true)
        whenever(mockSplitSelectStateController.isDismissingFromSplitPair).thenReturn(false)

        // Missing taskView icon
        whenever(mockIconView.drawable).thenReturn(null)

        val splitAnimInitProps: SplitAnimationController.Companion.SplitAnimInitProps =
            splitAnimationController.getFirstAnimInitViews({ mockTaskView }, { splitSelectSource })

        assertEquals(
            "Did not fallback to use splitSource icon drawable",
            mockSplitSourceDrawable,
            splitAnimInitProps.iconDrawable,
        )
    }

    @Test
    fun getFirstAnimInitViews_validTaskViewIcon_useTaskViewIcon() {
        // Hit fullscreen task dismissal state
        whenever(mockSplitSelectStateController.isAnimateCurrentTaskDismissal).thenReturn(true)
        whenever(mockSplitSelectStateController.isDismissingFromSplitPair).thenReturn(false)

        val splitAnimInitProps: SplitAnimationController.Companion.SplitAnimInitProps =
            splitAnimationController.getFirstAnimInitViews({ mockTaskView }, { splitSelectSource })

        assertEquals(
            "Did not use taskView icon drawable",
            mockTaskViewDrawable,
            splitAnimInitProps.iconDrawable,
        )
    }

    @Test
    fun getFirstAnimInitViews_validTaskViewNullSplitSource_useTaskViewIcon() {
        // Hit fullscreen task dismissal state
        whenever(mockSplitSelectStateController.isAnimateCurrentTaskDismissal).thenReturn(true)
        whenever(mockSplitSelectStateController.isDismissingFromSplitPair).thenReturn(false)

        // Set split source to null
        whenever(splitSelectSource.drawable).thenReturn(null)

        val splitAnimInitProps: SplitAnimationController.Companion.SplitAnimInitProps =
            splitAnimationController.getFirstAnimInitViews({ mockTaskView }, { splitSelectSource })

        assertEquals(
            "Did not use taskView icon drawable",
            mockTaskViewDrawable,
            splitAnimInitProps.iconDrawable,
        )
    }

    @Test
    fun getFirstAnimInitViews_nullTaskViewValidSplitSource_noTaskDismissal() {
        // Hit initiating split from home
        whenever(mockSplitSelectStateController.isAnimateCurrentTaskDismissal).thenReturn(false)
        whenever(mockSplitSelectStateController.isDismissingFromSplitPair).thenReturn(false)

        val splitAnimInitProps: SplitAnimationController.Companion.SplitAnimInitProps =
            splitAnimationController.getFirstAnimInitViews({ mockTaskView }, { splitSelectSource })

        assertEquals(
            "Did not use splitSource icon drawable",
            mockSplitSourceDrawable,
            splitAnimInitProps.iconDrawable,
        )
    }

    @Test
    fun getFirstAnimInitViews_nullTaskViewValidSplitSource_groupedTaskView() {
        // Hit groupedTaskView dismissal
        whenever(mockSplitSelectStateController.isAnimateCurrentTaskDismissal).thenReturn(true)
        whenever(mockSplitSelectStateController.isDismissingFromSplitPair).thenReturn(true)

        // Remove icon view from GroupedTaskView
        whenever(mockIconView.drawable).thenReturn(null)

        whenever(mockTaskContainer.task).thenReturn(mockTask)
        whenever(mockTaskContainer.iconView).thenReturn(mockIconView)
        whenever(mockTask.getKey()).thenReturn(mockTaskKey)
        whenever(mockTaskKey.getId()).thenReturn(taskId)
        whenever(mockSplitSelectStateController.initialTaskId).thenReturn(taskId)
        whenever(mockGroupedTaskView.taskContainers).thenReturn(List(1) { mockTaskContainer })
        val splitAnimInitProps: SplitAnimationController.Companion.SplitAnimInitProps =
            splitAnimationController.getFirstAnimInitViews(
                { mockGroupedTaskView },
                { splitSelectSource },
            )

        assertEquals(
            "Did not use splitSource icon drawable",
            mockSplitSourceDrawable,
            splitAnimInitProps.iconDrawable,
        )
    }

    @Test
    fun playsAppropriateSplitLaunchAnimation_playsLegacyLaunchCorrectly() {
        val spySplitAnimationController = spy(splitAnimationController)
        doNothing()
            .whenever(spySplitAnimationController)
            .composeRecentsSplitLaunchAnimatorLegacy(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )

        spySplitAnimationController.playSplitLaunchAnimation(
            mockGroupedTaskView,
            null /* launchingIconView */,
            taskId,
            taskId2,
            arrayOf() /* apps */,
            arrayOf() /* wallpapers */,
            arrayOf() /* nonApps */,
            stateManager,
            depthController,
            null /* info */,
            null /* t */,
            {} /* finishCallback */,
            1f, /* cornerRadius */
        )

        verify(spySplitAnimationController)
            .composeRecentsSplitLaunchAnimatorLegacy(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
    }

    @Test
    fun playsAppropriateSplitLaunchAnimation_playsRecentsLaunchCorrectly() {
        val spySplitAnimationController = spy(splitAnimationController)
        doNothing()
            .whenever(spySplitAnimationController)
            .composeRecentsSplitLaunchAnimator(any(), any(), any(), any(), any(), any())

        spySplitAnimationController.playSplitLaunchAnimation(
            mockGroupedTaskView,
            null /* launchingIconView */,
            taskId,
            taskId2,
            null /* apps */,
            null /* wallpapers */,
            null /* nonApps */,
            stateManager,
            depthController,
            transitionInfo,
            transaction,
            {} /* finishCallback */,
            1f, /* cornerRadius */
        )

        verify(spySplitAnimationController)
            .composeRecentsSplitLaunchAnimator(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun playsAppropriateSplitLaunchAnimation_playsIconLaunchCorrectly() {
        val spySplitAnimationController = spy(splitAnimationController)
        whenever(mockAppPairIcon.context).thenReturn(mockContextThemeWrapper)
        doNothing()
            .whenever(spySplitAnimationController)
            .composeIconSplitLaunchAnimator(any(), any(), any(), any(), any())
        doReturn(-1).whenever(spySplitAnimationController).hasChangesForBothAppPairs(any(), any())

        spySplitAnimationController.playSplitLaunchAnimation(
            null /* launchingTaskView */,
            mockAppPairIcon,
            taskId,
            taskId2,
            null /* apps */,
            null /* wallpapers */,
            null /* nonApps */,
            stateManager,
            depthController,
            transitionInfo,
            transaction,
            {} /* finishCallback */,
            1f, /* cornerRadius */
        )

        verify(spySplitAnimationController)
            .composeIconSplitLaunchAnimator(any(), any(), any(), any(), any())
    }

    @Test
    fun playsAppropriateSplitLaunchAnimation_playsIconFullscreenLaunchCorrectly() {
        val spySplitAnimationController = spy(splitAnimationController)
        whenever(mockAppPairIcon.context).thenReturn(mockContextThemeWrapper)
        doNothing()
            .whenever(spySplitAnimationController)
            .composeFullscreenIconSplitLaunchAnimator(any(), any(), any(), any(), any())
        doReturn(0).whenever(spySplitAnimationController).hasChangesForBothAppPairs(any(), any())

        spySplitAnimationController.playSplitLaunchAnimation(
            null /* launchingTaskView */,
            mockAppPairIcon,
            taskId,
            taskId2,
            null /* apps */,
            null /* wallpapers */,
            null /* nonApps */,
            stateManager,
            depthController,
            transitionInfo,
            transaction,
            {} /* finishCallback */,
            1f, /* cornerRadius */
        )

        verify(spySplitAnimationController)
            .composeFullscreenIconSplitLaunchAnimator(any(), any(), any(), any(), eq(0))
    }

    @Test
    fun playsAppropriateSplitLaunchAnimation_playsIconLaunchFromTaskbarCMultiWindow() {
        val spySplitAnimationController = spy(splitAnimationController)
        whenever(mockAppPairIcon.context).thenReturn(mockTaskbarActivityContext)
        doNothing()
            .whenever(spySplitAnimationController)
            .composeScaleUpLaunchAnimation(any(), any(), any(), any())
        doReturn(-1).whenever(spySplitAnimationController).hasChangesForBothAppPairs(any(), any())
        spySplitAnimationController.playSplitLaunchAnimation(
            null /* launchingTaskView */,
            mockAppPairIcon,
            taskId,
            taskId2,
            null /* apps */,
            null /* wallpapers */,
            null /* nonApps */,
            stateManager,
            depthController,
            transitionInfo,
            transaction,
            {} /* finishCallback */,
            1f, /* cornerRadius */
        )

        verify(spySplitAnimationController)
            .composeScaleUpLaunchAnimation(any(), any(), any(), eq(WINDOWING_MODE_MULTI_WINDOW))
    }

    @Test
    fun playsAppropriateSplitLaunchAnimation_playsIconLaunchFromTaskbarFullscreen() {
        val spySplitAnimationController = spy(splitAnimationController)
        whenever(mockAppPairIcon.context).thenReturn(mockTaskbarActivityContext)
        doNothing()
            .whenever(spySplitAnimationController)
            .composeScaleUpLaunchAnimation(any(), any(), any(), any())
        doReturn(0).whenever(spySplitAnimationController).hasChangesForBothAppPairs(any(), any())
        spySplitAnimationController.playSplitLaunchAnimation(
            null /* launchingTaskView */,
            mockAppPairIcon,
            taskId,
            taskId2,
            null /* apps */,
            null /* wallpapers */,
            null /* nonApps */,
            stateManager,
            depthController,
            transitionInfo,
            transaction,
            {} /* finishCallback */,
            1f, /* cornerRadius */
        )

        verify(spySplitAnimationController)
            .composeScaleUpLaunchAnimation(any(), any(), any(), eq(WINDOWING_MODE_FULLSCREEN))
    }

    @Test
    fun playsAppropriateSplitLaunchAnimation_playsFadeInLaunchCorrectly() {
        val spySplitAnimationController = spy(splitAnimationController)
        doNothing()
            .whenever(spySplitAnimationController)
            .composeFadeInSplitLaunchAnimator(any(), any(), any(), any(), any(), any())

        spySplitAnimationController.playSplitLaunchAnimation(
            null /* launchingTaskView */,
            null /* launchingIconView */,
            taskId,
            taskId2,
            null /* apps */,
            null /* wallpapers */,
            null /* nonApps */,
            stateManager,
            depthController,
            transitionInfo,
            transaction,
            {} /* finishCallback */,
            1f, /* cornerRadius */
        )

        verify(spySplitAnimationController)
            .composeFadeInSplitLaunchAnimator(any(), any(), any(), any(), any(), any())
    }
}
