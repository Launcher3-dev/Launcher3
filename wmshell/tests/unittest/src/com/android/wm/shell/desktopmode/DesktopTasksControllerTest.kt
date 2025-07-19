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

package com.android.wm.shell.desktopmode

import android.app.ActivityManager.RecentTaskInfo
import android.app.ActivityManager.RunningTaskInfo
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.WindowConfiguration
import android.app.WindowConfiguration.ACTIVITY_TYPE_HOME
import android.app.WindowConfiguration.ACTIVITY_TYPE_STANDARD
import android.app.WindowConfiguration.WINDOWING_MODE_FREEFORM
import android.app.WindowConfiguration.WINDOWING_MODE_FULLSCREEN
import android.app.WindowConfiguration.WINDOWING_MODE_MULTI_WINDOW
import android.app.WindowConfiguration.WINDOWING_MODE_UNDEFINED
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ActivityInfo.CONFIG_DENSITY
import android.content.pm.ActivityInfo.LAUNCH_SINGLE_INSTANCE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Resources
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.UserHandle
import android.os.UserManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.FlagsParameterization
import android.testing.TestableContext
import android.view.Display
import android.view.Display.DEFAULT_DISPLAY
import android.view.Display.INVALID_DISPLAY
import android.view.DragEvent
import android.view.Gravity
import android.view.MotionEvent
import android.view.SurfaceControl
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowManager.TRANSIT_CHANGE
import android.view.WindowManager.TRANSIT_CLOSE
import android.view.WindowManager.TRANSIT_OPEN
import android.view.WindowManager.TRANSIT_TO_BACK
import android.view.WindowManager.TRANSIT_TO_FRONT
import android.widget.Toast
import android.window.DisplayAreaInfo
import android.window.IWindowContainerToken
import android.window.RemoteTransition
import android.window.TransitionInfo
import android.window.TransitionRequestInfo
import android.window.WindowContainerToken
import android.window.WindowContainerTransaction
import android.window.WindowContainerTransaction.HierarchyOp.HIERARCHY_OP_TYPE_LAUNCH_TASK
import android.window.WindowContainerTransaction.HierarchyOp.HIERARCHY_OP_TYPE_PENDING_INTENT
import android.window.WindowContainerTransaction.HierarchyOp.HIERARCHY_OP_TYPE_REMOVE_TASK
import android.window.WindowContainerTransaction.HierarchyOp.HIERARCHY_OP_TYPE_REORDER
import android.window.WindowContainerTransaction.HierarchyOp.LAUNCH_KEY_TASK_ID
import androidx.test.filters.SmallTest
import com.android.dx.mockito.inline.extended.ExtendedMockito.doReturn
import com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession
import com.android.dx.mockito.inline.extended.ExtendedMockito.never
import com.android.dx.mockito.inline.extended.StaticMockitoSession
import com.android.internal.jank.InteractionJankMonitor
import com.android.internal.policy.SystemBarUtils.getDesktopViewAppHeaderHeightPx
import com.android.window.flags.Flags
import com.android.window.flags.Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODE
import com.android.window.flags.Flags.FLAG_ENABLE_DISPLAY_FOCUS_IN_SHELL_TRANSITIONS
import com.android.window.flags.Flags.FLAG_ENABLE_FULLY_IMMERSIVE_IN_DESKTOP
import com.android.window.flags.Flags.FLAG_ENABLE_MOVE_TO_NEXT_DISPLAY_SHORTCUT
import com.android.window.flags.Flags.FLAG_ENABLE_PER_DISPLAY_DESKTOP_WALLPAPER_ACTIVITY
import com.android.wm.shell.MockToken
import com.android.wm.shell.R
import com.android.wm.shell.RootTaskDisplayAreaOrganizer
import com.android.wm.shell.ShellTaskOrganizer
import com.android.wm.shell.ShellTestCase
import com.android.wm.shell.TestRunningTaskInfoBuilder
import com.android.wm.shell.TestShellExecutor
import com.android.wm.shell.bubbles.BubbleController
import com.android.wm.shell.common.DisplayController
import com.android.wm.shell.common.DisplayLayout
import com.android.wm.shell.common.HomeIntentProvider
import com.android.wm.shell.common.MultiInstanceHelper
import com.android.wm.shell.common.ShellExecutor
import com.android.wm.shell.common.SyncTransactionQueue
import com.android.wm.shell.common.UserProfileContexts
import com.android.wm.shell.desktopmode.DesktopImmersiveController.ExitResult
import com.android.wm.shell.desktopmode.DesktopModeEventLogger.Companion.InputMethod
import com.android.wm.shell.desktopmode.DesktopModeEventLogger.Companion.MinimizeReason
import com.android.wm.shell.desktopmode.DesktopModeEventLogger.Companion.ResizeTrigger
import com.android.wm.shell.desktopmode.DesktopModeEventLogger.Companion.UnminimizeReason
import com.android.wm.shell.desktopmode.DesktopTasksController.DesktopModeEntryExitTransitionListener
import com.android.wm.shell.desktopmode.DesktopTasksController.SnapPosition
import com.android.wm.shell.desktopmode.DesktopTasksController.TaskbarDesktopTaskListener
import com.android.wm.shell.desktopmode.DesktopTestHelpers.createFreeformTask
import com.android.wm.shell.desktopmode.DesktopTestHelpers.createFullscreenTask
import com.android.wm.shell.desktopmode.DesktopTestHelpers.createHomeTask
import com.android.wm.shell.desktopmode.DesktopTestHelpers.createSplitScreenTask
import com.android.wm.shell.desktopmode.EnterDesktopTaskTransitionHandler.FREEFORM_ANIMATION_DURATION
import com.android.wm.shell.desktopmode.ExitDesktopTaskTransitionHandler.FULLSCREEN_ANIMATION_DURATION
import com.android.wm.shell.desktopmode.common.ToggleTaskSizeInteraction
import com.android.wm.shell.desktopmode.desktopwallpaperactivity.DesktopWallpaperActivityTokenProvider
import com.android.wm.shell.desktopmode.minimize.DesktopWindowLimitRemoteHandler
import com.android.wm.shell.desktopmode.multidesks.DeskTransition
import com.android.wm.shell.desktopmode.multidesks.DesksOrganizer
import com.android.wm.shell.desktopmode.multidesks.DesksTransitionObserver
import com.android.wm.shell.desktopmode.persistence.Desktop
import com.android.wm.shell.desktopmode.persistence.DesktopPersistentRepository
import com.android.wm.shell.desktopmode.persistence.DesktopRepositoryInitializer
import com.android.wm.shell.draganddrop.DragAndDropController
import com.android.wm.shell.freeform.FreeformTaskTransitionStarter
import com.android.wm.shell.recents.RecentTasksController
import com.android.wm.shell.recents.RecentsTransitionHandler
import com.android.wm.shell.recents.RecentsTransitionStateListener
import com.android.wm.shell.recents.RecentsTransitionStateListener.TRANSITION_STATE_ANIMATING
import com.android.wm.shell.recents.RecentsTransitionStateListener.TRANSITION_STATE_REQUESTED
import com.android.wm.shell.shared.desktopmode.DesktopModeCompatPolicy
import com.android.wm.shell.shared.desktopmode.DesktopModeStatus
import com.android.wm.shell.shared.desktopmode.DesktopModeTransitionSource.UNKNOWN
import com.android.wm.shell.shared.split.SplitScreenConstants
import com.android.wm.shell.shared.split.SplitScreenConstants.SPLIT_INDEX_UNDEFINED
import com.android.wm.shell.splitscreen.SplitScreenController
import com.android.wm.shell.sysui.ShellCommandHandler
import com.android.wm.shell.sysui.ShellController
import com.android.wm.shell.sysui.ShellInit
import com.android.wm.shell.transition.FocusTransitionObserver
import com.android.wm.shell.transition.OneShotRemoteHandler
import com.android.wm.shell.transition.TestRemoteTransition
import com.android.wm.shell.transition.Transitions
import com.android.wm.shell.transition.Transitions.ENABLE_SHELL_TRANSITIONS
import com.android.wm.shell.transition.Transitions.TransitionHandler
import com.android.wm.shell.windowdecor.DesktopModeWindowDecorViewModelTestsBase.Companion.HOME_LAUNCHER_PACKAGE_NAME
import com.android.wm.shell.windowdecor.tiling.SnapEventHandler
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.Optional
import java.util.function.Consumer
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.isA
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import platform.test.runner.parameterized.ParameterizedAndroidJunit4
import platform.test.runner.parameterized.Parameters

/**
 * Test class for {@link DesktopTasksController}
 *
 * Usage: atest WMShellUnitTests:DesktopTasksControllerTest
 */
@SmallTest
@RunWith(ParameterizedAndroidJunit4::class)
@ExperimentalCoroutinesApi
@EnableFlags(FLAG_ENABLE_DESKTOP_WINDOWING_MODE)
class DesktopTasksControllerTest(flags: FlagsParameterization) : ShellTestCase() {

    @Mock lateinit var testExecutor: ShellExecutor
    @Mock lateinit var shellCommandHandler: ShellCommandHandler
    @Mock lateinit var shellController: ShellController
    @Mock lateinit var displayController: DisplayController
    @Mock lateinit var displayLayout: DisplayLayout
    @Mock lateinit var display: Display
    @Mock lateinit var shellTaskOrganizer: ShellTaskOrganizer
    @Mock lateinit var syncQueue: SyncTransactionQueue
    @Mock lateinit var rootTaskDisplayAreaOrganizer: RootTaskDisplayAreaOrganizer
    @Mock lateinit var transitions: Transitions
    @Mock lateinit var keyguardManager: KeyguardManager
    @Mock lateinit var mReturnToDragStartAnimator: ReturnToDragStartAnimator
    @Mock lateinit var desktopMixedTransitionHandler: DesktopMixedTransitionHandler
    @Mock lateinit var exitDesktopTransitionHandler: ExitDesktopTaskTransitionHandler
    @Mock lateinit var enterDesktopTransitionHandler: EnterDesktopTaskTransitionHandler
    @Mock lateinit var dragAndDropTransitionHandler: DesktopModeDragAndDropTransitionHandler
    @Mock
    lateinit var toggleResizeDesktopTaskTransitionHandler: ToggleResizeDesktopTaskTransitionHandler
    @Mock lateinit var dragToDesktopTransitionHandler: DragToDesktopTransitionHandler
    @Mock lateinit var mMockDesktopImmersiveController: DesktopImmersiveController
    @Mock lateinit var splitScreenController: SplitScreenController
    @Mock lateinit var recentsTransitionHandler: RecentsTransitionHandler
    @Mock lateinit var dragAndDropController: DragAndDropController
    @Mock lateinit var multiInstanceHelper: MultiInstanceHelper
    @Mock lateinit var desktopModeVisualIndicator: DesktopModeVisualIndicator
    @Mock lateinit var recentTasksController: RecentTasksController
    @Mock lateinit var snapEventHandler: SnapEventHandler
    @Mock private lateinit var mockInteractionJankMonitor: InteractionJankMonitor
    @Mock private lateinit var mockSurface: SurfaceControl
    @Mock private lateinit var taskbarDesktopTaskListener: TaskbarDesktopTaskListener
    @Mock private lateinit var freeformTaskTransitionStarter: FreeformTaskTransitionStarter
    @Mock private lateinit var mockHandler: Handler
    @Mock private lateinit var focusTransitionObserver: FocusTransitionObserver
    @Mock private lateinit var desktopModeEventLogger: DesktopModeEventLogger
    @Mock private lateinit var desktopModeUiEventLogger: DesktopModeUiEventLogger
    @Mock lateinit var persistentRepository: DesktopPersistentRepository
    @Mock lateinit var motionEvent: MotionEvent
    @Mock lateinit var repositoryInitializer: DesktopRepositoryInitializer
    @Mock private lateinit var mockToast: Toast
    private lateinit var mockitoSession: StaticMockitoSession
    @Mock private lateinit var bubbleController: BubbleController
    @Mock private lateinit var resources: Resources
    @Mock
    lateinit var desktopModeEnterExitTransitionListener: DesktopModeEntryExitTransitionListener
    @Mock private lateinit var userManager: UserManager
    @Mock
    private lateinit var desktopWallpaperActivityTokenProvider:
        DesktopWallpaperActivityTokenProvider
    @Mock
    private lateinit var overviewToDesktopTransitionObserver: OverviewToDesktopTransitionObserver
    @Mock private lateinit var desksOrganizer: DesksOrganizer
    @Mock private lateinit var userProfileContexts: UserProfileContexts
    @Mock private lateinit var desksTransitionsObserver: DesksTransitionObserver
    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var mockDisplayContext: Context
    @Mock private lateinit var dragToDisplayTransitionHandler: DragToDisplayTransitionHandler
    @Mock
    private lateinit var moveToDisplayTransitionHandler: DesktopModeMoveToDisplayTransitionHandler

    private lateinit var controller: DesktopTasksController
    private lateinit var shellInit: ShellInit
    private lateinit var taskRepository: DesktopRepository
    private lateinit var userRepositories: DesktopUserRepositories
    private lateinit var desktopTasksLimiter: DesktopTasksLimiter
    private lateinit var recentsTransitionStateListener: RecentsTransitionStateListener
    private lateinit var testScope: CoroutineScope
    private lateinit var desktopModeCompatPolicy: DesktopModeCompatPolicy
    private lateinit var spyContext: TestableContext
    private lateinit var homeIntentProvider: HomeIntentProvider

    private val shellExecutor = TestShellExecutor()
    private val bgExecutor = TestShellExecutor()

    // Mock running tasks are registered here so we can get the list from mock shell task organizer
    private val runningTasks = mutableListOf<RunningTaskInfo>()

    private val SECONDARY_DISPLAY_ID = 1
    private val DISPLAY_DIMENSION_SHORT = 1600
    private val DISPLAY_DIMENSION_LONG = 2560
    private val DEFAULT_LANDSCAPE_BOUNDS = Rect(320, 75, 2240, 1275)
    private val DEFAULT_PORTRAIT_BOUNDS = Rect(200, 165, 1400, 2085)
    private val RESIZABLE_LANDSCAPE_BOUNDS = Rect(25, 435, 1575, 1635)
    private val RESIZABLE_PORTRAIT_BOUNDS = Rect(680, 75, 1880, 1275)
    private val UNRESIZABLE_LANDSCAPE_BOUNDS = Rect(25, 448, 1575, 1611)
    private val UNRESIZABLE_PORTRAIT_BOUNDS = Rect(830, 75, 1730, 1275)
    private val wallpaperToken = MockToken().token()
    private val homeComponentName = ComponentName(HOME_LAUNCHER_PACKAGE_NAME, /* class */ "")

    init {
        mSetFlagsRule.setFlagsParameterization(flags)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        mockitoSession =
            mockitoSession()
                .strictness(Strictness.LENIENT)
                .spyStatic(DesktopModeStatus::class.java)
                .spyStatic(Toast::class.java)
                .startMocking()
        doReturn(true).`when` { DesktopModeStatus.canEnterDesktopMode(any()) }

        testScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        spyContext = spy(mContext)
        shellInit = spy(ShellInit(testExecutor))
        userRepositories =
            DesktopUserRepositories(
                context,
                shellInit,
                shellController,
                persistentRepository,
                repositoryInitializer,
                testScope,
                userManager,
            )
        desktopTasksLimiter =
            DesktopTasksLimiter(
                transitions,
                userRepositories,
                shellTaskOrganizer,
                desksOrganizer,
                MAX_TASK_LIMIT,
                mockInteractionJankMonitor,
                mContext,
                mockHandler,
            )
        desktopModeCompatPolicy = spy(DesktopModeCompatPolicy(spyContext))
        homeIntentProvider = HomeIntentProvider(context)

        whenever(shellTaskOrganizer.getRunningTasks(anyInt())).thenAnswer { runningTasks }
        whenever(transitions.startTransition(anyInt(), any(), anyOrNull())).thenAnswer { Binder() }
        whenever(enterDesktopTransitionHandler.moveToDesktop(any(), any())).thenAnswer { Binder() }
        whenever(exitDesktopTransitionHandler.startTransition(any(), any(), any(), any()))
            .thenReturn(Binder())
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    any(),
                    any(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Binder())
        whenever(displayController.getDisplayLayout(anyInt())).thenReturn(displayLayout)
        whenever(displayController.getDisplayContext(anyInt())).thenReturn(mockDisplayContext)
        whenever(mockDisplayContext.resources).thenReturn(resources)
        whenever(displayController.getDisplay(anyInt())).thenReturn(display)
        whenever(displayLayout.getStableBounds(any())).thenAnswer { i ->
            (i.arguments.first() as Rect).set(STABLE_BOUNDS)
        }
        whenever(displayLayout.densityDpi()).thenReturn(160)
        whenever(runBlocking { persistentRepository.readDesktop(any(), any()) })
            .thenReturn(Desktop.getDefaultInstance())
        doReturn(mockToast).`when` { Toast.makeText(any(), anyInt(), anyInt()) }

        val tda = DisplayAreaInfo(MockToken().token(), DEFAULT_DISPLAY, 0)
        tda.configuration.windowConfiguration.windowingMode = WINDOWING_MODE_FULLSCREEN
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY)).thenReturn(tda)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(SECONDARY_DISPLAY_ID))
            .thenReturn(tda)
        whenever(
                mMockDesktopImmersiveController.exitImmersiveIfApplicable(
                    any(),
                    any<RunningTaskInfo>(),
                    any(),
                )
            )
            .thenReturn(ExitResult.NoExit)
        whenever(
                mMockDesktopImmersiveController.exitImmersiveIfApplicable(
                    any(),
                    anyInt(),
                    anyOrNull(),
                    any(),
                )
            )
            .thenReturn(ExitResult.NoExit)
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(wallpaperToken)
        whenever(userProfileContexts[anyInt()]).thenReturn(context)
        whenever(userProfileContexts.getOrCreate(anyInt())).thenReturn(context)
        whenever(freeformTaskTransitionStarter.startPipTransition(any())).thenReturn(Binder())

        controller = createController()
        controller.setSplitScreenController(splitScreenController)
        controller.freeformTaskTransitionStarter = freeformTaskTransitionStarter
        controller.desktopModeEnterExitTransitionListener = desktopModeEnterExitTransitionListener

        shellInit.init()

        val captor = argumentCaptor<RecentsTransitionStateListener>()
        verify(recentsTransitionHandler).addTransitionStateListener(captor.capture())
        recentsTransitionStateListener = captor.firstValue

        controller.taskbarDesktopTaskListener = taskbarDesktopTaskListener
        controller.setSnapEventHandler(snapEventHandler)

        assumeTrue(ENABLE_SHELL_TRANSITIONS)

        taskRepository = userRepositories.current
        taskRepository.addDesk(displayId = DEFAULT_DISPLAY, deskId = DEFAULT_DISPLAY)
        taskRepository.setActiveDesk(displayId = DEFAULT_DISPLAY, deskId = DEFAULT_DISPLAY)

        spyContext.setMockPackageManager(packageManager)
        whenever(packageManager.getHomeActivities(ArrayList())).thenReturn(homeComponentName)
    }

    private fun createController() =
        DesktopTasksController(
            context,
            shellInit,
            shellCommandHandler,
            shellController,
            displayController,
            shellTaskOrganizer,
            syncQueue,
            rootTaskDisplayAreaOrganizer,
            dragAndDropController,
            transitions,
            keyguardManager,
            mReturnToDragStartAnimator,
            desktopMixedTransitionHandler,
            enterDesktopTransitionHandler,
            exitDesktopTransitionHandler,
            dragAndDropTransitionHandler,
            toggleResizeDesktopTaskTransitionHandler,
            dragToDesktopTransitionHandler,
            mMockDesktopImmersiveController,
            userRepositories,
            repositoryInitializer,
            recentsTransitionHandler,
            multiInstanceHelper,
            shellExecutor,
            bgExecutor,
            Optional.of(desktopTasksLimiter),
            recentTasksController,
            mockInteractionJankMonitor,
            mockHandler,
            focusTransitionObserver,
            desktopModeEventLogger,
            desktopModeUiEventLogger,
            desktopWallpaperActivityTokenProvider,
            Optional.of(bubbleController),
            overviewToDesktopTransitionObserver,
            desksOrganizer,
            desksTransitionsObserver,
            userProfileContexts,
            desktopModeCompatPolicy,
            dragToDisplayTransitionHandler,
            moveToDisplayTransitionHandler,
            homeIntentProvider,
        )

    @After
    fun tearDown() {
        mockitoSession.finishMocking()

        runningTasks.clear()
        testScope.cancel()
    }

    @Test
    fun instantiate_addInitCallback() {
        verify(shellInit).addInitCallback(any(), any<DesktopTasksController>())
    }

    @Test
    fun doesAnyTaskRequireTaskbarRounding_onlyFreeFormTaskIsRunning_returnFalse() {
        setUpFreeformTask()

        assertThat(controller.doesAnyTaskRequireTaskbarRounding(DEFAULT_DISPLAY)).isFalse()
    }

    @Test
    fun doesAnyTaskRequireTaskbarRounding_toggleResizeOfFreeFormTask_returnTrue() {
        val task1 = setUpFreeformTask()

        val argumentCaptor = argumentCaptor<Boolean>()
        controller.toggleDesktopTaskSize(
            task1,
            ToggleTaskSizeInteraction(
                ToggleTaskSizeInteraction.Direction.MAXIMIZE,
                ToggleTaskSizeInteraction.Source.HEADER_BUTTON_TO_MAXIMIZE,
                InputMethod.TOUCH,
            ),
        )

        verify(taskbarDesktopTaskListener).onTaskbarCornerRoundingUpdate(argumentCaptor.capture())
        verify(desktopModeEventLogger, times(1))
            .logTaskResizingEnded(
                ResizeTrigger.MAXIMIZE_BUTTON,
                InputMethod.TOUCH,
                task1,
                STABLE_BOUNDS.width(),
                STABLE_BOUNDS.height(),
                displayController,
            )
        assertThat(argumentCaptor.firstValue).isTrue()
    }

    @Test
    fun doesAnyTaskRequireTaskbarRounding_fullScreenTaskIsRunning_returnTrue() {
        val stableBounds = Rect().apply { displayLayout.getStableBounds(this) }
        setUpFreeformTask(bounds = stableBounds, active = true)
        assertThat(controller.doesAnyTaskRequireTaskbarRounding(DEFAULT_DISPLAY)).isTrue()
    }

    @Test
    fun doesAnyTaskRequireTaskbarRounding_toggleResizeOfMaximizedTask_returnFalse() {
        val stableBounds = Rect().apply { displayLayout.getStableBounds(this) }
        val task1 = setUpFreeformTask(bounds = stableBounds, active = true)

        val argumentCaptor = argumentCaptor<Boolean>()
        controller.toggleDesktopTaskSize(
            task1,
            ToggleTaskSizeInteraction(
                ToggleTaskSizeInteraction.Direction.RESTORE,
                ToggleTaskSizeInteraction.Source.HEADER_BUTTON_TO_RESTORE,
                InputMethod.TOUCH,
            ),
        )

        verify(taskbarDesktopTaskListener).onTaskbarCornerRoundingUpdate(argumentCaptor.capture())
        verify(desktopModeEventLogger, times(1))
            .logTaskResizingEnded(
                eq(ResizeTrigger.MAXIMIZE_BUTTON),
                eq(InputMethod.TOUCH),
                eq(task1),
                anyOrNull(),
                anyOrNull(),
                eq(displayController),
                anyOrNull(),
            )
        assertThat(argumentCaptor.firstValue).isFalse()
    }

    @Test
    fun doesAnyTaskRequireTaskbarRounding_splitScreenTaskIsRunning_returnTrue() {
        val stableBounds = Rect().apply { displayLayout.getStableBounds(this) }
        setUpFreeformTask(
            bounds = Rect(stableBounds.left, stableBounds.top, 500, stableBounds.bottom)
        )

        assertThat(controller.doesAnyTaskRequireTaskbarRounding(DEFAULT_DISPLAY)).isTrue()
    }

    @Test
    fun instantiate_cannotEnterDesktopMode_doNotAddInitCallback() {
        whenever(DesktopModeStatus.canEnterDesktopMode(context)).thenReturn(false)
        clearInvocations(shellInit)

        createController()

        verify(shellInit, never()).addInitCallback(any(), any<DesktopTasksController>())
    }

    @Test
    @DisableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun showDesktopApps_allAppsInvisible_bringsToFront_desktopWallpaperDisabled() {
        val homeTask = setUpHomeTask()
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        markTaskHidden(task1)
        markTaskHidden(task2)

        controller.showDesktopApps(DEFAULT_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        assertThat(wct.hierarchyOps).hasSize(3)
        // Expect order to be from bottom: home, task1, task2
        wct.assertReorderAt(index = 0, homeTask)
        wct.assertReorderAt(index = 1, task1)
        wct.assertReorderAt(index = 2, task2)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun showDesktopApps_allAppsInvisible_bringsToFront_desktopWallpaperEnabled() {
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        markTaskHidden(task1)
        markTaskHidden(task2)

        controller.showDesktopApps(DEFAULT_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        assertThat(wct.hierarchyOps).hasSize(3)
        // Expect order to be from bottom: wallpaper intent, task1, task2
        wct.assertPendingIntentAt(index = 0, desktopWallpaperIntent)
        wct.assertReorderAt(index = 1, task1)
        wct.assertReorderAt(index = 2, task2)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun showDesktopApps_deskInactive_bringsToFront_multipleDesksEnabled() {
        whenever(transitions.startTransition(eq(TRANSIT_TO_FRONT), any(), anyOrNull()))
            .thenReturn(Binder())
        val deskId = 0
        // Make desk inactive by activating another desk.
        taskRepository.addDesk(DEFAULT_DISPLAY, deskId = 1)
        taskRepository.setActiveDesk(DEFAULT_DISPLAY, deskId = 1)

        controller.activateDesk(deskId, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        // Wallpaper is moved to front.
        wct.assertReorderAt(index = 0, wallpaperToken)
        // Desk is activated.
        verify(desksOrganizer).activateDesk(wct, deskId)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun isDesktopModeShowing_noTasks_returnsFalse() {
        assertThat(controller.isDesktopModeShowing(displayId = 0)).isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun isDesktopModeShowing_noTasksVisible_returnsFalse() {
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        markTaskHidden(task1)
        markTaskHidden(task2)

        assertThat(controller.isDesktopModeShowing(displayId = 0)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun isDesktopModeShowing_noActiveDesk_returnsFalse() {
        taskRepository.setDeskInactive(deskId = 0)

        assertThat(controller.isDesktopModeShowing(displayId = 0)).isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun isDesktopModeShowing_tasksActiveAndVisible_returnsTrue() {
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        markTaskVisible(task1)
        markTaskHidden(task2)

        assertThat(controller.isDesktopModeShowing(displayId = 0)).isTrue()
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY,
        Flags.FLAG_INCLUDE_TOP_TRANSPARENT_FULLSCREEN_TASK_IN_DESKTOP_HEURISTIC,
    )
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun isDesktopModeShowing_topTransparentFullscreenTask_returnsTrue() {
        val topTransparentTask = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)
        taskRepository.setTopTransparentFullscreenTaskId(DEFAULT_DISPLAY, topTransparentTask.taskId)

        assertThat(controller.isDesktopModeShowing(displayId = DEFAULT_DISPLAY)).isTrue()
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY,
        Flags.FLAG_INCLUDE_TOP_TRANSPARENT_FULLSCREEN_TASK_IN_DESKTOP_HEURISTIC,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun isDesktopModeShowing_deskInactive_topTransparentFullscreenTask_returnsTrue() {
        val topTransparentTask = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)
        taskRepository.setTopTransparentFullscreenTaskId(DEFAULT_DISPLAY, topTransparentTask.taskId)
        taskRepository.setDeskInactive(deskId = 0)

        assertThat(controller.isDesktopModeShowing(displayId = DEFAULT_DISPLAY)).isTrue()
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_PER_DISPLAY_DESKTOP_WALLPAPER_ACTIVITY,
    )
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun showDesktopApps_onSecondaryDisplay_desktopWallpaperEnabled_perDisplayWallpaperEnabled_bringsTasksToFront() {
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        setUpHomeTask(SECOND_DISPLAY)
        val task1 = setUpFreeformTask(SECOND_DISPLAY)
        val task2 = setUpFreeformTask(SECOND_DISPLAY)
        markTaskHidden(task1)
        markTaskHidden(task2)

        assertThat(taskRepository.getExpandedTasksOrdered(SECOND_DISPLAY)).contains(task1.taskId)
        assertThat(taskRepository.getExpandedTasksOrdered(SECOND_DISPLAY)).contains(task2.taskId)
        controller.showDesktopApps(SECOND_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        wct.assertReorder(task1)
        wct.assertReorder(task2)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_PER_DISPLAY_DESKTOP_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun showDesktopApps_onSecondaryDisplay_desktopWallpaperEnabled_perDisplayWallpaperEnabled_multipleDesksEnabled_bringsDeskToFront() {
        whenever(transitions.startTransition(eq(TRANSIT_TO_FRONT), any(), anyOrNull()))
            .thenReturn(Binder())
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = 2)
        setUpHomeTask(SECOND_DISPLAY)

        controller.showDesktopApps(SECOND_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        verify(desksOrganizer).activateDesk(wct, deskId = 2)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_PER_DISPLAY_DESKTOP_WALLPAPER_ACTIVITY,
    )
    fun showDesktopApps_onSecondaryDisplay_desktopWallpaperEnabled_perDisplayWallpaperEnabled_shouldShowWallpaper() {
        whenever(transitions.startTransition(eq(TRANSIT_TO_FRONT), any(), anyOrNull()))
            .thenReturn(Binder())
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        setUpHomeTask(SECOND_DISPLAY)

        controller.showDesktopApps(SECOND_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        wct.assertPendingIntent(desktopWallpaperIntent)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    @DisableFlags(Flags.FLAG_ENABLE_PER_DISPLAY_DESKTOP_WALLPAPER_ACTIVITY)
    fun showDesktopApps_onSecondaryDisplay_desktopWallpaperEnabled_shouldNotShowWallpaper() {
        whenever(transitions.startTransition(eq(TRANSIT_TO_FRONT), any(), anyOrNull()))
            .thenReturn(Binder())
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        val homeTask = setUpHomeTask(SECOND_DISPLAY)

        controller.showDesktopApps(SECOND_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        wct.assertWithoutPendingIntent(desktopWallpaperIntent)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun showDesktopApps_appsAlreadyVisible_bringsToFront_desktopWallpaperDisabled() {
        val homeTask = setUpHomeTask()
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        markTaskVisible(task1)
        markTaskVisible(task2)

        controller.showDesktopApps(DEFAULT_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        assertThat(wct.hierarchyOps).hasSize(3)
        // Expect order to be from bottom: home, task1, task2
        wct.assertReorderAt(index = 0, homeTask)
        wct.assertReorderAt(index = 1, task1)
        wct.assertReorderAt(index = 2, task2)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun showDesktopApps_onSecondaryDisplay_desktopWallpaperDisabled_shouldNotMoveLauncher() {
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        val homeTask = setUpHomeTask(SECOND_DISPLAY)
        val task1 = setUpFreeformTask(SECOND_DISPLAY)
        val task2 = setUpFreeformTask(SECOND_DISPLAY)
        markTaskHidden(task1)
        markTaskHidden(task2)

        controller.showDesktopApps(SECOND_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        assertThat(wct.hierarchyOps).hasSize(3)
        // Expect order to be from bottom: home, task1, task2
        wct.assertReorderAt(index = 0, homeTask)
        wct.assertReorderAt(index = 1, task1)
        wct.assertReorderAt(index = 2, task2)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun showDesktopApps_appsAlreadyVisible_bringsToFront_desktopWallpaperEnabled() {
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        markTaskVisible(task1)
        markTaskVisible(task2)

        controller.showDesktopApps(DEFAULT_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        assertThat(wct.hierarchyOps).hasSize(3)
        // Expect order to be from bottom: wallpaper intent, task1, task2
        wct.assertPendingIntentAt(index = 0, desktopWallpaperIntent)
        wct.assertReorderAt(index = 1, task1)
        wct.assertReorderAt(index = 2, task2)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun showDesktopApps_someAppsInvisible_reordersAll_desktopWallpaperDisabled() {
        val homeTask = setUpHomeTask()
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        markTaskHidden(task1)
        markTaskVisible(task2)

        controller.showDesktopApps(DEFAULT_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        assertThat(wct.hierarchyOps).hasSize(3)
        // Expect order to be from bottom: home, task1, task2
        wct.assertReorderAt(index = 0, homeTask)
        wct.assertReorderAt(index = 1, task1)
        wct.assertReorderAt(index = 2, task2)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun showDesktopApps_someAppsInvisible_desktopWallpaperEnabled_reordersOnlyFreeformTasks() {
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        markTaskHidden(task1)
        markTaskVisible(task2)

        controller.showDesktopApps(DEFAULT_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        assertThat(wct.hierarchyOps).hasSize(3)
        // Expect order to be from bottom: wallpaper intent, task1, task2
        wct.assertPendingIntentAt(index = 0, desktopWallpaperIntent)
        wct.assertReorderAt(index = 1, task1)
        wct.assertReorderAt(index = 2, task2)
    }

    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun showDesktopApps_someAppsInvisible_desktopWallpaperEnabled_reordersAll() {
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        markTaskHidden(task1)
        markTaskVisible(task2)

        controller.showDesktopApps(DEFAULT_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        assertThat(wct.hierarchyOps).hasSize(3)
        // Expect order to be from bottom: wallpaper intent, task1, task2
        wct.assertReorderAt(index = 0, wallpaperToken)
        wct.assertReorderAt(index = 1, task1)
        wct.assertReorderAt(index = 2, task2)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun showDesktopApps_noActiveTasks_reorderHomeToTop_desktopWallpaperDisabled() {
        val homeTask = setUpHomeTask()

        controller.showDesktopApps(DEFAULT_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        assertThat(wct.hierarchyOps).hasSize(1)
        wct.assertReorderAt(index = 0, homeTask)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun showDesktopApps_noActiveTasks_desktopWallpaperEnabled_addsDesktopWallpaper() {
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)

        controller.showDesktopApps(DEFAULT_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        wct.assertPendingIntentAt(index = 0, desktopWallpaperIntent)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun showDesktopApps_noActiveTasks_desktopWallpaperEnabled_reordersDesktopWallpaper() {
        controller.showDesktopApps(DEFAULT_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        wct.assertReorderAt(index = 0, wallpaperToken)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun showDesktopApps_twoDisplays_bringsToFrontOnlyOneDisplay_desktopWallpaperDisabled() {
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        val homeTaskDefaultDisplay = setUpHomeTask(DEFAULT_DISPLAY)
        val taskDefaultDisplay = setUpFreeformTask(DEFAULT_DISPLAY)
        setUpHomeTask(SECOND_DISPLAY)
        val taskSecondDisplay = setUpFreeformTask(SECOND_DISPLAY)
        markTaskHidden(taskDefaultDisplay)
        markTaskHidden(taskSecondDisplay)

        controller.showDesktopApps(DEFAULT_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        assertThat(wct.hierarchyOps).hasSize(2)
        // Expect order to be from bottom: home, task
        wct.assertReorderAt(index = 0, homeTaskDefaultDisplay)
        wct.assertReorderAt(index = 1, taskDefaultDisplay)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun showDesktopApps_twoDisplays_bringsToFrontOnlyOneDisplay_desktopWallpaperEnabled() {
        whenever(transitions.startTransition(eq(TRANSIT_TO_FRONT), any(), anyOrNull()))
            .thenReturn(Binder())
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        val homeTaskDefaultDisplay = setUpHomeTask(DEFAULT_DISPLAY)
        val taskDefaultDisplay = setUpFreeformTask(DEFAULT_DISPLAY)
        setUpHomeTask(SECOND_DISPLAY)
        val taskSecondDisplay = setUpFreeformTask(SECOND_DISPLAY)
        markTaskHidden(taskDefaultDisplay)
        markTaskHidden(taskSecondDisplay)

        controller.showDesktopApps(DEFAULT_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        // Move home to front
        wct.assertReorderAt(index = 0, homeTaskDefaultDisplay)
        // Add desktop wallpaper activity
        wct.assertPendingIntentAt(index = 1, desktopWallpaperIntent)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun showDesktopApps_twoDisplays_bringsToFrontOnlyOneDisplayTasks_desktopWallpaperEnabled_multiDesksDisabled() {
        whenever(transitions.startTransition(eq(TRANSIT_TO_FRONT), any(), anyOrNull()))
            .thenReturn(Binder())
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        val homeTaskDefaultDisplay = setUpHomeTask(DEFAULT_DISPLAY)
        val taskDefaultDisplay = setUpFreeformTask(DEFAULT_DISPLAY)
        setUpHomeTask(SECOND_DISPLAY)
        val taskSecondDisplay = setUpFreeformTask(SECOND_DISPLAY)
        markTaskHidden(taskDefaultDisplay)
        markTaskHidden(taskSecondDisplay)

        controller.showDesktopApps(DEFAULT_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        // Move freeform task to front
        wct.assertReorderAt(index = 2, taskDefaultDisplay)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun showDesktopApps_twoDisplays_bringsToFrontOnlyOneDisplayTasks_desktopWallpaperEnabled_multiDesksEnabled() {
        whenever(transitions.startTransition(eq(TRANSIT_TO_FRONT), any(), anyOrNull()))
            .thenReturn(Binder())
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        val homeTaskDefaultDisplay = setUpHomeTask(DEFAULT_DISPLAY)
        val taskDefaultDisplay = setUpFreeformTask(DEFAULT_DISPLAY)
        setUpHomeTask(SECOND_DISPLAY)
        val taskSecondDisplay = setUpFreeformTask(SECOND_DISPLAY)
        markTaskHidden(taskDefaultDisplay)
        markTaskHidden(taskSecondDisplay)

        controller.showDesktopApps(DEFAULT_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        // Move desktop tasks to front
        verify(desksOrganizer).activateDesk(wct, deskId = DEFAULT_DISPLAY)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun showDesktopApps_desktopWallpaperDisabled_dontReorderMinimizedTask() {
        val homeTask = setUpHomeTask()
        val freeformTask = setUpFreeformTask()
        val minimizedTask = setUpFreeformTask()

        markTaskHidden(freeformTask)
        markTaskHidden(minimizedTask)
        taskRepository.minimizeTask(DEFAULT_DISPLAY, minimizedTask.taskId)
        controller.showDesktopApps(DEFAULT_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        assertThat(wct.hierarchyOps).hasSize(2)
        // Reorder home and freeform task to top, don't reorder the minimized task
        wct.assertReorderAt(index = 0, homeTask, toTop = true)
        wct.assertReorderAt(index = 1, freeformTask, toTop = true)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    /** TODO: b/362720497 - add multi-desk version when minimization is implemented. */
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun showDesktopApps_desktopWallpaperEnabled_dontReorderMinimizedTask() {
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)
        val homeTask = setUpHomeTask()
        val freeformTask = setUpFreeformTask()
        val minimizedTask = setUpFreeformTask()

        markTaskHidden(freeformTask)
        markTaskHidden(minimizedTask)
        taskRepository.minimizeTask(DEFAULT_DISPLAY, minimizedTask.taskId)
        controller.showDesktopApps(DEFAULT_DISPLAY, RemoteTransition(TestRemoteTransition()))

        val wct =
            getLatestWct(type = TRANSIT_TO_FRONT, handlerClass = OneShotRemoteHandler::class.java)
        assertThat(wct.hierarchyOps).hasSize(3)
        // Move home to front
        wct.assertReorderAt(index = 0, homeTask, toTop = true)
        // Add desktop wallpaper activity
        wct.assertPendingIntentAt(index = 1, desktopWallpaperIntent)
        // Reorder freeform task to top, don't reorder the minimized task
        wct.assertReorderAt(index = 2, freeformTask, toTop = true)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun isAnyDeskActive_noTasks_returnsFalse() {
        assertThat(controller.isAnyDeskActive(DEFAULT_DISPLAY)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun isAnyDeskActive_noActiveDesk_returnsFalse() {
        taskRepository.setDeskInactive(deskId = 0)

        assertThat(controller.isAnyDeskActive(DEFAULT_DISPLAY)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun isAnyDeskActive_withActiveDesk_returnsTrue() {
        taskRepository.setActiveDesk(displayId = DEFAULT_DISPLAY, deskId = 0)

        assertThat(controller.isAnyDeskActive(DEFAULT_DISPLAY)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun isAnyDeskActive_twoTasks_bothVisible_returnsTrue() {
        setUpHomeTask()

        setUpFreeformTask().also(::markTaskVisible)
        setUpFreeformTask().also(::markTaskVisible)

        assertThat(controller.isAnyDeskActive(DEFAULT_DISPLAY)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun isInDesktop_twoTasks_oneVisible_returnsTrue() {
        setUpHomeTask()

        setUpFreeformTask().also(::markTaskVisible)
        setUpFreeformTask().also(::markTaskHidden)

        assertThat(controller.isAnyDeskActive(DEFAULT_DISPLAY)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun isAnyDeskActive_twoTasksVisibleOnDifferentDisplays_returnsTrue() {
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        taskRepository.setActiveDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        setUpHomeTask()

        setUpFreeformTask(DEFAULT_DISPLAY).also(::markTaskVisible)
        setUpFreeformTask(SECOND_DISPLAY).also(::markTaskVisible)

        assertThat(controller.isAnyDeskActive(SECOND_DISPLAY)).isTrue()
    }

    @Test
    fun addMoveToDeskTaskChanges_gravityLeft_noBoundsApplied() {
        setUpLandscapeDisplay()
        val task = setUpFullscreenTask(gravity = Gravity.LEFT)
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        val finalBounds = findBoundsChange(wct, task)
        assertThat(finalBounds).isNull()
    }

    @Test
    fun addMoveToDeskTaskChanges_gravityRight_noBoundsApplied() {
        setUpLandscapeDisplay()
        val task = setUpFullscreenTask(gravity = Gravity.RIGHT)
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        val finalBounds = findBoundsChange(wct, task)
        assertThat(finalBounds).isNull()
    }

    @Test
    fun addMoveToDeskTaskChanges_gravityTop_noBoundsApplied() {
        setUpLandscapeDisplay()
        val task = setUpFullscreenTask(gravity = Gravity.TOP)
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        val finalBounds = findBoundsChange(wct, task)
        assertThat(finalBounds).isNull()
    }

    @Test
    fun addMoveToDeskTaskChanges_gravityBottom_noBoundsApplied() {
        setUpLandscapeDisplay()
        val task = setUpFullscreenTask(gravity = Gravity.BOTTOM)
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        val finalBounds = findBoundsChange(wct, task)
        assertThat(finalBounds).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_INHERIT_TASK_BOUNDS_FOR_TRAMPOLINE_TASK_LAUNCHES)
    fun addMoveToDeskTaskChanges_newTaskInstance_inheritsClosingInstanceBounds() {
        // Setup existing task.
        val existingTask = setUpFreeformTask(active = true)
        val testComponent = ComponentName(/* package */ "test.package", /* class */ "test.class")
        existingTask.topActivity = testComponent
        existingTask.configuration.windowConfiguration.setBounds(Rect(0, 0, 500, 500))
        // Set up new instance of already existing task.
        val launchingTask =
            setUpFullscreenTask().apply {
                topActivityInfo = ActivityInfo().apply { launchMode = LAUNCH_SINGLE_INSTANCE }
            }
        launchingTask.topActivity = testComponent

        // Move new instance to desktop. By default multi instance is not supported so first
        // instance will close.
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, launchingTask, deskId = 0)

        // New instance should inherit task bounds of old instance.
        assertThat(findBoundsChange(wct, launchingTask))
            .isEqualTo(existingTask.configuration.windowConfiguration.bounds)
    }

    @Test
    @EnableFlags(Flags.FLAG_INHERIT_TASK_BOUNDS_FOR_TRAMPOLINE_TASK_LAUNCHES)
    fun handleRequest_newTaskInstance_inheritsClosingInstanceBounds() {
        setUpLandscapeDisplay()
        // Setup existing task.
        val existingTask = setUpFreeformTask(active = true)
        val testComponent = ComponentName(/* package */ "test.package", /* class */ "test.class")
        existingTask.topActivity = testComponent
        existingTask.configuration.windowConfiguration.setBounds(Rect(0, 0, 500, 500))
        // Set up new instance of already existing task.
        val launchingTask =
            setUpFreeformTask(active = false).apply {
                topActivityInfo = ActivityInfo().apply { launchMode = LAUNCH_SINGLE_INSTANCE }
            }
        taskRepository.removeTask(launchingTask.displayId, launchingTask.taskId)
        launchingTask.topActivity = testComponent

        // Move new instance to desktop. By default multi instance is not supported so first
        // instance will close.
        val wct = controller.handleRequest(Binder(), createTransition(launchingTask))

        assertNotNull(wct, "should handle request")
        val finalBounds = findBoundsChange(wct, launchingTask)
        // New instance should inherit task bounds of old instance.
        assertThat(finalBounds).isEqualTo(existingTask.configuration.windowConfiguration.bounds)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CASCADING_WINDOWS)
    fun handleRequest_newFreeformTaskLaunch_cascadeApplied() {
        setUpLandscapeDisplay()
        val stableBounds = Rect()
        displayLayout.getStableBoundsForDesktopMode(stableBounds)

        setUpFreeformTask(bounds = DEFAULT_LANDSCAPE_BOUNDS)
        val freeformTask = setUpFreeformTask(bounds = DEFAULT_LANDSCAPE_BOUNDS, active = false)

        val wct = controller.handleRequest(Binder(), createTransition(freeformTask))

        assertNotNull(wct, "should handle request")
        val finalBounds = findBoundsChange(wct, freeformTask)
        assertThat(stableBounds.getDesktopTaskPosition(finalBounds!!))
            .isEqualTo(DesktopTaskPosition.BottomRight)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CASCADING_WINDOWS)
    fun handleRequest_freeformTaskAlreadyExistsInDesktopMode_cascadeNotApplied() {
        setUpLandscapeDisplay()
        val stableBounds = Rect()
        displayLayout.getStableBoundsForDesktopMode(stableBounds)

        setUpFreeformTask(bounds = DEFAULT_LANDSCAPE_BOUNDS)
        val freeformTask = setUpFreeformTask(bounds = DEFAULT_LANDSCAPE_BOUNDS)

        val wct = controller.handleRequest(Binder(), createTransition(freeformTask))

        assertNull(wct, "should not handle request")
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CASCADING_WINDOWS)
    fun addMoveToDeskTaskChanges_positionBottomRight() {
        setUpLandscapeDisplay()
        val stableBounds = Rect()
        displayLayout.getStableBoundsForDesktopMode(stableBounds)

        setUpFreeformTask(bounds = DEFAULT_LANDSCAPE_BOUNDS)

        val task = setUpFullscreenTask()
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        val finalBounds = findBoundsChange(wct, task)
        assertThat(stableBounds.getDesktopTaskPosition(finalBounds!!))
            .isEqualTo(DesktopTaskPosition.BottomRight)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CASCADING_WINDOWS)
    fun addMoveToDeskTaskChanges_positionTopLeft() {
        setUpLandscapeDisplay()
        val stableBounds = Rect()
        displayLayout.getStableBoundsForDesktopMode(stableBounds)

        addFreeformTaskAtPosition(DesktopTaskPosition.BottomRight, stableBounds)

        val task = setUpFullscreenTask()
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        val finalBounds = findBoundsChange(wct, task)
        assertThat(stableBounds.getDesktopTaskPosition(finalBounds!!))
            .isEqualTo(DesktopTaskPosition.TopLeft)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CASCADING_WINDOWS)
    fun addMoveToDeskTaskChanges_positionBottomLeft() {
        setUpLandscapeDisplay()
        val stableBounds = Rect()
        displayLayout.getStableBoundsForDesktopMode(stableBounds)

        addFreeformTaskAtPosition(DesktopTaskPosition.TopLeft, stableBounds)

        val task = setUpFullscreenTask()
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        val finalBounds = findBoundsChange(wct, task)
        assertThat(stableBounds.getDesktopTaskPosition(finalBounds!!))
            .isEqualTo(DesktopTaskPosition.BottomLeft)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CASCADING_WINDOWS)
    fun addMoveToDeskTaskChanges_positionTopRight() {
        setUpLandscapeDisplay()
        val stableBounds = Rect()
        displayLayout.getStableBoundsForDesktopMode(stableBounds)

        addFreeformTaskAtPosition(DesktopTaskPosition.BottomLeft, stableBounds)

        val task = setUpFullscreenTask()
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        val finalBounds = findBoundsChange(wct, task)
        assertThat(stableBounds.getDesktopTaskPosition(finalBounds!!))
            .isEqualTo(DesktopTaskPosition.TopRight)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CASCADING_WINDOWS)
    fun addMoveToDeskTaskChanges_positionResetsToCenter() {
        setUpLandscapeDisplay()
        val stableBounds = Rect()
        displayLayout.getStableBoundsForDesktopMode(stableBounds)

        addFreeformTaskAtPosition(DesktopTaskPosition.TopRight, stableBounds)

        val task = setUpFullscreenTask()
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        val finalBounds = findBoundsChange(wct, task)
        assertThat(stableBounds.getDesktopTaskPosition(finalBounds!!))
            .isEqualTo(DesktopTaskPosition.Center)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CASCADING_WINDOWS)
    fun addMoveToDeskTaskChanges_lastWindowSnapLeft_positionResetsToCenter() {
        setUpLandscapeDisplay()
        val stableBounds = Rect()
        displayLayout.getStableBoundsForDesktopMode(stableBounds)

        // Add freeform task with half display size snap bounds at left side.
        setUpFreeformTask(
            bounds = Rect(stableBounds.left, stableBounds.top, 500, stableBounds.bottom)
        )

        val task = setUpFullscreenTask()
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        val finalBounds = findBoundsChange(wct, task)
        assertThat(stableBounds.getDesktopTaskPosition(finalBounds!!))
            .isEqualTo(DesktopTaskPosition.Center)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CASCADING_WINDOWS)
    fun addMoveToDeskTaskChanges_lastWindowSnapRight_positionResetsToCenter() {
        setUpLandscapeDisplay()
        val stableBounds = Rect()
        displayLayout.getStableBoundsForDesktopMode(stableBounds)

        // Add freeform task with half display size snap bounds at right side.
        setUpFreeformTask(
            bounds =
                Rect(
                    stableBounds.right - 500,
                    stableBounds.top,
                    stableBounds.right,
                    stableBounds.bottom,
                )
        )

        val task = setUpFullscreenTask()
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        val finalBounds = findBoundsChange(wct, task)
        assertThat(stableBounds.getDesktopTaskPosition(finalBounds!!))
            .isEqualTo(DesktopTaskPosition.Center)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CASCADING_WINDOWS)
    fun addMoveToDeskTaskChanges_lastWindowMaximised_positionResetsToCenter() {
        setUpLandscapeDisplay()
        val stableBounds = Rect()
        displayLayout.getStableBoundsForDesktopMode(stableBounds)

        // Add maximised freeform task.
        setUpFreeformTask(bounds = Rect(stableBounds))

        val task = setUpFullscreenTask()
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        val finalBounds = findBoundsChange(wct, task)
        assertThat(stableBounds.getDesktopTaskPosition(finalBounds!!))
            .isEqualTo(DesktopTaskPosition.Center)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CASCADING_WINDOWS)
    fun addMoveToDeskTaskChanges_defaultToCenterIfFree() {
        setUpLandscapeDisplay()
        val stableBounds = Rect()
        displayLayout.getStableBoundsForDesktopMode(stableBounds)

        val minTouchTarget =
            context.resources.getDimensionPixelSize(
                R.dimen.freeform_required_visible_empty_space_in_header
            )
        addFreeformTaskAtPosition(
            DesktopTaskPosition.Center,
            stableBounds,
            Rect(0, 0, 1600, 1200),
            Point(0, minTouchTarget + 1),
        )

        val task = setUpFullscreenTask()
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        val finalBounds = findBoundsChange(wct, task)
        assertThat(stableBounds.getDesktopTaskPosition(finalBounds!!))
            .isEqualTo(DesktopTaskPosition.Center)
    }

    @Test
    fun addMoveToDeskTaskChanges_excludeCaptionFromAppBounds_nonResizableLandscape() {
        setUpLandscapeDisplay()
        val task =
            setUpFullscreenTask(
                isResizable = false,
                screenOrientation = SCREEN_ORIENTATION_LANDSCAPE,
            )
        whenever(desktopModeCompatPolicy.shouldExcludeCaptionFromAppBounds(task)).thenReturn(true)
        val initialAspectRatio = calculateAspectRatio(task)
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        val finalBounds = findBoundsChange(wct, task)
        val captionInsets = getDesktopViewAppHeaderHeightPx(context)
        finalBounds!!.top += captionInsets
        val finalAspectRatio =
            maxOf(finalBounds.height(), finalBounds.width()) /
                minOf(finalBounds.height(), finalBounds.width()).toFloat()
        assertThat(finalAspectRatio).isWithin(FLOAT_TOLERANCE).of(initialAspectRatio)
    }

    @Test
    fun addMoveToDeskTaskChanges_excludeCaptionFromAppBounds_nonResizablePortrait() {
        setUpLandscapeDisplay()
        val task =
            setUpFullscreenTask(
                isResizable = false,
                screenOrientation = SCREEN_ORIENTATION_PORTRAIT,
            )
        whenever(desktopModeCompatPolicy.shouldExcludeCaptionFromAppBounds(task)).thenReturn(true)
        val initialAspectRatio = calculateAspectRatio(task)
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        val finalBounds = findBoundsChange(wct, task)
        val captionInsets = getDesktopViewAppHeaderHeightPx(context)
        finalBounds!!.top += captionInsets
        val finalAspectRatio =
            maxOf(finalBounds.height(), finalBounds.width()) /
                minOf(finalBounds.height(), finalBounds.width()).toFloat()
        assertThat(finalAspectRatio).isWithin(FLOAT_TOLERANCE).of(initialAspectRatio)
    }

    @Test
    fun launchIntent_taskInDesktopMode_transitionStarted() {
        setUpLandscapeDisplay()
        val freeformTask = setUpFreeformTask()
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    eq(TRANSIT_OPEN),
                    any(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Binder())

        controller.startLaunchIntentTransition(
            freeformTask.baseIntent,
            Bundle.EMPTY,
            DEFAULT_DISPLAY,
        )

        val wct = getLatestDesktopMixedTaskWct(type = TRANSIT_OPEN)
        assertThat(wct.hierarchyOps).hasSize(1)
    }

    @Test
    fun launchIntent_taskInDesktopMode_onSecondaryDisplay_transitionStarted() {
        setUpLandscapeDisplay()
        taskRepository.addDesk(SECOND_DISPLAY, deskId = 2)
        val intent = Intent().setComponent(homeComponentName)
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    eq(TRANSIT_OPEN),
                    any(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Binder())

        controller.startLaunchIntentTransition(intent, Bundle.EMPTY, SECOND_DISPLAY)

        val wct = getLatestDesktopMixedTaskWct(type = TRANSIT_OPEN)
        // We expect two actions: open the app and start the desk
        assertThat(wct.hierarchyOps).hasSize(2)
        val hOps0 = wct.hierarchyOps[0]
        val hOps1 = wct.hierarchyOps[1]
        assertThat(hOps0.type).isEqualTo(HIERARCHY_OP_TYPE_PENDING_INTENT)
        val activityOptions0 = ActivityOptions.fromBundle(hOps0.launchOptions)
        assertThat(activityOptions0.launchDisplayId).isEqualTo(SECOND_DISPLAY)
        assertThat(hOps1.type).isEqualTo(HIERARCHY_OP_TYPE_PENDING_INTENT)
        val activityOptions1 = ActivityOptions.fromBundle(hOps1.launchOptions)
        assertThat(activityOptions1.launchDisplayId).isEqualTo(SECOND_DISPLAY)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS)
    fun addMoveToDeskTaskChanges_landscapeDevice_userFullscreenOverride_defaultPortraitBounds() {
        setUpLandscapeDisplay()
        val task = setUpFullscreenTask(enableUserFullscreenOverride = true)
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        assertThat(findBoundsChange(wct, task)).isEqualTo(DEFAULT_LANDSCAPE_BOUNDS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS)
    fun addMoveToDeskTaskChanges_landscapeDevice_systemFullscreenOverride_defaultPortraitBounds() {
        setUpLandscapeDisplay()
        val task = setUpFullscreenTask(enableSystemFullscreenOverride = true)
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        assertThat(findBoundsChange(wct, task)).isEqualTo(DEFAULT_LANDSCAPE_BOUNDS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS)
    fun addMoveToDeskTaskChanges_landscapeDevice_portraitResizableApp_aspectRatioOverridden() {
        setUpLandscapeDisplay()
        val task =
            setUpFullscreenTask(
                screenOrientation = SCREEN_ORIENTATION_PORTRAIT,
                shouldLetterbox = true,
                aspectRatioOverrideApplied = true,
            )
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        assertThat(findBoundsChange(wct, task)).isEqualTo(UNRESIZABLE_PORTRAIT_BOUNDS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS)
    fun addMoveToDeskTaskChanges_portraitDevice_userFullscreenOverride_defaultPortraitBounds() {
        setUpPortraitDisplay()
        val task = setUpFullscreenTask(enableUserFullscreenOverride = true)
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        assertThat(findBoundsChange(wct, task)).isEqualTo(DEFAULT_PORTRAIT_BOUNDS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS)
    fun addMoveToDeskTaskChanges_portraitDevice_systemFullscreenOverride_defaultPortraitBounds() {
        setUpPortraitDisplay()
        val task = setUpFullscreenTask(enableSystemFullscreenOverride = true)
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        assertThat(findBoundsChange(wct, task)).isEqualTo(DEFAULT_PORTRAIT_BOUNDS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS)
    fun addMoveToDeskTaskChanges_portraitDevice_landscapeResizableApp_aspectRatioOverridden() {
        setUpPortraitDisplay()
        val task =
            setUpFullscreenTask(
                screenOrientation = SCREEN_ORIENTATION_LANDSCAPE,
                deviceOrientation = ORIENTATION_PORTRAIT,
                shouldLetterbox = true,
                aspectRatioOverrideApplied = true,
            )
        val wct = WindowContainerTransaction()
        controller.addMoveToDeskTaskChanges(wct, task, deskId = 0)

        assertThat(findBoundsChange(wct, task)).isEqualTo(UNRESIZABLE_LANDSCAPE_BOUNDS)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveToDesktop_tdaFullscreen_windowingModeSetToFreeform() {
        val task = setUpFullscreenTask()
        val tda = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY)!!
        tda.configuration.windowConfiguration.windowingMode = WINDOWING_MODE_FULLSCREEN
        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)
        val wct = getLatestEnterDesktopWct()
        assertThat(wct.changes[task.token.asBinder()]?.windowingMode)
            .isEqualTo(WINDOWING_MODE_FREEFORM)
        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveRunningTaskToDesktop_tdaFreeform_windowingModeSetToUndefined() {
        val task = setUpFullscreenTask()
        val tda = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY)!!
        tda.configuration.windowConfiguration.windowingMode = WINDOWING_MODE_FREEFORM
        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)
        val wct = getLatestEnterDesktopWct()
        assertThat(wct.changes[task.token.asBinder()]?.windowingMode)
            .isEqualTo(WINDOWING_MODE_UNDEFINED)
        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveRunningTaskToDesktop_movesTaskToDefaultDesk() {
        val task = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)

        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)

        val wct = getLatestEnterDesktopWct()
        verify(desksOrganizer).moveTaskToDesk(wct, deskId = 0, task)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveRunningTaskToDesktop_activatesDesk() {
        val task = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)

        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)

        val wct = getLatestEnterDesktopWct()
        verify(desksOrganizer).activateDesk(wct, deskId = 0)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveRunningTaskToDesktop_triggersEnterDesktopListener() {
        val task = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)

        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)

        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveTaskToDesk_nonDefaultDesk_movesTaskToDesk() {
        val transition = Binder()
        whenever(enterDesktopTransitionHandler.moveToDesktop(any(), any())).thenReturn(transition)
        taskRepository.addDesk(DEFAULT_DISPLAY, deskId = 3)
        val task = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)
        task.isVisible = true

        controller.moveTaskToDesk(taskId = task.taskId, deskId = 3, transitionSource = UNKNOWN)

        val wct = getLatestEnterDesktopWct()
        verify(desksOrganizer).moveTaskToDesk(wct, deskId = 3, task)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveTaskToDesk_nonDefaultDesk_activatesDesk() {
        val transition = Binder()
        whenever(enterDesktopTransitionHandler.moveToDesktop(any(), any())).thenReturn(transition)
        taskRepository.addDesk(DEFAULT_DISPLAY, deskId = 3)
        val task = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)
        task.isVisible = true

        controller.moveTaskToDesk(taskId = task.taskId, deskId = 3, transitionSource = UNKNOWN)

        val wct = getLatestEnterDesktopWct()
        verify(desksOrganizer).activateDesk(wct, deskId = 3)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveTaskToDesk_nonDefaultDesk_triggersEnterDesktopListener() {
        taskRepository.addDesk(DEFAULT_DISPLAY, deskId = 3)
        val task = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)

        controller.moveTaskToDesk(taskId = task.taskId, deskId = 3, transitionSource = UNKNOWN)

        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun moveTaskToDesktop_desktopWallpaperDisabled_nonRunningTask_launchesInFreeform() {
        val task = createRecentTaskInfo(1)
        whenever(shellTaskOrganizer.getRunningTaskInfo(anyInt())).thenReturn(null)
        whenever(recentTasksController.findTaskInBackground(anyInt())).thenReturn(task)

        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)

        with(getLatestEnterDesktopWct()) {
            assertLaunchTaskAt(0, task.taskId, WINDOWING_MODE_FREEFORM)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun moveTaskToDesktop_desktopWallpaperEnabled_nonRunningTask_launchesInFreeform() {
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)
        val task = createRecentTaskInfo(1)
        whenever(shellTaskOrganizer.getRunningTaskInfo(anyInt())).thenReturn(null)
        whenever(recentTasksController.findTaskInBackground(anyInt())).thenReturn(task)

        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)

        with(getLatestEnterDesktopWct()) {
            // Add desktop wallpaper activity
            assertPendingIntentAt(index = 0, desktopWallpaperIntent)
            // Launch task
            assertLaunchTaskAt(index = 1, task.taskId, WINDOWING_MODE_FREEFORM)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveRunningTaskToDesktop_topActivityTranslucentWithoutDisplay_taskIsMovedToDesktop_multiDesksDisabled() {
        val task =
            setUpFullscreenTask().apply {
                isActivityStackTransparent = true
                isTopActivityNoDisplay = true
                numActivities = 1
            }

        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)
        val wct = getLatestEnterDesktopWct()
        assertThat(wct.changes[task.token.asBinder()]?.windowingMode)
            .isEqualTo(WINDOWING_MODE_FREEFORM)
        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun moveRunningTaskToDesktop_topActivityTranslucentWithoutDisplay_taskIsMovedToDesktop_multiDesksEnabled() {
        val task =
            setUpFullscreenTask().apply {
                isActivityStackTransparent = true
                isTopActivityNoDisplay = true
                numActivities = 1
            }

        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)
        val wct = getLatestEnterDesktopWct()
        verify(desksOrganizer).moveTaskToDesk(wct, deskId = 0, task = task)
        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY)
    @DisableFlags(Flags.FLAG_ENABLE_MODALS_FULLSCREEN_WITH_PERMISSION)
    fun moveRunningTaskToDesktop_topActivityTranslucentWithDisplay_doesNothing() {
        val task =
            setUpFullscreenTask().apply {
                isActivityStackTransparent = true
                isTopActivityNoDisplay = false
                numActivities = 1
            }

        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)
        verifyEnterDesktopWCTNotExecuted()
        verify(desktopModeEnterExitTransitionListener, times(0))
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY)
    fun moveRunningTaskToDesktop_systemUIActivityWithDisplay_doesNothing() {
        // Set task as systemUI package
        val systemUIPackageName =
            context.resources.getString(com.android.internal.R.string.config_systemUi)
        val baseComponent = ComponentName(systemUIPackageName, /* cls= */ "")
        val task =
            setUpFullscreenTask().apply {
                baseActivity = baseComponent
                isTopActivityNoDisplay = false
            }

        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)
        verifyEnterDesktopWCTNotExecuted()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveRunningTaskToDesktop_systemUIActivityWithoutDisplay_doesNothing_multiDesksDisabled() {
        // Set task as systemUI package
        val systemUIPackageName =
            context.resources.getString(com.android.internal.R.string.config_systemUi)
        val baseComponent = ComponentName(systemUIPackageName, /* cls= */ "")
        val task =
            setUpFullscreenTask().apply {
                baseActivity = baseComponent
                isTopActivityNoDisplay = true
            }

        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)

        val wct = getLatestEnterDesktopWct()
        assertThat(wct.changes[task.token.asBinder()]?.windowingMode)
            .isEqualTo(WINDOWING_MODE_FREEFORM)
    }

    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY)
    fun moveRunningTaskToDesktop_defaultHomePackageWithDisplay_doesNothing() {
        val packageManager: PackageManager = org.mockito.kotlin.mock()
        val homeActivities = ComponentName("defaultHomePackage", /* class */ "")
        val task =
            setUpFullscreenTask().apply {
                baseActivity = homeActivities
                isTopActivityNoDisplay = false
            }
        mContext.setMockPackageManager(packageManager)
        whenever(packageManager.getHomeActivities(any())).thenReturn(homeActivities)

        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)
        verifyEnterDesktopWCTNotExecuted()
    }

    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY)
    fun moveRunningTaskToDesktop_defaultHomePackageWithoutDisplay_doesNothing() {
        val packageManager: PackageManager = org.mockito.kotlin.mock()
        val homeActivities = ComponentName("defaultHomePackage", /* class */ "")
        val task =
            setUpFullscreenTask().apply {
                baseActivity = homeActivities
                isTopActivityNoDisplay = false
            }
        mContext.setMockPackageManager(packageManager)
        whenever(packageManager.getHomeActivities(any())).thenReturn(homeActivities)

        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)

        val wct = getLatestEnterDesktopWct()
        assertThat(wct.changes[task.token.asBinder()]?.windowingMode)
            .isEqualTo(WINDOWING_MODE_FREEFORM)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun moveRunningTaskToDesktop_systemUIActivityWithoutDisplay_doesNothing_multiDesksEnabled() {
        // Set task as systemUI package
        val systemUIPackageName =
            context.resources.getString(com.android.internal.R.string.config_systemUi)
        val baseComponent = ComponentName(systemUIPackageName, /* cls= */ "")
        val task =
            setUpFullscreenTask().apply {
                baseActivity = baseComponent
                isTopActivityNoDisplay = true
            }

        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)

        val wct = getLatestEnterDesktopWct()
        verify(desksOrganizer).moveTaskToDesk(wct, deskId = 0, task = task)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun moveBackgroundTaskToDesktop_remoteTransition_usesOneShotHandler() {
        val transitionHandlerArgCaptor = argumentCaptor<TransitionHandler>()
        whenever(transitions.startTransition(anyInt(), any(), transitionHandlerArgCaptor.capture()))
            .thenReturn(Binder())

        val task = createRecentTaskInfo(1)
        whenever(shellTaskOrganizer.getRunningTaskInfo(anyInt())).thenReturn(null)
        whenever(recentTasksController.findTaskInBackground(anyInt())).thenReturn(task)
        controller.moveTaskToDefaultDeskAndActivate(
            taskId = task.taskId,
            transitionSource = UNKNOWN,
            remoteTransition = RemoteTransition(spy(TestRemoteTransition())),
        )

        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
        assertIs<OneShotRemoteHandler>(transitionHandlerArgCaptor.firstValue)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_PER_DISPLAY_DESKTOP_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
    )
    fun moveBackgroundTaskToDesktop_nonDefaultDisplay_reordersHomeAndWallpaperOfNonDefaultDisplay() {
        val homeTask = setUpHomeTask(displayId = SECOND_DISPLAY)
        val wallpaperToken = MockToken().token()
        whenever(desktopWallpaperActivityTokenProvider.getToken(SECOND_DISPLAY))
            .thenReturn(wallpaperToken)
        taskRepository.addDesk(SECOND_DISPLAY, deskId = 2)
        val task = setUpFreeformTask(displayId = SECOND_DISPLAY, deskId = 2, background = true)

        controller.moveTaskToDefaultDeskAndActivate(
            taskId = task.taskId,
            transitionSource = UNKNOWN,
            remoteTransition = RemoteTransition(spy(TestRemoteTransition())),
        )

        val wct = getLatestTransition()
        val homeReorderIndex = wct.indexOfReorder(homeTask, toTop = true)
        val wallpaperReorderIndex = wct.indexOfReorder(wallpaperToken, toTop = true)
        assertThat(homeReorderIndex).isNotEqualTo(-1)
        assertThat(wallpaperReorderIndex).isNotEqualTo(-1)
        // Wallpaper last, to be in front of Home.
        assertThat(wallpaperReorderIndex).isGreaterThan(homeReorderIndex)
    }

    @Test
    fun moveRunningTaskToDesktop_remoteTransition_usesOneShotHandler() {
        val transitionHandlerArgCaptor = argumentCaptor<TransitionHandler>()
        whenever(transitions.startTransition(anyInt(), any(), transitionHandlerArgCaptor.capture()))
            .thenReturn(Binder())

        controller.moveTaskToDefaultDeskAndActivate(
            taskId = setUpFullscreenTask().taskId,
            transitionSource = UNKNOWN,
            remoteTransition = RemoteTransition(spy(TestRemoteTransition())),
        )

        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
        assertIs<OneShotRemoteHandler>(transitionHandlerArgCaptor.firstValue)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun moveRunningTaskToDesktop_otherFreeformTasksBroughtToFront_desktopWallpaperDisabled() {
        val homeTask = setUpHomeTask()
        val freeformTask = setUpFreeformTask()
        val fullscreenTask = setUpFullscreenTask()
        markTaskHidden(freeformTask)

        controller.moveTaskToDefaultDeskAndActivate(
            fullscreenTask.taskId,
            transitionSource = UNKNOWN,
        )

        with(getLatestEnterDesktopWct()) {
            // Operations should include home task, freeform task
            assertThat(hierarchyOps).hasSize(3)
            assertReorderSequence(homeTask, freeformTask, fullscreenTask)
            assertThat(changes[fullscreenTask.token.asBinder()]?.windowingMode)
                .isEqualTo(WINDOWING_MODE_FREEFORM)
        }
        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveRunningTaskToDesktop_otherFreeformTasksBroughtToFront_desktopWallpaperEnabled() {
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)
        val freeformTask = setUpFreeformTask()
        val fullscreenTask = setUpFullscreenTask()
        markTaskHidden(freeformTask)

        controller.moveTaskToDefaultDeskAndActivate(
            fullscreenTask.taskId,
            transitionSource = UNKNOWN,
        )

        with(getLatestEnterDesktopWct()) {
            // Operations should include wallpaper intent, freeform task, fullscreen task
            assertThat(hierarchyOps).hasSize(3)
            assertPendingIntentAt(index = 0, desktopWallpaperIntent)
            assertReorderAt(index = 1, freeformTask)
            assertReorderAt(index = 2, fullscreenTask)
            assertThat(changes[fullscreenTask.token.asBinder()]?.windowingMode)
                .isEqualTo(WINDOWING_MODE_FREEFORM)
        }
        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
    )
    fun moveRunningTaskToDesktop_desktopWallpaperEnabled_multiDesksEnabled() {
        val freeformTask = setUpFreeformTask()
        val fullscreenTask = setUpFullscreenTask()
        markTaskHidden(freeformTask)

        controller.moveTaskToDefaultDeskAndActivate(
            fullscreenTask.taskId,
            transitionSource = UNKNOWN,
        )

        val wct = getLatestEnterDesktopWct()
        wct.assertReorderAt(index = 0, wallpaperToken)
        verify(desksOrganizer).moveTaskToDesk(wct, deskId = 0, fullscreenTask)
        verify(desksOrganizer).activateDesk(wct, deskId = 0)
        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveRunningTaskToDesktop_activatesDesk_desktopWallpaperEnabled_multiDesksDisabled() {
        val fullscreenTask = setUpFullscreenTask()

        controller.moveTaskToDefaultDeskAndActivate(
            fullscreenTask.taskId,
            transitionSource = UNKNOWN,
        )

        assertThat(taskRepository.getActiveDeskId(DEFAULT_DISPLAY)).isEqualTo(DEFAULT_DISPLAY)
    }

    @Test
    fun moveRunningTaskToDesktop_onlyFreeformTasksFromCurrentDisplayBroughtToFront() {
        setUpHomeTask(displayId = DEFAULT_DISPLAY)
        val freeformTaskDefault = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        val fullscreenTaskDefault = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)
        markTaskHidden(freeformTaskDefault)

        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        val homeTaskSecond = setUpHomeTask(displayId = SECOND_DISPLAY)
        val freeformTaskSecond = setUpFreeformTask(displayId = SECOND_DISPLAY)
        markTaskHidden(freeformTaskSecond)

        controller.moveTaskToDefaultDeskAndActivate(
            fullscreenTaskDefault.taskId,
            transitionSource = UNKNOWN,
        )

        with(getLatestEnterDesktopWct()) {
            // Check that hierarchy operations do not include tasks from second display
            assertThat(hierarchyOps.map { it.container })
                .doesNotContain(homeTaskSecond.token.asBinder())
            assertThat(hierarchyOps.map { it.container })
                .doesNotContain(freeformTaskSecond.token.asBinder())
        }
        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveRunningTaskToDesktop_splitTaskExitsSplit_multiDesksDisabled() {
        val task = setUpSplitScreenTask()
        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)
        val wct = getLatestEnterDesktopWct()
        assertThat(wct.changes[task.token.asBinder()]?.windowingMode)
            .isEqualTo(WINDOWING_MODE_FREEFORM)
        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
        verify(splitScreenController)
            .prepareExitSplitScreen(
                any(),
                anyInt(),
                eq(SplitScreenController.EXIT_REASON_DESKTOP_MODE),
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveRunningTaskToDesktop_splitTaskExitsSplit_multiDesksEnabled() {
        val task = setUpSplitScreenTask()
        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)
        val wct = getLatestEnterDesktopWct()
        verify(desksOrganizer).moveTaskToDesk(wct, deskId = 0, task)
        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
        verify(splitScreenController)
            .prepareExitSplitScreen(
                any(),
                anyInt(),
                eq(SplitScreenController.EXIT_REASON_DESKTOP_MODE),
            )
    }

    @Test
    fun moveRunningTaskToDesktop_fullscreenTaskDoesNotExitSplit() {
        val task = setUpFullscreenTask()
        controller.moveTaskToDefaultDeskAndActivate(task.taskId, transitionSource = UNKNOWN)
        val wct = getLatestEnterDesktopWct()
        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
        verify(splitScreenController, never())
            .prepareExitSplitScreen(
                any(),
                anyInt(),
                eq(SplitScreenController.EXIT_REASON_DESKTOP_MODE),
            )
    }

    @Test
    @DisableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun moveRunningTaskToDesktop_desktopWallpaperDisabled_bringsTasksOver_dontShowBackTask() {
        val freeformTasks = (1..MAX_TASK_LIMIT).map { _ -> setUpFreeformTask() }
        val newTask = setUpFullscreenTask()
        val homeTask = setUpHomeTask()

        controller.moveTaskToDefaultDeskAndActivate(newTask.taskId, transitionSource = UNKNOWN)

        val wct = getLatestEnterDesktopWct()
        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
        assertThat(wct.hierarchyOps.size).isEqualTo(MAX_TASK_LIMIT + 1) // visible tasks + home
        wct.assertReorderAt(0, homeTask)
        wct.assertReorderSequenceInRange(
            range = 1..<(MAX_TASK_LIMIT + 1),
            *freeformTasks.drop(1).toTypedArray(), // Skipping freeformTasks[0]
            newTask,
        )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveRunningTaskToDesktop_desktopWallpaperEnabled_bringsTasksOverLimit_dontShowBackTask() {
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)
        val freeformTasks = (1..MAX_TASK_LIMIT).map { _ -> setUpFreeformTask() }
        val newTask = setUpFullscreenTask()
        val homeTask = setUpHomeTask()

        controller.moveTaskToDefaultDeskAndActivate(newTask.taskId, transitionSource = UNKNOWN)

        val wct = getLatestEnterDesktopWct()
        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
        assertThat(wct.hierarchyOps.size).isEqualTo(MAX_TASK_LIMIT + 2) // tasks + home + wallpaper
        // Move home to front
        wct.assertReorderAt(0, homeTask)
        // Add desktop wallpaper activity
        wct.assertPendingIntentAt(1, desktopWallpaperIntent)
        // Bring freeform tasks to front
        wct.assertReorderSequenceInRange(
            range = 2..<(MAX_TASK_LIMIT + 2),
            *freeformTasks.drop(1).toTypedArray(), // Skipping freeformTasks[0]
            newTask,
        )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveToFullscreen_fromDesk_reparentsToTaskDisplayArea() {
        val task = setUpFreeformTask()
        val tda = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY)!!

        controller.moveToFullscreen(task.taskId, transitionSource = UNKNOWN)

        val wct = getLatestExitDesktopWct()
        wct.assertHop(ReparentPredicate(token = task.token, parentToken = tda.token, toTop = true))
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveToFullscreen_fromDesk_deactivatesDesk() {
        val task = setUpFreeformTask()
        val tda = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY)!!

        controller.moveToFullscreen(task.taskId, transitionSource = UNKNOWN)

        val wct = getLatestExitDesktopWct()
        verify(desksOrganizer).deactivateDesk(wct, deskId = 0)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveToFullscreen_fromDeskWithMultipleTasks_deactivatesDesk() {
        val deskId = 1
        taskRepository.addDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)
        taskRepository.setActiveDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)
        val task1 = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId)
        val task2 = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId)

        controller.moveToFullscreen(task1.taskId, transitionSource = UNKNOWN)

        val wct = getLatestExitDesktopWct()
        verify(desksOrganizer).deactivateDesk(wct, deskId = deskId)
    }

    @Test
    fun moveToFullscreen_tdaFullscreen_windowingModeSetToUndefined() {
        val task = setUpFreeformTask()
        val tda = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY)!!
        tda.configuration.windowConfiguration.windowingMode = WINDOWING_MODE_FULLSCREEN
        controller.moveToFullscreen(task.taskId, transitionSource = UNKNOWN)
        val wct = getLatestExitDesktopWct()
        verify(desktopModeEnterExitTransitionListener, times(1))
            .onExitDesktopModeTransitionStarted(FULLSCREEN_ANIMATION_DURATION)
        assertThat(wct.changes[task.token.asBinder()]?.windowingMode)
            .isEqualTo(WINDOWING_MODE_UNDEFINED)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveToFullscreen_tdaFullscreen_windowingModeUndefined_removesWallpaperActivity() {
        whenever(DesktopModeStatus.enterDesktopByDefaultOnFreeformDisplay(context)).thenReturn(true)
        val homeTask = setUpHomeTask()
        val task = setUpFreeformTask()
        assertNotNull(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY))
            .configuration
            .windowConfiguration
            .windowingMode = WINDOWING_MODE_FULLSCREEN

        controller.moveToFullscreen(task.taskId, transitionSource = UNKNOWN)

        val wct = getLatestExitDesktopWct()
        val taskChange = assertNotNull(wct.changes[task.token.asBinder()])
        verify(desktopModeEnterExitTransitionListener)
            .onExitDesktopModeTransitionStarted(FULLSCREEN_ANIMATION_DURATION)
        assertThat(taskChange.windowingMode).isEqualTo(WINDOWING_MODE_UNDEFINED)
        assertThat(wct.hierarchyOps).hasSize(3)
        // Removes wallpaper activity when leaving desktop
        wct.assertReorderAt(index = 0, wallpaperToken, toTop = false)
        // Moves home task behind the fullscreen task
        wct.assertReorderAt(index = 1, homeTask.getToken(), toTop = true)
        wct.assertReorderAt(index = 2, task.getToken(), toTop = true)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun moveToFullscreen_tdaFullscreen_windowingModeUndefined_removesWallpaperActivity_multiDesksEnabled() {
        whenever(DesktopModeStatus.enterDesktopByDefaultOnFreeformDisplay(context)).thenReturn(true)
        setUpHomeTask()
        val task = setUpFreeformTask()
        assertNotNull(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY))
            .configuration
            .windowConfiguration
            .windowingMode = WINDOWING_MODE_FULLSCREEN

        controller.moveToFullscreen(task.taskId, transitionSource = UNKNOWN)

        val wct = getLatestExitDesktopWct()
        val taskChange = assertNotNull(wct.changes[task.token.asBinder()])
        verify(desktopModeEnterExitTransitionListener)
            .onExitDesktopModeTransitionStarted(FULLSCREEN_ANIMATION_DURATION)
        assertThat(taskChange.windowingMode).isEqualTo(WINDOWING_MODE_UNDEFINED)
        // Removes wallpaper activity when leaving desktop
        wct.assertReorder(wallpaperToken, toTop = false)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun moveToFullscreen_tdaFullscreen_windowingModeUndefined_homeBehindFullscreen_multiDesksEnabled() {
        whenever(DesktopModeStatus.enterDesktopByDefaultOnFreeformDisplay(context)).thenReturn(true)
        val homeTask = setUpHomeTask()
        val task = setUpFreeformTask()
        assertNotNull(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY))
            .configuration
            .windowConfiguration
            .windowingMode = WINDOWING_MODE_FULLSCREEN

        controller.moveToFullscreen(task.taskId, transitionSource = UNKNOWN)

        val wct = getLatestExitDesktopWct()
        val taskChange = assertNotNull(wct.changes[task.token.asBinder()])
        verify(desktopModeEnterExitTransitionListener)
            .onExitDesktopModeTransitionStarted(FULLSCREEN_ANIMATION_DURATION)
        assertThat(taskChange.windowingMode).isEqualTo(WINDOWING_MODE_UNDEFINED)
        // Moves home task behind the fullscreen task
        val homeReorderIndex = wct.indexOfReorder(homeTask, toTop = true)
        val fullscreenReorderIndex = wct.indexOfReorder(task, toTop = true)
        assertThat(homeReorderIndex).isNotEqualTo(-1)
        assertThat(fullscreenReorderIndex).isNotEqualTo(-1)
        assertThat(fullscreenReorderIndex).isGreaterThan(homeReorderIndex)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER)
    fun moveToFullscreen_tdaFreeform_enforcedDesktop_doesNotReorderHome() {
        whenever(DesktopModeStatus.enterDesktopByDefaultOnFreeformDisplay(context)).thenReturn(true)
        val homeTask = setUpHomeTask()
        val task = setUpFreeformTask()
        assertNotNull(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY))
            .configuration
            .windowConfiguration
            .windowingMode = WINDOWING_MODE_FREEFORM

        controller.moveToFullscreen(task.taskId, transitionSource = UNKNOWN)

        val wct = getLatestExitDesktopWct()
        verify(desktopModeEnterExitTransitionListener)
            .onExitDesktopModeTransitionStarted(FULLSCREEN_ANIMATION_DURATION)
        // Removes wallpaper activity when leaving desktop but doesn't reorder home or the task
        wct.assertReorder(wallpaperToken, toTop = false)
        wct.assertWithoutHop(ReorderPredicate(homeTask.token, toTop = null))
    }

    @Test
    fun moveToFullscreen_tdaFreeform_windowingModeSetToFullscreen() {
        val task = setUpFreeformTask()
        val tda = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY)!!
        tda.configuration.windowConfiguration.windowingMode = WINDOWING_MODE_FREEFORM
        controller.moveToFullscreen(task.taskId, transitionSource = UNKNOWN)
        val wct = getLatestExitDesktopWct()
        assertThat(wct.changes[task.token.asBinder()]?.windowingMode)
            .isEqualTo(WINDOWING_MODE_FULLSCREEN)
        verify(desktopModeEnterExitTransitionListener)
            .onExitDesktopModeTransitionStarted(FULLSCREEN_ANIMATION_DURATION)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveToFullscreen_tdaFreeform_windowingModeFullscreen_removesWallpaperActivity() {
        whenever(DesktopModeStatus.enterDesktopByDefaultOnFreeformDisplay(context))
            .thenReturn(false)
        val homeTask = setUpHomeTask()
        val task = setUpFreeformTask()
        assertNotNull(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY))
            .configuration
            .windowConfiguration
            .windowingMode = WINDOWING_MODE_FREEFORM

        controller.moveToFullscreen(task.taskId, transitionSource = UNKNOWN)

        val wct = getLatestExitDesktopWct()
        val taskChange = assertNotNull(wct.changes[task.token.asBinder()])
        assertThat(taskChange.windowingMode).isEqualTo(WINDOWING_MODE_FULLSCREEN)
        verify(desktopModeEnterExitTransitionListener)
            .onExitDesktopModeTransitionStarted(FULLSCREEN_ANIMATION_DURATION)
        assertThat(wct.hierarchyOps).hasSize(3)
        // Removes wallpaper activity when leaving desktop
        wct.assertReorderAt(index = 0, wallpaperToken, toTop = false)
        // Moves home task behind the fullscreen task
        wct.assertReorderAt(index = 1, homeTask.getToken(), toTop = true)
        wct.assertReorderAt(index = 2, task.getToken(), toTop = true)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun moveToFullscreen_tdaFreeform_windowingModeFullscreen_removesWallpaperActivity_multiDesksEnabled() {
        val homeTask = setUpHomeTask()
        val task = setUpFreeformTask()

        assertNotNull(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY))
            .configuration
            .windowConfiguration
            .windowingMode = WINDOWING_MODE_FREEFORM

        controller.moveToFullscreen(task.taskId, transitionSource = UNKNOWN)

        val wct = getLatestExitDesktopWct()
        val taskChange = assertNotNull(wct.changes[task.token.asBinder()])
        assertThat(taskChange.windowingMode).isEqualTo(WINDOWING_MODE_FULLSCREEN)
        verify(desktopModeEnterExitTransitionListener)
            .onExitDesktopModeTransitionStarted(FULLSCREEN_ANIMATION_DURATION)
        // Removes wallpaper activity when leaving desktop
        wct.assertReorder(wallpaperToken, toTop = false)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun moveToFullscreen_tdaFreeform_windowingModeFullscreen_homeBehindFullscreen_multiDesksEnabled() {
        whenever(DesktopModeStatus.enterDesktopByDefaultOnFreeformDisplay(context))
            .thenReturn(false)
        val homeTask = setUpHomeTask()
        val task = setUpFreeformTask()
        assertNotNull(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY))
            .configuration
            .windowConfiguration
            .windowingMode = WINDOWING_MODE_FREEFORM

        controller.moveToFullscreen(task.taskId, transitionSource = UNKNOWN)

        val wct = getLatestExitDesktopWct()
        val taskChange = assertNotNull(wct.changes[task.token.asBinder()])
        assertThat(taskChange.windowingMode).isEqualTo(WINDOWING_MODE_FULLSCREEN)
        verify(desktopModeEnterExitTransitionListener)
            .onExitDesktopModeTransitionStarted(FULLSCREEN_ANIMATION_DURATION)
        // Moves home task behind the fullscreen task
        val homeReorderIndex = wct.indexOfReorder(homeTask, toTop = true)
        val fullscreenReorderIndex = wct.indexOfReorder(task, toTop = true)
        assertThat(homeReorderIndex).isNotEqualTo(-1)
        assertThat(fullscreenReorderIndex).isNotEqualTo(-1)
        assertThat(fullscreenReorderIndex).isGreaterThan(homeReorderIndex)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_PIP,
    )
    fun moveToFullscreen_multipleVisibleNonMinimizedTasks_doesNotRemoveWallpaperActivity() {
        val homeTask = setUpHomeTask()
        val task1 = setUpFreeformTask()
        // Setup task2
        setUpFreeformTask()

        assertNotNull(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY))
            .configuration
            .windowConfiguration
            .windowingMode = WINDOWING_MODE_FULLSCREEN

        controller.moveToFullscreen(task1.taskId, transitionSource = UNKNOWN)

        val wct = getLatestExitDesktopWct()
        val task1Change = assertNotNull(wct.changes[task1.token.asBinder()])
        assertThat(task1Change.windowingMode).isEqualTo(WINDOWING_MODE_UNDEFINED)
        verify(desktopModeEnterExitTransitionListener)
            .onExitDesktopModeTransitionStarted(FULLSCREEN_ANIMATION_DURATION)
        // Does not remove wallpaper activity, as desktop still has a visible desktop task
        assertThat(wct.hierarchyOps).hasSize(2)
        // Moves home task behind the fullscreen task
        wct.assertReorderAt(index = 0, homeTask.getToken(), toTop = true)
        wct.assertReorderAt(index = 1, task1.getToken(), toTop = true)
    }

    @Test
    fun moveToFullscreen_nonExistentTask_doesNothing() {
        controller.moveToFullscreen(999, transitionSource = UNKNOWN)
        verifyExitDesktopWCTNotExecuted()
    }

    @Test
    fun moveToFullscreen_secondDisplayTaskHasFreeform_secondDisplayNotAffected() {
        val taskDefaultDisplay = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        val taskSecondDisplay = setUpFreeformTask(displayId = SECOND_DISPLAY)
        controller.moveToFullscreen(taskDefaultDisplay.taskId, transitionSource = UNKNOWN)

        with(getLatestExitDesktopWct()) {
            assertThat(changes.keys).contains(taskDefaultDisplay.token.asBinder())
            assertThat(changes.keys).doesNotContain(taskSecondDisplay.token.asBinder())
        }
        verify(desktopModeEnterExitTransitionListener)
            .onExitDesktopModeTransitionStarted(FULLSCREEN_ANIMATION_DURATION)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveTaskToFront_postsWctWithReorderOp() {
        val task1 = setUpFreeformTask()
        setUpFreeformTask()
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    eq(TRANSIT_TO_FRONT),
                    any(),
                    eq(task1.taskId),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Binder())

        controller.moveTaskToFront(task1, remoteTransition = null)

        val wct = getLatestDesktopMixedTaskWct(type = TRANSIT_TO_FRONT)
        assertThat(wct.hierarchyOps).hasSize(1)
        wct.assertReorderAt(index = 0, task1)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveTaskToFront_reordersToFront() {
        val task1 = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = 0)
        setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = 0)
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    eq(TRANSIT_TO_FRONT),
                    any(),
                    eq(task1.taskId),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Binder())

        controller.moveTaskToFront(task1, remoteTransition = null)

        verify(desksOrganizer).reorderTaskToFront(any(), eq(0), eq(task1))
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveTaskToFront_bringsTasksOverLimit_multiDesksDisabled_minimizesBackTask() {
        setUpHomeTask()
        val freeformTasks =
            (1..MAX_TASK_LIMIT + 1).map { _ ->
                setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = 0)
            }
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    eq(TRANSIT_TO_FRONT),
                    any(),
                    eq(freeformTasks[0].taskId),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Binder())

        controller.moveTaskToFront(freeformTasks[0], remoteTransition = null)

        val wct = getLatestDesktopMixedTaskWct(type = TRANSIT_TO_FRONT)
        assertThat(wct.hierarchyOps.size).isEqualTo(2) // move-to-front + minimize
        wct.assertReorderAt(0, freeformTasks[0], toTop = true)
        wct.assertReorderAt(1, freeformTasks[1], toTop = false)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveTaskToFront_bringsTasksOverLimit_multiDesksEnabled_minimizesBackTask() {
        val deskId = 0
        setUpHomeTask()
        val freeformTasks =
            (1..MAX_TASK_LIMIT + 1).map { _ ->
                setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId)
            }
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    eq(TRANSIT_TO_FRONT),
                    any(),
                    eq(freeformTasks[0].taskId),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Binder())

        controller.moveTaskToFront(freeformTasks[0], remoteTransition = null)

        val wct = getLatestDesktopMixedTaskWct(type = TRANSIT_TO_FRONT)
        verify(desksOrganizer).minimizeTask(wct, deskId, freeformTasks[1])
    }

    @Test
    fun moveTaskToFront_minimizedTask_marksTaskAsUnminimized() {
        val transition = Binder()
        val freeformTask = setUpFreeformTask()
        taskRepository.minimizeTask(DEFAULT_DISPLAY, freeformTask.taskId)
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    eq(TRANSIT_TO_FRONT),
                    any(),
                    eq(freeformTask.taskId),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(transition)

        controller.moveTaskToFront(freeformTask, unminimizeReason = UnminimizeReason.ALT_TAB)

        val task = desktopTasksLimiter.getUnminimizingTask(transition)
        assertThat(task).isNotNull()
        assertThat(task?.taskId).isEqualTo(freeformTask.taskId)
        assertThat(task?.unminimizeReason).isEqualTo(UnminimizeReason.ALT_TAB)
    }

    @Test
    fun handleRequest_minimizedFreeformTask_marksTaskAsUnminimized() {
        val transition = Binder()
        // Create a visible task so we stay in Desktop Mode when minimizing task under test.
        setUpFreeformTask().also { markTaskVisible(it) }
        val freeformTask = setUpFreeformTask()
        taskRepository.minimizeTask(DEFAULT_DISPLAY, freeformTask.taskId)

        controller.handleRequest(transition, createTransition(freeformTask, TRANSIT_OPEN))

        val task = desktopTasksLimiter.getUnminimizingTask(transition)
        assertThat(task).isNotNull()
        assertThat(task?.taskId).isEqualTo(freeformTask.taskId)
        assertThat(task?.unminimizeReason).isEqualTo(UnminimizeReason.TASK_LAUNCH)
    }

    @Test
    fun moveTaskToFront_remoteTransition_usesOneshotHandler() {
        setUpHomeTask()
        val freeformTasks = List(MAX_TASK_LIMIT) { setUpFreeformTask() }
        val transitionHandlerArgCaptor = argumentCaptor<TransitionHandler>()
        whenever(transitions.startTransition(anyInt(), any(), transitionHandlerArgCaptor.capture()))
            .thenReturn(Binder())

        controller.moveTaskToFront(freeformTasks[0], RemoteTransition(TestRemoteTransition()))

        assertIs<OneShotRemoteHandler>(transitionHandlerArgCaptor.firstValue)
    }

    @Test
    fun moveTaskToFront_bringsTasksOverLimit_remoteTransition_usesWindowLimitHandler() {
        setUpHomeTask()
        val freeformTasks = List(MAX_TASK_LIMIT + 1) { setUpFreeformTask() }
        val transitionHandlerArgCaptor = argumentCaptor<TransitionHandler>()
        whenever(transitions.startTransition(anyInt(), any(), transitionHandlerArgCaptor.capture()))
            .thenReturn(Binder())

        controller.moveTaskToFront(freeformTasks[0], RemoteTransition(TestRemoteTransition()))

        assertThat(transitionHandlerArgCaptor.firstValue)
            .isInstanceOf(DesktopWindowLimitRemoteHandler::class.java)
    }

    @Test
    fun moveTaskToFront_backgroundTask_launchesTask() {
        val task = createRecentTaskInfo(1)
        whenever(shellTaskOrganizer.getRunningTaskInfo(anyInt())).thenReturn(null)
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    eq(TRANSIT_OPEN),
                    any(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Binder())

        controller.moveTaskToFront(task.taskId, unminimizeReason = UnminimizeReason.UNKNOWN)

        val wct = getLatestDesktopMixedTaskWct(type = TRANSIT_OPEN)
        wct.assertLaunchTask(task.taskId, WINDOWING_MODE_FREEFORM)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveTaskToFront_backgroundTaskBringsTasksOverLimit_multiDesksDisabled_minimizesBackTask() {
        val deskId = 0
        val freeformTasks =
            (1..MAX_TASK_LIMIT).map { _ ->
                setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId)
            }
        val task = createRecentTaskInfo(1001)
        whenever(shellTaskOrganizer.getRunningTaskInfo(task.taskId)).thenReturn(null)
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    eq(TRANSIT_OPEN),
                    any(),
                    eq(task.taskId),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Binder())

        controller.moveTaskToFront(task.taskId, unminimizeReason = UnminimizeReason.UNKNOWN)

        val wct = getLatestDesktopMixedTaskWct(type = TRANSIT_OPEN)
        assertThat(wct.hierarchyOps.size).isEqualTo(2) // launch + minimize
        wct.assertLaunchTaskAt(0, task.taskId, WINDOWING_MODE_FREEFORM)
        wct.assertReorderAt(1, freeformTasks[0], toTop = false)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveTaskToFront_backgroundTaskBringsTasksOverLimit_multiDesksEnabled_minimizesBackTask() {
        val deskId = 0
        val freeformTasks =
            (1..MAX_TASK_LIMIT).map { _ ->
                setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId)
            }
        val task = createRecentTaskInfo(1001)
        whenever(shellTaskOrganizer.getRunningTaskInfo(task.taskId)).thenReturn(null)
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    eq(TRANSIT_OPEN),
                    any(),
                    eq(task.taskId),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Binder())

        controller.moveTaskToFront(task.taskId, unminimizeReason = UnminimizeReason.UNKNOWN)

        val wct = getLatestDesktopMixedTaskWct(type = TRANSIT_OPEN)
        verify(desksOrganizer).minimizeTask(wct, deskId, freeformTasks[0])
    }

    @Test
    fun moveToNextDisplay_noOtherDisplays() {
        whenever(rootTaskDisplayAreaOrganizer.displayIds).thenReturn(intArrayOf(DEFAULT_DISPLAY))
        val task = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        controller.moveToNextDisplay(task.taskId)
        verify(transitions, never()).startTransition(anyInt(), any(), anyOrNull())
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveToNextDisplay_moveFromFirstToSecondDisplay_multiDesksDisabled() {
        // Set up two display ids
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        // Create a mock for the target display area: second display
        val secondDisplayArea = DisplayAreaInfo(MockToken().token(), SECOND_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(SECOND_DISPLAY))
            .thenReturn(secondDisplayArea)

        val task = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        controller.moveToNextDisplay(task.taskId)

        val taskChange =
            getLatestWct(
                    type = TRANSIT_CHANGE,
                    handlerClass = DesktopModeMoveToDisplayTransitionHandler::class.java,
                )
                .hierarchyOps
                .find { it.container == task.token.asBinder() && it.isReparent }
        assertNotNull(taskChange)
        assertThat(taskChange.newParent).isEqualTo(secondDisplayArea.token.asBinder())
        assertThat(taskChange.toTop).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveToNextDisplay_moveFromFirstToSecondDisplay_multiDesksEnabled() {
        // Set up two display ids
        val targetDeskId = 2
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = targetDeskId)
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        // Create a mock for the target display area: second display
        val secondDisplayArea = DisplayAreaInfo(MockToken().token(), SECOND_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(SECOND_DISPLAY))
            .thenReturn(secondDisplayArea)

        val task = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        controller.moveToNextDisplay(task.taskId)

        verify(desksOrganizer).moveTaskToDesk(any(), eq(targetDeskId), eq(task))
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveToNextDisplay_moveFromSecondToFirstDisplay_multiDesksDisabled() {
        // Set up two display ids
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        // Create a mock for the target display area: default display
        val defaultDisplayArea = DisplayAreaInfo(MockToken().token(), DEFAULT_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY))
            .thenReturn(defaultDisplayArea)

        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        val task = setUpFreeformTask(displayId = SECOND_DISPLAY)
        controller.moveToNextDisplay(task.taskId)

        val taskChange =
            getLatestWct(
                    type = TRANSIT_CHANGE,
                    handlerClass = DesktopModeMoveToDisplayTransitionHandler::class.java,
                )
                .hierarchyOps
                .find { it.container == task.token.asBinder() && it.isReparent }
        assertNotNull(taskChange)
        assertThat(taskChange.newParent).isEqualTo(defaultDisplayArea.token.asBinder())
        assertThat(taskChange.toTop).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveToNextDisplay_moveFromSecondToFirstDisplay_multiDesksEnabled() {
        // Set up two display ids
        val targetDeskId = 0
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        // Create a mock for the target display area: default display
        val defaultDisplayArea = DisplayAreaInfo(MockToken().token(), DEFAULT_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY))
            .thenReturn(defaultDisplayArea)

        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        val task = setUpFreeformTask(displayId = SECOND_DISPLAY)
        controller.moveToNextDisplay(task.taskId)

        verify(desksOrganizer).moveTaskToDesk(any(), eq(targetDeskId), eq(task))
    }

    @Test
    @EnableFlags(
        FLAG_ENABLE_PER_DISPLAY_DESKTOP_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
    )
    fun moveToNextDisplay_wallpaperOnSystemUser_reorderWallpaperToBack() {
        // Set up two display ids
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        // Create a mock for the target display area: second display
        val secondDisplayArea = DisplayAreaInfo(MockToken().token(), SECOND_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(SECOND_DISPLAY))
            .thenReturn(secondDisplayArea)
        // Add a task and a wallpaper
        val task = setUpFreeformTask(displayId = DEFAULT_DISPLAY)

        controller.moveToNextDisplay(task.taskId)

        with(
            getLatestWct(
                type = TRANSIT_CHANGE,
                handlerClass = DesktopModeMoveToDisplayTransitionHandler::class.java,
            )
        ) {
            val wallpaperChange =
                hierarchyOps.find { op -> op.container == wallpaperToken.asBinder() }
            assertNotNull(wallpaperChange)
            assertThat(wallpaperChange.type).isEqualTo(HIERARCHY_OP_TYPE_REORDER)
        }
    }

    @Test
    @EnableFlags(FLAG_ENABLE_PER_DISPLAY_DESKTOP_WALLPAPER_ACTIVITY)
    @DisableFlags(Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER)
    fun moveToNextDisplay_wallpaperNotOnSystemUser_removeWallpaper() {
        // Set up two display ids
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        // Create a mock for the target display area: second display
        val secondDisplayArea = DisplayAreaInfo(MockToken().token(), SECOND_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(SECOND_DISPLAY))
            .thenReturn(secondDisplayArea)
        // Add a task and a wallpaper
        val task = setUpFreeformTask(displayId = DEFAULT_DISPLAY)

        controller.moveToNextDisplay(task.taskId)

        val wallpaperChange =
            getLatestWct(
                    type = TRANSIT_CHANGE,
                    handlerClass = DesktopModeMoveToDisplayTransitionHandler::class.java,
                )
                .hierarchyOps
                .find { op -> op.container == wallpaperToken.asBinder() }
        assertNotNull(wallpaperChange)
        assertThat(wallpaperChange.type).isEqualTo(HIERARCHY_OP_TYPE_REMOVE_TASK)
    }

    @Test
    @EnableFlags(FLAG_ENABLE_MOVE_TO_NEXT_DISPLAY_SHORTCUT)
    fun moveToNextDisplay_sizeInDpPreserved() {
        // Set up two display ids
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = 2)
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        // Create a mock for the target display area: second display
        val secondDisplayArea = DisplayAreaInfo(MockToken().token(), SECOND_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(SECOND_DISPLAY))
            .thenReturn(secondDisplayArea)
        // Two displays have different density
        whenever(displayLayout.densityDpi()).thenReturn(320)
        whenever(displayLayout.width()).thenReturn(2400)
        whenever(displayLayout.height()).thenReturn(1600)
        val secondaryLayout = mock(DisplayLayout::class.java)
        whenever(displayController.getDisplayLayout(SECOND_DISPLAY)).thenReturn(secondaryLayout)
        whenever(secondaryLayout.densityDpi()).thenReturn(160)
        whenever(secondaryLayout.width()).thenReturn(1280)
        whenever(secondaryLayout.height()).thenReturn(720)

        // Place a task with a size of 640x480 at a position where the ratio of the left margin to
        // the right margin is 1:3 and the ratio of top margin to the bottom margin is 1:2.
        val task =
            setUpFreeformTask(displayId = DEFAULT_DISPLAY, bounds = Rect(440, 374, 1080, 854))

        controller.moveToNextDisplay(task.taskId)

        val taskChange =
            getLatestWct(
                    type = TRANSIT_CHANGE,
                    handlerClass = DesktopModeMoveToDisplayTransitionHandler::class.java,
                )
                .changes[task.token.asBinder()]
        assertNotNull(taskChange)
        // To preserve DP size, pixel size is changed to 320x240. The ratio of the left margin
        // to the right margin and the ratio of the top margin to bottom margin are also
        // preserved.
        assertThat(taskChange.configuration.windowConfiguration.bounds)
            .isEqualTo(Rect(240, 160, 560, 400))
    }

    @Test
    @EnableFlags(FLAG_ENABLE_MOVE_TO_NEXT_DISPLAY_SHORTCUT)
    fun moveToNextDisplay_shiftWithinDestinationDisplayBounds() {
        // Set up two display ids
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = 2)
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        // Create a mock for the target display area: second display
        val secondDisplayArea = DisplayAreaInfo(MockToken().token(), SECOND_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(SECOND_DISPLAY))
            .thenReturn(secondDisplayArea)
        // Two displays have different density
        whenever(displayLayout.densityDpi()).thenReturn(320)
        whenever(displayLayout.width()).thenReturn(2400)
        whenever(displayLayout.height()).thenReturn(1600)
        val secondaryLayout = mock(DisplayLayout::class.java)
        whenever(displayController.getDisplayLayout(SECOND_DISPLAY)).thenReturn(secondaryLayout)
        whenever(secondaryLayout.densityDpi()).thenReturn(160)
        whenever(secondaryLayout.width()).thenReturn(1280)
        whenever(secondaryLayout.height()).thenReturn(720)

        // Place a task with a size of 640x480 at a position where the bottom-right corner of the
        // window is outside the source display bounds. The destination display still has enough
        // space to place the window within its bounds.
        val task =
            setUpFreeformTask(displayId = DEFAULT_DISPLAY, bounds = Rect(2000, 1200, 2640, 1680))

        controller.moveToNextDisplay(task.taskId)

        val taskChange =
            getLatestWct(
                    type = TRANSIT_CHANGE,
                    handlerClass = DesktopModeMoveToDisplayTransitionHandler::class.java,
                )
                .changes[task.token.asBinder()]
        assertNotNull(taskChange)
        assertThat(taskChange.configuration.windowConfiguration.bounds)
            .isEqualTo(Rect(960, 480, 1280, 720))
    }

    @Test
    @EnableFlags(FLAG_ENABLE_MOVE_TO_NEXT_DISPLAY_SHORTCUT)
    fun moveToNextDisplay_maximizedTask() {
        // Set up two display ids
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = 2)
        // Create a mock for the target display area: second display
        val secondDisplayArea = DisplayAreaInfo(MockToken().token(), SECOND_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(SECOND_DISPLAY))
            .thenReturn(secondDisplayArea)
        // Two displays have different density
        whenever(displayLayout.densityDpi()).thenReturn(320)
        whenever(displayLayout.width()).thenReturn(1280)
        whenever(displayLayout.height()).thenReturn(960)
        val secondaryLayout = mock(DisplayLayout::class.java)
        whenever(displayController.getDisplayLayout(SECOND_DISPLAY)).thenReturn(secondaryLayout)
        whenever(secondaryLayout.densityDpi()).thenReturn(160)
        whenever(secondaryLayout.width()).thenReturn(1280)
        whenever(secondaryLayout.height()).thenReturn(720)

        // Place a task with a size equals to display size.
        val task = setUpFreeformTask(displayId = DEFAULT_DISPLAY, bounds = Rect(0, 0, 1280, 960))

        controller.moveToNextDisplay(task.taskId)

        val taskChange =
            getLatestWct(
                    type = TRANSIT_CHANGE,
                    handlerClass = DesktopModeMoveToDisplayTransitionHandler::class.java,
                )
                .changes[task.token.asBinder()]
        assertNotNull(taskChange)
        // DP size is preserved. The window is centered in the destination display.
        assertThat(taskChange.configuration.windowConfiguration.bounds)
            .isEqualTo(Rect(320, 120, 960, 600))
    }

    @Test
    @EnableFlags(FLAG_ENABLE_MOVE_TO_NEXT_DISPLAY_SHORTCUT)
    fun moveToNextDisplay_defaultBoundsWhenDestinationTooSmall() {
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        // Set up two display ids
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        // Create a mock for the target display area: second display
        val secondDisplayArea = DisplayAreaInfo(MockToken().token(), SECOND_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(SECOND_DISPLAY))
            .thenReturn(secondDisplayArea)
        // Two displays have different density
        whenever(displayLayout.densityDpi()).thenReturn(320)
        whenever(displayLayout.width()).thenReturn(2400)
        whenever(displayLayout.height()).thenReturn(1600)
        val secondaryLayout = mock(DisplayLayout::class.java)
        whenever(displayController.getDisplayLayout(SECOND_DISPLAY)).thenReturn(secondaryLayout)
        whenever(secondaryLayout.densityDpi()).thenReturn(160)
        whenever(secondaryLayout.width()).thenReturn(640)
        whenever(secondaryLayout.height()).thenReturn(480)
        whenever(secondaryLayout.getStableBoundsForDesktopMode(any())).thenAnswer { i ->
            (i.arguments.first() as Rect).set(0, 0, 640, 480)
        }

        // A task with a size of 1800x1200 is being placed. To preserve DP size,
        // 900x600 pixels are needed, which does not fit in the destination display.
        val task =
            setUpFreeformTask(displayId = DEFAULT_DISPLAY, bounds = Rect(300, 200, 2100, 1400))

        controller.moveToNextDisplay(task.taskId)

        val taskChange =
            getLatestWct(
                    type = TRANSIT_CHANGE,
                    handlerClass = DesktopModeMoveToDisplayTransitionHandler::class.java,
                )
                .changes[task.token.asBinder()]
        assertNotNull(taskChange)
        assertThat(taskChange.configuration.windowConfiguration.bounds.left).isAtLeast(0)
        assertThat(taskChange.configuration.windowConfiguration.bounds.top).isAtLeast(0)
        assertThat(taskChange.configuration.windowConfiguration.bounds.right).isAtMost(640)
        assertThat(taskChange.configuration.windowConfiguration.bounds.bottom).isAtMost(480)
    }

    @Test
    @EnableFlags(
        FLAG_ENABLE_DISPLAY_FOCUS_IN_SHELL_TRANSITIONS,
        FLAG_ENABLE_MOVE_TO_NEXT_DISPLAY_SHORTCUT,
    )
    fun moveToNextDisplay_destinationGainGlobalFocus() {
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        // Set up two display ids
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        // Create a mock for the target display area: second display
        val secondDisplayArea = DisplayAreaInfo(MockToken().token(), SECOND_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(SECOND_DISPLAY))
            .thenReturn(secondDisplayArea)

        val task = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        controller.moveToNextDisplay(task.taskId)

        val taskChange =
            getLatestWct(
                    type = TRANSIT_CHANGE,
                    handlerClass = DesktopModeMoveToDisplayTransitionHandler::class.java,
                )
                .hierarchyOps
                .find {
                    it.container == task.token.asBinder() && it.type == HIERARCHY_OP_TYPE_REORDER
                }
        assertNotNull(taskChange)
        assertThat(taskChange.toTop).isTrue()
        assertThat(taskChange.includingParents()).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveToNextDisplay_toDesktopInOtherDisplay_multiDesksDisabled_bringsExistingTasksToFront() {
        val transition = Binder()
        val sourceDeskId = 0
        val targetDeskId = 2
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = targetDeskId)
        taskRepository.setDeskInactive(deskId = targetDeskId)
        // Set up two display ids
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        // Create a mock for the target display area: second display
        val secondDisplayArea = DisplayAreaInfo(MockToken().token(), SECOND_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(SECOND_DISPLAY))
            .thenReturn(secondDisplayArea)
        whenever(transitions.startTransition(eq(TRANSIT_CHANGE), any(), anyOrNull()))
            .thenReturn(transition)
        val task1 = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = sourceDeskId)
        val task2 = setUpFreeformTask(displayId = SECOND_DISPLAY, deskId = targetDeskId)

        controller.moveToNextDisplay(task1.taskId)

        // Existing desktop task in the target display is moved to front.
        val wct = getLatestTransition()
        wct.assertReorder(task2.token, /* toTop= */ true)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveToNextDisplay_toDesktopInOtherDisplay_multiDesksEnabled_bringsExistingTasksToFront() {
        val transition = Binder()
        val sourceDeskId = 0
        val targetDeskId = 2
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = targetDeskId)
        taskRepository.setDeskInactive(deskId = targetDeskId)
        // Set up two display ids
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        // Create a mock for the target display area: second display
        val secondDisplayArea = DisplayAreaInfo(MockToken().token(), SECOND_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(SECOND_DISPLAY))
            .thenReturn(secondDisplayArea)
        whenever(transitions.startTransition(eq(TRANSIT_CHANGE), any(), anyOrNull()))
            .thenReturn(transition)
        val task1 = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = sourceDeskId)
        val task2 = setUpFreeformTask(displayId = SECOND_DISPLAY, deskId = targetDeskId)

        controller.moveToNextDisplay(task1.taskId)

        // Existing desktop task in the target display is moved to front.
        val wct = getLatestTransition()
        assertNotNull(wct)
        verify(desksOrganizer).reorderTaskToFront(wct, targetDeskId, task2)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveToNextDisplay_toDesktopInOtherDisplay_appliesTaskLimit() {
        val transition = Binder()
        val sourceDeskId = 0
        val targetDeskId = 2
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = targetDeskId)
        taskRepository.setDeskInactive(deskId = targetDeskId)
        val targetDeskTasks =
            (1..MAX_TASK_LIMIT + 1).map { _ ->
                setUpFreeformTask(displayId = SECOND_DISPLAY, deskId = targetDeskId)
            }
        // Set up two display ids
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        // Create a mock for the target display area: second display
        val secondDisplayArea = DisplayAreaInfo(MockToken().token(), SECOND_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(SECOND_DISPLAY))
            .thenReturn(secondDisplayArea)
        whenever(transitions.startTransition(eq(TRANSIT_CHANGE), any(), anyOrNull()))
            .thenReturn(transition)
        val task = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = sourceDeskId)

        controller.moveToNextDisplay(task.taskId)

        val wct = getLatestTransition()
        assertNotNull(wct)
        verify(desksOrganizer).minimizeTask(wct, targetDeskId, targetDeskTasks[0])
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_PER_DISPLAY_DESKTOP_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
    )
    fun moveToNextDisplay_toDesktopInOtherDisplay_movesHomeAndWallpaperToFront() {
        val homeTask = setUpHomeTask(displayId = SECOND_DISPLAY)
        whenever(desktopWallpaperActivityTokenProvider.getToken(SECOND_DISPLAY))
            .thenReturn(wallpaperToken)
        val transition = Binder()
        val sourceDeskId = 0
        val targetDeskId = 2
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = targetDeskId)
        taskRepository.setDeskInactive(deskId = targetDeskId)
        // Set up two display ids
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        // Create a mock for the target display area: second display
        val secondDisplayArea = DisplayAreaInfo(MockToken().token(), SECOND_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(SECOND_DISPLAY))
            .thenReturn(secondDisplayArea)
        whenever(transitions.startTransition(eq(TRANSIT_CHANGE), any(), anyOrNull()))
            .thenReturn(transition)
        val task1 = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = sourceDeskId)

        controller.moveToNextDisplay(task1.taskId)

        // Home / Wallpaper should be moved to front as the background of desktop tasks, otherwise
        // fullscreen (non-desktop) tasks could remain visible.
        val wct = getLatestTransition()
        val homeReorderIndex = wct.indexOfReorder(homeTask, toTop = true)
        val wallpaperReorderIndex = wct.indexOfReorder(wallpaperToken, toTop = true)
        assertThat(homeReorderIndex).isNotEqualTo(-1)
        assertThat(wallpaperReorderIndex).isNotEqualTo(-1)
        // Wallpaper last, to be in front of Home.
        assertThat(wallpaperReorderIndex).isGreaterThan(homeReorderIndex)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveToNextDisplay_toDeskInOtherDisplay_movesToDeskAndActivates() {
        val transition = Binder()
        val targetDeskId = 4
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = targetDeskId)
        taskRepository.setDeskInactive(deskId = targetDeskId)
        // Set up two display ids
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        // Create a mock for the target display area: second display
        val secondDisplayArea = DisplayAreaInfo(MockToken().token(), SECOND_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(SECOND_DISPLAY))
            .thenReturn(secondDisplayArea)
        whenever(transitions.startTransition(eq(TRANSIT_CHANGE), any(), anyOrNull()))
            .thenReturn(transition)

        val task = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        taskRepository.addTaskToDesk(
            displayId = DEFAULT_DISPLAY,
            deskId = 0,
            taskId = task.taskId,
            isVisible = true,
        )
        controller.moveToNextDisplay(task.taskId)

        verify(desksOrganizer).moveTaskToDesk(any(), eq(targetDeskId), eq(task))
        verify(desksOrganizer).activateDesk(any(), eq(targetDeskId))
        verify(desksTransitionsObserver)
            .addPendingTransition(
                DeskTransition.ActiveDeskWithTask(
                    token = transition,
                    displayId = SECOND_DISPLAY,
                    deskId = targetDeskId,
                    enterTaskId = task.taskId,
                )
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveToNextDisplay_wasLastTaskInSourceDesk_deactivates() {
        val transition = Binder()
        val sourceDeskId = 0
        val targetDeskId = 4
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = targetDeskId)
        taskRepository.setDeskInactive(deskId = targetDeskId)
        // Set up two display ids
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        // Create a mock for the target display area: second display
        val secondDisplayArea = DisplayAreaInfo(MockToken().token(), SECOND_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(SECOND_DISPLAY))
            .thenReturn(secondDisplayArea)
        whenever(transitions.startTransition(eq(TRANSIT_CHANGE), any(), anyOrNull()))
            .thenReturn(transition)

        val task = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        taskRepository.addTaskToDesk(
            displayId = DEFAULT_DISPLAY,
            deskId = sourceDeskId,
            taskId = task.taskId,
            isVisible = true,
        )
        controller.moveToNextDisplay(task.taskId)

        verify(desksOrganizer).deactivateDesk(any(), eq(sourceDeskId))
        verify(desksTransitionsObserver)
            .addPendingTransition(
                DeskTransition.DeactivateDesk(token = transition, deskId = sourceDeskId)
            )
    }

    @Test
    fun moveToNextDisplay_movingToDesktop_sendsTaskbarRoundingUpdate() {
        val transition = Binder()
        val sourceDeskId = 0
        val targetDeskId = 2
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = targetDeskId)
        taskRepository.setDeskInactive(deskId = targetDeskId)
        // Set up two display ids
        whenever(rootTaskDisplayAreaOrganizer.displayIds)
            .thenReturn(intArrayOf(DEFAULT_DISPLAY, SECOND_DISPLAY))
        // Create a mock for the target display area: second display
        val secondDisplayArea = DisplayAreaInfo(MockToken().token(), SECOND_DISPLAY, 0)
        whenever(rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(SECOND_DISPLAY))
            .thenReturn(secondDisplayArea)
        whenever(transitions.startTransition(eq(TRANSIT_CHANGE), any(), anyOrNull()))
            .thenReturn(transition)

        val task = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = sourceDeskId)
        taskRepository.addTaskToDesk(
            displayId = DEFAULT_DISPLAY,
            deskId = sourceDeskId,
            taskId = task.taskId,
            isVisible = true,
        )
        controller.moveToNextDisplay(task.taskId)

        verify(taskbarDesktopTaskListener).onTaskbarCornerRoundingUpdate(anyBoolean())
    }

    @Test
    fun getTaskWindowingMode() {
        val fullscreenTask = setUpFullscreenTask()
        val freeformTask = setUpFreeformTask()

        assertThat(controller.getTaskWindowingMode(fullscreenTask.taskId))
            .isEqualTo(WINDOWING_MODE_FULLSCREEN)
        assertThat(controller.getTaskWindowingMode(freeformTask.taskId))
            .isEqualTo(WINDOWING_MODE_FREEFORM)
        assertThat(controller.getTaskWindowingMode(999)).isEqualTo(WINDOWING_MODE_UNDEFINED)
    }

    @Test
    fun onDesktopWindowClose_noActiveTasks() {
        val task = setUpFreeformTask(active = false)
        val wct = WindowContainerTransaction()
        controller.onDesktopWindowClose(wct, displayId = DEFAULT_DISPLAY, task)
        // Doesn't modify transaction
        assertThat(wct.hierarchyOps).isEmpty()
    }

    @Test
    fun onDesktopWindowClose_singleActiveTask_noWallpaperActivityToken_launchesHome() {
        val task = setUpFreeformTask()
        val wct = WindowContainerTransaction()
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)

        controller.onDesktopWindowClose(wct, displayId = DEFAULT_DISPLAY, task)

        // Should launch home
        wct.assertPendingIntentAt(0, launchHomeIntent(DEFAULT_DISPLAY))
        wct.assertPendingIntentActivityOptionsLaunchDisplayIdAt(0, DEFAULT_DISPLAY)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER)
    fun onDesktopWindowClose_singleActiveTask_hasWallpaperActivityToken() {
        val task = setUpFreeformTask()

        val wct = WindowContainerTransaction()
        controller.onDesktopWindowClose(wct, displayId = DEFAULT_DISPLAY, task)
        // Adds remove wallpaper operation
        wct.assertReorderAt(index = 0, wallpaperToken, toTop = false)
    }

    @Test
    fun onDesktopWindowClose_singleActiveTask_isClosing() {
        val task = setUpFreeformTask()

        taskRepository.addClosingTask(displayId = DEFAULT_DISPLAY, deskId = 0, taskId = task.taskId)

        val wct = WindowContainerTransaction()
        controller.onDesktopWindowClose(wct, displayId = DEFAULT_DISPLAY, task)
        // Doesn't modify transaction
        assertThat(wct.hierarchyOps).isEmpty()
    }

    @Test
    fun onDesktopWindowClose_singleActiveTask_isMinimized() {
        val task = setUpFreeformTask()

        taskRepository.minimizeTask(DEFAULT_DISPLAY, task.taskId)

        val wct = WindowContainerTransaction()
        controller.onDesktopWindowClose(wct, displayId = DEFAULT_DISPLAY, task)
        // Doesn't modify transaction
        assertThat(wct.hierarchyOps).isEmpty()
    }

    @Test
    fun onDesktopWindowClose_multipleActiveTasks() {
        val task1 = setUpFreeformTask()
        setUpFreeformTask()

        val wct = WindowContainerTransaction()
        controller.onDesktopWindowClose(wct, displayId = DEFAULT_DISPLAY, task1)
        // Doesn't modify transaction
        assertThat(wct.hierarchyOps).isEmpty()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER)
    fun onDesktopWindowClose_multipleActiveTasks_isOnlyNonClosingTask() {
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()

        taskRepository.addClosingTask(
            displayId = DEFAULT_DISPLAY,
            deskId = 0,
            taskId = task2.taskId,
        )

        val wct = WindowContainerTransaction()
        controller.onDesktopWindowClose(wct, displayId = DEFAULT_DISPLAY, task1)
        // Adds remove wallpaper operation
        wct.assertReorderAt(index = 0, wallpaperToken, toTop = false)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER)
    fun onDesktopWindowClose_multipleActiveTasks_hasMinimized() {
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()

        taskRepository.minimizeTask(DEFAULT_DISPLAY, task2.taskId)

        val wct = WindowContainerTransaction()
        controller.onDesktopWindowClose(wct, displayId = DEFAULT_DISPLAY, task1)
        // Adds remove wallpaper operation
        wct.assertReorderAt(index = 0, wallpaperToken, toTop = false)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun onDesktopWindowClose_lastWindow_deactivatesDesk() {
        val task = setUpFreeformTask()
        val wct = WindowContainerTransaction()

        controller.onDesktopWindowClose(wct, displayId = DEFAULT_DISPLAY, task)

        verify(desksOrganizer).deactivateDesk(wct, deskId = 0)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun onDesktopWindowClose_lastWindow_addsPendingDeactivateTransition() {
        val task = setUpFreeformTask()
        val wct = WindowContainerTransaction()

        val transition = Binder()
        val runOnTransitStart =
            controller.onDesktopWindowClose(wct, displayId = DEFAULT_DISPLAY, task)
        runOnTransitStart(transition)

        verify(desksTransitionsObserver)
            .addPendingTransition(DeskTransition.DeactivateDesk(transition, deskId = 0))
    }

    @Test
    fun onDesktopWindowMinimize_noActiveTask_doesntRemoveWallpaper() {
        val task = setUpFreeformTask(active = false)
        val transition = Binder()
        whenever(
                freeformTaskTransitionStarter.startMinimizedModeTransition(
                    any(),
                    anyInt(),
                    anyBoolean(),
                )
            )
            .thenReturn(transition)

        controller.minimizeTask(task, MinimizeReason.MINIMIZE_BUTTON)

        val captor = argumentCaptor<WindowContainerTransaction>()
        verify(freeformTaskTransitionStarter)
            .startMinimizedModeTransition(captor.capture(), eq(task.taskId), eq(false))
        assertThat(
                captor.firstValue.hierarchyOps.none { hop ->
                    hop.type == HIERARCHY_OP_TYPE_REMOVE_TASK &&
                        hop.container == wallpaperToken.asBinder()
                }
            )
            .isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun onDesktopWindowMinimize_lastWindow_deactivatesDesk() {
        val task = setUpFreeformTask()
        val transition = Binder()
        whenever(
                freeformTaskTransitionStarter.startMinimizedModeTransition(
                    any(),
                    anyInt(),
                    anyBoolean(),
                )
            )
            .thenReturn(transition)

        controller.minimizeTask(task, MinimizeReason.MINIMIZE_BUTTON)

        val captor = argumentCaptor<WindowContainerTransaction>()
        verify(freeformTaskTransitionStarter)
            .startMinimizedModeTransition(captor.capture(), eq(task.taskId), eq(true))
        verify(desksOrganizer).deactivateDesk(captor.firstValue, deskId = 0)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun onDesktopWindowMinimize_lastWindow_addsPendingDeactivateTransition() {
        val task = setUpFreeformTask()
        val transition = Binder()
        whenever(
                freeformTaskTransitionStarter.startMinimizedModeTransition(
                    any(),
                    anyInt(),
                    anyBoolean(),
                )
            )
            .thenReturn(transition)

        controller.minimizeTask(task, MinimizeReason.MINIMIZE_BUTTON)

        verify(desksTransitionsObserver)
            .addPendingTransition(DeskTransition.DeactivateDesk(token = transition, deskId = 0))
    }

    private fun minimizePipTask(task: RunningTaskInfo) {
        val handler = mock(TransitionHandler::class.java)
        whenever(transitions.dispatchRequest(any(), any(), anyOrNull()))
            .thenReturn(android.util.Pair(handler, WindowContainerTransaction()))

        controller.minimizeTask(task, MinimizeReason.MINIMIZE_BUTTON)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_PIP)
    fun onPipTaskMinimize_autoEnterEnabled_startPipTransition() {
        val task = setUpPipTask(autoEnterEnabled = true)

        minimizePipTask(task)

        verify(freeformTaskTransitionStarter).startPipTransition(any())
        verify(freeformTaskTransitionStarter, never())
            .startMinimizedModeTransition(any(), anyInt(), anyBoolean())
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_PIP)
    fun onPipTaskMinimize_autoEnterDisabled_startMinimizeTransition() {
        val task = setUpPipTask(autoEnterEnabled = false)
        whenever(
                freeformTaskTransitionStarter.startMinimizedModeTransition(
                    any(),
                    anyInt(),
                    anyBoolean(),
                )
            )
            .thenReturn(Binder())

        minimizePipTask(task)

        verify(freeformTaskTransitionStarter)
            .startMinimizedModeTransition(any(), eq(task.taskId), anyBoolean())
        verify(freeformTaskTransitionStarter, never()).startPipTransition(any())
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_PIP)
    fun onPipTaskMinimize_autoEnterEnabled_sendsTaskbarRoundingUpdate() {
        val task = setUpPipTask(autoEnterEnabled = true)

        minimizePipTask(task)

        verify(taskbarDesktopTaskListener).onTaskbarCornerRoundingUpdate(anyBoolean())
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_PIP,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun onPipTaskMinimize_isLastTask_deactivatesDesk() {
        val deskId = DEFAULT_DISPLAY
        val task = setUpPipTask(autoEnterEnabled = true, deskId = deskId)
        val transition = Binder()
        whenever(freeformTaskTransitionStarter.startPipTransition(any())).thenReturn(transition)

        minimizePipTask(task)

        verify(desksOrganizer).deactivateDesk(any(), eq(deskId))
        verify(desksTransitionsObserver)
            .addPendingTransition(DeskTransition.DeactivateDesk(transition, deskId))
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_PIP,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
    )
    fun onPipTaskMinimize_isLastTask_removesWallpaper() {
        val task = setUpPipTask(autoEnterEnabled = true)

        minimizePipTask(task)

        val arg = argumentCaptor<WindowContainerTransaction>()
        verify(freeformTaskTransitionStarter).startPipTransition(arg.capture())
        // Wallpaper is moved to the back
        arg.lastValue.assertReorder(wallpaperToken, /* toTop= */ false)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_PIP)
    fun onPipTaskMinimize_isLastTask_launchesHome() {
        val task = setUpPipTask(autoEnterEnabled = true)

        minimizePipTask(task)

        val arg = argumentCaptor<WindowContainerTransaction>()
        verify(freeformTaskTransitionStarter).startPipTransition(arg.capture())
        arg.lastValue.assertPendingIntent(launchHomeIntent(DEFAULT_DISPLAY))
    }

    @Test
    fun onDesktopWindowMinimize_singleActiveTask_noWallpaperActivityToken_doesntRemoveWallpaper() {
        val task = setUpFreeformTask(active = true)
        val transition = Binder()
        whenever(
                freeformTaskTransitionStarter.startMinimizedModeTransition(
                    any(),
                    anyInt(),
                    anyBoolean(),
                )
            )
            .thenReturn(transition)

        controller.minimizeTask(task, MinimizeReason.MINIMIZE_BUTTON)

        val captor = argumentCaptor<WindowContainerTransaction>()
        verify(freeformTaskTransitionStarter)
            .startMinimizedModeTransition(captor.capture(), eq(task.taskId), eq(true))
        assertThat(
                captor.firstValue.hierarchyOps.none { hop ->
                    hop.type == HIERARCHY_OP_TYPE_REMOVE_TASK
                }
            )
            .isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER)
    fun onTaskMinimize_singleActiveTask_hasWallpaperActivityToken_removesWallpaper() {
        val task = setUpFreeformTask()
        val transition = Binder()
        whenever(
                freeformTaskTransitionStarter.startMinimizedModeTransition(
                    any(),
                    anyInt(),
                    anyBoolean(),
                )
            )
            .thenReturn(transition)

        // The only active task is being minimized.
        controller.minimizeTask(task, MinimizeReason.MINIMIZE_BUTTON)

        val captor = argumentCaptor<WindowContainerTransaction>()
        verify(freeformTaskTransitionStarter)
            .startMinimizedModeTransition(captor.capture(), eq(task.taskId), eq(true))
        // Adds remove wallpaper operation
        captor.firstValue.assertReorderAt(index = 0, wallpaperToken, toTop = false)
    }

    @Test
    fun onDesktopWindowMinimize_singleActiveTask_alreadyMinimized_doesntRemoveWallpaper() {
        val task = setUpFreeformTask()
        val transition = Binder()
        whenever(
                freeformTaskTransitionStarter.startMinimizedModeTransition(
                    any(),
                    anyInt(),
                    anyBoolean(),
                )
            )
            .thenReturn(transition)
        taskRepository.minimizeTask(DEFAULT_DISPLAY, task.taskId)

        // The only active task is already minimized.
        controller.minimizeTask(task, MinimizeReason.MINIMIZE_BUTTON)

        val captor = argumentCaptor<WindowContainerTransaction>()
        verify(freeformTaskTransitionStarter)
            .startMinimizedModeTransition(captor.capture(), eq(task.taskId), eq(false))
        assertThat(
                captor.firstValue.hierarchyOps.none { hop ->
                    hop.type == HIERARCHY_OP_TYPE_REMOVE_TASK &&
                        hop.container == wallpaperToken.asBinder()
                }
            )
            .isTrue()
    }

    @Test
    fun onDesktopWindowMinimize_multipleActiveTasks_doesntRemoveWallpaper() {
        val task1 = setUpFreeformTask(active = true)
        setUpFreeformTask(active = true)
        val transition = Binder()
        whenever(
                freeformTaskTransitionStarter.startMinimizedModeTransition(
                    any(),
                    anyInt(),
                    anyBoolean(),
                )
            )
            .thenReturn(transition)

        controller.minimizeTask(task1, MinimizeReason.MINIMIZE_BUTTON)

        val captor = argumentCaptor<WindowContainerTransaction>()
        verify(freeformTaskTransitionStarter)
            .startMinimizedModeTransition(captor.capture(), eq(task1.taskId), eq(false))
        assertThat(
                captor.firstValue.hierarchyOps.none { hop ->
                    hop.type == HIERARCHY_OP_TYPE_REMOVE_TASK &&
                        hop.container == wallpaperToken.asBinder()
                }
            )
            .isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER)
    fun onDesktopWindowMinimize_multipleActiveTasks_minimizesTheOnlyVisibleTask_removesWallpaper() {
        val task1 = setUpFreeformTask(active = true)
        val task2 = setUpFreeformTask(active = true)
        val transition = Binder()
        whenever(
                freeformTaskTransitionStarter.startMinimizedModeTransition(
                    any(),
                    anyInt(),
                    anyBoolean(),
                )
            )
            .thenReturn(transition)
        taskRepository.minimizeTask(DEFAULT_DISPLAY, task2.taskId)

        // task1 is the only visible task as task2 is minimized.
        controller.minimizeTask(task1, MinimizeReason.MINIMIZE_BUTTON)
        // Adds remove wallpaper operation
        val captor = argumentCaptor<WindowContainerTransaction>()
        verify(freeformTaskTransitionStarter)
            .startMinimizedModeTransition(captor.capture(), eq(task1.taskId), eq(true))
        // Adds remove wallpaper operation
        captor.firstValue.assertReorderAt(index = 0, wallpaperToken, toTop = false)
    }

    @Test
    fun onDesktopWindowMinimize_triesToExitImmersive() {
        val task = setUpFreeformTask()
        val transition = Binder()
        whenever(
                freeformTaskTransitionStarter.startMinimizedModeTransition(
                    any(),
                    anyInt(),
                    anyBoolean(),
                )
            )
            .thenReturn(transition)

        controller.minimizeTask(task, MinimizeReason.MINIMIZE_BUTTON)

        verify(mMockDesktopImmersiveController).exitImmersiveIfApplicable(any(), eq(task), any())
    }

    @Test
    fun onDesktopWindowMinimize_invokesImmersiveTransitionStartCallback() {
        val task = setUpFreeformTask()
        val transition = Binder()
        val runOnTransit = RunOnStartTransitionCallback()
        whenever(
                freeformTaskTransitionStarter.startMinimizedModeTransition(
                    any(),
                    anyInt(),
                    anyBoolean(),
                )
            )
            .thenReturn(transition)
        whenever(mMockDesktopImmersiveController.exitImmersiveIfApplicable(any(), eq(task), any()))
            .thenReturn(
                ExitResult.Exit(exitingTask = task.taskId, runOnTransitionStart = runOnTransit)
            )

        controller.minimizeTask(task, MinimizeReason.MINIMIZE_BUTTON)

        assertThat(runOnTransit.invocations).isEqualTo(1)
        assertThat(runOnTransit.lastInvoked).isEqualTo(transition)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun onDesktopWindowMinimize_minimizesTask() {
        val task = setUpFreeformTask()
        val transition = Binder()
        val runOnTransit = RunOnStartTransitionCallback()
        whenever(
                freeformTaskTransitionStarter.startMinimizedModeTransition(
                    any(),
                    anyInt(),
                    anyBoolean(),
                )
            )
            .thenReturn(transition)
        whenever(mMockDesktopImmersiveController.exitImmersiveIfApplicable(any(), eq(task), any()))
            .thenReturn(
                ExitResult.Exit(exitingTask = task.taskId, runOnTransitionStart = runOnTransit)
            )

        controller.minimizeTask(task, MinimizeReason.MINIMIZE_BUTTON)

        verify(desksOrganizer).minimizeTask(any(), /* deskId= */ eq(0), eq(task))
    }

    @Test
    fun onDesktopWindowMinimize_triesToStopTiling() {
        val task = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        val transition = Binder()
        whenever(
                freeformTaskTransitionStarter.startMinimizedModeTransition(
                    any(),
                    anyInt(),
                    anyBoolean(),
                )
            )
            .thenReturn(transition)

        controller.minimizeTask(task, MinimizeReason.MINIMIZE_BUTTON)

        verify(snapEventHandler).removeTaskIfTiled(eq(DEFAULT_DISPLAY), eq(task.taskId))
    }

    @Test
    fun onDesktopWindowMinimize_sendsTaskbarRoundingUpdate() {
        val task = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        val transition = Binder()
        whenever(
                freeformTaskTransitionStarter.startMinimizedModeTransition(
                    any(),
                    anyInt(),
                    anyBoolean(),
                )
            )
            .thenReturn(transition)

        controller.minimizeTask(task, MinimizeReason.MINIMIZE_BUTTON)

        verify(taskbarDesktopTaskListener).onTaskbarCornerRoundingUpdate(anyBoolean())
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTask_switchToDesktop_movesTaskToDesk() {
        taskRepository.addDesk(displayId = DEFAULT_DISPLAY, deskId = 5)
        setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = 5)
        taskRepository.setActiveDesk(displayId = DEFAULT_DISPLAY, deskId = 5)

        val fullscreenTask = createFullscreenTask()
        val wct = controller.handleRequest(Binder(), createTransition(fullscreenTask))

        assertNotNull(wct, "should handle request")
        verify(desksOrganizer).moveTaskToDesk(wct = wct, deskId = 5, task = fullscreenTask)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTaskThatWasInactiveInDesk_tracksDeskDeactivation() {
        // Set up and existing desktop task in an active desk.
        val inactiveInDeskTask = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = 0)
        taskRepository.setDeskInactive(deskId = 0)

        // Now the task is launching as fullscreen.
        inactiveInDeskTask.configuration.windowConfiguration.windowingMode =
            WINDOWING_MODE_FULLSCREEN
        val transition = Binder()
        val wct = controller.handleRequest(transition, createTransition(inactiveInDeskTask))

        // Desk is deactivated.
        assertNotNull(wct, "should handle request")
        verify(desksTransitionsObserver)
            .addPendingTransition(DeskTransition.DeactivateDesk(transition, deskId = 0))
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTask_freeformVisible_multiDesksDisabled_returnSwitchToFreeformWCT() {
        val homeTask = setUpHomeTask()
        val freeformTask = setUpFreeformTask()
        markTaskVisible(freeformTask)
        val fullscreenTask = createFullscreenTask()

        val wct = controller.handleRequest(Binder(), createTransition(fullscreenTask))

        assertNotNull(wct, "should handle request")
        assertThat(wct.changes[fullscreenTask.token.asBinder()]?.windowingMode)
            .isEqualTo(WINDOWING_MODE_FREEFORM)

        assertThat(wct.hierarchyOps).hasSize(1)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTask_deskActive_multiDesksEnabled_movesToDesk() {
        val deskId = 0
        taskRepository.setActiveDesk(DEFAULT_DISPLAY, deskId = deskId)
        setUpHomeTask()
        val fullscreenTask = createFullscreenTask()

        val wct = controller.handleRequest(Binder(), createTransition(fullscreenTask))

        assertNotNull(wct, "should handle request")
        verify(desksOrganizer).moveTaskToDesk(wct, deskId, fullscreenTask)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTaskWithTaskOnHome_freeformVisible_multiDesksDisabled_returnSwitchToFreeformWCT() {
        val homeTask = setUpHomeTask()
        val freeformTask = setUpFreeformTask()
        markTaskVisible(freeformTask)
        val fullscreenTask = createFullscreenTask()
        fullscreenTask.baseIntent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)

        val wct = controller.handleRequest(Binder(), createTransition(fullscreenTask))

        assertNotNull(wct, "should handle request")
        assertThat(wct.changes[fullscreenTask.token.asBinder()]?.windowingMode)
            .isEqualTo(WINDOWING_MODE_FREEFORM)

        // There are 5 hops that are happening in this case:
        // 1. Moving the fullscreen task to top as we add moveToDesktop() changes
        // 2. Bringing home task to front
        // 3. Pending intent for the wallpaper
        // 4. Bringing the existing freeform task to top
        // 5. Bringing the fullscreen task back at the top
        assertThat(wct.hierarchyOps).hasSize(5)
        wct.assertReorderAt(1, homeTask, toTop = true)
        wct.assertReorderAt(4, fullscreenTask, toTop = true)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTaskWithTaskOnHome_activeDesk_multiDesksEnabled_movesToDesk() {
        val deskId = 0
        setUpHomeTask()
        setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId)
        val fullscreenTask = createFullscreenTask()
        fullscreenTask.baseIntent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)

        val wct = controller.handleRequest(Binder(), createTransition(fullscreenTask))

        assertNotNull(wct, "should handle request")
        verify(desksOrganizer).moveTaskToDesk(wct, deskId, fullscreenTask)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTaskToDesk_underTaskLimit_multiDesksDisabled_dontMinimize() {
        val freeformTask = setUpFreeformTask()
        markTaskVisible(freeformTask)
        val fullscreenTask = createFullscreenTask()

        val wct = controller.handleRequest(Binder(), createTransition(fullscreenTask))

        // Make sure we only reorder the new task to top (we don't reorder the old task to bottom)
        assertThat(wct?.hierarchyOps?.size).isEqualTo(1)
        wct!!.assertReorderAt(0, fullscreenTask, toTop = true)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTaskToDesk_underTaskLimit_multiDesksEnabled_dontMinimize() {
        val deskId = 5
        taskRepository.addDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)
        taskRepository.setActiveDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)
        val freeformTask =
            setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId).also {
                markTaskVisible(it)
            }

        // Launch a fullscreen task while in the desk.
        val fullscreenTask = createFullscreenTask()
        val transition = Binder()
        val wct = controller.handleRequest(transition, createTransition(fullscreenTask))

        assertNotNull(wct)
        verify(desksOrganizer, never()).minimizeTask(eq(wct), eq(deskId), any())
        assertNull(desktopTasksLimiter.getMinimizingTask(transition))
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTaskToDesk_bringsTasksOverLimit_multiDesksDisabled_otherTaskIsMinimized() {
        val freeformTasks = (1..MAX_TASK_LIMIT).map { _ -> setUpFreeformTask() }
        freeformTasks.forEach { markTaskVisible(it) }
        val fullscreenTask = createFullscreenTask()

        val wct = controller.handleRequest(Binder(), createTransition(fullscreenTask))

        // Make sure we reorder the new task to top, and the back task to the bottom
        assertThat(wct!!.hierarchyOps.size).isEqualTo(2)
        wct.assertReorderAt(0, fullscreenTask, toTop = true)
        wct.assertReorderAt(1, freeformTasks[0], toTop = false)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTaskToDesk_bringsTasksOverLimit_multiDesksEnabled_otherTaskIsMinimized() {
        val deskId = 5
        taskRepository.addDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)
        taskRepository.setActiveDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)
        val freeformTasks =
            (1..MAX_TASK_LIMIT).map { _ ->
                setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId).also {
                    markTaskVisible(it)
                }
            }

        // Launch a fullscreen task while in the desk.
        setUpHomeTask()
        val fullscreenTask = createFullscreenTask()
        val transition = Binder()
        val wct = controller.handleRequest(transition, createTransition(fullscreenTask))

        assertNotNull(wct)
        verify(desksOrganizer).minimizeTask(wct, deskId, freeformTasks[0])
        val minimizingTask =
            assertNotNull(desktopTasksLimiter.getMinimizingTask(transition)?.taskId)
        assertThat(minimizingTask).isEqualTo(freeformTasks[0].taskId)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTaskWithTaskOnHome_bringsTasksOverLimit_multiDesksDisabled_otherTaskIsMinimized() {
        val freeformTasks = (1..MAX_TASK_LIMIT).map { _ -> setUpFreeformTask() }
        freeformTasks.forEach { markTaskVisible(it) }
        val fullscreenTask = createFullscreenTask()
        fullscreenTask.baseIntent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)

        val wct = controller.handleRequest(Binder(), createTransition(fullscreenTask))

        // Make sure we reorder the new task to top, and the back task to the bottom
        assertThat(wct!!.hierarchyOps.size).isEqualTo(8)
        wct.assertReorderAt(0, fullscreenTask, toTop = true)
        // Oldest task that needs to minimized is never reordered to top over Home.
        val taskToMinimize = freeformTasks[0]
        wct.assertWithoutHop { hop ->
            hop.container == taskToMinimize.token &&
                hop.type == HIERARCHY_OP_TYPE_REORDER &&
                hop.toTop == true
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTaskWithTaskOnHome_bringsTasksOverLimit_multiDesksEnabled_otherTaskIsMinimized() {
        val deskId = 5
        taskRepository.addDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)
        taskRepository.setActiveDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)
        val freeformTasks =
            (1..MAX_TASK_LIMIT).map { _ ->
                setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId)
            }
        freeformTasks.forEach { markTaskVisible(it) }
        val fullscreenTask = createFullscreenTask()
        fullscreenTask.baseIntent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)

        val wct = controller.handleRequest(Binder(), createTransition(fullscreenTask))

        assertNotNull(wct)
        // The launching task is moved to the desk.
        verify(desksOrganizer).moveTaskToDesk(wct, deskId, fullscreenTask)
        // The bottom-most task is minimized.
        verify(desksOrganizer).minimizeTask(wct, deskId, freeformTasks[0])
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTaskWithTaskOnHome_beyondLimit_multiDesksDisabled_existingAndNewTasksAreMinimized() {
        val minimizedTask = setUpFreeformTask()
        taskRepository.minimizeTask(displayId = DEFAULT_DISPLAY, taskId = minimizedTask.taskId)
        val freeformTasks = (1..MAX_TASK_LIMIT).map { _ -> setUpFreeformTask() }
        freeformTasks.forEach { markTaskVisible(it) }
        val homeTask = setUpHomeTask()
        val fullscreenTask = createFullscreenTask()
        fullscreenTask.baseIntent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)

        val wct = controller.handleRequest(Binder(), createTransition(fullscreenTask))

        assertThat(wct!!.hierarchyOps.size).isEqualTo(9)
        wct.assertReorderAt(0, fullscreenTask, toTop = true)
        // Make sure we reorder the home task to the top, desktop tasks to top of them and minimized
        // task is under the home task.
        wct.assertReorderAt(1, homeTask, toTop = true)
        // Oldest task that needs to minimized is never reordered to top over Home.
        val taskToMinimize = freeformTasks[0]
        wct.assertWithoutHop { hop ->
            hop.container == taskToMinimize.token &&
                hop.type == HIERARCHY_OP_TYPE_REORDER &&
                hop.toTop == true
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTaskWithTaskOnHome_beyondLimit_multiDesksEnabled_existingAndNewTasksAreMinimized() {
        // A desk with a minimized tasks, and non-minimized tasks already at the task limit.
        val deskId = 5
        taskRepository.addDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)
        taskRepository.setActiveDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)
        val minimizedTask = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId)
        taskRepository.minimizeTaskInDesk(
            displayId = DEFAULT_DISPLAY,
            deskId = deskId,
            taskId = minimizedTask.taskId,
        )
        val freeformTasks =
            (1..MAX_TASK_LIMIT).map { _ ->
                setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId).also {
                    markTaskVisible(it)
                }
            }

        // Launch a fullscreen task that brings Home to front with it.
        setUpHomeTask()
        val fullscreenTask = createFullscreenTask()
        fullscreenTask.baseIntent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)
        val wct = controller.handleRequest(Binder(), createTransition(fullscreenTask))

        assertNotNull(wct)
        verify(desksOrganizer).minimizeTask(wct, deskId, freeformTasks[0])
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTaskWithTaskOnHome_taskAddedToDesk() {
        // A desk with a minimized tasks, and non-minimized tasks already at the task limit.
        val deskId = 5
        taskRepository.addDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)
        taskRepository.setActiveDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)

        // Launch a fullscreen task that brings Home to front with it.
        setUpHomeTask()
        val fullscreenTask = createFullscreenTask()
        fullscreenTask.baseIntent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)
        val wct = controller.handleRequest(Binder(), createTransition(fullscreenTask))

        assertNotNull(wct)
        verify(desksOrganizer).moveTaskToDesk(wct, deskId, fullscreenTask)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_PER_DISPLAY_DESKTOP_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
    )
    fun handleRequest_fullscreenTaskWithTaskOnHome_activatesDesk() {
        val deskId = 5
        taskRepository.addDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)
        taskRepository.setActiveDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)

        // Launch a fullscreen task that brings Home to front with it.
        val homeTask = setUpHomeTask()
        val fullscreenTask = createFullscreenTask()
        fullscreenTask.baseIntent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)
        val transition = Binder()
        val wct = controller.handleRequest(transition, createTransition(fullscreenTask))

        assertNotNull(wct)
        wct.assertReorder(homeTask, toTop = true)
        wct.assertReorder(wallpaperToken, toTop = true)
        verify(desksOrganizer).activateDesk(wct, deskId)
        verify(desksTransitionsObserver)
            .addPendingTransition(
                DeskTransition.ActiveDeskWithTask(
                    token = transition,
                    displayId = DEFAULT_DISPLAY,
                    deskId = deskId,
                    enterTaskId = fullscreenTask.taskId,
                )
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTask_noTasks_enforceDesktop_freeformDisplay_returnFreeformWCT() {
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)
        whenever(DesktopModeStatus.enterDesktopByDefaultOnFreeformDisplay(context)).thenReturn(true)
        val tda = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY)!!
        tda.configuration.windowConfiguration.windowingMode = WINDOWING_MODE_FREEFORM

        val fullscreenTask = createFullscreenTask()
        val wct = controller.handleRequest(Binder(), createTransition(fullscreenTask))

        assertNotNull(wct, "should handle request")
        assertThat(wct.changes[fullscreenTask.token.asBinder()]?.windowingMode)
            .isEqualTo(WINDOWING_MODE_UNDEFINED)
        assertThat(wct.hierarchyOps).hasSize(3)
        // There are 3 hops that are happening in this case:
        // 1. Moving the fullscreen task to top as we add moveToDesktop() changes
        // 2. Pending intent for the wallpaper
        // 3. Bringing the fullscreen task back at the top
        wct.assertPendingIntentAt(1, desktopWallpaperIntent)
        wct.assertReorderAt(2, fullscreenTask, toTop = true)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun handleRequest_fullscreenTask_noInDesk_enforceDesktop_freeformDisplay_movesToDesk() {
        val deskId = 0
        taskRepository.setDeskInactive(deskId)
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)
        whenever(DesktopModeStatus.enterDesktopByDefaultOnFreeformDisplay(context)).thenReturn(true)
        val tda = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY)!!
        tda.configuration.windowConfiguration.windowingMode = WINDOWING_MODE_FREEFORM

        val fullscreenTask = createFullscreenTask()
        val wct = controller.handleRequest(Binder(), createTransition(fullscreenTask))

        assertNotNull(wct, "should handle request")
        verify(desksOrganizer).moveTaskToDesk(wct, deskId, fullscreenTask)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun handleRequest_fullscreenTask_noInDesk_enforceDesktop_secondaryDisplay_movesToDesk() {
        val deskId = 5
        taskRepository.addDesk(displayId = SECONDARY_DISPLAY_ID, deskId = deskId)
        taskRepository.setDeskInactive(deskId)
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)
        whenever(DesktopModeStatus.enterDesktopByDefaultOnFreeformDisplay(context)).thenReturn(true)

        val fullscreenTask = createFullscreenTask(displayId = SECONDARY_DISPLAY_ID)
        val wct = controller.handleRequest(Binder(), createTransition(fullscreenTask))

        assertNotNull(wct, "should handle request")
        verify(desksOrganizer).moveTaskToDesk(wct, deskId, fullscreenTask)
    }

    @Test
    fun handleRequest_fullscreenTask_notInDesk_enforceDesktop_fullscreenDisplay_returnNull() {
        taskRepository.setDeskInactive(deskId = 0)
        whenever(DesktopModeStatus.enterDesktopByDefaultOnFreeformDisplay(context)).thenReturn(true)
        val tda = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY)!!
        tda.configuration.windowConfiguration.windowingMode = WINDOWING_MODE_FULLSCREEN

        val fullscreenTask = createFullscreenTask()
        val wct = controller.handleRequest(Binder(), createTransition(fullscreenTask))

        assertThat(wct).isNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTask_freeformNotVisible_returnNull() {
        val freeformTask = setUpFreeformTask()
        markTaskHidden(freeformTask)
        val fullscreenTask = createFullscreenTask()
        assertThat(controller.handleRequest(Binder(), createTransition(fullscreenTask))).isNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTask_noOtherTasks_returnNull() {
        val fullscreenTask = createFullscreenTask()
        assertThat(controller.handleRequest(Binder(), createTransition(fullscreenTask))).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTask_notInDesk_returnNull() {
        taskRepository.setDeskInactive(deskId = 0)
        val fullscreenTask = createFullscreenTask()

        assertThat(controller.handleRequest(Binder(), createTransition(fullscreenTask))).isNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTask_freeformTaskOnOtherDisplay_returnNull() {
        val fullscreenTaskDefaultDisplay = createFullscreenTask(displayId = DEFAULT_DISPLAY)
        createFreeformTask(displayId = SECOND_DISPLAY)

        val result =
            controller.handleRequest(Binder(), createTransition(fullscreenTaskDefaultDisplay))
        assertThat(result).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_fullscreenTask_deskInOtherDisplayActive_returnNull() {
        taskRepository.setDeskInactive(deskId = 0)
        val fullscreenTaskDefaultDisplay = createFullscreenTask(displayId = DEFAULT_DISPLAY)
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = 2)
        taskRepository.setActiveDesk(displayId = SECOND_DISPLAY, deskId = 2)

        val result =
            controller.handleRequest(Binder(), createTransition(fullscreenTaskDefaultDisplay))

        assertThat(result).isNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_freeformTask_freeformVisible_aboveTaskLimit_multiDesksDisabled_minimize() {
        val deskId = 0
        val freeformTasks =
            (1..MAX_TASK_LIMIT).map { _ ->
                setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId)
            }
        freeformTasks.forEach { markTaskVisible(it) }
        val newFreeformTask = createFreeformTask()

        val wct =
            controller.handleRequest(Binder(), createTransition(newFreeformTask, TRANSIT_OPEN))

        assertThat(wct?.hierarchyOps?.size).isEqualTo(1)
        wct!!.assertReorderAt(0, freeformTasks[0], toTop = false) // Reorder to the bottom
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_freeformTask_freeformVisible_aboveTaskLimit_multiDesksEnabled_minimize() {
        val deskId = 0
        val freeformTasks =
            (1..MAX_TASK_LIMIT).map { _ ->
                setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId)
            }
        freeformTasks.forEach { markTaskVisible(it) }
        val newFreeformTask = createFreeformTask()

        val wct =
            controller.handleRequest(Binder(), createTransition(newFreeformTask, TRANSIT_OPEN))

        assertNotNull(wct)
        verify(desksOrganizer).minimizeTask(wct, deskId, freeformTasks[0])
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_freeformTaskFromInactiveDesk_tracksDeskDeactivation() {
        val deskId = 0
        val freeformTask = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId)
        taskRepository.setDeskInactive(deskId = deskId)

        val transition = Binder()
        controller.handleRequest(transition, createTransition(freeformTask))

        verify(desksTransitionsObserver)
            .addPendingTransition(DeskTransition.DeactivateDesk(transition, deskId))
    }

    @Test
    fun handleRequest_freeformTask_relaunchActiveTask_taskBecomesUndefined() {
        taskRepository.setDeskInactive(deskId = 0)
        val freeformTask = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = 0)
        markTaskHidden(freeformTask)

        val wct = controller.handleRequest(Binder(), createTransition(freeformTask))

        // Should become undefined as the TDA is set to fullscreen. It will inherit from the TDA.
        assertNotNull(wct, "should handle request")
        assertThat(wct.changes[freeformTask.token.asBinder()]?.windowingMode)
            .isEqualTo(WINDOWING_MODE_UNDEFINED)
    }

    @Test
    fun handleRequest_freeformTask_relaunchTask_enforceDesktop_freeformDisplay_noWinModeChange() {
        whenever(DesktopModeStatus.enterDesktopByDefaultOnFreeformDisplay(context)).thenReturn(true)
        val tda = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY)!!
        tda.configuration.windowConfiguration.windowingMode = WINDOWING_MODE_FREEFORM

        val freeformTask = setUpFreeformTask()
        markTaskHidden(freeformTask)
        val wct = controller.handleRequest(Binder(), createTransition(freeformTask))

        assertNotNull(wct, "should handle request")
        assertFalse(wct.anyWindowingModeChange(freeformTask.token))
    }

    @Test
    fun handleRequest_freeformTask_relaunchTask_enforceDesktop_fullscreenDisplay_becomesUndefined() {
        whenever(DesktopModeStatus.enterDesktopByDefaultOnFreeformDisplay(context)).thenReturn(true)
        val tda = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY)!!
        tda.configuration.windowConfiguration.windowingMode = WINDOWING_MODE_FULLSCREEN
        taskRepository.setDeskInactive(deskId = 0)
        val freeformTask = setUpFreeformTask(DEFAULT_DISPLAY, deskId = 0)
        markTaskHidden(freeformTask)

        val wct = controller.handleRequest(Binder(), createTransition(freeformTask))

        assertNotNull(wct, "should handle request")
        assertThat(wct.changes[freeformTask.token.asBinder()]?.windowingMode)
            .isEqualTo(WINDOWING_MODE_UNDEFINED)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun handleRequest_freeformTask_desktopWallpaperDisabled_freeformNotVisible_reorderedToTop() {
        val freeformTask1 = setUpFreeformTask()
        val freeformTask2 = createFreeformTask()

        markTaskHidden(freeformTask1)
        val result =
            controller.handleRequest(
                Binder(),
                createTransition(freeformTask2, type = TRANSIT_TO_FRONT),
            )

        assertNotNull(result, "Should handle request")
        assertThat(result.hierarchyOps?.size).isEqualTo(2)
        result.assertReorderAt(1, freeformTask2, toTop = true)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_freeformTask_desktopWallpaperEnabled_freeformNotVisible_multiDesksDisabled_reorderedToTop() {
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)
        val freeformTask1 = setUpFreeformTask()
        val freeformTask2 = createFreeformTask()

        markTaskHidden(freeformTask1)
        val result =
            controller.handleRequest(
                Binder(),
                createTransition(freeformTask2, type = TRANSIT_TO_FRONT),
            )

        assertNotNull(result, "Should handle request")
        assertThat(result.hierarchyOps?.size).isEqualTo(3)
        // Add desktop wallpaper activity
        result.assertPendingIntentAt(0, desktopWallpaperIntent)
        // Bring active desktop tasks to front
        result.assertReorderAt(1, freeformTask1, toTop = true)
        // Bring new task to front
        result.assertReorderAt(2, freeformTask2, toTop = true)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun handleRequest_freeformTask_desktopWallpaperEnabled_notInDesk_reorderedToTop() {
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)
        val deskId = 0
        taskRepository.setDeskInactive(deskId)
        val freeformTask1 = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId)
        val freeformTask2 = createFreeformTask()

        val wct =
            controller.handleRequest(
                Binder(),
                createTransition(freeformTask2, type = TRANSIT_TO_FRONT),
            )

        assertNotNull(wct, "Should handle request")
        verify(desksOrganizer).reorderTaskToFront(wct, deskId, freeformTask1)
        wct.assertReorder(freeformTask2, toTop = true)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun handleRequest_freeformTask_desktopWallpaperDisabled_noOtherTasks_reorderedToTop() {
        val task = createFreeformTask()
        val result = controller.handleRequest(Binder(), createTransition(task))

        assertNotNull(result, "Should handle request")
        assertThat(result.hierarchyOps?.size).isEqualTo(1)
        result.assertReorderAt(0, task, toTop = true)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun handleRequest_freeformTask_dskWallpaperDisabled_freeformOnOtherDisplayOnly_reorderedToTop() {
        val taskDefaultDisplay = createFreeformTask(displayId = DEFAULT_DISPLAY)
        // Second display task
        createFreeformTask(displayId = SECOND_DISPLAY)

        val result = controller.handleRequest(Binder(), createTransition(taskDefaultDisplay))

        assertNotNull(result, "Should handle request")
        assertThat(result.hierarchyOps?.size).isEqualTo(1)
        result.assertReorderAt(0, taskDefaultDisplay, toTop = true)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun handleRequest_freeformTask_dskWallpaperEnabled_freeformOnOtherDisplayOnly_reorderedToTop() {
        taskRepository.setDeskInactive(deskId = 0)
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)
        val taskDefaultDisplay = createFreeformTask(displayId = DEFAULT_DISPLAY)
        // Second display task
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = 2)
        taskRepository.setActiveDesk(displayId = SECOND_DISPLAY, deskId = 2)
        setUpFreeformTask(displayId = SECOND_DISPLAY, deskId = 2)

        val result = controller.handleRequest(Binder(), createTransition(taskDefaultDisplay))

        assertNotNull(result, "Should handle request")
        assertThat(result.hierarchyOps?.size).isEqualTo(2)
        // Add desktop wallpaper activity
        result.assertPendingIntentAt(0, desktopWallpaperIntent)
        // Bring new task to front
        result.assertReorderAt(1, taskDefaultDisplay, toTop = true)
    }

    @Test
    fun handleRequest_freeformTask_alreadyInDesktop_noOverrideDensity_noConfigDensityChange() {
        whenever(DesktopModeStatus.useDesktopOverrideDensity()).thenReturn(false)

        val freeformTask1 = setUpFreeformTask()
        markTaskVisible(freeformTask1)

        val freeformTask2 = createFreeformTask()
        val result =
            controller.handleRequest(
                freeformTask2.token.asBinder(),
                createTransition(freeformTask2),
            )
        assertFalse(result.anyDensityConfigChange(freeformTask2.token))
    }

    @Test
    fun handleRequest_freeformTask_alreadyInDesktop_overrideDensity_hasConfigDensityChange() {
        whenever(DesktopModeStatus.useDesktopOverrideDensity()).thenReturn(true)

        val freeformTask1 = setUpFreeformTask()
        markTaskVisible(freeformTask1)

        val freeformTask2 = createFreeformTask()
        val result =
            controller.handleRequest(
                freeformTask2.token.asBinder(),
                createTransition(freeformTask2),
            )
        assertTrue(result.anyDensityConfigChange(freeformTask2.token))
    }

    @Test
    fun handleRequest_freeformTask_keyguardLocked_returnNull() {
        whenever(keyguardManager.isKeyguardLocked).thenReturn(true)
        val freeformTask = createFreeformTask(displayId = DEFAULT_DISPLAY)

        val result = controller.handleRequest(Binder(), createTransition(freeformTask))

        assertNull(result, "Should NOT handle request")
    }

    @Test
    fun handleRequest_notOpenOrToFrontTransition_returnNull() {
        val task =
            TestRunningTaskInfoBuilder()
                .setActivityType(ACTIVITY_TYPE_STANDARD)
                .setWindowingMode(WINDOWING_MODE_FULLSCREEN)
                .build()
        val transition = createTransition(task = task, type = TRANSIT_CLOSE)
        val result = controller.handleRequest(Binder(), transition)
        assertThat(result).isNull()
    }

    @Test
    fun handleRequest_noTriggerTask_returnNull() {
        assertThat(controller.handleRequest(Binder(), createTransition(task = null))).isNull()
    }

    @Test
    fun handleRequest_triggerTaskNotStandard_returnNull() {
        val task = TestRunningTaskInfoBuilder().setActivityType(ACTIVITY_TYPE_HOME).build()
        assertThat(controller.handleRequest(Binder(), createTransition(task))).isNull()
    }

    @Test
    fun handleRequest_triggerTaskNotFullscreenOrFreeform_returnNull() {
        val task =
            TestRunningTaskInfoBuilder()
                .setActivityType(ACTIVITY_TYPE_STANDARD)
                .setWindowingMode(WINDOWING_MODE_MULTI_WINDOW)
                .build()
        assertThat(controller.handleRequest(Binder(), createTransition(task))).isNull()
    }

    @Test
    fun handleRequest_recentsAnimationRunning_returnNull() {
        // Set up a visible freeform task so a fullscreen task should be converted to freeform
        val freeformTask = setUpFreeformTask()
        markTaskVisible(freeformTask)

        // Mark recents animation running
        recentsTransitionStateListener.onTransitionStateChanged(TRANSITION_STATE_ANIMATING)

        // Open a fullscreen task, check that it does not result in a WCT with changes to it
        val fullscreenTask = createFullscreenTask()
        assertThat(controller.handleRequest(Binder(), createTransition(fullscreenTask))).isNull()
    }

    @Test
    fun handleRequest_recentsAnimationRunning_relaunchActiveTask_taskBecomesUndefined() {
        // Set up a visible freeform task
        val freeformTask = setUpFreeformTask()
        markTaskVisible(freeformTask)

        // Mark recents animation running
        recentsTransitionStateListener.onTransitionStateChanged(TRANSITION_STATE_ANIMATING)

        // Should become undefined as the TDA is set to fullscreen. It will inherit from the TDA.
        val result = controller.handleRequest(Binder(), createTransition(freeformTask))
        assertThat(result?.changes?.get(freeformTask.token.asBinder())?.windowingMode)
            .isEqualTo(WINDOWING_MODE_UNDEFINED)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_recentsAnimationRunning_relaunchActiveTask_tracksDeskDeactivation() {
        // Set up a visible freeform task
        val freeformTask = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = 0)
        markTaskVisible(freeformTask)

        // Mark recents animation running
        recentsTransitionStateListener.onTransitionStateChanged(TRANSITION_STATE_ANIMATING)

        val transition = Binder()
        controller.handleRequest(transition, createTransition(freeformTask))

        desksTransitionsObserver.addPendingTransition(
            DeskTransition.DeactivateDesk(transition, deskId = 0)
        )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_topActivityTransparentWithoutDisplay_multiDesksDisabled_returnSwitchToFreeformWCT() {
        val freeformTask = setUpFreeformTask()
        markTaskVisible(freeformTask)

        val task =
            setUpFullscreenTask().apply {
                isActivityStackTransparent = true
                isTopActivityNoDisplay = true
                numActivities = 1
            }

        val result = controller.handleRequest(Binder(), createTransition(task))
        assertThat(result?.changes?.get(task.token.asBinder())?.windowingMode)
            .isEqualTo(WINDOWING_MODE_FREEFORM)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun handleRequest_topActivityTransparentWithoutDisplay_multiDesksEnabled_returnSwitchToFreeformWCT() {
        val deskId = 0
        val freeformTask = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId)
        markTaskVisible(freeformTask)

        val task =
            setUpFullscreenTask().apply {
                isActivityStackTransparent = true
                isTopActivityNoDisplay = true
                numActivities = 1
            }

        val wct = controller.handleRequest(Binder(), createTransition(task))

        assertNotNull(wct)
        verify(desksOrganizer).moveTaskToDesk(wct, deskId, task)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY)
    @DisableFlags(Flags.FLAG_ENABLE_MODALS_FULLSCREEN_WITH_PERMISSION)
    fun handleRequest_topActivityTransparentWithDisplay_returnSwitchToFullscreenWCT() {
        val freeformTask = setUpFreeformTask()
        markTaskVisible(freeformTask)

        val task =
            setUpFreeformTask().apply {
                isActivityStackTransparent = true
                isTopActivityNoDisplay = false
                numActivities = 1
            }

        val result = controller.handleRequest(Binder(), createTransition(task))
        assertThat(result?.changes?.get(task.token.asBinder())?.windowingMode)
            .isEqualTo(WINDOWING_MODE_UNDEFINED) // inherited FULLSCREEN
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY,
        Flags.FLAG_INCLUDE_TOP_TRANSPARENT_FULLSCREEN_TASK_IN_DESKTOP_HEURISTIC,
    )
    @DisableFlags(Flags.FLAG_ENABLE_MODALS_FULLSCREEN_WITH_PERMISSION)
    fun handleRequest_topActivityTransparentWithDisplay_savedToDesktopRepository() {
        val freeformTask = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        markTaskVisible(freeformTask)

        val transparentTask =
            setUpFreeformTask(displayId = DEFAULT_DISPLAY).apply {
                isActivityStackTransparent = true
                isTopActivityNoDisplay = false
                numActivities = 1
            }

        controller.handleRequest(Binder(), createTransition(transparentTask))
        assertThat(taskRepository.getTopTransparentFullscreenTaskId(DEFAULT_DISPLAY))
            .isEqualTo(transparentTask.taskId)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY,
        Flags.FLAG_INCLUDE_TOP_TRANSPARENT_FULLSCREEN_TASK_IN_DESKTOP_HEURISTIC,
    )
    @DisableFlags(Flags.FLAG_ENABLE_MODALS_FULLSCREEN_WITH_PERMISSION)
    fun handleRequest_desktopNotShowing_topTransparentFullscreenTask_notSavedToDesktopRepository() {
        val task = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)

        controller.handleRequest(Binder(), createTransition(task))
        assertThat(taskRepository.getTopTransparentFullscreenTaskId(DEFAULT_DISPLAY)).isNull()
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY,
        Flags.FLAG_INCLUDE_TOP_TRANSPARENT_FULLSCREEN_TASK_IN_DESKTOP_HEURISTIC,
    )
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_onlyTopTransparentFullscreenTask_multiDesksDisabled_returnSwitchToFreeformWCT() {
        val topTransparentTask = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)
        taskRepository.setTopTransparentFullscreenTaskId(DEFAULT_DISPLAY, topTransparentTask.taskId)

        val task = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)

        val result = controller.handleRequest(Binder(), createTransition(task))
        assertThat(result?.changes?.get(task.token.asBinder())?.windowingMode)
            .isEqualTo(WINDOWING_MODE_FREEFORM)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY,
        Flags.FLAG_INCLUDE_TOP_TRANSPARENT_FULLSCREEN_TASK_IN_DESKTOP_HEURISTIC,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun handleRequest_onlyTopTransparentFullscreenTask_multiDesksEnabled_movesToDesktop() {
        val deskId = 0
        val topTransparentTask = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)
        taskRepository.setTopTransparentFullscreenTaskId(DEFAULT_DISPLAY, topTransparentTask.taskId)
        taskRepository.setDeskInactive(deskId = deskId)

        val task = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)

        val wct = controller.handleRequest(Binder(), createTransition(task))
        assertNotNull(wct)
        verify(desksOrganizer).moveTaskToDesk(wct, deskId, task)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY)
    fun handleRequest_desktopNotShowing_topTransparentFullscreenTask_returnNull() {
        taskRepository.setDeskInactive(deskId = 0)
        val task = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)

        assertThat(controller.handleRequest(Binder(), createTransition(task))).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY)
    fun handleRequest_systemUIActivityWithDisplay_returnSwitchToFullscreenWCT() {
        val freeformTask = setUpFreeformTask()
        markTaskVisible(freeformTask)

        // Set task as systemUI package
        val systemUIPackageName =
            context.resources.getString(com.android.internal.R.string.config_systemUi)
        val baseComponent = ComponentName(systemUIPackageName, /* cls= */ "")
        val task =
            setUpFreeformTask().apply {
                baseActivity = baseComponent
                isTopActivityNoDisplay = false
            }

        val result = controller.handleRequest(Binder(), createTransition(task))
        assertThat(result?.changes?.get(task.token.asBinder())?.windowingMode)
            .isEqualTo(WINDOWING_MODE_UNDEFINED) // inherited FULLSCREEN
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY,
    )
    fun handleRequest_systemUIActivityWithDisplayInFreeformTask_inDesktop_tracksDeskDeactivation() {
        val deskId = 5
        taskRepository.addDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)
        taskRepository.setActiveDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)
        val systemUIPackageName =
            context.resources.getString(com.android.internal.R.string.config_systemUi)
        val baseComponent = ComponentName(systemUIPackageName, /* cls= */ "")
        val task =
            setUpFreeformTask(displayId = DEFAULT_DISPLAY).apply {
                baseActivity = baseComponent
                isTopActivityNoDisplay = false
            }

        val transition = Binder()
        controller.handleRequest(transition, createTransition(task))

        verify(desksTransitionsObserver)
            .addPendingTransition(DeskTransition.DeactivateDesk(transition, deskId))
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun handleRequest_systemUIActivityWithoutDisplay_multiDesksDisabled_returnSwitchToFreeformWCT() {
        val freeformTask = setUpFreeformTask()
        markTaskVisible(freeformTask)

        // Set task as systemUI package
        val systemUIPackageName =
            context.resources.getString(com.android.internal.R.string.config_systemUi)
        val baseComponent = ComponentName(systemUIPackageName, /* cls= */ "")
        val task =
            setUpFullscreenTask().apply {
                baseActivity = baseComponent
                isTopActivityNoDisplay = true
            }

        val result = controller.handleRequest(Binder(), createTransition(task))
        assertThat(result?.changes?.get(task.token.asBinder())?.windowingMode)
            .isEqualTo(WINDOWING_MODE_FREEFORM)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun handleRequest_systemUIActivityWithoutDisplay_multiDesksEnabled_movesTaskToDesk() {
        val deskId = 0
        val freeformTask = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId)
        markTaskVisible(freeformTask)

        // Set task as systemUI package
        val systemUIPackageName =
            context.resources.getString(com.android.internal.R.string.config_systemUi)
        val baseComponent = ComponentName(systemUIPackageName, /* cls= */ "")
        val task =
            setUpFullscreenTask(displayId = DEFAULT_DISPLAY).apply {
                baseActivity = baseComponent
                isTopActivityNoDisplay = true
            }

        val wct = controller.handleRequest(Binder(), createTransition(task))

        assertNotNull(wct)
        verify(desksOrganizer).moveTaskToDesk(wct, deskId, task)
    }

    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY)
    fun handleRequest_defaultHomePackageWithDisplay_returnSwitchToFullscreenWCT() {
        val freeformTask = setUpFreeformTask()
        markTaskVisible(freeformTask)

        val packageManager: PackageManager = org.mockito.kotlin.mock()
        val homeActivities = ComponentName("defaultHomePackage", /* class */ "")
        val task =
            setUpFullscreenTask().apply {
                baseActivity = homeActivities
                isTopActivityNoDisplay = false
            }
        mContext.setMockPackageManager(packageManager)
        whenever(packageManager.getHomeActivities(any())).thenReturn(homeActivities)

        val result = controller.handleRequest(Binder(), createTransition(task))
        assertThat(result?.changes?.get(task.token.asBinder())?.windowingMode)
            .isEqualTo(WINDOWING_MODE_FREEFORM)
    }

    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MODALS_POLICY)
    fun handleRequest_defaultHomePackageWithoutDisplay_returnSwitchToFreeformWCT() {
        val freeformTask = setUpFreeformTask()
        markTaskVisible(freeformTask)

        val packageManager: PackageManager = org.mockito.kotlin.mock()
        val homeActivities = ComponentName("defaultHomePackage", /* class */ "")
        val task =
            setUpFullscreenTask().apply {
                baseActivity = homeActivities
                isTopActivityNoDisplay = false
            }
        mContext.setMockPackageManager(packageManager)
        whenever(packageManager.getHomeActivities(any())).thenReturn(homeActivities)

        val result = controller.handleRequest(Binder(), createTransition(task))
        assertThat(result?.changes?.get(task.token.asBinder())?.windowingMode)
            .isEqualTo(WINDOWING_MODE_FREEFORM)
    }

    @Test
    fun handleRequest_systemUIActivityWithDisplay_returnSwitchToFullscreenWCT_enforcedDesktop() {
        taskRepository.setDeskInactive(deskId = 0)
        whenever(DesktopModeStatus.enterDesktopByDefaultOnFreeformDisplay(context)).thenReturn(true)
        val tda = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY)!!
        tda.configuration.windowConfiguration.windowingMode = WINDOWING_MODE_FREEFORM
        // Set task as systemUI package
        val systemUIPackageName =
            context.resources.getString(com.android.internal.R.string.config_systemUi)
        val baseComponent = ComponentName(systemUIPackageName, /* cls= */ "")
        val task =
            createFreeformTask().apply {
                baseActivity = baseComponent
                isTopActivityNoDisplay = false
            }

        assertThat(controller.isDesktopModeShowing(DEFAULT_DISPLAY)).isFalse()
        val result = controller.handleRequest(Binder(), createTransition(task))
        assertThat(result?.changes?.get(task.token.asBinder())?.windowingMode)
            .isEqualTo(WINDOWING_MODE_FULLSCREEN)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun handleRequest_backTransition_singleTaskNoToken_noWallpaper_doesNotHandle() {
        val task = setUpFreeformTask()

        val result =
            controller.handleRequest(Binder(), createTransition(task, type = TRANSIT_TO_BACK))

        assertNull(result, "Should not handle request")
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun handleRequest_backTransition_singleTaskNoToken_withWallpaper_launchesHome() {
        val task = setUpFreeformTask()
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)

        val result =
            controller.handleRequest(Binder(), createTransition(task, type = TRANSIT_TO_BACK))

        // Should launch home
        assertNotNull(result, "Should handle request")
            .assertPendingIntentAt(0, launchHomeIntent(DEFAULT_DISPLAY))
        result!!.assertPendingIntentActivityOptionsLaunchDisplayIdAt(0, DEFAULT_DISPLAY)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun handleRequest_backTransition_singleTaskNoToken_withWallpaper_notInDesktop_doesNotHandle() {
        val task = setUpFreeformTask()
        markTaskHidden(task)

        val result =
            controller.handleRequest(Binder(), createTransition(task, type = TRANSIT_TO_BACK))

        assertNull(result, "Should not handle request")
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun handleRequest_backTransition_singleTaskNoToken_launchesHomes() {
        val task = setUpFreeformTask()
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)

        val result =
            controller.handleRequest(Binder(), createTransition(task, type = TRANSIT_TO_BACK))

        // Should launch home
        assertNotNull(result, "Should handle request")
            .assertPendingIntentAt(0, launchHomeIntent(DEFAULT_DISPLAY))
        result!!.assertPendingIntentActivityOptionsLaunchDisplayIdAt(0, DEFAULT_DISPLAY)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun handleRequest_backTransition_singleTaskWithToken_noWallpaper_doesNotHandle() {
        val task = setUpFreeformTask()

        val result =
            controller.handleRequest(Binder(), createTransition(task, type = TRANSIT_TO_BACK))

        assertNull(result, "Should not handle request")
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
    )
    fun handleRequest_backTransition_singleTaskWithToken_removesWallpaper() {
        val task = setUpFreeformTask()

        val result =
            controller.handleRequest(Binder(), createTransition(task, type = TRANSIT_TO_BACK))

        // Should create remove wallpaper transaction
        assertNotNull(result, "Should handle request")
            .assertReorderAt(index = 0, wallpaperToken, toTop = false)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun handleRequest_backTransition_multipleTasks_noWallpaper_doesNotHandle() {
        val task1 = setUpFreeformTask()
        setUpFreeformTask()

        val result =
            controller.handleRequest(Binder(), createTransition(task1, type = TRANSIT_TO_BACK))

        assertNull(result, "Should not handle request")
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun handleRequest_backTransition_multipleTasks_doesNotHandle() {
        val task1 = setUpFreeformTask()
        setUpFreeformTask()

        val result =
            controller.handleRequest(Binder(), createTransition(task1, type = TRANSIT_TO_BACK))

        assertNull(result, "Should not handle request")
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_BACK_NAVIGATION,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
    )
    fun handleRequest_backTransition_multipleTasksSingleNonClosing_removesWallpaperAndTask() {
        val task1 = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        val task2 = setUpFreeformTask(displayId = DEFAULT_DISPLAY)

        taskRepository.addClosingTask(
            displayId = DEFAULT_DISPLAY,
            deskId = 0,
            taskId = task2.taskId,
        )
        val result =
            controller.handleRequest(Binder(), createTransition(task1, type = TRANSIT_TO_BACK))

        // Should create remove wallpaper transaction
        assertNotNull(result, "Should handle request")
            .assertReorderAt(index = 0, wallpaperToken, toTop = false)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
    )
    fun handleRequest_backTransition_multipleTasksSingleNonMinimized_removesWallpaperAndTask() {
        val task1 = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        val task2 = setUpFreeformTask(displayId = DEFAULT_DISPLAY)

        taskRepository.minimizeTask(displayId = DEFAULT_DISPLAY, taskId = task2.taskId)
        val result =
            controller.handleRequest(Binder(), createTransition(task1, type = TRANSIT_TO_BACK))

        // Should create remove wallpaper transaction
        assertNotNull(result, "Should handle request")
            .assertReorderAt(index = 0, wallpaperToken, toTop = false)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun handleRequest_backTransition_nonMinimizadTask_withWallpaper_removesWallpaper() {
        val task1 = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        val task2 = setUpFreeformTask(displayId = DEFAULT_DISPLAY)

        taskRepository.minimizeTask(displayId = DEFAULT_DISPLAY, taskId = task2.taskId)
        // Task is being minimized so mark it as not visible.
        taskRepository.updateTask(displayId = DEFAULT_DISPLAY, task2.taskId, isVisible = false)
        val result =
            controller.handleRequest(Binder(), createTransition(task2, type = TRANSIT_TO_BACK))

        assertNull(result, "Should not handle request")
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun handleRequest_closeTransition_singleTaskNoToken_noWallpaper_doesNotHandle() {
        val task = setUpFreeformTask()

        val result =
            controller.handleRequest(Binder(), createTransition(task, type = TRANSIT_CLOSE))

        assertNull(result, "Should not handle request")
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun handleRequest_closeTransition_singleTaskNoToken_launchesHome() {
        val task = setUpFreeformTask()
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)

        val result =
            controller.handleRequest(Binder(), createTransition(task, type = TRANSIT_CLOSE))

        // Should launch home
        assertNotNull(result, "Should handle request")
            .assertPendingIntentAt(0, launchHomeIntent(DEFAULT_DISPLAY))
        result!!.assertPendingIntentActivityOptionsLaunchDisplayIdAt(0, DEFAULT_DISPLAY)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    @DisableFlags(Flags.FLAG_ENABLE_MODALS_FULLSCREEN_WITH_PERMISSION)
    fun handleRequest_closeTransition_singleTaskNoToken_secondaryDisplay_launchesHome() {
        taskRepository.addDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        taskRepository.setActiveDesk(displayId = SECOND_DISPLAY, deskId = SECOND_DISPLAY)
        val task = setUpFreeformTask(displayId = SECOND_DISPLAY)
        whenever(desktopWallpaperActivityTokenProvider.getToken()).thenReturn(null)

        val result =
            controller.handleRequest(Binder(), createTransition(task, type = TRANSIT_CLOSE))

        // Should launch home
        assertNotNull(result, "Should handle request")
            .assertPendingIntentAt(0, launchHomeIntent(SECOND_DISPLAY))
        result!!.assertPendingIntentActivityOptionsLaunchDisplayIdAt(0, SECOND_DISPLAY)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun handleRequest_closeTransition_singleTaskWithToken_noWallpaper_doesNotHandle() {
        val task = setUpFreeformTask()

        val result =
            controller.handleRequest(Binder(), createTransition(task, type = TRANSIT_CLOSE))

        assertNull(result, "Should not handle request")
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
    )
    fun handleRequest_closeTransition_singleTaskWithToken_withWallpaper_removesWallpaper() {
        val task = setUpFreeformTask()

        val result =
            controller.handleRequest(Binder(), createTransition(task, type = TRANSIT_CLOSE))

        // Should create remove wallpaper transaction
        assertNotNull(result, "Should handle request")
            .assertReorderAt(index = 0, wallpaperToken, toTop = false)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun handleRequest_closeTransition_onlyDesktopTask_deactivatesDesk() {
        val task = setUpFreeformTask()

        controller.handleRequest(Binder(), createTransition(task, type = TRANSIT_CLOSE))

        verify(desksOrganizer).deactivateDesk(any(), /* deskId= */ eq(0))
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun handleRequest_closeTransition_onlyDesktopTask_addsDeactivatesDeskTransition() {
        val transition = Binder()
        val task = setUpFreeformTask()

        controller.handleRequest(transition, createTransition(task, type = TRANSIT_CLOSE))

        verify(desksTransitionsObserver)
            .addPendingTransition(DeskTransition.DeactivateDesk(token = transition, deskId = 0))
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun handleRequest_closeTransition_multipleTasks_noWallpaper_doesNotHandle() {
        val task1 = setUpFreeformTask()
        setUpFreeformTask()

        val result =
            controller.handleRequest(Binder(), createTransition(task1, type = TRANSIT_CLOSE))

        assertNull(result, "Should not handle request")
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun handleRequest_closeTransition_multipleTasksFlagEnabled_doesNotHandle() {
        val task1 = setUpFreeformTask()
        setUpFreeformTask()

        val result =
            controller.handleRequest(Binder(), createTransition(task1, type = TRANSIT_CLOSE))

        assertNull(result, "Should not handle request")
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
    )
    fun handleRequest_closeTransition_multipleTasksSingleNonClosing_removesWallpaper() {
        val task1 = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        val task2 = setUpFreeformTask(displayId = DEFAULT_DISPLAY)

        taskRepository.addClosingTask(
            displayId = DEFAULT_DISPLAY,
            deskId = 0,
            taskId = task2.taskId,
        )
        val result =
            controller.handleRequest(Binder(), createTransition(task1, type = TRANSIT_CLOSE))

        // Should create remove wallpaper transaction
        assertNotNull(result, "Should handle request")
            .assertReorderAt(index = 0, wallpaperToken, toTop = false)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
    )
    fun handleRequest_closeTransition_multipleTasksSingleNonMinimized_removesWallpaper() {
        val task1 = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        val task2 = setUpFreeformTask(displayId = DEFAULT_DISPLAY)

        taskRepository.minimizeTask(displayId = DEFAULT_DISPLAY, taskId = task2.taskId)
        val result =
            controller.handleRequest(Binder(), createTransition(task1, type = TRANSIT_CLOSE))

        // Should create remove wallpaper transaction
        assertNotNull(result, "Should handle request")
            .assertReorderAt(index = 0, wallpaperToken, toTop = false)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY)
    fun handleRequest_closeTransition_minimizadTask_withWallpaper_removesWallpaper() {
        val task1 = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        val task2 = setUpFreeformTask(displayId = DEFAULT_DISPLAY)

        taskRepository.minimizeTask(displayId = DEFAULT_DISPLAY, taskId = task2.taskId)
        // Task is being minimized so mark it as not visible.
        taskRepository.updateTask(displayId = DEFAULT_DISPLAY, task2.taskId, isVisible = false)
        val result =
            controller.handleRequest(Binder(), createTransition(task2, type = TRANSIT_TO_BACK))

        assertNull(result, "Should not handle request")
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveFocusedTaskToDesktop_fullscreenTaskIsMovedToDesktop_multiDesksDisabled() {
        val task1 = setUpFullscreenTask()
        val task2 = setUpFullscreenTask()
        val task3 = setUpFullscreenTask()

        task1.isFocused = true
        task2.isFocused = false
        task3.isFocused = false

        controller.moveFocusedTaskToDesktop(DEFAULT_DISPLAY, transitionSource = UNKNOWN)

        val wct = getLatestEnterDesktopWct()
        assertThat(wct.changes[task1.token.asBinder()]?.windowingMode)
            .isEqualTo(WINDOWING_MODE_FREEFORM)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveFocusedTaskToDesktop_fullscreenTaskIsMovedToDesktop_multiDesksEnabled() {
        val task1 = setUpFullscreenTask()
        val task2 = setUpFullscreenTask()
        val task3 = setUpFullscreenTask()

        task1.isFocused = true
        task2.isFocused = false
        task3.isFocused = false

        controller.moveFocusedTaskToDesktop(DEFAULT_DISPLAY, transitionSource = UNKNOWN)

        val wct = getLatestEnterDesktopWct()
        verify(desksOrganizer).moveTaskToDesk(wct, deskId = 0, task1)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveFocusedTaskToDesktop_splitScreenTaskIsMovedToDesktop_multiDesksDisabled() {
        val task1 = setUpSplitScreenTask()
        val task2 = setUpFullscreenTask()
        val task3 = setUpFullscreenTask()
        val task4 = setUpSplitScreenTask()

        task1.isFocused = true
        task2.isFocused = false
        task3.isFocused = false
        task4.isFocused = true

        task4.parentTaskId = task1.taskId

        controller.moveFocusedTaskToDesktop(DEFAULT_DISPLAY, transitionSource = UNKNOWN)

        val wct = getLatestEnterDesktopWct()
        assertThat(wct.changes[task4.token.asBinder()]?.windowingMode)
            .isEqualTo(WINDOWING_MODE_FREEFORM)
        verify(splitScreenController)
            .prepareExitSplitScreen(
                any(),
                anyInt(),
                eq(SplitScreenController.EXIT_REASON_DESKTOP_MODE),
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveFocusedTaskToDesktop_splitScreenTaskIsMovedToDesktop_multiDesksEnabled() {
        val task1 = setUpSplitScreenTask()
        val task2 = setUpFullscreenTask()
        val task3 = setUpFullscreenTask()
        val task4 = setUpSplitScreenTask()

        task1.isFocused = true
        task2.isFocused = false
        task3.isFocused = false
        task4.isFocused = true

        task4.parentTaskId = task1.taskId

        controller.moveFocusedTaskToDesktop(DEFAULT_DISPLAY, transitionSource = UNKNOWN)

        val wct = getLatestEnterDesktopWct()
        verify(desksOrganizer).moveTaskToDesk(wct, deskId = 0, task4)
        verify(splitScreenController)
            .prepareExitSplitScreen(
                any(),
                anyInt(),
                eq(SplitScreenController.EXIT_REASON_DESKTOP_MODE),
            )
    }

    @Test
    fun moveFocusedTaskToFullscreen() {
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        val task3 = setUpFreeformTask()

        task1.isFocused = false
        task2.isFocused = true
        task3.isFocused = false

        controller.enterFullscreen(DEFAULT_DISPLAY, transitionSource = UNKNOWN)

        val wct = getLatestExitDesktopWct()
        assertThat(wct.changes[task2.token.asBinder()]?.windowingMode)
            .isEqualTo(WINDOWING_MODE_UNDEFINED) // inherited FULLSCREEN
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER)
    fun moveFocusedTaskToFullscreen_onlyVisibleNonMinimizedTask_removesWallpaperActivity() {
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        val task3 = setUpFreeformTask()

        task1.isFocused = false
        task2.isFocused = true
        task3.isFocused = false
        taskRepository.minimizeTask(DEFAULT_DISPLAY, task1.taskId)
        taskRepository.updateTask(DEFAULT_DISPLAY, task3.taskId, isVisible = false)

        controller.enterFullscreen(DEFAULT_DISPLAY, transitionSource = UNKNOWN)

        val wct = getLatestExitDesktopWct()
        val taskChange = assertNotNull(wct.changes[task2.token.asBinder()])
        assertThat(taskChange.windowingMode)
            .isEqualTo(WINDOWING_MODE_UNDEFINED) // inherited FULLSCREEN
        wct.assertReorder(wallpaperToken, toTop = false)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_PIP,
    )
    fun moveFocusedTaskToFullscreen_multipleVisibleTasks_doesNotRemoveWallpaperActivity() {
        val homeTask = setUpHomeTask()
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        val task3 = setUpFreeformTask()

        task1.isFocused = false
        task2.isFocused = true
        task3.isFocused = false
        controller.enterFullscreen(DEFAULT_DISPLAY, transitionSource = UNKNOWN)

        val wct = getLatestExitDesktopWct()
        val taskChange = assertNotNull(wct.changes[task2.token.asBinder()])
        assertThat(taskChange.windowingMode)
            .isEqualTo(WINDOWING_MODE_UNDEFINED) // inherited FULLSCREEN
        // Does not remove wallpaper activity, as desktop still has visible desktop tasks
        assertThat(wct.hierarchyOps).hasSize(2)
        // Moves home task behind the fullscreen task
        wct.assertReorderAt(index = 0, homeTask.getToken(), toTop = true)
        wct.assertReorderAt(index = 1, task2.getToken(), toTop = true)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun moveFocusedTaskToFullscreen_multipleVisibleTasks_fullscreenOverHome_multiDesksEnabled() {
        val homeTask = setUpHomeTask()
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        val task3 = setUpFreeformTask()

        task1.isFocused = false
        task2.isFocused = true
        task3.isFocused = false
        controller.enterFullscreen(DEFAULT_DISPLAY, transitionSource = UNKNOWN)

        val wct = getLatestExitDesktopWct()
        val taskChange = assertNotNull(wct.changes[task2.token.asBinder()])
        assertThat(taskChange.windowingMode)
            .isEqualTo(WINDOWING_MODE_UNDEFINED) // inherited FULLSCREEN
        // Moves home task behind the fullscreen task
        val homeReorderIndex = wct.indexOfReorder(homeTask, toTop = true)
        val fullscreenReorderIndex = wct.indexOfReorder(task2, toTop = true)
        assertThat(homeReorderIndex).isNotEqualTo(-1)
        assertThat(fullscreenReorderIndex).isNotEqualTo(-1)
        assertThat(fullscreenReorderIndex).isGreaterThan(homeReorderIndex)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_BACK_NAVIGATION)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun removeDesk_multipleTasks_removesAll() {
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        val task3 = setUpFreeformTask()
        taskRepository.minimizeTask(DEFAULT_DISPLAY, task2.taskId)

        controller.removeDefaultDeskInDisplay(displayId = DEFAULT_DISPLAY)

        val wct = getLatestWct(TRANSIT_CLOSE)
        assertThat(wct.hierarchyOps).hasSize(3)
        wct.assertRemoveAt(index = 0, task1.token)
        wct.assertRemoveAt(index = 1, task2.token)
        wct.assertRemoveAt(index = 2, task3.token)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_BACK_NAVIGATION)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun removeDesk_multipleTasksWithBackgroundTask_removesAll() {
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        val task3 = setUpFreeformTask()
        taskRepository.minimizeTask(DEFAULT_DISPLAY, task2.taskId)
        whenever(shellTaskOrganizer.getRunningTaskInfo(task3.taskId)).thenReturn(null)

        controller.removeDefaultDeskInDisplay(displayId = DEFAULT_DISPLAY)

        val wct = getLatestWct(TRANSIT_CLOSE)
        assertThat(wct.hierarchyOps).hasSize(2)
        wct.assertRemoveAt(index = 0, task1.token)
        wct.assertRemoveAt(index = 1, task2.token)
        verify(recentTasksController).removeBackgroundTask(task3.taskId)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_BACK_NAVIGATION,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun removeDesk_multipleDesks_addsPendingTransition() {
        val transition = Binder()
        whenever(transitions.startTransition(eq(TRANSIT_CLOSE), any(), anyOrNull()))
            .thenReturn(transition)
        taskRepository.addDesk(DEFAULT_DISPLAY, deskId = 2)

        controller.removeDesk(deskId = 2)

        verify(desksOrganizer).removeDesk(any(), eq(2), any())
        verify(desksTransitionsObserver)
            .addPendingTransition(
                argThat {
                    this is DeskTransition.RemoveDesk &&
                        this.token == transition &&
                        this.deskId == 2
                }
            )
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_BACK_NAVIGATION,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun removeDesk_multipleDesks_removesRunningTasks() {
        val transition = Binder()
        whenever(transitions.startTransition(eq(TRANSIT_CLOSE), any(), anyOrNull()))
            .thenReturn(transition)
        taskRepository.addDesk(DEFAULT_DISPLAY, deskId = 2)
        val task1 = setUpFreeformTask(deskId = 2)
        val task2 = setUpFreeformTask(deskId = 2)
        val task3 = setUpFreeformTask(deskId = 2)

        controller.removeDesk(deskId = 2)

        val wct = getLatestWct(TRANSIT_CLOSE)
        wct.assertRemove(task1.token)
        wct.assertRemove(task2.token)
        wct.assertRemove(task3.token)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_BACK_NAVIGATION,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun removeDesk_multipleDesks_removesRecentTasks() {
        val transition = Binder()
        whenever(transitions.startTransition(eq(TRANSIT_CLOSE), any(), anyOrNull()))
            .thenReturn(transition)
        taskRepository.addDesk(DEFAULT_DISPLAY, deskId = 2)
        val task1 = setUpFreeformTask(deskId = 2, background = true)
        val task2 = setUpFreeformTask(deskId = 2, background = true)
        val task3 = setUpFreeformTask(deskId = 2, background = true)

        controller.removeDesk(deskId = 2)

        verify(recentTasksController).removeBackgroundTask(task1.taskId)
        verify(recentTasksController).removeBackgroundTask(task2.taskId)
        verify(recentTasksController).removeBackgroundTask(task3.taskId)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_BACK_NAVIGATION,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun activateDesk_multipleDesks_addsPendingTransition() {
        val deskId = 0
        val transition = Binder()
        val deskChange = mock(TransitionInfo.Change::class.java)
        whenever(transitions.startTransition(eq(TRANSIT_TO_FRONT), any(), anyOrNull()))
            .thenReturn(transition)
        whenever(desksOrganizer.isDeskActiveAtEnd(deskChange, deskId)).thenReturn(true)
        // Make desk inactive by activating another desk.
        taskRepository.addDesk(DEFAULT_DISPLAY, deskId = 1)
        taskRepository.setActiveDesk(DEFAULT_DISPLAY, deskId = 1)

        controller.activateDesk(deskId, RemoteTransition(TestRemoteTransition()))

        verify(desksTransitionsObserver)
            .addPendingTransition(
                argThat {
                    this is DeskTransition.ActivateDesk &&
                        this.token == transition &&
                        this.deskId == 0
                }
            )
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_BACK_NAVIGATION,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun activateDesk_hasNonRunningTask_startsTask() {
        val deskId = 0
        val nonRunningTask =
            setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = 0, background = true)

        val transition = Binder()
        val deskChange = mock(TransitionInfo.Change::class.java)
        whenever(transitions.startTransition(eq(TRANSIT_TO_FRONT), any(), anyOrNull()))
            .thenReturn(transition)
        whenever(desksOrganizer.isDeskActiveAtEnd(deskChange, deskId)).thenReturn(true)
        // Make desk inactive by activating another desk.
        taskRepository.addDesk(DEFAULT_DISPLAY, deskId = 1)
        taskRepository.setActiveDesk(DEFAULT_DISPLAY, deskId = 1)

        controller.activateDesk(deskId, RemoteTransition(TestRemoteTransition()))

        val wct = getLatestWct(TRANSIT_TO_FRONT, OneShotRemoteHandler::class.java)
        assertNotNull(wct)
        wct.assertLaunchTask(nonRunningTask.taskId, WINDOWING_MODE_FREEFORM)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_BACK_NAVIGATION,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun activateDesk_hasRunningTask_reordersTask() {
        val deskId = 0
        val runningTask = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = 0)

        val transition = Binder()
        val deskChange = mock(TransitionInfo.Change::class.java)
        whenever(transitions.startTransition(eq(TRANSIT_TO_FRONT), any(), anyOrNull()))
            .thenReturn(transition)
        whenever(desksOrganizer.isDeskActiveAtEnd(deskChange, deskId)).thenReturn(true)
        // Make desk inactive by activating another desk.
        taskRepository.addDesk(DEFAULT_DISPLAY, deskId = 1)
        taskRepository.setActiveDesk(DEFAULT_DISPLAY, deskId = 1)

        controller.activateDesk(deskId, RemoteTransition(TestRemoteTransition()))

        verify(desksOrganizer).reorderTaskToFront(any(), eq(deskId), eq(runningTask))
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun activateDesk_otherDeskWasActive_deactivatesOtherDesk() {
        val previouslyActiveDeskId = 1
        val activatingDeskId = 0
        val transition = Binder()
        val deskChange = mock(TransitionInfo.Change::class.java)
        whenever(transitions.startTransition(eq(TRANSIT_TO_FRONT), any(), anyOrNull()))
            .thenReturn(transition)
        whenever(desksOrganizer.isDeskActiveAtEnd(deskChange, activatingDeskId)).thenReturn(true)
        // Make desk inactive by activating another desk.
        taskRepository.addDesk(DEFAULT_DISPLAY, deskId = previouslyActiveDeskId)
        taskRepository.setActiveDesk(DEFAULT_DISPLAY, deskId = previouslyActiveDeskId)

        controller.activateDesk(activatingDeskId, RemoteTransition(TestRemoteTransition()))

        verify(desksOrganizer).deactivateDesk(any(), eq(previouslyActiveDeskId))
        verify(desksTransitionsObserver)
            .addPendingTransition(
                argThat {
                    this is DeskTransition.DeactivateDesk &&
                        this.token == transition &&
                        this.deskId == previouslyActiveDeskId
                }
            )
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_BACK_NAVIGATION,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun moveTaskToDesk_multipleDesks_addsPendingTransition() {
        val transition = Binder()
        whenever(enterDesktopTransitionHandler.moveToDesktop(any(), any())).thenReturn(transition)
        taskRepository.addDesk(DEFAULT_DISPLAY, deskId = 3)
        val task = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)
        task.isVisible = true

        controller.moveTaskToDesk(taskId = task.taskId, deskId = 3, transitionSource = UNKNOWN)

        verify(desksTransitionsObserver)
            .addPendingTransition(
                argThat {
                    this is DeskTransition.ActiveDeskWithTask &&
                        this.token == transition &&
                        this.deskId == 3 &&
                        this.enterTaskId == task.taskId
                }
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS)
    fun dragToDesktop_landscapeDevice_resizable_undefinedOrientation_defaultLandscapeBounds() {
        val spyController = spy(controller)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.TO_DESKTOP_INDICATOR)

        val task = setUpFullscreenTask()
        setUpLandscapeDisplay()

        spyController.onDragPositioningEndThroughStatusBar(PointF(800f, 1280f), task, mockSurface)
        val wct = getLatestDragToDesktopWct()
        assertThat(findBoundsChange(wct, task)).isEqualTo(DEFAULT_LANDSCAPE_BOUNDS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS)
    fun dragToDesktop_landscapeDevice_resizable_landscapeOrientation_defaultLandscapeBounds() {
        val spyController = spy(controller)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.TO_DESKTOP_INDICATOR)

        val task = setUpFullscreenTask(screenOrientation = SCREEN_ORIENTATION_LANDSCAPE)
        setUpLandscapeDisplay()

        spyController.onDragPositioningEndThroughStatusBar(PointF(800f, 1280f), task, mockSurface)
        val wct = getLatestDragToDesktopWct()
        assertThat(findBoundsChange(wct, task)).isEqualTo(DEFAULT_LANDSCAPE_BOUNDS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS)
    fun dragToDesktop_landscapeDevice_resizable_portraitOrientation_resizablePortraitBounds() {
        val spyController = spy(controller)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.TO_DESKTOP_INDICATOR)

        val task =
            setUpFullscreenTask(
                screenOrientation = SCREEN_ORIENTATION_PORTRAIT,
                shouldLetterbox = true,
            )
        setUpLandscapeDisplay()

        spyController.onDragPositioningEndThroughStatusBar(PointF(800f, 1280f), task, mockSurface)
        val wct = getLatestDragToDesktopWct()
        assertThat(findBoundsChange(wct, task)).isEqualTo(RESIZABLE_PORTRAIT_BOUNDS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS)
    fun dragToDesktop_landscapeDevice_unResizable_landscapeOrientation_defaultLandscapeBounds() {
        val spyController = spy(controller)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.TO_DESKTOP_INDICATOR)

        val task =
            setUpFullscreenTask(
                isResizable = false,
                screenOrientation = SCREEN_ORIENTATION_LANDSCAPE,
            )
        setUpLandscapeDisplay()

        spyController.onDragPositioningEndThroughStatusBar(PointF(800f, 1280f), task, mockSurface)
        val wct = getLatestDragToDesktopWct()
        assertThat(findBoundsChange(wct, task)).isEqualTo(DEFAULT_LANDSCAPE_BOUNDS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS)
    fun dragToDesktop_landscapeDevice_unResizable_portraitOrientation_unResizablePortraitBounds() {
        val spyController = spy(controller)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.TO_DESKTOP_INDICATOR)

        val task =
            setUpFullscreenTask(
                isResizable = false,
                screenOrientation = SCREEN_ORIENTATION_PORTRAIT,
                shouldLetterbox = true,
            )
        setUpLandscapeDisplay()
        spyController.onDragPositioningEndThroughStatusBar(PointF(800f, 1280f), task, mockSurface)
        val wct = getLatestDragToDesktopWct()
        assertThat(findBoundsChange(wct, task)).isEqualTo(UNRESIZABLE_PORTRAIT_BOUNDS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS)
    fun dragToDesktop_portraitDevice_resizable_undefinedOrientation_defaultPortraitBounds() {
        val spyController = spy(controller)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.TO_DESKTOP_INDICATOR)

        val task = setUpFullscreenTask(deviceOrientation = ORIENTATION_PORTRAIT)
        setUpPortraitDisplay()

        spyController.onDragPositioningEndThroughStatusBar(PointF(800f, 1280f), task, mockSurface)
        val wct = getLatestDragToDesktopWct()
        assertThat(findBoundsChange(wct, task)).isEqualTo(DEFAULT_PORTRAIT_BOUNDS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS)
    fun dragToDesktop_portraitDevice_resizable_portraitOrientation_defaultPortraitBounds() {
        val spyController = spy(controller)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.TO_DESKTOP_INDICATOR)

        val task =
            setUpFullscreenTask(
                deviceOrientation = ORIENTATION_PORTRAIT,
                screenOrientation = SCREEN_ORIENTATION_PORTRAIT,
            )
        setUpPortraitDisplay()

        spyController.onDragPositioningEndThroughStatusBar(PointF(800f, 1280f), task, mockSurface)
        val wct = getLatestDragToDesktopWct()
        assertThat(findBoundsChange(wct, task)).isEqualTo(DEFAULT_PORTRAIT_BOUNDS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS)
    fun dragToDesktop_portraitDevice_resizable_landscapeOrientation_resizableLandscapeBounds() {
        val spyController = spy(controller)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.TO_DESKTOP_INDICATOR)

        val task =
            setUpFullscreenTask(
                deviceOrientation = ORIENTATION_PORTRAIT,
                screenOrientation = SCREEN_ORIENTATION_LANDSCAPE,
                shouldLetterbox = true,
            )
        setUpPortraitDisplay()

        spyController.onDragPositioningEndThroughStatusBar(PointF(800f, 1280f), task, mockSurface)
        val wct = getLatestDragToDesktopWct()
        assertThat(findBoundsChange(wct, task)).isEqualTo(RESIZABLE_LANDSCAPE_BOUNDS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS)
    fun dragToDesktop_portraitDevice_unResizable_portraitOrientation_defaultPortraitBounds() {
        val spyController = spy(controller)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.TO_DESKTOP_INDICATOR)

        val task =
            setUpFullscreenTask(
                isResizable = false,
                deviceOrientation = ORIENTATION_PORTRAIT,
                screenOrientation = SCREEN_ORIENTATION_PORTRAIT,
            )
        setUpPortraitDisplay()

        spyController.onDragPositioningEndThroughStatusBar(PointF(800f, 1280f), task, mockSurface)
        val wct = getLatestDragToDesktopWct()
        assertThat(findBoundsChange(wct, task)).isEqualTo(DEFAULT_PORTRAIT_BOUNDS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS)
    fun dragToDesktop_portraitDevice_unResizable_landscapeOrientation_unResizableLandscapeBounds() {
        val spyController = spy(controller)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.TO_DESKTOP_INDICATOR)

        val task =
            setUpFullscreenTask(
                isResizable = false,
                deviceOrientation = ORIENTATION_PORTRAIT,
                screenOrientation = SCREEN_ORIENTATION_LANDSCAPE,
                shouldLetterbox = true,
            )
        setUpPortraitDisplay()

        spyController.onDragPositioningEndThroughStatusBar(PointF(200f, 200f), task, mockSurface)
        val wct = getLatestDragToDesktopWct()
        assertThat(findBoundsChange(wct, task)).isEqualTo(UNRESIZABLE_LANDSCAPE_BOUNDS)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun dragToDesktop_movesTaskToDesk() {
        val spyController = spy(controller)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.TO_DESKTOP_INDICATOR)
        val task = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)

        spyController.onDragPositioningEndThroughStatusBar(PointF(200f, 200f), task, mockSurface)

        val wct = getLatestDragToDesktopWct()
        verify(desksOrganizer).moveTaskToDesk(wct, deskId = 0, task)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun dragToDesktop_activatesDesk() {
        val spyController = spy(controller)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.TO_DESKTOP_INDICATOR)
        val task = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)

        spyController.onDragPositioningEndThroughStatusBar(PointF(200f, 200f), task, mockSurface)

        val wct = getLatestDragToDesktopWct()
        verify(desksOrganizer).activateDesk(wct, deskId = 0)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun dragToDesktop_triggersEnterDesktopListener() {
        val spyController = spy(controller)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.TO_DESKTOP_INDICATOR)
        val task = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)

        spyController.onDragPositioningEndThroughStatusBar(PointF(200f, 200f), task, mockSurface)

        verify(desktopModeEnterExitTransitionListener)
            .onEnterDesktopModeTransitionStarted(FREEFORM_ANIMATION_DURATION)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun dragToDesktop_multipleDesks_addsPendingTransition() {
        val transition = Binder()
        val spyController = spy(controller)
        whenever(dragToDesktopTransitionHandler.finishDragToDesktopTransition(any()))
            .thenReturn(transition)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.TO_DESKTOP_INDICATOR)
        val task = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)

        spyController.onDragPositioningEndThroughStatusBar(PointF(200f, 200f), task, mockSurface)

        verify(desksTransitionsObserver)
            .addPendingTransition(
                argThat {
                    this is DeskTransition.ActiveDeskWithTask &&
                        this.token == transition &&
                        this.deskId == 0 &&
                        this.enterTaskId == task.taskId
                }
            )
    }

    @Test
    fun onDesktopDragMove_endsOutsideValidDragArea_snapsToValidBounds() {
        val task = setUpFreeformTask()
        val spyController = spy(controller)
        val mockSurface = mock(SurfaceControl::class.java)
        val mockDisplayLayout = mock(DisplayLayout::class.java)
        whenever(displayController.getDisplayLayout(task.displayId)).thenReturn(mockDisplayLayout)
        whenever(mockDisplayLayout.stableInsets()).thenReturn(Rect(0, 100, 2000, 2000))
        spyController.onDragPositioningMove(task, mockSurface, 200f, Rect(100, -100, 500, 1000))

        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.NO_INDICATOR)
        spyController.onDragPositioningEnd(
            task,
            mockSurface,
            inputCoordinate = PointF(200f, -200f),
            currentDragBounds = Rect(100, -100, 500, 1000),
            validDragArea = Rect(0, 50, 2000, 2000),
            dragStartBounds = Rect(),
            motionEvent,
        )
        val rectAfterEnd = Rect(100, 50, 500, 1150)
        verify(transitions)
            .startTransition(
                eq(TRANSIT_CHANGE),
                Mockito.argThat { wct ->
                    return@argThat wct.changes.any { (token, change) ->
                        change.configuration.windowConfiguration.bounds == rectAfterEnd
                    }
                },
                eq(null),
            )
    }

    @Test
    fun onDesktopDragEnd_noIndicator_updatesTaskBounds() {
        val task = setUpFreeformTask()
        val spyController = spy(controller)
        val mockSurface = mock(SurfaceControl::class.java)
        val mockDisplayLayout = mock(DisplayLayout::class.java)
        whenever(displayController.getDisplayLayout(task.displayId)).thenReturn(mockDisplayLayout)
        whenever(mockDisplayLayout.stableInsets()).thenReturn(Rect(0, 100, 2000, 2000))
        spyController.onDragPositioningMove(task, mockSurface, 200f, Rect(100, 200, 500, 1000))

        val currentDragBounds = Rect(100, 200, 500, 1000)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.NO_INDICATOR)

        spyController.onDragPositioningEnd(
            task,
            mockSurface,
            inputCoordinate = PointF(200f, 300f),
            currentDragBounds = currentDragBounds,
            validDragArea = Rect(0, 50, 2000, 2000),
            dragStartBounds = Rect(),
            motionEvent,
        )

        verify(transitions)
            .startTransition(
                eq(TRANSIT_CHANGE),
                Mockito.argThat { wct ->
                    return@argThat wct.changes.any { (token, change) ->
                        change.configuration.windowConfiguration.bounds == currentDragBounds
                    }
                },
                eq(null),
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CONNECTED_DISPLAYS_WINDOW_DRAG)
    fun onDesktopDragEnd_noIndicatorAndMoveToNewDisplay_reparent() {
        val task = setUpFreeformTask()
        val spyController = spy(controller)
        val mockSurface = mock(SurfaceControl::class.java)
        val mockDisplayLayout = mock(DisplayLayout::class.java)
        whenever(displayController.getDisplayLayout(task.displayId)).thenReturn(mockDisplayLayout)
        whenever(mockDisplayLayout.stableInsets()).thenReturn(Rect(0, 100, 2000, 2000))
        spyController.onDragPositioningMove(task, mockSurface, 200f, Rect(100, 200, 500, 1000))

        val currentDragBounds = Rect(100, 200, 500, 1000)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.NO_INDICATOR)
        whenever(motionEvent.displayId).thenReturn(SECONDARY_DISPLAY_ID)

        spyController.onDragPositioningEnd(
            task,
            mockSurface,
            inputCoordinate = PointF(200f, 300f),
            currentDragBounds,
            validDragArea = Rect(0, 50, 2000, 2000),
            dragStartBounds = Rect(),
            motionEvent,
        )

        verify(transitions)
            .startTransition(
                eq(TRANSIT_CHANGE),
                Mockito.argThat { wct ->
                    return@argThat wct.hierarchyOps[0].isReparent
                },
                eq(dragToDisplayTransitionHandler),
            )
    }

    @Test
    fun onDesktopDragEnd_fullscreenIndicator_dragToExitDesktop() {
        val task = setUpFreeformTask(bounds = Rect(0, 0, 100, 100))
        val spyController = spy(controller)
        val mockSurface = mock(SurfaceControl::class.java)
        val mockDisplayLayout = mock(DisplayLayout::class.java)
        val tda = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY)!!
        tda.configuration.windowConfiguration.windowingMode = WINDOWING_MODE_FULLSCREEN
        whenever(displayController.getDisplayLayout(task.displayId)).thenReturn(mockDisplayLayout)
        whenever(mockDisplayLayout.stableInsets()).thenReturn(Rect(0, 100, 2000, 2000))
        whenever(mockDisplayLayout.getStableBounds(any())).thenAnswer { i ->
            (i.arguments.first() as Rect).set(STABLE_BOUNDS)
        }
        whenever(DesktopModeStatus.shouldMaximizeWhenDragToTopEdge(context)).thenReturn(false)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.TO_FULLSCREEN_INDICATOR)

        // Drag move the task to the top edge
        spyController.onDragPositioningMove(task, mockSurface, 200f, Rect(100, 200, 500, 1000))
        spyController.onDragPositioningEnd(
            task,
            mockSurface,
            inputCoordinate = PointF(200f, 300f),
            currentDragBounds = Rect(100, 50, 500, 1000),
            validDragArea = Rect(0, 50, 2000, 2000),
            dragStartBounds = Rect(),
            motionEvent,
        )

        // Assert the task exits desktop mode
        val wct = getLatestExitDesktopWct()
        assertThat(wct.changes[task.token.asBinder()]?.windowingMode)
            .isEqualTo(WINDOWING_MODE_UNDEFINED)
    }

    @Test
    fun onDesktopDragEnd_fullscreenIndicator_dragToMaximize() {
        val task = setUpFreeformTask(bounds = Rect(0, 0, 100, 100))
        val spyController = spy(controller)
        val mockSurface = mock(SurfaceControl::class.java)
        val mockDisplayLayout = mock(DisplayLayout::class.java)
        val tda = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY)!!
        tda.configuration.windowConfiguration.windowingMode = WINDOWING_MODE_FULLSCREEN
        whenever(displayController.getDisplayLayout(task.displayId)).thenReturn(mockDisplayLayout)
        whenever(mockDisplayLayout.stableInsets()).thenReturn(Rect(0, 100, 2000, 2000))
        whenever(mockDisplayLayout.getStableBounds(any())).thenAnswer { i ->
            (i.arguments.first() as Rect).set(STABLE_BOUNDS)
        }
        whenever(DesktopModeStatus.shouldMaximizeWhenDragToTopEdge(context)).thenReturn(true)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.TO_FULLSCREEN_INDICATOR)

        // Drag move the task to the top edge
        val currentDragBounds = Rect(100, 50, 500, 1000)
        spyController.onDragPositioningMove(task, mockSurface, 200f, Rect(100, 200, 500, 1000))
        spyController.onDragPositioningEnd(
            task,
            mockSurface,
            inputCoordinate = PointF(200f, 300f),
            currentDragBounds,
            validDragArea = Rect(0, 50, 2000, 2000),
            dragStartBounds = Rect(),
            motionEvent,
        )

        // Assert bounds set to stable bounds
        val wct = getLatestToggleResizeDesktopTaskWct(currentDragBounds)
        assertThat(findBoundsChange(wct, task)).isEqualTo(STABLE_BOUNDS)
        // Assert event is properly logged
        verify(desktopModeEventLogger, times(1))
            .logTaskResizingStarted(
                ResizeTrigger.DRAG_TO_TOP_RESIZE_TRIGGER,
                InputMethod.UNKNOWN_INPUT_METHOD,
                task,
                task.configuration.windowConfiguration.bounds.width(),
                task.configuration.windowConfiguration.bounds.height(),
                displayController,
            )
        verify(desktopModeEventLogger, times(1))
            .logTaskResizingEnded(
                ResizeTrigger.DRAG_TO_TOP_RESIZE_TRIGGER,
                InputMethod.UNKNOWN_INPUT_METHOD,
                task,
                STABLE_BOUNDS.width(),
                STABLE_BOUNDS.height(),
                displayController,
            )
    }

    @Test
    fun onDesktopDragEnd_fullscreenIndicator_dragToMaximize_noBoundsChange() {
        val task = setUpFreeformTask(bounds = STABLE_BOUNDS)
        val spyController = spy(controller)
        val mockSurface = mock(SurfaceControl::class.java)
        val mockDisplayLayout = mock(DisplayLayout::class.java)
        val tda = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(DEFAULT_DISPLAY)!!
        tda.configuration.windowConfiguration.windowingMode = WINDOWING_MODE_FULLSCREEN
        whenever(displayController.getDisplayLayout(task.displayId)).thenReturn(mockDisplayLayout)
        whenever(mockDisplayLayout.stableInsets()).thenReturn(Rect(0, 100, 2000, 2000))
        whenever(mockDisplayLayout.getStableBounds(any())).thenAnswer { i ->
            (i.arguments.first() as Rect).set(STABLE_BOUNDS)
        }
        whenever(DesktopModeStatus.shouldMaximizeWhenDragToTopEdge(context)).thenReturn(true)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        whenever(desktopModeVisualIndicator.updateIndicatorType(anyOrNull()))
            .thenReturn(DesktopModeVisualIndicator.IndicatorType.TO_FULLSCREEN_INDICATOR)

        // Drag move the task to the top edge
        val currentDragBounds = Rect(100, 50, 500, 1000)
        spyController.onDragPositioningMove(task, mockSurface, 200f, Rect(100, 200, 500, 1000))
        spyController.onDragPositioningEnd(
            task,
            mockSurface,
            inputCoordinate = PointF(200f, 300f),
            currentDragBounds = currentDragBounds,
            validDragArea = Rect(0, 50, 2000, 2000),
            dragStartBounds = Rect(),
            motionEvent,
        )

        // Assert that task is NOT updated via WCT
        verify(toggleResizeDesktopTaskTransitionHandler, never())
            .startTransition(any(), any(), any())
        // Assert that task leash is updated via Surface Animations
        verify(mReturnToDragStartAnimator)
            .start(
                eq(task.taskId),
                eq(mockSurface),
                eq(currentDragBounds),
                eq(STABLE_BOUNDS),
                anyOrNull(),
            )
        // Assert no event is logged
        verify(desktopModeEventLogger, never())
            .logTaskResizingStarted(any(), any(), any(), any(), any(), any(), any())
        verify(desktopModeEventLogger, never())
            .logTaskResizingEnded(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun enterSplit_freeformTaskIsMovedToSplit() {
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        val task3 = setUpFreeformTask()

        task1.isFocused = false
        task2.isFocused = true
        task3.isFocused = false

        controller.enterSplit(DEFAULT_DISPLAY, leftOrTop = false)

        verify(splitScreenController)
            .requestEnterSplitSelect(
                eq(task2),
                any(),
                eq(SplitScreenConstants.SPLIT_POSITION_BOTTOM_OR_RIGHT),
                eq(task2.configuration.windowConfiguration.bounds),
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER)
    fun enterSplit_onlyVisibleNonMinimizedTask_removesWallpaperActivity() {
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        val task3 = setUpFreeformTask()

        task1.isFocused = false
        task2.isFocused = true
        task3.isFocused = false
        taskRepository.minimizeTask(DEFAULT_DISPLAY, task1.taskId)
        taskRepository.updateTask(DEFAULT_DISPLAY, task3.taskId, isVisible = false)

        controller.enterSplit(DEFAULT_DISPLAY, leftOrTop = false)

        val wctArgument = argumentCaptor<WindowContainerTransaction>()
        verify(splitScreenController)
            .requestEnterSplitSelect(
                eq(task2),
                wctArgument.capture(),
                eq(SplitScreenConstants.SPLIT_POSITION_BOTTOM_OR_RIGHT),
                eq(task2.configuration.windowConfiguration.bounds),
            )
        assertThat(wctArgument.firstValue.hierarchyOps).hasSize(1)
        // Removes wallpaper activity when leaving desktop
        wctArgument.firstValue.assertReorderAt(index = 0, wallpaperToken, toTop = false)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_PIP,
    )
    fun enterSplit_multipleVisibleNonMinimizedTasks_removesWallpaperActivity() {
        val task1 = setUpFreeformTask()
        val task2 = setUpFreeformTask()
        val task3 = setUpFreeformTask()

        task1.isFocused = false
        task2.isFocused = true
        task3.isFocused = false

        controller.enterSplit(DEFAULT_DISPLAY, leftOrTop = false)

        val wctArgument = argumentCaptor<WindowContainerTransaction>()
        verify(splitScreenController)
            .requestEnterSplitSelect(
                eq(task2),
                wctArgument.capture(),
                eq(SplitScreenConstants.SPLIT_POSITION_BOTTOM_OR_RIGHT),
                eq(task2.configuration.windowConfiguration.bounds),
            )
        // Does not remove wallpaper activity, as desktop still has visible desktop tasks
        assertThat(wctArgument.firstValue.hierarchyOps).isEmpty()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun enterSplit_wasInDesk_deactivatesDesk() {
        val deskId = 5
        taskRepository.addDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)
        taskRepository.setActiveDesk(displayId = DEFAULT_DISPLAY, deskId = deskId)
        val task = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId)
        val transition = Binder()
        whenever(splitScreenController.requestEnterSplitSelect(eq(task), any(), any(), any()))
            .thenReturn(transition)

        controller.requestSplit(task, leftOrTop = false)

        verify(desksOrganizer).deactivateDesk(any(), eq(deskId))
        verify(desksTransitionsObserver)
            .addPendingTransition(DeskTransition.DeactivateDesk(transition, deskId))
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MULTI_INSTANCE_FEATURES)
    fun newWindow_fromFullscreenOpensInSplit() {
        setUpLandscapeDisplay()
        val task = setUpFullscreenTask()
        val optionsCaptor = argumentCaptor<Bundle>()
        runOpenNewWindow(task)
        verify(splitScreenController)
            .startIntent(
                any(),
                anyInt(),
                any(),
                any(),
                optionsCaptor.capture(),
                anyOrNull(),
                eq(true),
                eq(SPLIT_INDEX_UNDEFINED),
            )
        assertThat(ActivityOptions.fromBundle(optionsCaptor.firstValue).launchWindowingMode)
            .isEqualTo(WINDOWING_MODE_MULTI_WINDOW)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MULTI_INSTANCE_FEATURES)
    fun newWindow_fromSplitOpensInSplit() {
        setUpLandscapeDisplay()
        val task = setUpSplitScreenTask()
        val optionsCaptor = argumentCaptor<Bundle>()
        runOpenNewWindow(task)
        verify(splitScreenController)
            .startIntent(
                any(),
                anyInt(),
                any(),
                any(),
                optionsCaptor.capture(),
                anyOrNull(),
                eq(true),
                eq(SPLIT_INDEX_UNDEFINED),
            )
        assertThat(ActivityOptions.fromBundle(optionsCaptor.firstValue).launchWindowingMode)
            .isEqualTo(WINDOWING_MODE_MULTI_WINDOW)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MULTI_INSTANCE_FEATURES)
    fun newWindow_fromFreeformAddsNewWindow() {
        setUpLandscapeDisplay()
        val task = setUpFreeformTask()
        val wctCaptor = argumentCaptor<WindowContainerTransaction>()
        val transition = Binder()
        whenever(
                mMockDesktopImmersiveController.exitImmersiveIfApplicable(
                    any(),
                    anyInt(),
                    anyOrNull(),
                    any(),
                )
            )
            .thenReturn(ExitResult.NoExit)
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    anyInt(),
                    any(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(transition)

        runOpenNewWindow(task)

        verify(desktopMixedTransitionHandler)
            .startLaunchTransition(
                anyInt(),
                wctCaptor.capture(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        assertThat(
                ActivityOptions.fromBundle(wctCaptor.firstValue.hierarchyOps[0].launchOptions)
                    .launchWindowingMode
            )
            .isEqualTo(WINDOWING_MODE_FREEFORM)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MULTI_INSTANCE_FEATURES)
    fun newWindow_fromFreeform_exitsImmersiveIfNeeded() {
        setUpLandscapeDisplay()
        val immersiveTask = setUpFreeformTask()
        val task = setUpFreeformTask()
        val runOnStart = RunOnStartTransitionCallback()
        val transition = Binder()
        whenever(
                mMockDesktopImmersiveController.exitImmersiveIfApplicable(
                    any(),
                    anyInt(),
                    anyOrNull(),
                    any(),
                )
            )
            .thenReturn(ExitResult.Exit(immersiveTask.taskId, runOnStart))
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    anyInt(),
                    any(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(transition)

        runOpenNewWindow(task)

        runOnStart.assertOnlyInvocation(transition)
    }

    private fun runOpenNewWindow(task: RunningTaskInfo) {
        markTaskVisible(task)
        task.baseActivity = mock(ComponentName::class.java)
        task.isFocused = true
        runningTasks.add(task)
        controller.openNewWindow(task)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MULTI_INSTANCE_FEATURES)
    fun openInstance_fromFullscreenOpensInSplit() {
        setUpLandscapeDisplay()
        val task = setUpFullscreenTask()
        val taskToRequest = setUpFreeformTask()
        val optionsCaptor = argumentCaptor<Bundle>()
        runOpenInstance(task, taskToRequest.taskId)
        verify(splitScreenController)
            .startTask(anyInt(), anyInt(), optionsCaptor.capture(), anyOrNull(), any())
        assertThat(ActivityOptions.fromBundle(optionsCaptor.firstValue).launchWindowingMode)
            .isEqualTo(WINDOWING_MODE_MULTI_WINDOW)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MULTI_INSTANCE_FEATURES)
    fun openInstance_fromSplitOpensInSplit() {
        setUpLandscapeDisplay()
        val task = setUpSplitScreenTask()
        val taskToRequest = setUpFreeformTask()
        val optionsCaptor = argumentCaptor<Bundle>()
        runOpenInstance(task, taskToRequest.taskId)
        verify(splitScreenController)
            .startTask(anyInt(), anyInt(), optionsCaptor.capture(), anyOrNull(), any())
        assertThat(ActivityOptions.fromBundle(optionsCaptor.firstValue).launchWindowingMode)
            .isEqualTo(WINDOWING_MODE_MULTI_WINDOW)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MULTI_INSTANCE_FEATURES)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun openInstance_fromFreeformAddsNewWindow_multiDesksDisabled() {
        setUpLandscapeDisplay()
        val task = setUpFreeformTask()
        val taskToRequest = setUpFreeformTask()
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    eq(TRANSIT_TO_FRONT),
                    any(),
                    eq(taskToRequest.taskId),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Binder())

        runOpenInstance(task, taskToRequest.taskId)

        verify(desktopMixedTransitionHandler)
            .startLaunchTransition(anyInt(), any(), anyInt(), anyOrNull(), anyOrNull())
        val wct = getLatestDesktopMixedTaskWct(type = TRANSIT_TO_FRONT)
        assertThat(wct.hierarchyOps).hasSize(1)
        wct.assertReorderAt(index = 0, taskToRequest)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MULTI_INSTANCE_FEATURES,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun openInstance_fromFreeformAddsNewWindow_multiDesksEnabled() {
        setUpLandscapeDisplay()
        val task = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = 0)
        val taskToRequest = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = 0)
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    eq(TRANSIT_TO_FRONT),
                    any(),
                    eq(taskToRequest.taskId),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Binder())

        runOpenInstance(task, taskToRequest.taskId)

        verify(desktopMixedTransitionHandler)
            .startLaunchTransition(anyInt(), any(), anyInt(), anyOrNull(), anyOrNull())
        verify(desksOrganizer).reorderTaskToFront(any(), eq(0), eq(taskToRequest))
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MULTI_INSTANCE_FEATURES)
    @DisableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun openInstance_fromFreeform_multiDesksDisabled_minimizesIfNeeded() {
        setUpLandscapeDisplay()
        val deskId = 0
        val freeformTasks =
            (1..MAX_TASK_LIMIT + 1).map { _ ->
                setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId)
            }
        val oldestTask = freeformTasks.first()
        val newestTask = freeformTasks.last()

        val transition = Binder()
        val wctCaptor = argumentCaptor<WindowContainerTransaction>()
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    anyInt(),
                    wctCaptor.capture(),
                    anyInt(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(transition)

        runOpenInstance(newestTask, freeformTasks[1].taskId)

        val wct = wctCaptor.firstValue
        assertThat(wct.hierarchyOps.size).isEqualTo(2) // move-to-front + minimize
        wct.assertReorderAt(0, freeformTasks[1], toTop = true)
        wct.assertReorderAt(1, oldestTask, toTop = false)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MULTI_INSTANCE_FEATURES,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun openInstance_fromFreeform_multiDesksEnabled_minimizesIfNeeded() {
        setUpLandscapeDisplay()
        val deskId = 0
        val freeformTasks =
            (1..MAX_TASK_LIMIT + 1).map { _ ->
                setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = deskId)
            }
        val oldestTask = freeformTasks.first()
        val newestTask = freeformTasks.last()

        val transition = Binder()
        val wctCaptor = argumentCaptor<WindowContainerTransaction>()
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    anyInt(),
                    wctCaptor.capture(),
                    anyInt(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(transition)

        runOpenInstance(newestTask, freeformTasks[1].taskId)

        val wct = wctCaptor.firstValue
        verify(desksOrganizer).minimizeTask(wct, deskId, oldestTask)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_WINDOWING_MULTI_INSTANCE_FEATURES)
    fun openInstance_fromFreeform_exitsImmersiveIfNeeded() {
        setUpLandscapeDisplay()
        val freeformTask = setUpFreeformTask()
        val immersiveTask = setUpFreeformTask()
        taskRepository.setTaskInFullImmersiveState(
            displayId = immersiveTask.displayId,
            taskId = immersiveTask.taskId,
            immersive = true,
        )
        val runOnStartTransit = RunOnStartTransitionCallback()
        val transition = Binder()
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    anyInt(),
                    any(),
                    anyInt(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(transition)
        whenever(
                mMockDesktopImmersiveController.exitImmersiveIfApplicable(
                    any(),
                    eq(DEFAULT_DISPLAY),
                    eq(freeformTask.taskId),
                    any(),
                )
            )
            .thenReturn(
                ExitResult.Exit(
                    exitingTask = immersiveTask.taskId,
                    runOnTransitionStart = runOnStartTransit,
                )
            )

        runOpenInstance(immersiveTask, freeformTask.taskId)

        verify(mMockDesktopImmersiveController)
            .exitImmersiveIfApplicable(
                any(),
                eq(immersiveTask.displayId),
                eq(freeformTask.taskId),
                any(),
            )
        runOnStartTransit.assertOnlyInvocation(transition)
    }

    private fun runOpenInstance(callingTask: RunningTaskInfo, requestedTaskId: Int) {
        markTaskVisible(callingTask)
        callingTask.baseActivity = mock(ComponentName::class.java)
        callingTask.isFocused = true
        runningTasks.add(callingTask)
        controller.openInstance(callingTask, requestedTaskId)
    }

    @Test
    fun toggleBounds_togglesToStableBounds() {
        val bounds = Rect(0, 0, 100, 100)
        val task = setUpFreeformTask(DEFAULT_DISPLAY, bounds)

        controller.toggleDesktopTaskSize(
            task,
            ToggleTaskSizeInteraction(
                ToggleTaskSizeInteraction.Direction.MAXIMIZE,
                ToggleTaskSizeInteraction.Source.HEADER_BUTTON_TO_MAXIMIZE,
                InputMethod.TOUCH,
            ),
        )

        // Assert bounds set to stable bounds
        val wct = getLatestToggleResizeDesktopTaskWct()
        assertThat(findBoundsChange(wct, task)).isEqualTo(STABLE_BOUNDS)
        verify(desktopModeEventLogger, times(1))
            .logTaskResizingEnded(
                ResizeTrigger.MAXIMIZE_BUTTON,
                InputMethod.TOUCH,
                task,
                STABLE_BOUNDS.width(),
                STABLE_BOUNDS.height(),
                displayController,
            )
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_TILE_RESIZING)
    fun snapToHalfScreen_getSnapBounds_calculatesBoundsForResizable() {
        val bounds = Rect(100, 100, 300, 300)
        val task =
            setUpFreeformTask(DEFAULT_DISPLAY, bounds).apply {
                topActivityInfo =
                    ActivityInfo().apply {
                        screenOrientation = SCREEN_ORIENTATION_LANDSCAPE
                        configuration.windowConfiguration.appBounds = bounds
                    }
                isResizeable = true
            }

        val currentDragBounds = Rect(0, 100, 200, 300)
        val expectedBounds =
            Rect(
                STABLE_BOUNDS.left,
                STABLE_BOUNDS.top,
                STABLE_BOUNDS.right / 2,
                STABLE_BOUNDS.bottom,
            )

        controller.snapToHalfScreen(
            task,
            mockSurface,
            currentDragBounds,
            SnapPosition.LEFT,
            ResizeTrigger.SNAP_LEFT_MENU,
            InputMethod.TOUCH,
        )
        // Assert bounds set to stable bounds
        val wct = getLatestToggleResizeDesktopTaskWct(currentDragBounds)
        assertThat(findBoundsChange(wct, task)).isEqualTo(expectedBounds)
        verify(desktopModeEventLogger, times(1))
            .logTaskResizingEnded(
                ResizeTrigger.SNAP_LEFT_MENU,
                InputMethod.TOUCH,
                task,
                expectedBounds.width(),
                expectedBounds.height(),
                displayController,
            )
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_TILE_RESIZING)
    fun snapToHalfScreen_snapBoundsWhenAlreadySnapped_animatesSurfaceWithoutWCT() {
        // Set up task to already be in snapped-left bounds
        val bounds =
            Rect(
                STABLE_BOUNDS.left,
                STABLE_BOUNDS.top,
                STABLE_BOUNDS.right / 2,
                STABLE_BOUNDS.bottom,
            )
        val task =
            setUpFreeformTask(DEFAULT_DISPLAY, bounds).apply {
                topActivityInfo =
                    ActivityInfo().apply {
                        screenOrientation = SCREEN_ORIENTATION_LANDSCAPE
                        configuration.windowConfiguration.appBounds = bounds
                    }
                isResizeable = true
            }

        // Attempt to snap left again
        val currentDragBounds = Rect(bounds).apply { offset(-100, 0) }
        controller.snapToHalfScreen(
            task,
            mockSurface,
            currentDragBounds,
            SnapPosition.LEFT,
            ResizeTrigger.SNAP_LEFT_MENU,
            InputMethod.TOUCH,
        )
        // Assert that task is NOT updated via WCT
        verify(toggleResizeDesktopTaskTransitionHandler, never())
            .startTransition(any(), any(), any())

        // Assert that task leash is updated via Surface Animations
        verify(mReturnToDragStartAnimator)
            .start(eq(task.taskId), eq(mockSurface), eq(currentDragBounds), eq(bounds), anyOrNull())
        verify(desktopModeEventLogger, times(1))
            .logTaskResizingEnded(
                ResizeTrigger.SNAP_LEFT_MENU,
                InputMethod.TOUCH,
                task,
                bounds.width(),
                bounds.height(),
                displayController,
            )
    }

    @Test
    @DisableFlags(
        Flags.FLAG_DISABLE_NON_RESIZABLE_APP_SNAP_RESIZING,
        Flags.FLAG_ENABLE_TILE_RESIZING,
    )
    fun handleSnapResizingTaskOnDrag_nonResizable_snapsToHalfScreen() {
        val task =
            setUpFreeformTask(DEFAULT_DISPLAY, Rect(0, 0, 200, 100)).apply { isResizeable = false }
        val preDragBounds = Rect(100, 100, 400, 500)
        val currentDragBounds = Rect(0, 100, 300, 500)
        val expectedBounds =
            Rect(
                STABLE_BOUNDS.left,
                STABLE_BOUNDS.top,
                STABLE_BOUNDS.right / 2,
                STABLE_BOUNDS.bottom,
            )

        controller.handleSnapResizingTaskOnDrag(
            task,
            SnapPosition.LEFT,
            mockSurface,
            currentDragBounds,
            preDragBounds,
            motionEvent,
        )
        val wct = getLatestToggleResizeDesktopTaskWct(currentDragBounds)
        assertThat(findBoundsChange(wct, task)).isEqualTo(expectedBounds)
        verify(desktopModeEventLogger, times(1))
            .logTaskResizingStarted(
                ResizeTrigger.DRAG_LEFT,
                InputMethod.UNKNOWN_INPUT_METHOD,
                task,
                preDragBounds.width(),
                preDragBounds.height(),
                displayController,
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_DISABLE_NON_RESIZABLE_APP_SNAP_RESIZING)
    fun handleSnapResizingTaskOnDrag_nonResizable_startsRepositionAnimation() {
        val task =
            setUpFreeformTask(DEFAULT_DISPLAY, Rect(0, 0, 200, 100)).apply { isResizeable = false }
        val preDragBounds = Rect(100, 100, 400, 500)
        val currentDragBounds = Rect(0, 100, 300, 500)

        controller.handleSnapResizingTaskOnDrag(
            task,
            SnapPosition.LEFT,
            mockSurface,
            currentDragBounds,
            preDragBounds,
            motionEvent,
        )
        verify(mReturnToDragStartAnimator)
            .start(
                eq(task.taskId),
                eq(mockSurface),
                eq(currentDragBounds),
                eq(preDragBounds),
                any(),
            )
        verify(desktopModeEventLogger, never())
            .logTaskResizingStarted(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    @EnableFlags(Flags.FLAG_DISABLE_NON_RESIZABLE_APP_SNAP_RESIZING)
    fun handleInstantSnapResizingTask_nonResizable_animatorNotStartedAndShowsToast() {
        val taskBounds = Rect(0, 0, 200, 100)
        val task = setUpFreeformTask(DEFAULT_DISPLAY, taskBounds).apply { isResizeable = false }

        controller.handleInstantSnapResizingTask(
            task,
            SnapPosition.LEFT,
            ResizeTrigger.SNAP_LEFT_MENU,
            InputMethod.MOUSE,
        )

        // Assert that task is NOT updated via WCT
        verify(toggleResizeDesktopTaskTransitionHandler, never())
            .startTransition(any(), any(), any())
        verify(mockToast).show()
    }

    @Test
    @EnableFlags(Flags.FLAG_DISABLE_NON_RESIZABLE_APP_SNAP_RESIZING)
    @DisableFlags(Flags.FLAG_ENABLE_TILE_RESIZING)
    fun handleInstantSnapResizingTask_resizable_snapsToHalfScreenAndNotShowToast() {
        val taskBounds = Rect(0, 0, 200, 100)
        val task = setUpFreeformTask(DEFAULT_DISPLAY, taskBounds).apply { isResizeable = true }
        val expectedBounds =
            Rect(
                STABLE_BOUNDS.left,
                STABLE_BOUNDS.top,
                STABLE_BOUNDS.right / 2,
                STABLE_BOUNDS.bottom,
            )

        controller.handleInstantSnapResizingTask(
            task,
            SnapPosition.LEFT,
            ResizeTrigger.SNAP_LEFT_MENU,
            InputMethod.MOUSE,
        )

        // Assert bounds set to half of the stable bounds
        val wct = getLatestToggleResizeDesktopTaskWct(taskBounds)
        assertThat(findBoundsChange(wct, task)).isEqualTo(expectedBounds)
        verify(mockToast, never()).show()
        verify(desktopModeEventLogger, times(1))
            .logTaskResizingStarted(
                ResizeTrigger.SNAP_LEFT_MENU,
                InputMethod.MOUSE,
                task,
                taskBounds.width(),
                taskBounds.height(),
                displayController,
            )
        verify(desktopModeEventLogger, times(1))
            .logTaskResizingEnded(
                ResizeTrigger.SNAP_LEFT_MENU,
                InputMethod.MOUSE,
                task,
                expectedBounds.width(),
                expectedBounds.height(),
                displayController,
            )
    }

    @Test
    fun toggleBounds_togglesToCalculatedBoundsForNonResizable() {
        val bounds = Rect(0, 0, 200, 100)
        val task =
            setUpFreeformTask(DEFAULT_DISPLAY, bounds).apply {
                topActivityInfo.apply {
                    this?.screenOrientation = SCREEN_ORIENTATION_LANDSCAPE
                    configuration.windowConfiguration.appBounds = bounds
                }
                appCompatTaskInfo.topActivityAppBounds.set(0, 0, bounds.width(), bounds.height())
                isResizeable = false
            }

        // Bounds should be 1000 x 500, vertically centered in the 1000 x 1000 stable bounds
        val expectedBounds = Rect(STABLE_BOUNDS.left, 250, STABLE_BOUNDS.right, 750)

        controller.toggleDesktopTaskSize(
            task,
            ToggleTaskSizeInteraction(
                ToggleTaskSizeInteraction.Direction.MAXIMIZE,
                ToggleTaskSizeInteraction.Source.HEADER_BUTTON_TO_MAXIMIZE,
                InputMethod.TOUCH,
            ),
        )

        // Assert bounds set to stable bounds
        val wct = getLatestToggleResizeDesktopTaskWct()
        assertThat(findBoundsChange(wct, task)).isEqualTo(expectedBounds)
        verify(desktopModeEventLogger, times(1))
            .logTaskResizingEnded(
                ResizeTrigger.MAXIMIZE_BUTTON,
                InputMethod.TOUCH,
                task,
                expectedBounds.width(),
                expectedBounds.height(),
                displayController,
            )
    }

    @Test
    fun toggleBounds_lastBoundsBeforeMaximizeSaved() {
        val bounds = Rect(0, 0, 100, 100)
        val task = setUpFreeformTask(DEFAULT_DISPLAY, bounds)

        controller.toggleDesktopTaskSize(
            task,
            ToggleTaskSizeInteraction(
                ToggleTaskSizeInteraction.Direction.MAXIMIZE,
                ToggleTaskSizeInteraction.Source.HEADER_BUTTON_TO_MAXIMIZE,
                InputMethod.TOUCH,
            ),
        )

        assertThat(taskRepository.removeBoundsBeforeMaximize(task.taskId)).isEqualTo(bounds)
        verify(desktopModeEventLogger, never())
            .logTaskResizingEnded(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun toggleBounds_togglesFromStableBoundsToLastBoundsBeforeMaximize() {
        val boundsBeforeMaximize = Rect(0, 0, 100, 100)
        val task = setUpFreeformTask(DEFAULT_DISPLAY, boundsBeforeMaximize)

        // Maximize
        controller.toggleDesktopTaskSize(
            task,
            ToggleTaskSizeInteraction(
                ToggleTaskSizeInteraction.Direction.MAXIMIZE,
                ToggleTaskSizeInteraction.Source.HEADER_BUTTON_TO_MAXIMIZE,
                InputMethod.TOUCH,
            ),
        )
        task.configuration.windowConfiguration.bounds.set(STABLE_BOUNDS)

        // Restore
        controller.toggleDesktopTaskSize(
            task,
            ToggleTaskSizeInteraction(
                ToggleTaskSizeInteraction.Direction.RESTORE,
                ToggleTaskSizeInteraction.Source.HEADER_BUTTON_TO_RESTORE,
                InputMethod.TOUCH,
            ),
        )

        // Assert bounds set to last bounds before maximize
        val wct = getLatestToggleResizeDesktopTaskWct()
        assertThat(findBoundsChange(wct, task)).isEqualTo(boundsBeforeMaximize)
        verify(desktopModeEventLogger, times(1))
            .logTaskResizingEnded(
                ResizeTrigger.MAXIMIZE_BUTTON,
                InputMethod.TOUCH,
                task,
                boundsBeforeMaximize.width(),
                boundsBeforeMaximize.height(),
                displayController,
            )
    }

    @Test
    fun toggleBounds_togglesFromStableBoundsToLastBoundsBeforeMaximize_nonResizeableEqualWidth() {
        val boundsBeforeMaximize = Rect(0, 0, 100, 100)
        val task =
            setUpFreeformTask(DEFAULT_DISPLAY, boundsBeforeMaximize).apply { isResizeable = false }

        // Maximize
        controller.toggleDesktopTaskSize(
            task,
            ToggleTaskSizeInteraction(
                ToggleTaskSizeInteraction.Direction.MAXIMIZE,
                ToggleTaskSizeInteraction.Source.HEADER_BUTTON_TO_MAXIMIZE,
                InputMethod.TOUCH,
            ),
        )
        task.configuration.windowConfiguration.bounds.set(
            STABLE_BOUNDS.left,
            boundsBeforeMaximize.top,
            STABLE_BOUNDS.right,
            boundsBeforeMaximize.bottom,
        )

        // Restore
        controller.toggleDesktopTaskSize(
            task,
            ToggleTaskSizeInteraction(
                ToggleTaskSizeInteraction.Direction.RESTORE,
                ToggleTaskSizeInteraction.Source.HEADER_BUTTON_TO_RESTORE,
                InputMethod.TOUCH,
            ),
        )

        // Assert bounds set to last bounds before maximize
        val wct = getLatestToggleResizeDesktopTaskWct()
        assertThat(findBoundsChange(wct, task)).isEqualTo(boundsBeforeMaximize)
        verify(desktopModeEventLogger, times(1))
            .logTaskResizingEnded(
                ResizeTrigger.MAXIMIZE_BUTTON,
                InputMethod.TOUCH,
                task,
                boundsBeforeMaximize.width(),
                boundsBeforeMaximize.height(),
                displayController,
            )
    }

    @Test
    fun toggleBounds_togglesFromStableBoundsToLastBoundsBeforeMaximize_nonResizeableEqualHeight() {
        val boundsBeforeMaximize = Rect(0, 0, 100, 100)
        val task =
            setUpFreeformTask(DEFAULT_DISPLAY, boundsBeforeMaximize).apply { isResizeable = false }

        // Maximize
        controller.toggleDesktopTaskSize(
            task,
            ToggleTaskSizeInteraction(
                ToggleTaskSizeInteraction.Direction.MAXIMIZE,
                ToggleTaskSizeInteraction.Source.HEADER_BUTTON_TO_MAXIMIZE,
                InputMethod.TOUCH,
            ),
        )
        task.configuration.windowConfiguration.bounds.set(
            boundsBeforeMaximize.left,
            STABLE_BOUNDS.top,
            boundsBeforeMaximize.right,
            STABLE_BOUNDS.bottom,
        )

        // Restore
        controller.toggleDesktopTaskSize(
            task,
            ToggleTaskSizeInteraction(
                ToggleTaskSizeInteraction.Direction.RESTORE,
                ToggleTaskSizeInteraction.Source.HEADER_BUTTON_TO_RESTORE,
                InputMethod.TOUCH,
            ),
        )

        // Assert bounds set to last bounds before maximize
        val wct = getLatestToggleResizeDesktopTaskWct()
        assertThat(findBoundsChange(wct, task)).isEqualTo(boundsBeforeMaximize)
        verify(desktopModeEventLogger, times(1))
            .logTaskResizingEnded(
                ResizeTrigger.MAXIMIZE_BUTTON,
                InputMethod.TOUCH,
                task,
                boundsBeforeMaximize.width(),
                boundsBeforeMaximize.height(),
                displayController,
            )
    }

    @Test
    fun toggleBounds_removesLastBoundsBeforeMaximizeAfterRestoringBounds() {
        val boundsBeforeMaximize = Rect(0, 0, 100, 100)
        val task = setUpFreeformTask(DEFAULT_DISPLAY, boundsBeforeMaximize)

        // Maximize
        controller.toggleDesktopTaskSize(
            task,
            ToggleTaskSizeInteraction(
                ToggleTaskSizeInteraction.Direction.MAXIMIZE,
                ToggleTaskSizeInteraction.Source.HEADER_BUTTON_TO_MAXIMIZE,
                InputMethod.TOUCH,
            ),
        )
        task.configuration.windowConfiguration.bounds.set(STABLE_BOUNDS)

        // Restore
        controller.toggleDesktopTaskSize(
            task,
            ToggleTaskSizeInteraction(
                ToggleTaskSizeInteraction.Direction.RESTORE,
                ToggleTaskSizeInteraction.Source.HEADER_BUTTON_TO_RESTORE,
                InputMethod.TOUCH,
            ),
        )

        // Assert last bounds before maximize removed after use
        assertThat(taskRepository.removeBoundsBeforeMaximize(task.taskId)).isNull()
        verify(desktopModeEventLogger, times(1))
            .logTaskResizingEnded(
                ResizeTrigger.MAXIMIZE_BUTTON,
                InputMethod.TOUCH,
                task,
                boundsBeforeMaximize.width(),
                boundsBeforeMaximize.height(),
                displayController,
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_TAB_TEARING_MINIMIZE_ANIMATION_BUGFIX)
    fun onUnhandledDrag_newFreeformIntent_tabTearingAnimationBugfixFlagEnabled() {
        testOnUnhandledDrag(
            DesktopModeVisualIndicator.IndicatorType.TO_DESKTOP_INDICATOR,
            PointF(1200f, 700f),
            Rect(240, 700, 2160, 1900),
            tabTearingAnimationFlagEnabled = true,
        )
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_DESKTOP_TAB_TEARING_MINIMIZE_ANIMATION_BUGFIX)
    fun onUnhandledDrag_newFreeformIntent_tabTearingAnimationBugfixFlagDisabled() {
        testOnUnhandledDrag(
            DesktopModeVisualIndicator.IndicatorType.TO_DESKTOP_INDICATOR,
            PointF(1200f, 700f),
            Rect(240, 700, 2160, 1900),
            tabTearingAnimationFlagEnabled = false,
        )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_TAB_TEARING_MINIMIZE_ANIMATION_BUGFIX)
    fun onUnhandledDrag_newFreeformIntentSplitLeft_tabTearingAnimationBugfixFlagEnabled() {
        testOnUnhandledDrag(
            DesktopModeVisualIndicator.IndicatorType.TO_SPLIT_LEFT_INDICATOR,
            PointF(50f, 700f),
            Rect(0, 0, 500, 1000),
            tabTearingAnimationFlagEnabled = true,
        )
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_DESKTOP_TAB_TEARING_MINIMIZE_ANIMATION_BUGFIX)
    fun onUnhandledDrag_newFreeformIntentSplitLeft_tabTearingAnimationBugfixFlagDisabled() {
        testOnUnhandledDrag(
            DesktopModeVisualIndicator.IndicatorType.TO_SPLIT_LEFT_INDICATOR,
            PointF(50f, 700f),
            Rect(0, 0, 500, 1000),
            tabTearingAnimationFlagEnabled = false,
        )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_TAB_TEARING_MINIMIZE_ANIMATION_BUGFIX)
    fun onUnhandledDrag_newFreeformIntentSplitRight_tabTearingAnimationBugfixFlagEnabled() {
        testOnUnhandledDrag(
            DesktopModeVisualIndicator.IndicatorType.TO_SPLIT_RIGHT_INDICATOR,
            PointF(2500f, 700f),
            Rect(500, 0, 1000, 1000),
            tabTearingAnimationFlagEnabled = true,
        )
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_DESKTOP_TAB_TEARING_MINIMIZE_ANIMATION_BUGFIX)
    fun onUnhandledDrag_newFreeformIntentSplitRight_tabTearingAnimationBugfixFlagDisabled() {
        testOnUnhandledDrag(
            DesktopModeVisualIndicator.IndicatorType.TO_SPLIT_RIGHT_INDICATOR,
            PointF(2500f, 700f),
            Rect(500, 0, 1000, 1000),
            tabTearingAnimationFlagEnabled = false,
        )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DESKTOP_TAB_TEARING_MINIMIZE_ANIMATION_BUGFIX)
    fun onUnhandledDrag_newFullscreenIntent_tabTearingAnimationBugfixFlagEnabled() {
        testOnUnhandledDrag(
            DesktopModeVisualIndicator.IndicatorType.TO_FULLSCREEN_INDICATOR,
            PointF(1200f, 50f),
            Rect(),
            tabTearingAnimationFlagEnabled = true,
        )
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_DESKTOP_TAB_TEARING_MINIMIZE_ANIMATION_BUGFIX)
    fun onUnhandledDrag_newFullscreenIntent_tabTearingAnimationBugfixFlagDisabled() {
        testOnUnhandledDrag(
            DesktopModeVisualIndicator.IndicatorType.TO_FULLSCREEN_INDICATOR,
            PointF(1200f, 50f),
            Rect(),
            tabTearingAnimationFlagEnabled = false,
        )
    }

    @Test
    fun shellController_registersUserChangeListener() {
        verify(shellController, times(2)).addUserChangeListener(any())
    }

    @Test
    @EnableFlags(FLAG_ENABLE_FULLY_IMMERSIVE_IN_DESKTOP)
    fun onTaskInfoChanged_inImmersiveUnrequestsImmersive_exits() {
        val task = setUpFreeformTask(DEFAULT_DISPLAY)
        taskRepository.setTaskInFullImmersiveState(DEFAULT_DISPLAY, task.taskId, immersive = true)

        task.requestedVisibleTypes = WindowInsets.Type.statusBars()
        controller.onTaskInfoChanged(task)

        verify(mMockDesktopImmersiveController).moveTaskToNonImmersive(eq(task), any())
    }

    @Test
    @EnableFlags(FLAG_ENABLE_FULLY_IMMERSIVE_IN_DESKTOP)
    fun onTaskInfoChanged_notInImmersiveUnrequestsImmersive_noReExit() {
        val task = setUpFreeformTask(DEFAULT_DISPLAY)
        taskRepository.setTaskInFullImmersiveState(DEFAULT_DISPLAY, task.taskId, immersive = false)

        task.requestedVisibleTypes = WindowInsets.Type.statusBars()
        controller.onTaskInfoChanged(task)

        verify(mMockDesktopImmersiveController, never()).moveTaskToNonImmersive(eq(task), any())
    }

    @Test
    @EnableFlags(FLAG_ENABLE_FULLY_IMMERSIVE_IN_DESKTOP)
    fun onTaskInfoChanged_inImmersiveUnrequestsImmersive_inRecentsTransition_noExit() {
        val task = setUpFreeformTask(DEFAULT_DISPLAY)
        taskRepository.setTaskInFullImmersiveState(DEFAULT_DISPLAY, task.taskId, immersive = true)
        recentsTransitionStateListener.onTransitionStateChanged(TRANSITION_STATE_REQUESTED)

        task.requestedVisibleTypes = WindowInsets.Type.statusBars()
        controller.onTaskInfoChanged(task)

        verify(mMockDesktopImmersiveController, never()).moveTaskToNonImmersive(eq(task), any())
    }

    @Test
    fun moveTaskToDesktop_background_attemptsImmersiveExit() {
        val task = setUpFreeformTask(background = true)
        val wct = WindowContainerTransaction()
        val runOnStartTransit = RunOnStartTransitionCallback()
        val transition = Binder()
        whenever(
                mMockDesktopImmersiveController.exitImmersiveIfApplicable(
                    eq(wct),
                    eq(task.displayId),
                    eq(task.taskId),
                    any(),
                )
            )
            .thenReturn(ExitResult.Exit(exitingTask = 5, runOnTransitionStart = runOnStartTransit))
        whenever(enterDesktopTransitionHandler.moveToDesktop(wct, UNKNOWN)).thenReturn(transition)

        controller.moveTaskToDefaultDeskAndActivate(
            taskId = task.taskId,
            wct = wct,
            transitionSource = UNKNOWN,
        )

        verify(mMockDesktopImmersiveController)
            .exitImmersiveIfApplicable(eq(wct), eq(task.displayId), eq(task.taskId), any())
        runOnStartTransit.assertOnlyInvocation(transition)
    }

    @Test
    fun moveTaskToDesktop_foreground_attemptsImmersiveExit() {
        val task = setUpFreeformTask(background = false)
        val wct = WindowContainerTransaction()
        val runOnStartTransit = RunOnStartTransitionCallback()
        val transition = Binder()
        whenever(
                mMockDesktopImmersiveController.exitImmersiveIfApplicable(
                    eq(wct),
                    eq(task.displayId),
                    eq(task.taskId),
                    any(),
                )
            )
            .thenReturn(ExitResult.Exit(exitingTask = 5, runOnTransitionStart = runOnStartTransit))
        whenever(enterDesktopTransitionHandler.moveToDesktop(wct, UNKNOWN)).thenReturn(transition)

        controller.moveTaskToDefaultDeskAndActivate(
            taskId = task.taskId,
            wct = wct,
            transitionSource = UNKNOWN,
        )

        verify(mMockDesktopImmersiveController)
            .exitImmersiveIfApplicable(eq(wct), eq(task.displayId), eq(task.taskId), any())
        runOnStartTransit.assertOnlyInvocation(transition)
    }

    @Test
    fun moveTaskToFront_background_attemptsImmersiveExit() {
        val task = setUpFreeformTask(background = true)
        val runOnStartTransit = RunOnStartTransitionCallback()
        val transition = Binder()
        whenever(
                mMockDesktopImmersiveController.exitImmersiveIfApplicable(
                    any(),
                    eq(task.displayId),
                    eq(task.taskId),
                    any(),
                )
            )
            .thenReturn(ExitResult.Exit(exitingTask = 5, runOnTransitionStart = runOnStartTransit))
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    any(),
                    any(),
                    anyInt(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(transition)

        controller.moveTaskToFront(task.taskId, unminimizeReason = UnminimizeReason.UNKNOWN)

        verify(mMockDesktopImmersiveController)
            .exitImmersiveIfApplicable(any(), eq(task.displayId), eq(task.taskId), any())
        runOnStartTransit.assertOnlyInvocation(transition)
    }

    @Test
    fun moveTaskToFront_foreground_attemptsImmersiveExit() {
        val task = setUpFreeformTask(background = false)
        val runOnStartTransit = RunOnStartTransitionCallback()
        val transition = Binder()
        whenever(
                mMockDesktopImmersiveController.exitImmersiveIfApplicable(
                    any(),
                    eq(task.displayId),
                    eq(task.taskId),
                    any(),
                )
            )
            .thenReturn(ExitResult.Exit(exitingTask = 5, runOnTransitionStart = runOnStartTransit))
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    any(),
                    any(),
                    eq(task.taskId),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(transition)

        controller.moveTaskToFront(task.taskId, unminimizeReason = UnminimizeReason.UNKNOWN)

        verify(mMockDesktopImmersiveController)
            .exitImmersiveIfApplicable(any(), eq(task.displayId), eq(task.taskId), any())
        runOnStartTransit.assertOnlyInvocation(transition)
    }

    @Test
    fun handleRequest_freeformLaunchToDesktop_attemptsImmersiveExit() {
        markTaskVisible(setUpFreeformTask())
        val task = setUpFreeformTask()
        markTaskVisible(task)
        val binder = Binder()

        controller.handleRequest(binder, createTransition(task))

        verify(mMockDesktopImmersiveController)
            .exitImmersiveIfApplicable(eq(binder), any(), eq(task.displayId), any())
    }

    @Test
    fun handleRequest_fullscreenLaunchToDesktop_attemptsImmersiveExit() {
        setUpFreeformTask()
        val task = setUpFullscreenTask()
        val binder = Binder()

        controller.handleRequest(binder, createTransition(task))

        verify(mMockDesktopImmersiveController)
            .exitImmersiveIfApplicable(eq(binder), any(), eq(task.displayId), any())
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_FULLY_IMMERSIVE_IN_DESKTOP)
    fun shouldPlayDesktopAnimation_notShowingDesktop_doesNotPlay() {
        taskRepository.setDeskInactive(deskId = 0)
        val triggerTask = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)
        taskRepository.setTaskInFullImmersiveState(
            displayId = triggerTask.displayId,
            taskId = triggerTask.taskId,
            immersive = true,
        )

        assertThat(
                controller.shouldPlayDesktopAnimation(
                    TransitionRequestInfo(TRANSIT_OPEN, triggerTask, /* remoteTransition= */ null)
                )
            )
            .isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_FULLY_IMMERSIVE_IN_DESKTOP)
    fun shouldPlayDesktopAnimation_notOpening_doesNotPlay() {
        val triggerTask = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        taskRepository.setTaskInFullImmersiveState(
            displayId = triggerTask.displayId,
            taskId = triggerTask.taskId,
            immersive = true,
        )

        assertThat(
                controller.shouldPlayDesktopAnimation(
                    TransitionRequestInfo(TRANSIT_CHANGE, triggerTask, /* remoteTransition= */ null)
                )
            )
            .isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_FULLY_IMMERSIVE_IN_DESKTOP)
    fun shouldPlayDesktopAnimation_notImmersive_doesNotPlay() {
        val triggerTask = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        taskRepository.setTaskInFullImmersiveState(
            displayId = triggerTask.displayId,
            taskId = triggerTask.taskId,
            immersive = false,
        )

        assertThat(
                controller.shouldPlayDesktopAnimation(
                    TransitionRequestInfo(TRANSIT_OPEN, triggerTask, /* remoteTransition= */ null)
                )
            )
            .isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_FULLY_IMMERSIVE_IN_DESKTOP)
    fun shouldPlayDesktopAnimation_fullscreenEntersDesktop_plays() {
        // At least one freeform task to be in a desktop.
        val existingTask = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        val triggerTask = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)
        assertThat(controller.isDesktopModeShowing(triggerTask.displayId)).isTrue()
        taskRepository.setTaskInFullImmersiveState(
            displayId = existingTask.displayId,
            taskId = existingTask.taskId,
            immersive = true,
        )

        assertThat(
                controller.shouldPlayDesktopAnimation(
                    TransitionRequestInfo(TRANSIT_OPEN, triggerTask, /* remoteTransition= */ null)
                )
            )
            .isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_FULLY_IMMERSIVE_IN_DESKTOP)
    fun shouldPlayDesktopAnimation_fullscreenStaysFullscreen_doesNotPlay() {
        val triggerTask = setUpFullscreenTask(displayId = DEFAULT_DISPLAY)
        taskRepository.setDeskInactive(deskId = 0)
        assertThat(controller.isDesktopModeShowing(triggerTask.displayId)).isFalse()

        assertThat(
                controller.shouldPlayDesktopAnimation(
                    TransitionRequestInfo(TRANSIT_OPEN, triggerTask, /* remoteTransition= */ null)
                )
            )
            .isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_FULLY_IMMERSIVE_IN_DESKTOP)
    fun shouldPlayDesktopAnimation_freeformStaysInDesktop_plays() {
        // At least one freeform task to be in a desktop.
        val existingTask = setUpFreeformTask(displayId = DEFAULT_DISPLAY)
        val triggerTask = setUpFreeformTask(displayId = DEFAULT_DISPLAY, active = false)
        assertThat(controller.isDesktopModeShowing(triggerTask.displayId)).isTrue()
        taskRepository.setTaskInFullImmersiveState(
            displayId = existingTask.displayId,
            taskId = existingTask.taskId,
            immersive = true,
        )

        assertThat(
                controller.shouldPlayDesktopAnimation(
                    TransitionRequestInfo(TRANSIT_OPEN, triggerTask, /* remoteTransition= */ null)
                )
            )
            .isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_FULLY_IMMERSIVE_IN_DESKTOP)
    fun shouldPlayDesktopAnimation_freeformExitsDesktop_doesNotPlay() {
        val triggerTask = setUpFreeformTask(displayId = DEFAULT_DISPLAY, active = false)
        taskRepository.setDeskInactive(deskId = 0)
        assertThat(controller.isDesktopModeShowing(triggerTask.displayId)).isFalse()

        assertThat(
                controller.shouldPlayDesktopAnimation(
                    TransitionRequestInfo(TRANSIT_OPEN, triggerTask, /* remoteTransition= */ null)
                )
            )
            .isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun testCreateDesk() {
        val currentDeskCount = taskRepository.getNumberOfDesks(DEFAULT_DISPLAY)
        whenever(desksOrganizer.createDesk(eq(DEFAULT_DISPLAY), any(), any())).thenAnswer {
            invocation ->
            (invocation.arguments[2] as DesksOrganizer.OnCreateCallback).onCreated(deskId = 5)
        }

        controller.createDesk(DEFAULT_DISPLAY, taskRepository.userId)

        assertThat(taskRepository.getNumberOfDesks(DEFAULT_DISPLAY)).isEqualTo(currentDeskCount + 1)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun testCreateDesk_invalidDisplay_dropsRequest() {
        controller.createDesk(INVALID_DISPLAY)

        verify(desksOrganizer, never()).createDesk(any(), any(), any())
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun testCreateDesk_systemUser_dropsRequest() {
        assumeTrue(UserManager.isHeadlessSystemUserMode())

        controller.createDesk(DEFAULT_DISPLAY, UserHandle.USER_SYSTEM)

        verify(desksOrganizer, never()).createDesk(any(), any(), any())
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
    )
    fun startLaunchTransition_desktopNotShowing_movesWallpaperToFront() {
        taskRepository.setDeskInactive(deskId = 0)
        val launchingTask = createFreeformTask(displayId = DEFAULT_DISPLAY)
        val wct = WindowContainerTransaction()
        wct.reorder(launchingTask.token, /* onTop= */ true)
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    eq(TRANSIT_OPEN),
                    any(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Binder())

        controller.startLaunchTransition(
            transitionType = TRANSIT_OPEN,
            wct = wct,
            launchingTaskId = null,
            deskId = 0,
            displayId = DEFAULT_DISPLAY,
        )

        val latestWct = getLatestDesktopMixedTaskWct(type = TRANSIT_OPEN)
        val launchingTaskReorderIndex = latestWct.indexOfReorder(launchingTask, toTop = true)
        val wallpaperReorderIndex = latestWct.indexOfReorder(wallpaperToken, toTop = true)
        assertThat(launchingTaskReorderIndex).isNotEqualTo(-1)
        assertThat(wallpaperReorderIndex).isNotEqualTo(-1)
        assertThat(launchingTaskReorderIndex).isGreaterThan(wallpaperReorderIndex)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun startLaunchTransition_desktopNotShowing_updatesDesktopEnterExitListener() {
        setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = 0)
        taskRepository.setDeskInactive(deskId = 0)

        controller.startLaunchTransition(
            transitionType = TRANSIT_OPEN,
            wct = WindowContainerTransaction(),
            launchingTaskId = null,
            deskId = 0,
            displayId = DEFAULT_DISPLAY,
        )

        verify(desktopModeEnterExitTransitionListener).onEnterDesktopModeTransitionStarted(any())
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
        Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND,
    )
    fun startLaunchTransition_launchingTaskFromInactiveDesk_otherDeskActive_activatesDesk() {
        val activeDeskId = 4
        taskRepository.addDesk(displayId = DEFAULT_DISPLAY, deskId = activeDeskId)
        taskRepository.setActiveDesk(displayId = DEFAULT_DISPLAY, deskId = activeDeskId)
        val inactiveDesk = 5
        taskRepository.addDesk(displayId = DEFAULT_DISPLAY, deskId = inactiveDesk)
        val launchingTask = setUpFreeformTask(displayId = DEFAULT_DISPLAY, deskId = inactiveDesk)
        val transition = Binder()
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    eq(TRANSIT_OPEN),
                    any(),
                    eq(launchingTask.taskId),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(transition)

        val wct = WindowContainerTransaction()
        controller.startLaunchTransition(
            transitionType = TRANSIT_OPEN,
            wct = wct,
            launchingTaskId = launchingTask.taskId,
            deskId = inactiveDesk,
            displayId = DEFAULT_DISPLAY,
        )

        verify(desksOrganizer).activateDesk(any(), eq(inactiveDesk))
        verify(desksTransitionsObserver)
            .addPendingTransition(
                DeskTransition.ActivateDesk(
                    token = transition,
                    displayId = DEFAULT_DISPLAY,
                    deskId = inactiveDesk,
                )
            )
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY,
        Flags.FLAG_ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER,
    )
    fun startLaunchTransition_desktopShowing_doesNotReorderWallpaper() {
        val wct = WindowContainerTransaction()
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    eq(TRANSIT_OPEN),
                    any(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Binder())

        setUpFreeformTask(deskId = 0, displayId = DEFAULT_DISPLAY)
        controller.startLaunchTransition(
            transitionType = TRANSIT_OPEN,
            wct = wct,
            launchingTaskId = null,
            deskId = 0,
            displayId = DEFAULT_DISPLAY,
        )

        val latestWct = getLatestDesktopMixedTaskWct(type = TRANSIT_OPEN)
        assertNull(latestWct.hierarchyOps.find { op -> op.container == wallpaperToken.asBinder() })
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun onRecentsInDesktopAnimationFinishing_returningToApp_noDeskDeactivation() {
        val deskId = 0
        taskRepository.setActiveDesk(DEFAULT_DISPLAY, deskId)

        val transition = Binder()
        val finishWct = WindowContainerTransaction()
        controller.onRecentsInDesktopAnimationFinishing(
            transition = transition,
            finishWct = finishWct,
            returnToApp = true,
        )

        verify(desksOrganizer, never()).deactivateDesk(finishWct, deskId)
        verify(desksTransitionsObserver, never())
            .addPendingTransition(
                argThat { t -> t.token == transition && t is DeskTransition.DeactivateDesk }
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun onRecentsInDesktopAnimationFinishing_noActiveDesk_noDeskDeactivation() {
        val deskId = 0
        taskRepository.setDeskInactive(deskId)

        val transition = Binder()
        val finishWct = WindowContainerTransaction()
        controller.onRecentsInDesktopAnimationFinishing(
            transition = transition,
            finishWct = finishWct,
            returnToApp = false,
        )

        verify(desksOrganizer, never()).deactivateDesk(finishWct, deskId)
        verify(desksTransitionsObserver, never())
            .addPendingTransition(
                argThat { t -> t.token == transition && t is DeskTransition.DeactivateDesk }
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun onRecentsInDesktopAnimationFinishing_activeDesk_notReturningToDesk_deactivatesDesk() {
        val deskId = 0
        taskRepository.setActiveDesk(DEFAULT_DISPLAY, deskId)

        val transition = Binder()
        val finishWct = WindowContainerTransaction()
        controller.onRecentsInDesktopAnimationFinishing(
            transition = transition,
            finishWct = finishWct,
            returnToApp = false,
        )

        verify(desksOrganizer).deactivateDesk(finishWct, deskId)
        verify(desksTransitionsObserver)
            .addPendingTransition(DeskTransition.DeactivateDesk(transition, deskId))
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun onRecentsInDesktopAnimationFinishing_activeDesk_notReturningToDesk_notifiesDesktopExit() {
        val deskId = 0
        taskRepository.setActiveDesk(DEFAULT_DISPLAY, deskId)

        val transition = Binder()
        val finishWct = WindowContainerTransaction()
        controller.onRecentsInDesktopAnimationFinishing(
            transition = transition,
            finishWct = finishWct,
            returnToApp = false,
        )

        verify(desktopModeEnterExitTransitionListener).onExitDesktopModeTransitionStarted(any())
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    fun onRecentsInDesktopAnimationFinishing_activeDesk_notReturningToDesk_doesNotBringUpWallpaperOrHome() {
        val deskId = 0
        taskRepository.setActiveDesk(DEFAULT_DISPLAY, deskId)

        val transition = Binder()
        val finishWct = WindowContainerTransaction()
        controller.onRecentsInDesktopAnimationFinishing(
            transition = transition,
            finishWct = finishWct,
            returnToApp = false,
        )

        finishWct.assertWithoutHop { hop ->
            hop.type == HIERARCHY_OP_TYPE_REORDER &&
                hop.container == wallpaperToken.asBinder() &&
                !hop.toTop
        }
        finishWct.assertWithoutHop { hop -> hop.type == HIERARCHY_OP_TYPE_PENDING_INTENT }
    }

    private class RunOnStartTransitionCallback : ((IBinder) -> Unit) {
        var invocations = 0
            private set

        var lastInvoked: IBinder? = null
            private set

        override fun invoke(transition: IBinder) {
            invocations++
            lastInvoked = transition
        }
    }

    private fun RunOnStartTransitionCallback.assertOnlyInvocation(transition: IBinder) {
        assertThat(invocations).isEqualTo(1)
        assertThat(lastInvoked).isEqualTo(transition)
    }

    /**
     * Assert that an unhandled drag event launches a PendingIntent with the windowing mode and
     * bounds we are expecting.
     */
    private fun testOnUnhandledDrag(
        indicatorType: DesktopModeVisualIndicator.IndicatorType,
        inputCoordinate: PointF,
        expectedBounds: Rect,
        tabTearingAnimationFlagEnabled: Boolean,
    ) {
        setUpLandscapeDisplay()
        val task = setUpFreeformTask()
        markTaskVisible(task)
        task.isFocused = true
        val runningTasks = ArrayList<RunningTaskInfo>()
        runningTasks.add(task)
        val spyController = spy(controller)
        val mockPendingIntent = mock(PendingIntent::class.java)
        val mockDragEvent = mock(DragEvent::class.java)
        val mockCallback = mock(Consumer::class.java)
        val b = SurfaceControl.Builder()
        b.setName("test surface")
        val dragSurface = b.build()
        whenever(shellTaskOrganizer.runningTasks).thenReturn(runningTasks)
        whenever(mockDragEvent.dragSurface).thenReturn(dragSurface)
        whenever(mockDragEvent.x).thenReturn(inputCoordinate.x)
        whenever(mockDragEvent.y).thenReturn(inputCoordinate.y)
        whenever(multiInstanceHelper.supportsMultiInstanceSplit(anyOrNull(), anyInt()))
            .thenReturn(true)
        whenever(spyController.getVisualIndicator()).thenReturn(desktopModeVisualIndicator)
        doReturn(indicatorType)
            .whenever(spyController)
            .updateVisualIndicator(
                eq(task),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                eq(DesktopModeVisualIndicator.DragStartState.DRAGGED_INTENT),
            )
        whenever(
                desktopMixedTransitionHandler.startLaunchTransition(
                    eq(TRANSIT_OPEN),
                    any(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                )
            )
            .thenReturn(Binder())

        spyController.onUnhandledDrag(
            mockPendingIntent,
            context.userId,
            mockDragEvent,
            mockCallback as Consumer<Boolean>,
        )
        val arg = argumentCaptor<WindowContainerTransaction>()
        var expectedWindowingMode: Int
        if (indicatorType == DesktopModeVisualIndicator.IndicatorType.TO_FULLSCREEN_INDICATOR) {
            expectedWindowingMode = WINDOWING_MODE_FULLSCREEN
            // Fullscreen launches currently use default transitions
            verify(transitions).startTransition(any(), arg.capture(), anyOrNull())
        } else {
            expectedWindowingMode = WINDOWING_MODE_FREEFORM
            if (tabTearingAnimationFlagEnabled) {
                verify(desktopMixedTransitionHandler)
                    .startLaunchTransition(
                        eq(TRANSIT_OPEN),
                        arg.capture(),
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull(),
                    )
            } else {
                // All other launches use a special handler.
                verify(dragAndDropTransitionHandler).handleDropEvent(arg.capture())
            }
        }
        assertThat(
                ActivityOptions.fromBundle(arg.firstValue.hierarchyOps[0].launchOptions)
                    .launchWindowingMode
            )
            .isEqualTo(expectedWindowingMode)
        assertThat(
                ActivityOptions.fromBundle(arg.firstValue.hierarchyOps[0].launchOptions)
                    .launchBounds
            )
            .isEqualTo(expectedBounds)
    }

    private val desktopWallpaperIntent: Intent
        get() = Intent(context, DesktopWallpaperActivity::class.java)

    private fun launchHomeIntent(displayId: Int): Intent {
        return Intent(Intent.ACTION_MAIN).apply {
            if (displayId != DEFAULT_DISPLAY) {
                addCategory(Intent.CATEGORY_SECONDARY_HOME)
            } else {
                addCategory(Intent.CATEGORY_HOME)
            }
        }
    }

    private fun addFreeformTaskAtPosition(
        pos: DesktopTaskPosition,
        stableBounds: Rect,
        bounds: Rect = DEFAULT_LANDSCAPE_BOUNDS,
        offsetPos: Point = Point(0, 0),
    ): RunningTaskInfo {
        val offset = pos.getTopLeftCoordinates(stableBounds, bounds)
        val prevTaskBounds = Rect(bounds)
        prevTaskBounds.offsetTo(offset.x + offsetPos.x, offset.y + offsetPos.y)
        return setUpFreeformTask(bounds = prevTaskBounds)
    }

    private fun setUpFreeformTask(
        displayId: Int = DEFAULT_DISPLAY,
        bounds: Rect? = null,
        active: Boolean = true,
        background: Boolean = false,
        deskId: Int? = null,
    ): RunningTaskInfo {
        val task = createFreeformTask(displayId, bounds)
        val activityInfo = ActivityInfo()
        activityInfo.applicationInfo = ApplicationInfo()
        task.topActivityInfo = activityInfo
        if (background) {
            whenever(shellTaskOrganizer.getRunningTaskInfo(task.taskId)).thenReturn(null)
            whenever(recentTasksController.findTaskInBackground(task.taskId))
                .thenReturn(createRecentTaskInfo(taskId = task.taskId, displayId = displayId))
        } else {
            whenever(shellTaskOrganizer.getRunningTaskInfo(task.taskId)).thenReturn(task)
        }
        if (deskId != null) {
            taskRepository.addTaskToDesk(displayId, deskId, task.taskId, isVisible = active)
        } else {
            taskRepository.addTask(displayId, task.taskId, isVisible = active)
        }
        if (!background) {
            runningTasks.add(task)
        }
        return task
    }

    private fun setUpPipTask(
        autoEnterEnabled: Boolean,
        displayId: Int = DEFAULT_DISPLAY,
        deskId: Int = DEFAULT_DISPLAY,
    ): RunningTaskInfo =
        setUpFreeformTask(displayId = displayId, deskId = deskId).apply {
            pictureInPictureParams =
                PictureInPictureParams.Builder().setAutoEnterEnabled(autoEnterEnabled).build()
        }

    private fun setUpHomeTask(displayId: Int = DEFAULT_DISPLAY): RunningTaskInfo {
        val task = createHomeTask(displayId)
        whenever(shellTaskOrganizer.getRunningTaskInfo(task.taskId)).thenReturn(task)
        runningTasks.add(task)
        return task
    }

    private fun setUpFullscreenTask(
        displayId: Int = DEFAULT_DISPLAY,
        isResizable: Boolean = true,
        windowingMode: Int = WINDOWING_MODE_FULLSCREEN,
        deviceOrientation: Int = ORIENTATION_LANDSCAPE,
        screenOrientation: Int = SCREEN_ORIENTATION_UNSPECIFIED,
        shouldLetterbox: Boolean = false,
        gravity: Int = Gravity.NO_GRAVITY,
        enableUserFullscreenOverride: Boolean = false,
        enableSystemFullscreenOverride: Boolean = false,
        aspectRatioOverrideApplied: Boolean = false,
    ): RunningTaskInfo {
        val task = createFullscreenTask(displayId)
        val activityInfo = ActivityInfo()
        activityInfo.screenOrientation = screenOrientation
        activityInfo.windowLayout = ActivityInfo.WindowLayout(0, 0F, 0, 0F, gravity, 0, 0)
        activityInfo.applicationInfo = ApplicationInfo()
        with(task) {
            topActivityInfo = activityInfo
            isResizeable = isResizable
            configuration.orientation = deviceOrientation
            configuration.windowConfiguration.windowingMode = windowingMode
            appCompatTaskInfo.isUserFullscreenOverrideEnabled = enableUserFullscreenOverride
            appCompatTaskInfo.isSystemFullscreenOverrideEnabled = enableSystemFullscreenOverride

            if (deviceOrientation == ORIENTATION_LANDSCAPE) {
                appCompatTaskInfo.topActivityAppBounds.set(
                    0,
                    0,
                    DISPLAY_DIMENSION_LONG,
                    DISPLAY_DIMENSION_SHORT,
                )
            } else {
                appCompatTaskInfo.topActivityAppBounds.set(
                    0,
                    0,
                    DISPLAY_DIMENSION_SHORT,
                    DISPLAY_DIMENSION_LONG,
                )
            }

            if (shouldLetterbox) {
                appCompatTaskInfo.setHasMinAspectRatioOverride(aspectRatioOverrideApplied)
                if (
                    deviceOrientation == ORIENTATION_LANDSCAPE &&
                        screenOrientation == SCREEN_ORIENTATION_PORTRAIT
                ) {
                    // Letterbox to portrait size
                    appCompatTaskInfo.isTopActivityLetterboxed = true
                    appCompatTaskInfo.topActivityAppBounds.set(0, 0, 1200, 1600)
                } else if (
                    deviceOrientation == ORIENTATION_PORTRAIT &&
                        screenOrientation == SCREEN_ORIENTATION_LANDSCAPE
                ) {
                    // Letterbox to landscape size
                    appCompatTaskInfo.isTopActivityLetterboxed = true
                    appCompatTaskInfo.topActivityAppBounds.set(0, 0, 1600, 1200)
                }
            }
        }
        whenever(shellTaskOrganizer.getRunningTaskInfo(task.taskId)).thenReturn(task)
        runningTasks.add(task)
        return task
    }

    private fun setUpLandscapeDisplay() {
        whenever(displayLayout.width()).thenReturn(DISPLAY_DIMENSION_LONG)
        whenever(displayLayout.height()).thenReturn(DISPLAY_DIMENSION_SHORT)
        val stableBounds =
            Rect(
                0,
                0,
                DISPLAY_DIMENSION_LONG,
                DISPLAY_DIMENSION_SHORT - Companion.TASKBAR_FRAME_HEIGHT,
            )
        whenever(displayLayout.getStableBoundsForDesktopMode(any())).thenAnswer { i ->
            (i.arguments.first() as Rect).set(stableBounds)
        }
    }

    private fun setUpPortraitDisplay() {
        whenever(displayLayout.width()).thenReturn(DISPLAY_DIMENSION_SHORT)
        whenever(displayLayout.height()).thenReturn(DISPLAY_DIMENSION_LONG)
        val stableBounds =
            Rect(
                0,
                0,
                DISPLAY_DIMENSION_SHORT,
                DISPLAY_DIMENSION_LONG - Companion.TASKBAR_FRAME_HEIGHT,
            )
        whenever(displayLayout.getStableBoundsForDesktopMode(any())).thenAnswer { i ->
            (i.arguments.first() as Rect).set(stableBounds)
        }
    }

    private fun setUpSplitScreenTask(displayId: Int = DEFAULT_DISPLAY): RunningTaskInfo {
        val task = createSplitScreenTask(displayId)
        whenever(splitScreenController.isTaskInSplitScreen(task.taskId)).thenReturn(true)
        whenever(shellTaskOrganizer.getRunningTaskInfo(task.taskId)).thenReturn(task)
        runningTasks.add(task)
        return task
    }

    private fun markTaskVisible(task: RunningTaskInfo) {
        taskRepository.updateTask(task.displayId, task.taskId, isVisible = true)
    }

    private fun markTaskHidden(task: RunningTaskInfo) {
        taskRepository.updateTask(task.displayId, task.taskId, isVisible = false)
    }

    private fun getLatestWct(
        @WindowManager.TransitionType type: Int = TRANSIT_OPEN,
        handlerClass: Class<out TransitionHandler>? = null,
    ): WindowContainerTransaction {
        val arg = argumentCaptor<WindowContainerTransaction>()
        if (handlerClass == null) {
            verify(transitions).startTransition(eq(type), arg.capture(), isNull())
        } else {
            verify(transitions).startTransition(eq(type), arg.capture(), isA(handlerClass))
        }
        return arg.lastValue
    }

    private fun getLatestToggleResizeDesktopTaskWct(
        currentBounds: Rect? = null
    ): WindowContainerTransaction {
        val arg = argumentCaptor<WindowContainerTransaction>()
        verify(toggleResizeDesktopTaskTransitionHandler, atLeastOnce())
            .startTransition(arg.capture(), eq(currentBounds), isNull())
        return arg.lastValue
    }

    private fun getLatestDesktopMixedTaskWct(
        @WindowManager.TransitionType type: Int = TRANSIT_OPEN
    ): WindowContainerTransaction {
        val arg = argumentCaptor<WindowContainerTransaction>()
        verify(desktopMixedTransitionHandler)
            .startLaunchTransition(eq(type), arg.capture(), anyOrNull(), anyOrNull(), anyOrNull())
        return arg.lastValue
    }

    private fun getLatestTransition(): WindowContainerTransaction {
        val arg = argumentCaptor<WindowContainerTransaction>()
        verify(transitions).startTransition(any(), arg.capture(), anyOrNull())
        return arg.lastValue
    }

    private fun getLatestEnterDesktopWct(): WindowContainerTransaction {
        val arg = argumentCaptor<WindowContainerTransaction>()
        verify(enterDesktopTransitionHandler).moveToDesktop(arg.capture(), any())
        return arg.lastValue
    }

    private fun getLatestDragToDesktopWct(): WindowContainerTransaction {
        val arg = argumentCaptor<WindowContainerTransaction>()
        verify(dragToDesktopTransitionHandler).finishDragToDesktopTransition(arg.capture())
        return arg.lastValue
    }

    private fun getLatestExitDesktopWct(): WindowContainerTransaction {
        val arg = argumentCaptor<WindowContainerTransaction>()
        verify(exitDesktopTransitionHandler).startTransition(any(), arg.capture(), any(), any())
        return arg.lastValue
    }

    private fun findBoundsChange(wct: WindowContainerTransaction, task: RunningTaskInfo): Rect? =
        wct.changes.entries
            .find { (token, change) ->
                token == task.token.asBinder() &&
                    (change.windowSetMask and WindowConfiguration.WINDOW_CONFIG_BOUNDS) != 0
            }
            ?.value
            ?.configuration
            ?.windowConfiguration
            ?.bounds

    private fun verifyWCTNotExecuted() {
        verify(transitions, never()).startTransition(anyInt(), any(), isNull())
    }

    private fun verifyExitDesktopWCTNotExecuted() {
        verify(exitDesktopTransitionHandler, never()).startTransition(any(), any(), any(), any())
    }

    private fun verifyEnterDesktopWCTNotExecuted() {
        verify(enterDesktopTransitionHandler, never()).moveToDesktop(any(), any())
    }

    private fun createTransition(
        task: RunningTaskInfo?,
        @WindowManager.TransitionType type: Int = TRANSIT_OPEN,
    ): TransitionRequestInfo {
        return TransitionRequestInfo(type, task, /* remoteTransition= */ null)
    }

    private companion object {
        const val SECOND_DISPLAY = 2
        val STABLE_BOUNDS = Rect(0, 0, 1000, 1000)
        const val MAX_TASK_LIMIT = 6
        private const val TASKBAR_FRAME_HEIGHT = 200
        private const val FLOAT_TOLERANCE = 0.005f

        @JvmStatic
        @Parameters(name = "{0}")
        fun getParams(): List<FlagsParameterization> =
            FlagsParameterization.allCombinationsOf(Flags.FLAG_ENABLE_MULTIPLE_DESKTOPS_BACKEND)
    }
}

private fun WindowContainerTransaction.assertIndexInBounds(index: Int) {
    assertWithMessage("WCT does not have a hierarchy operation at index $index")
        .that(hierarchyOps.size)
        .isGreaterThan(index)
}

private fun WindowContainerTransaction.assertHop(
    predicate: (WindowContainerTransaction.HierarchyOp) -> Boolean
) {
    assertThat(hierarchyOps.any(predicate)).isTrue()
}

private fun WindowContainerTransaction.assertWithoutHop(
    predicate: (WindowContainerTransaction.HierarchyOp) -> Boolean
) {
    assertThat(hierarchyOps.none(predicate)).isTrue()
}

private fun WindowContainerTransaction.indexOfReorder(
    token: WindowContainerToken,
    toTop: Boolean? = null,
): Int {
    val hop = hierarchyOps.singleOrNull(ReorderPredicate(token, toTop)) ?: return -1
    return hierarchyOps.indexOf(hop)
}

private fun WindowContainerTransaction.indexOfReorder(
    task: RunningTaskInfo,
    toTop: Boolean? = null,
): Int {
    return indexOfReorder(task.token, toTop)
}

private class ReorderPredicate(val token: WindowContainerToken, val toTop: Boolean? = null) :
    ((WindowContainerTransaction.HierarchyOp) -> Boolean) {
    override fun invoke(hop: WindowContainerTransaction.HierarchyOp): Boolean =
        hop.type == HIERARCHY_OP_TYPE_REORDER &&
            (toTop == null || hop.toTop == toTop) &&
            hop.container == token.asBinder()
}

private class ReparentPredicate(
    val token: WindowContainerToken,
    val parentToken: WindowContainerToken,
    val toTop: Boolean? = null,
) : ((WindowContainerTransaction.HierarchyOp) -> Boolean) {
    override fun invoke(hop: WindowContainerTransaction.HierarchyOp): Boolean =
        hop.isReparent &&
            (toTop == null || hop.toTop == toTop) &&
            hop.container == token.asBinder() &&
            hop.newParent == parentToken.asBinder()
}

private fun WindowContainerTransaction.assertReorder(
    task: RunningTaskInfo,
    toTop: Boolean? = null,
) {
    assertReorder(task.token, toTop)
}

private fun WindowContainerTransaction.assertReorder(
    token: WindowContainerToken,
    toTop: Boolean? = null,
) {
    assertHop(ReorderPredicate(token, toTop))
}

private fun WindowContainerTransaction.assertReorderAt(
    index: Int,
    task: RunningTaskInfo,
    toTop: Boolean? = null,
) {
    assertIndexInBounds(index)
    val op = hierarchyOps[index]
    assertThat(op.type).isEqualTo(HIERARCHY_OP_TYPE_REORDER)
    assertThat(op.container).isEqualTo(task.token.asBinder())
    toTop?.let { assertThat(op.toTop).isEqualTo(it) }
}

private fun WindowContainerTransaction.assertReorderAt(
    index: Int,
    token: WindowContainerToken,
    toTop: Boolean? = null,
) {
    assertIndexInBounds(index)
    val op = hierarchyOps[index]
    assertThat(op.type).isEqualTo(HIERARCHY_OP_TYPE_REORDER)
    assertThat(op.container).isEqualTo(token.asBinder())
    toTop?.let { assertThat(op.toTop).isEqualTo(it) }
}

private fun WindowContainerTransaction.assertReorderSequence(vararg tasks: RunningTaskInfo) {
    for (i in tasks.indices) {
        assertReorderAt(i, tasks[i])
    }
}

/** Checks if the reorder hierarchy operations in [range] correspond to [tasks] list */
private fun WindowContainerTransaction.assertReorderSequenceInRange(
    range: IntRange,
    vararg tasks: RunningTaskInfo,
) {
    assertThat(hierarchyOps.slice(range).map { it.type to it.container })
        .containsExactlyElementsIn(tasks.map { HIERARCHY_OP_TYPE_REORDER to it.token.asBinder() })
        .inOrder()
}

private fun WindowContainerTransaction.assertRemove(token: WindowContainerToken) {
    assertThat(
            hierarchyOps.any { hop ->
                hop.container == token.asBinder() && hop.type == HIERARCHY_OP_TYPE_REMOVE_TASK
            }
        )
        .isTrue()
}

private fun WindowContainerTransaction.assertRemoveAt(index: Int, token: WindowContainerToken) {
    assertIndexInBounds(index)
    val op = hierarchyOps[index]
    assertThat(op.type).isEqualTo(HIERARCHY_OP_TYPE_REMOVE_TASK)
    assertThat(op.container).isEqualTo(token.asBinder())
}

private fun WindowContainerTransaction.assertNoRemoveAt(index: Int, token: WindowContainerToken) {
    assertIndexInBounds(index)
    val op = hierarchyOps[index]
    assertThat(op.type).isEqualTo(HIERARCHY_OP_TYPE_REMOVE_TASK)
    assertThat(op.container).isEqualTo(token.asBinder())
}

private fun WindowContainerTransaction.hasRemoveAt(index: Int, token: WindowContainerToken) {
    assertIndexInBounds(index)
    val op = hierarchyOps[index]
    assertThat(op.type).isEqualTo(HIERARCHY_OP_TYPE_REMOVE_TASK)
    assertThat(op.container).isEqualTo(token.asBinder())
}

private fun WindowContainerTransaction.assertPendingIntent(intent: Intent) {
    assertHop { hop ->
        hop.type == HIERARCHY_OP_TYPE_PENDING_INTENT &&
            hop.pendingIntent?.intent?.component == intent.component
    }
}

private fun WindowContainerTransaction.assertWithoutPendingIntent(intent: Intent) {
    assertWithoutHop { hop ->
        hop.type == HIERARCHY_OP_TYPE_PENDING_INTENT &&
            hop.pendingIntent?.intent?.component == intent.component
    }
}

private fun WindowContainerTransaction.assertPendingIntentAt(index: Int, intent: Intent) {
    assertIndexInBounds(index)
    val op = hierarchyOps[index]
    assertThat(op.type).isEqualTo(HIERARCHY_OP_TYPE_PENDING_INTENT)
    assertThat(op.pendingIntent?.intent?.component).isEqualTo(intent.component)
    assertThat(op.pendingIntent?.intent?.categories).isEqualTo(intent.categories)
}

private fun WindowContainerTransaction.assertPendingIntentActivityOptionsLaunchDisplayIdAt(
    index: Int,
    displayId: Int,
) {
    assertIndexInBounds(index)
    val op = hierarchyOps[index]
    if (op.launchOptions != null) {
        val options = ActivityOptions(op.launchOptions)
        assertThat(options.launchDisplayId).isEqualTo(displayId)
    }
}

private fun WindowContainerTransaction.assertLaunchTask(taskId: Int, windowingMode: Int) {
    val keyLaunchWindowingMode = "android.activity.windowingMode"

    assertHop { hop ->
        hop.type == HIERARCHY_OP_TYPE_LAUNCH_TASK &&
            hop.launchOptions?.getInt(LAUNCH_KEY_TASK_ID) == taskId &&
            hop.launchOptions?.getInt(keyLaunchWindowingMode, WINDOWING_MODE_UNDEFINED) ==
                windowingMode
    }
}

private fun WindowContainerTransaction.assertLaunchTaskAt(
    index: Int,
    taskId: Int,
    windowingMode: Int,
) {
    val keyLaunchWindowingMode = "android.activity.windowingMode"

    assertIndexInBounds(index)
    val op = hierarchyOps[index]
    assertThat(op.type).isEqualTo(HIERARCHY_OP_TYPE_LAUNCH_TASK)
    assertThat(op.launchOptions?.getInt(LAUNCH_KEY_TASK_ID)).isEqualTo(taskId)
    assertThat(op.launchOptions?.getInt(keyLaunchWindowingMode, WINDOWING_MODE_UNDEFINED))
        .isEqualTo(windowingMode)
}

private fun WindowContainerTransaction?.anyDensityConfigChange(
    token: WindowContainerToken
): Boolean {
    return this?.changes?.any { change ->
        change.key == token.asBinder() && ((change.value.configSetMask and CONFIG_DENSITY) != 0)
    } ?: false
}

private fun WindowContainerTransaction?.anyWindowingModeChange(
    token: WindowContainerToken
): Boolean {
    return this?.changes?.any { change ->
        change.key == token.asBinder() && change.value.windowingMode >= 0
    } ?: false
}

private fun createRecentTaskInfo(taskId: Int, displayId: Int = DEFAULT_DISPLAY) =
    RecentTaskInfo().apply {
        this.taskId = taskId
        this.displayId = displayId
        token = WindowContainerToken(mock(IWindowContainerToken::class.java))
    }
