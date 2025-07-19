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

import android.annotation.UserIdInt
import android.app.ActivityManager
import android.app.ActivityManager.RunningTaskInfo
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.PendingIntent
import android.app.TaskInfo
import android.app.WindowConfiguration.ACTIVITY_TYPE_HOME
import android.app.WindowConfiguration.ACTIVITY_TYPE_STANDARD
import android.app.WindowConfiguration.WINDOWING_MODE_FREEFORM
import android.app.WindowConfiguration.WINDOWING_MODE_FULLSCREEN
import android.app.WindowConfiguration.WINDOWING_MODE_MULTI_WINDOW
import android.app.WindowConfiguration.WINDOWING_MODE_UNDEFINED
import android.app.WindowConfiguration.WindowingMode
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Region
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.SystemProperties
import android.os.UserHandle
import android.os.UserManager
import android.util.Slog
import android.view.Display
import android.view.Display.DEFAULT_DISPLAY
import android.view.DragEvent
import android.view.MotionEvent
import android.view.SurfaceControl
import android.view.SurfaceControl.Transaction
import android.view.WindowManager
import android.view.WindowManager.TRANSIT_CHANGE
import android.view.WindowManager.TRANSIT_CLOSE
import android.view.WindowManager.TRANSIT_NONE
import android.view.WindowManager.TRANSIT_OPEN
import android.view.WindowManager.TRANSIT_PIP
import android.view.WindowManager.TRANSIT_TO_FRONT
import android.widget.Toast
import android.window.DesktopExperienceFlags
import android.window.DesktopModeFlags
import android.window.DesktopModeFlags.DISABLE_NON_RESIZABLE_APP_SNAP_RESIZE
import android.window.DesktopModeFlags.ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER
import android.window.DesktopModeFlags.ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY
import android.window.DesktopModeFlags.ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS
import android.window.RemoteTransition
import android.window.SplashScreen.SPLASH_SCREEN_STYLE_ICON
import android.window.TransitionInfo
import android.window.TransitionInfo.Change
import android.window.TransitionRequestInfo
import android.window.WindowContainerTransaction
import androidx.annotation.BinderThread
import com.android.internal.annotations.VisibleForTesting
import com.android.internal.jank.Cuj.CUJ_DESKTOP_MODE_ENTER_APP_HANDLE_DRAG_HOLD
import com.android.internal.jank.Cuj.CUJ_DESKTOP_MODE_ENTER_APP_HANDLE_DRAG_RELEASE
import com.android.internal.jank.Cuj.CUJ_DESKTOP_MODE_SNAP_RESIZE
import com.android.internal.jank.InteractionJankMonitor
import com.android.internal.policy.SystemBarUtils.getDesktopViewAppHeaderHeightPx
import com.android.internal.protolog.ProtoLog
import com.android.internal.util.LatencyTracker
import com.android.window.flags.Flags
import com.android.wm.shell.Flags.enableFlexibleSplit
import com.android.wm.shell.R
import com.android.wm.shell.RootTaskDisplayAreaOrganizer
import com.android.wm.shell.ShellTaskOrganizer
import com.android.wm.shell.bubbles.BubbleController
import com.android.wm.shell.common.DisplayController
import com.android.wm.shell.common.DisplayLayout
import com.android.wm.shell.common.ExternalInterfaceBinder
import com.android.wm.shell.common.HomeIntentProvider
import com.android.wm.shell.common.MultiInstanceHelper
import com.android.wm.shell.common.MultiInstanceHelper.Companion.getComponent
import com.android.wm.shell.common.RemoteCallable
import com.android.wm.shell.common.ShellExecutor
import com.android.wm.shell.common.SingleInstanceRemoteListener
import com.android.wm.shell.common.SyncTransactionQueue
import com.android.wm.shell.common.UserProfileContexts
import com.android.wm.shell.desktopmode.DesktopModeEventLogger.Companion.InputMethod
import com.android.wm.shell.desktopmode.DesktopModeEventLogger.Companion.MinimizeReason
import com.android.wm.shell.desktopmode.DesktopModeEventLogger.Companion.ResizeTrigger
import com.android.wm.shell.desktopmode.DesktopModeEventLogger.Companion.UnminimizeReason
import com.android.wm.shell.desktopmode.DesktopModeUiEventLogger.DesktopUiEventEnum
import com.android.wm.shell.desktopmode.DesktopModeVisualIndicator.DragStartState
import com.android.wm.shell.desktopmode.DesktopModeVisualIndicator.IndicatorType
import com.android.wm.shell.desktopmode.DesktopRepository.DeskChangeListener
import com.android.wm.shell.desktopmode.DesktopRepository.VisibleTasksListener
import com.android.wm.shell.desktopmode.DragToDesktopTransitionHandler.Companion.DRAG_TO_DESKTOP_FINISH_ANIM_DURATION_MS
import com.android.wm.shell.desktopmode.DragToDesktopTransitionHandler.DragToDesktopStateListener
import com.android.wm.shell.desktopmode.EnterDesktopTaskTransitionHandler.FREEFORM_ANIMATION_DURATION
import com.android.wm.shell.desktopmode.ExitDesktopTaskTransitionHandler.FULLSCREEN_ANIMATION_DURATION
import com.android.wm.shell.desktopmode.common.ToggleTaskSizeInteraction
import com.android.wm.shell.desktopmode.desktopwallpaperactivity.DesktopWallpaperActivityTokenProvider
import com.android.wm.shell.desktopmode.minimize.DesktopWindowLimitRemoteHandler
import com.android.wm.shell.desktopmode.multidesks.DeskTransition
import com.android.wm.shell.desktopmode.multidesks.DesksOrganizer
import com.android.wm.shell.desktopmode.multidesks.DesksTransitionObserver
import com.android.wm.shell.desktopmode.multidesks.OnDeskRemovedListener
import com.android.wm.shell.desktopmode.persistence.DesktopRepositoryInitializer
import com.android.wm.shell.desktopmode.persistence.DesktopRepositoryInitializer.DeskRecreationFactory
import com.android.wm.shell.draganddrop.DragAndDropController
import com.android.wm.shell.freeform.FreeformTaskTransitionStarter
import com.android.wm.shell.protolog.ShellProtoLogGroup.WM_SHELL_DESKTOP_MODE
import com.android.wm.shell.recents.RecentTasksController
import com.android.wm.shell.recents.RecentsTransitionHandler
import com.android.wm.shell.recents.RecentsTransitionStateListener
import com.android.wm.shell.recents.RecentsTransitionStateListener.RecentsTransitionState
import com.android.wm.shell.recents.RecentsTransitionStateListener.TRANSITION_STATE_NOT_RUNNING
import com.android.wm.shell.shared.R as SharedR
import com.android.wm.shell.shared.TransitionUtil
import com.android.wm.shell.shared.annotations.ExternalThread
import com.android.wm.shell.shared.annotations.ShellDesktopThread
import com.android.wm.shell.shared.annotations.ShellMainThread
import com.android.wm.shell.shared.desktopmode.DesktopModeCompatPolicy
import com.android.wm.shell.shared.desktopmode.DesktopModeStatus
import com.android.wm.shell.shared.desktopmode.DesktopModeStatus.DESKTOP_DENSITY_OVERRIDE
import com.android.wm.shell.shared.desktopmode.DesktopModeStatus.useDesktopOverrideDensity
import com.android.wm.shell.shared.desktopmode.DesktopModeTransitionSource
import com.android.wm.shell.shared.desktopmode.DesktopTaskToFrontReason
import com.android.wm.shell.shared.split.SplitScreenConstants.SPLIT_INDEX_UNDEFINED
import com.android.wm.shell.shared.split.SplitScreenConstants.SPLIT_POSITION_BOTTOM_OR_RIGHT
import com.android.wm.shell.shared.split.SplitScreenConstants.SPLIT_POSITION_TOP_OR_LEFT
import com.android.wm.shell.splitscreen.SplitScreenController
import com.android.wm.shell.splitscreen.SplitScreenController.EXIT_REASON_DESKTOP_MODE
import com.android.wm.shell.sysui.ShellCommandHandler
import com.android.wm.shell.sysui.ShellController
import com.android.wm.shell.sysui.ShellInit
import com.android.wm.shell.sysui.UserChangeListener
import com.android.wm.shell.transition.FocusTransitionObserver
import com.android.wm.shell.transition.OneShotRemoteHandler
import com.android.wm.shell.transition.Transitions
import com.android.wm.shell.transition.Transitions.TransitionFinishCallback
import com.android.wm.shell.windowdecor.DragPositioningCallbackUtility
import com.android.wm.shell.windowdecor.MoveToDesktopAnimator
import com.android.wm.shell.windowdecor.OnTaskRepositionAnimationListener
import com.android.wm.shell.windowdecor.OnTaskResizeAnimationListener
import com.android.wm.shell.windowdecor.extension.isFullscreen
import com.android.wm.shell.windowdecor.extension.isMultiWindow
import com.android.wm.shell.windowdecor.extension.requestingImmersive
import com.android.wm.shell.windowdecor.tiling.SnapEventHandler
import java.io.PrintWriter
import java.util.Optional
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.coroutines.suspendCoroutine
import kotlin.jvm.optionals.getOrNull

/**
 * A callback to be invoked when a transition is started via |Transitions.startTransition| with the
 * transition binder token that it produces.
 *
 * Useful when multiple components are appending WCT operations to a single transition that is
 * started outside of their control, and each of them wants to track the transition lifecycle
 * independently by cross-referencing the transition token with future ready-transitions.
 */
typealias RunOnTransitStart = (IBinder) -> Unit

/** Handles moving tasks in and out of desktop */
class DesktopTasksController(
    private val context: Context,
    shellInit: ShellInit,
    private val shellCommandHandler: ShellCommandHandler,
    private val shellController: ShellController,
    private val displayController: DisplayController,
    private val shellTaskOrganizer: ShellTaskOrganizer,
    private val syncQueue: SyncTransactionQueue,
    private val rootTaskDisplayAreaOrganizer: RootTaskDisplayAreaOrganizer,
    private val dragAndDropController: DragAndDropController,
    private val transitions: Transitions,
    private val keyguardManager: KeyguardManager,
    private val returnToDragStartAnimator: ReturnToDragStartAnimator,
    private val desktopMixedTransitionHandler: DesktopMixedTransitionHandler,
    private val enterDesktopTaskTransitionHandler: EnterDesktopTaskTransitionHandler,
    private val exitDesktopTaskTransitionHandler: ExitDesktopTaskTransitionHandler,
    private val desktopModeDragAndDropTransitionHandler: DesktopModeDragAndDropTransitionHandler,
    private val toggleResizeDesktopTaskTransitionHandler: ToggleResizeDesktopTaskTransitionHandler,
    private val dragToDesktopTransitionHandler: DragToDesktopTransitionHandler,
    private val desktopImmersiveController: DesktopImmersiveController,
    private val userRepositories: DesktopUserRepositories,
    desktopRepositoryInitializer: DesktopRepositoryInitializer,
    private val recentsTransitionHandler: RecentsTransitionHandler,
    private val multiInstanceHelper: MultiInstanceHelper,
    @ShellMainThread private val mainExecutor: ShellExecutor,
    @ShellDesktopThread private val desktopExecutor: ShellExecutor,
    private val desktopTasksLimiter: Optional<DesktopTasksLimiter>,
    private val recentTasksController: RecentTasksController?,
    private val interactionJankMonitor: InteractionJankMonitor,
    @ShellMainThread private val handler: Handler,
    private val focusTransitionObserver: FocusTransitionObserver,
    private val desktopModeEventLogger: DesktopModeEventLogger,
    private val desktopModeUiEventLogger: DesktopModeUiEventLogger,
    private val desktopWallpaperActivityTokenProvider: DesktopWallpaperActivityTokenProvider,
    private val bubbleController: Optional<BubbleController>,
    private val overviewToDesktopTransitionObserver: OverviewToDesktopTransitionObserver,
    private val desksOrganizer: DesksOrganizer,
    private val desksTransitionObserver: DesksTransitionObserver,
    private val userProfileContexts: UserProfileContexts,
    private val desktopModeCompatPolicy: DesktopModeCompatPolicy,
    private val dragToDisplayTransitionHandler: DragToDisplayTransitionHandler,
    private val moveToDisplayTransitionHandler: DesktopModeMoveToDisplayTransitionHandler,
    private val homeIntentProvider: HomeIntentProvider,
) :
    RemoteCallable<DesktopTasksController>,
    Transitions.TransitionHandler,
    DragAndDropController.DragAndDropListener,
    UserChangeListener {

    private val desktopMode: DesktopModeImpl
    private var taskRepository: DesktopRepository
    private var visualIndicator: DesktopModeVisualIndicator? = null
    private var userId: Int
    private val desktopModeShellCommandHandler: DesktopModeShellCommandHandler =
        DesktopModeShellCommandHandler(this, focusTransitionObserver)

    private val mOnAnimationFinishedCallback = { releaseVisualIndicator() }
    private lateinit var snapEventHandler: SnapEventHandler
    private val dragToDesktopStateListener =
        object : DragToDesktopStateListener {
            override fun onCommitToDesktopAnimationStart() {
                removeVisualIndicator()
            }

            override fun onCancelToDesktopAnimationEnd() {
                removeVisualIndicator()
            }

            override fun onTransitionInterrupted() {
                removeVisualIndicator()
            }

            private fun removeVisualIndicator() {
                visualIndicator?.fadeOutIndicator { releaseVisualIndicator() }
            }
        }

    @VisibleForTesting var taskbarDesktopTaskListener: TaskbarDesktopTaskListener? = null

    @VisibleForTesting
    var desktopModeEnterExitTransitionListener: DesktopModeEntryExitTransitionListener? = null

    /** Task id of the task currently being dragged from fullscreen/split. */
    val draggingTaskId
        get() = dragToDesktopTransitionHandler.draggingTaskId

    @RecentsTransitionState private var recentsTransitionState = TRANSITION_STATE_NOT_RUNNING

    private lateinit var splitScreenController: SplitScreenController
    lateinit var freeformTaskTransitionStarter: FreeformTaskTransitionStarter
    // Launch cookie used to identify a drag and drop transition to fullscreen after it has begun.
    // Used to prevent handleRequest from moving the new fullscreen task to freeform.
    private var dragAndDropFullscreenCookie: Binder? = null

    // A listener that is invoked after a desk has been remove from the system. */
    var onDeskRemovedListener: OnDeskRemovedListener? = null

    init {
        desktopMode = DesktopModeImpl()
        if (DesktopModeStatus.canEnterDesktopMode(context)) {
            shellInit.addInitCallback({ onInit() }, this)
        }
        userId = ActivityManager.getCurrentUser()
        taskRepository = userRepositories.getProfile(userId)

        if (DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
            desktopRepositoryInitializer.deskRecreationFactory =
                DeskRecreationFactory { deskUserId, destinationDisplayId, _ ->
                    createDeskSuspending(displayId = destinationDisplayId, userId = deskUserId)
                }
        }
    }

    private fun onInit() {
        logD("onInit")
        shellCommandHandler.addDumpCallback(this::dump, this)
        shellCommandHandler.addCommandCallback("desktopmode", desktopModeShellCommandHandler, this)
        shellController.addExternalInterface(
            IDesktopMode.DESCRIPTOR,
            { createExternalInterface() },
            this,
        )
        shellController.addUserChangeListener(this)
        // Update the current user id again because it might be updated between init and onInit().
        updateCurrentUser(ActivityManager.getCurrentUser())
        transitions.addHandler(this)
        dragToDesktopTransitionHandler.dragToDesktopStateListener = dragToDesktopStateListener
        recentsTransitionHandler.addTransitionStateListener(
            object : RecentsTransitionStateListener {
                override fun onTransitionStateChanged(@RecentsTransitionState state: Int) {
                    logV(
                        "Recents transition state changed: %s",
                        RecentsTransitionStateListener.stateToString(state),
                    )
                    recentsTransitionState = state
                    snapEventHandler.onOverviewAnimationStateChange(
                        RecentsTransitionStateListener.isAnimating(state)
                    )
                }
            }
        )
        dragAndDropController.addListener(this)
    }

    @VisibleForTesting
    fun getVisualIndicator(): DesktopModeVisualIndicator? {
        return visualIndicator
    }

    fun setOnTaskResizeAnimationListener(listener: OnTaskResizeAnimationListener) {
        toggleResizeDesktopTaskTransitionHandler.setOnTaskResizeAnimationListener(listener)
        enterDesktopTaskTransitionHandler.setOnTaskResizeAnimationListener(listener)
        dragToDesktopTransitionHandler.onTaskResizeAnimationListener = listener
        desktopImmersiveController.onTaskResizeAnimationListener = listener
    }

    fun setOnTaskRepositionAnimationListener(listener: OnTaskRepositionAnimationListener) {
        returnToDragStartAnimator.setTaskRepositionAnimationListener(listener)
    }

    /** Setter needed to avoid cyclic dependency. */
    fun setSplitScreenController(controller: SplitScreenController) {
        splitScreenController = controller
        dragToDesktopTransitionHandler.setSplitScreenController(controller)
    }

    /** Setter to handle snap events */
    fun setSnapEventHandler(handler: SnapEventHandler) {
        snapEventHandler = handler
    }

    /** Returns the transition type for the given remote transition. */
    private fun transitionType(remoteTransition: RemoteTransition?): Int {
        if (remoteTransition == null) {
            logV("RemoteTransition is null")
            return TRANSIT_NONE
        }
        return TRANSIT_TO_FRONT
    }

    /** Show all tasks, that are part of the desktop, on top of launcher */
    @Deprecated("Use activateDesk() instead.", ReplaceWith("activateDesk()"))
    fun showDesktopApps(displayId: Int, remoteTransition: RemoteTransition? = null) {
        logV("showDesktopApps")
        activateDefaultDeskInDisplay(displayId, remoteTransition)
    }

    /** Returns whether the given display has an active desk. */
    fun isAnyDeskActive(displayId: Int): Boolean = taskRepository.isAnyDeskActive(displayId)

    /**
     * Returns true if any freeform tasks are visible or if a transparent fullscreen task exists on
     * top in Desktop Mode.
     *
     * TODO: b/362720497 - consolidate with [isAnyDeskActive].
     *     - top-transparent-fullscreen case: should not be needed if we allow it to launch inside
     *       the desk in fullscreen instead of force-exiting desktop and having to trick this method
     *       into thinking it is in desktop mode when a task in this state exists.
     */
    fun isDesktopModeShowing(displayId: Int): Boolean {
        val hasVisibleTasks = taskRepository.isAnyDeskActive(displayId)
        val hasTopTransparentFullscreenTask =
            taskRepository.getTopTransparentFullscreenTaskId(displayId) != null
        if (
            DesktopModeFlags.INCLUDE_TOP_TRANSPARENT_FULLSCREEN_TASK_IN_DESKTOP_HEURISTIC
                .isTrue() && DesktopModeFlags.ENABLE_DESKTOP_WINDOWING_MODALS_POLICY.isTrue()
        ) {
            logV(
                "isDesktopModeShowing: hasVisibleTasks=%s hasTopTransparentFullscreenTask=%s",
                hasVisibleTasks,
                hasTopTransparentFullscreenTask,
            )
            return hasVisibleTasks || hasTopTransparentFullscreenTask
        }
        logV("isDesktopModeShowing: hasVisibleTasks=%s", hasVisibleTasks)
        return hasVisibleTasks
    }

    /** Moves focused task to desktop mode for given [displayId]. */
    fun moveFocusedTaskToDesktop(displayId: Int, transitionSource: DesktopModeTransitionSource) {
        val allFocusedTasks = getAllFocusedTasks(displayId)
        when (allFocusedTasks.size) {
            0 -> return
            // Full screen case
            1 ->
                moveTaskToDefaultDeskAndActivate(
                    allFocusedTasks.single().taskId,
                    transitionSource = transitionSource,
                )
            // Split-screen case where there are two focused tasks, then we find the child
            // task to move to desktop.
            2 ->
                moveTaskToDefaultDeskAndActivate(
                    getSplitFocusedTask(allFocusedTasks[0], allFocusedTasks[1]).taskId,
                    transitionSource = transitionSource,
                )
            else ->
                logW(
                    "DesktopTasksController: Cannot enter desktop, expected less " +
                        "than 3 focused tasks but found %d",
                    allFocusedTasks.size,
                )
        }
    }

    /**
     * Returns all focused tasks in full screen or split screen mode in [displayId] when it is not
     * the home activity.
     */
    private fun getAllFocusedTasks(displayId: Int): List<RunningTaskInfo> =
        shellTaskOrganizer.getRunningTasks(displayId).filter {
            it.isFocused &&
                (it.windowingMode == WINDOWING_MODE_FULLSCREEN ||
                    it.windowingMode == WINDOWING_MODE_MULTI_WINDOW) &&
                it.activityType != ACTIVITY_TYPE_HOME
        }

    /** Returns child task from two focused tasks in split screen mode. */
    private fun getSplitFocusedTask(task1: RunningTaskInfo, task2: RunningTaskInfo) =
        if (task1.taskId == task2.parentTaskId) task2 else task1

    private fun forceEnterDesktop(displayId: Int): Boolean {
        if (!DesktopModeStatus.enterDesktopByDefaultOnFreeformDisplay(context)) {
            return false
        }

        // Secondary displays are always desktop-first
        if (displayId != DEFAULT_DISPLAY) {
            return true
        }

        val tdaInfo = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(displayId)
        // A non-organized display (e.g., non-trusted virtual displays used in CTS) doesn't have
        // TDA.
        if (tdaInfo == null) {
            logW(
                "forceEnterDesktop cannot find DisplayAreaInfo for displayId=%d. This could happen" +
                    " when the display is a non-trusted virtual display.",
                displayId,
            )
            return false
        }
        val tdaWindowingMode = tdaInfo.configuration.windowConfiguration.windowingMode
        val isFreeformDisplay = tdaWindowingMode == WINDOWING_MODE_FREEFORM
        return isFreeformDisplay
    }

    /** Called when the recents transition that started while in desktop is finishing. */
    fun onRecentsInDesktopAnimationFinishing(
        transition: IBinder,
        finishWct: WindowContainerTransaction,
        returnToApp: Boolean,
    ) {
        if (!DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) return
        logV("onRecentsInDesktopAnimationFinishing returnToApp=%b", returnToApp)
        if (returnToApp) return
        // Home/Recents only exists in the default display.
        val activeDesk = taskRepository.getActiveDeskId(DEFAULT_DISPLAY) ?: return
        // Not going back to the active desk, deactivate it.
        val runOnTransitStart =
            performDesktopExitCleanUp(
                wct = finishWct,
                deskId = activeDesk,
                displayId = DEFAULT_DISPLAY,
                willExitDesktop = true,
                shouldEndUpAtHome = true,
                fromRecentsTransition = true,
            )
        runOnTransitStart?.invoke(transition)
    }

    /** Adds a new desk to the given display for the given user. */
    fun createDesk(displayId: Int, userId: Int = this.userId) {
        logV("addDesk displayId=%d, userId=%d", displayId, userId)
        val repository = userRepositories.getProfile(userId)
        createDesk(displayId, userId) { deskId ->
            if (deskId == null) {
                logW("Failed to add desk in displayId=%d for userId=%d", displayId, userId)
            } else {
                repository.addDesk(displayId = displayId, deskId = deskId)
            }
        }
    }

    private fun createDesk(displayId: Int, userId: Int = this.userId, onResult: (Int?) -> Unit) {
        if (displayId == Display.INVALID_DISPLAY) {
            logW("createDesk attempt with invalid displayId", displayId)
            onResult(null)
            return
        }
        if (!DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
            // In single-desk, the desk reuses the display id.
            logD("createDesk reusing displayId=%d for single-desk", displayId)
            onResult(displayId)
            return
        }
        if (
            DesktopModeFlags.ENABLE_DESKTOP_WINDOWING_HSUM.isTrue &&
                UserManager.isHeadlessSystemUserMode() &&
                UserHandle.USER_SYSTEM == userId
        ) {
            logW("createDesk ignoring attempt for system user")
            return
        }
        desksOrganizer.createDesk(displayId, userId) { deskId ->
            logD(
                "createDesk obtained deskId=%d for displayId=%d and userId=%d",
                deskId,
                displayId,
                userId,
            )
            onResult(deskId)
        }
    }

    private suspend fun createDeskSuspending(displayId: Int, userId: Int = this.userId): Int? =
        suspendCoroutine { cont ->
            createDesk(displayId, userId) { deskId -> cont.resumeWith(Result.success(deskId)) }
        }

    /** Moves task to desktop mode if task is running, else launches it in desktop mode. */
    @JvmOverloads
    fun moveTaskToDefaultDeskAndActivate(
        taskId: Int,
        wct: WindowContainerTransaction = WindowContainerTransaction(),
        transitionSource: DesktopModeTransitionSource,
        remoteTransition: RemoteTransition? = null,
        callback: IMoveToDesktopCallback? = null,
    ): Boolean {
        val task =
            shellTaskOrganizer.getRunningTaskInfo(taskId)
                ?: recentTasksController?.findTaskInBackground(taskId)
        if (task == null) {
            logW("moveTaskToDefaultDeskAndActivate taskId=%d not found", taskId)
            return false
        }
        val deskId = getDefaultDeskId(task.displayId)
        return moveTaskToDesk(
            taskId = taskId,
            deskId = deskId,
            wct = wct,
            transitionSource = transitionSource,
            remoteTransition = remoteTransition,
        )
    }

    /** Moves task to desktop mode if task is running, else launches it in desktop mode. */
    fun moveTaskToDesk(
        taskId: Int,
        deskId: Int,
        wct: WindowContainerTransaction = WindowContainerTransaction(),
        transitionSource: DesktopModeTransitionSource,
        remoteTransition: RemoteTransition? = null,
        callback: IMoveToDesktopCallback? = null,
    ): Boolean {
        val runningTask = shellTaskOrganizer.getRunningTaskInfo(taskId)
        if (runningTask != null) {
            return moveRunningTaskToDesk(
                task = runningTask,
                deskId = deskId,
                wct = wct,
                transitionSource = transitionSource,
                remoteTransition = remoteTransition,
                callback = callback,
            )
        }
        val backgroundTask = recentTasksController?.findTaskInBackground(taskId)
        if (backgroundTask != null) {
            // TODO: b/391484662 - add support for |deskId|.
            return moveBackgroundTaskToDesktop(
                taskId,
                wct,
                transitionSource,
                remoteTransition,
                callback,
            )
        }
        logW("moveTaskToDesk taskId=%d not found", taskId)
        return false
    }

    private fun moveBackgroundTaskToDesktop(
        taskId: Int,
        wct: WindowContainerTransaction,
        transitionSource: DesktopModeTransitionSource,
        remoteTransition: RemoteTransition? = null,
        callback: IMoveToDesktopCallback? = null,
    ): Boolean {
        val task = recentTasksController?.findTaskInBackground(taskId)
        if (task == null) {
            logW("moveBackgroundTaskToDesktop taskId=%d not found", taskId)
            return false
        }
        logV("moveBackgroundTaskToDesktop with taskId=%d", taskId)
        val deskId = getDefaultDeskId(task.displayId)
        val runOnTransitStart = addDeskActivationChanges(deskId, wct, task)
        val exitResult =
            desktopImmersiveController.exitImmersiveIfApplicable(
                wct = wct,
                displayId = DEFAULT_DISPLAY,
                excludeTaskId = taskId,
                reason = DesktopImmersiveController.ExitReason.TASK_LAUNCH,
            )
        wct.startTask(
            taskId,
            ActivityOptions.makeBasic()
                .apply { launchWindowingMode = WINDOWING_MODE_FREEFORM }
                .toBundle(),
        )

        val transition: IBinder
        if (remoteTransition != null) {
            val transitionType = transitionType(remoteTransition)
            val remoteTransitionHandler = OneShotRemoteHandler(mainExecutor, remoteTransition)
            transition = transitions.startTransition(transitionType, wct, remoteTransitionHandler)
            remoteTransitionHandler.setTransition(transition)
        } else {
            // TODO(343149901): Add DPI changes for task launch
            transition = enterDesktopTaskTransitionHandler.moveToDesktop(wct, transitionSource)
            invokeCallbackToOverview(transition, callback)
        }
        desktopModeEnterExitTransitionListener?.onEnterDesktopModeTransitionStarted(
            FREEFORM_ANIMATION_DURATION
        )
        runOnTransitStart?.invoke(transition)
        exitResult.asExit()?.runOnTransitionStart?.invoke(transition)
        return true
    }

    /** Moves a running task to desktop. */
    private fun moveRunningTaskToDesk(
        task: RunningTaskInfo,
        deskId: Int,
        wct: WindowContainerTransaction = WindowContainerTransaction(),
        transitionSource: DesktopModeTransitionSource,
        remoteTransition: RemoteTransition? = null,
        callback: IMoveToDesktopCallback? = null,
    ): Boolean {
        if (desktopModeCompatPolicy.isTopActivityExemptFromDesktopWindowing(task)) {
            logW("Cannot enter desktop for taskId %d, ineligible top activity found", task.taskId)
            return false
        }
        val displayId = taskRepository.getDisplayForDesk(deskId)
        logV(
            "moveRunningTaskToDesk taskId=%d deskId=%d displayId=%d",
            task.taskId,
            deskId,
            displayId,
        )
        exitSplitIfApplicable(wct, task)
        val exitResult =
            desktopImmersiveController.exitImmersiveIfApplicable(
                wct = wct,
                displayId = displayId,
                excludeTaskId = task.taskId,
                reason = DesktopImmersiveController.ExitReason.TASK_LAUNCH,
            )

        val runOnTransitStart = addDeskActivationWithMovingTaskChanges(deskId, wct, task)

        val transition: IBinder
        if (remoteTransition != null) {
            val transitionType = transitionType(remoteTransition)
            val remoteTransitionHandler = OneShotRemoteHandler(mainExecutor, remoteTransition)
            transition = transitions.startTransition(transitionType, wct, remoteTransitionHandler)
            remoteTransitionHandler.setTransition(transition)
        } else {
            transition = enterDesktopTaskTransitionHandler.moveToDesktop(wct, transitionSource)
            invokeCallbackToOverview(transition, callback)
        }
        desktopModeEnterExitTransitionListener?.onEnterDesktopModeTransitionStarted(
            FREEFORM_ANIMATION_DURATION
        )
        runOnTransitStart?.invoke(transition)
        exitResult.asExit()?.runOnTransitionStart?.invoke(transition)
        if (!DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
            taskRepository.setActiveDesk(displayId = displayId, deskId = deskId)
        }
        return true
    }

    private fun invokeCallbackToOverview(transition: IBinder, callback: IMoveToDesktopCallback?) {
        // TODO: b/333524374 - Remove this later.
        // This is a temporary implementation for adding CUJ end and
        // should be removed when animation is moved to launcher through remote transition.
        if (callback != null) {
            overviewToDesktopTransitionObserver.addPendingOverviewTransition(transition, callback)
        }
    }

    /**
     * The first part of the animated drag to desktop transition. This is followed with a call to
     * [finalizeDragToDesktop] or [cancelDragToDesktop].
     */
    fun startDragToDesktop(
        taskInfo: RunningTaskInfo,
        dragToDesktopValueAnimator: MoveToDesktopAnimator,
        taskSurface: SurfaceControl,
        dragInterruptedCallback: Runnable,
    ) {
        logV("startDragToDesktop taskId=%d", taskInfo.taskId)
        val jankConfigBuilder =
            InteractionJankMonitor.Configuration.Builder.withSurface(
                    CUJ_DESKTOP_MODE_ENTER_APP_HANDLE_DRAG_HOLD,
                    context,
                    taskSurface,
                    handler,
                )
                .setTimeout(APP_HANDLE_DRAG_HOLD_CUJ_TIMEOUT_MS)
        interactionJankMonitor.begin(jankConfigBuilder)
        dragToDesktopTransitionHandler.startDragToDesktopTransition(
            taskInfo,
            dragToDesktopValueAnimator,
            visualIndicator,
            dragInterruptedCallback,
        )
    }

    /**
     * The second part of the animated drag to desktop transition, called after
     * [startDragToDesktop].
     */
    private fun finalizeDragToDesktop(taskInfo: RunningTaskInfo) {
        val deskId = getDefaultDeskId(taskInfo.displayId)
        ProtoLog.v(
            WM_SHELL_DESKTOP_MODE,
            "DesktopTasksController: finalizeDragToDesktop taskId=%d deskId=%d",
            taskInfo.taskId,
            deskId,
        )
        val wct = WindowContainerTransaction()
        exitSplitIfApplicable(wct, taskInfo)
        if (!DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
            // |moveHomeTask| is also called in |bringDesktopAppsToFrontBeforeShowingNewTask|, so
            // this shouldn't be necessary at all.
            if (Flags.enablePerDisplayDesktopWallpaperActivity()) {
                moveHomeTask(taskInfo.displayId, wct)
            } else {
                moveHomeTask(context.displayId, wct)
            }
        }
        val runOnTransitStart = addDeskActivationWithMovingTaskChanges(deskId, wct, taskInfo)
        val exitResult =
            desktopImmersiveController.exitImmersiveIfApplicable(
                wct = wct,
                displayId = taskInfo.displayId,
                excludeTaskId = null,
                reason = DesktopImmersiveController.ExitReason.TASK_LAUNCH,
            )
        val transition = dragToDesktopTransitionHandler.finishDragToDesktopTransition(wct)
        desktopModeEnterExitTransitionListener?.onEnterDesktopModeTransitionStarted(
            DRAG_TO_DESKTOP_FINISH_ANIM_DURATION_MS.toInt()
        )
        if (transition != null) {
            runOnTransitStart?.invoke(transition)
            exitResult.asExit()?.runOnTransitionStart?.invoke(transition)
            if (!DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
                taskRepository.setActiveDesk(displayId = taskInfo.displayId, deskId = deskId)
            }
        } else {
            LatencyTracker.getInstance(context)
                .onActionCancel(LatencyTracker.ACTION_DESKTOP_MODE_ENTER_APP_HANDLE_DRAG)
        }
    }

    /**
     * Perform needed cleanup transaction once animation is complete. Bounds need to be set here
     * instead of initial wct to both avoid flicker and to have task bounds to use for the staging
     * animation.
     *
     * @param taskInfo task entering split that requires a bounds update
     */
    fun onDesktopSplitSelectAnimComplete(taskInfo: RunningTaskInfo) {
        val wct = WindowContainerTransaction()
        wct.setBounds(taskInfo.token, Rect())
        wct.setWindowingMode(taskInfo.token, WINDOWING_MODE_UNDEFINED)
        shellTaskOrganizer.applyTransaction(wct)
    }

    /**
     * Perform clean up of the desktop wallpaper activity if the closed window task is the last
     * active task.
     *
     * @param wct transaction to modify if the last active task is closed
     * @param displayId display id of the window that's being closed
     * @param taskId task id of the window that's being closed
     */
    fun onDesktopWindowClose(
        wct: WindowContainerTransaction,
        displayId: Int,
        taskInfo: RunningTaskInfo,
    ): ((IBinder) -> Unit) {
        val taskId = taskInfo.taskId
        val deskId = taskRepository.getDeskIdForTask(taskInfo.taskId)
        if (deskId == null && DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
            error("Did not find desk for task: $taskId")
        }
        snapEventHandler.removeTaskIfTiled(displayId, taskId)
        val shouldExitDesktop =
            willExitDesktop(
                triggerTaskId = taskInfo.taskId,
                displayId = displayId,
                forceExitDesktop = false,
            )
        val desktopExitRunnable =
            performDesktopExitCleanUp(
                wct = wct,
                deskId = deskId,
                displayId = displayId,
                willExitDesktop = shouldExitDesktop,
                shouldEndUpAtHome = true,
            )

        taskRepository.addClosingTask(displayId = displayId, deskId = deskId, taskId = taskId)
        taskbarDesktopTaskListener?.onTaskbarCornerRoundingUpdate(
            doesAnyTaskRequireTaskbarRounding(displayId, taskId)
        )

        val immersiveRunnable =
            desktopImmersiveController
                .exitImmersiveIfApplicable(
                    wct = wct,
                    taskInfo = taskInfo,
                    reason = DesktopImmersiveController.ExitReason.CLOSED,
                )
                .asExit()
                ?.runOnTransitionStart
        return { transitionToken ->
            immersiveRunnable?.invoke(transitionToken)
            desktopExitRunnable?.invoke(transitionToken)
        }
    }

    fun minimizeTask(taskInfo: RunningTaskInfo, minimizeReason: MinimizeReason) {
        val wct = WindowContainerTransaction()
        val taskId = taskInfo.taskId
        val displayId = taskInfo.displayId
        val deskId =
            taskRepository.getDeskIdForTask(taskInfo.taskId)
                ?: if (DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
                    logW("minimizeTask: desk not found for task: ${taskInfo.taskId}")
                    return
                } else {
                    getDefaultDeskId(taskInfo.displayId)
                }
        val isLastTask =
            if (DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
                taskRepository.isOnlyVisibleNonClosingTaskInDesk(
                    taskId = taskId,
                    deskId = checkNotNull(deskId) { "Expected non-null deskId" },
                    displayId = displayId,
                )
            } else {
                taskRepository.isOnlyVisibleNonClosingTask(taskId = taskId, displayId = displayId)
            }
        val isMinimizingToPip =
            DesktopModeFlags.ENABLE_DESKTOP_WINDOWING_PIP.isTrue &&
                (taskInfo.pictureInPictureParams?.isAutoEnterEnabled ?: false)

        // If task is going to PiP, start a PiP transition instead of a minimize transition
        if (isMinimizingToPip) {
            val requestInfo =
                TransitionRequestInfo(
                    TRANSIT_PIP,
                    /* triggerTask= */ null,
                    taskInfo,
                    /* remoteTransition= */ null,
                    /* displayChange= */ null,
                    /* flags= */ 0,
                )
            val requestRes =
                transitions.dispatchRequest(SYNTHETIC_TRANSITION, requestInfo, /* skip= */ null)
            wct.merge(requestRes.second, true)

            // If the task minimizing to PiP is the last task, modify wct to perform Desktop cleanup
            var desktopExitRunnable: RunOnTransitStart? = null
            if (isLastTask) {
                desktopExitRunnable =
                    performDesktopExitCleanUp(
                        wct = wct,
                        deskId = deskId,
                        displayId = displayId,
                        willExitDesktop = true,
                    )
            }
            val transition = freeformTaskTransitionStarter.startPipTransition(wct)
            desktopExitRunnable?.invoke(transition)
        } else {
            snapEventHandler.removeTaskIfTiled(displayId, taskId)
            val willExitDesktop = willExitDesktop(taskId, displayId, forceExitDesktop = false)
            val desktopExitRunnable =
                performDesktopExitCleanUp(
                    wct = wct,
                    deskId = deskId,
                    displayId = displayId,
                    willExitDesktop = willExitDesktop,
                )
            // Notify immersive handler as it might need to exit immersive state.
            val exitResult =
                desktopImmersiveController.exitImmersiveIfApplicable(
                    wct = wct,
                    taskInfo = taskInfo,
                    reason = DesktopImmersiveController.ExitReason.MINIMIZED,
                )
            if (DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
                desksOrganizer.minimizeTask(
                    wct = wct,
                    deskId = checkNotNull(deskId) { "Expected non-null deskId" },
                    task = taskInfo,
                )
            } else {
                wct.reorder(taskInfo.token, /* onTop= */ false)
            }
            val transition =
                freeformTaskTransitionStarter.startMinimizedModeTransition(wct, taskId, isLastTask)
            desktopTasksLimiter.ifPresent {
                it.addPendingMinimizeChange(
                    transition = transition,
                    displayId = displayId,
                    taskId = taskId,
                    minimizeReason = minimizeReason,
                )
            }
            exitResult.asExit()?.runOnTransitionStart?.invoke(transition)
            desktopExitRunnable?.invoke(transition)
        }
        taskbarDesktopTaskListener?.onTaskbarCornerRoundingUpdate(
            doesAnyTaskRequireTaskbarRounding(displayId, taskId)
        )
    }

    /** Move a task with given `taskId` to fullscreen */
    fun moveToFullscreen(taskId: Int, transitionSource: DesktopModeTransitionSource) {
        shellTaskOrganizer.getRunningTaskInfo(taskId)?.let { task ->
            snapEventHandler.removeTaskIfTiled(task.displayId, taskId)
            moveToFullscreenWithAnimation(task, task.positionInParent, transitionSource)
        }
    }

    /** Enter fullscreen by moving the focused freeform task in given `displayId` to fullscreen. */
    fun enterFullscreen(displayId: Int, transitionSource: DesktopModeTransitionSource) {
        getFocusedFreeformTask(displayId)?.let {
            snapEventHandler.removeTaskIfTiled(displayId, it.taskId)
            moveToFullscreenWithAnimation(it, it.positionInParent, transitionSource)
        }
    }

    private fun exitSplitIfApplicable(wct: WindowContainerTransaction, taskInfo: RunningTaskInfo) {
        if (splitScreenController.isTaskInSplitScreen(taskInfo.taskId)) {
            splitScreenController.prepareExitSplitScreen(
                wct,
                splitScreenController.getStageOfTask(taskInfo.taskId),
                EXIT_REASON_DESKTOP_MODE,
            )
            splitScreenController.transitionHandler?.onSplitToDesktop()
        }
    }

    /**
     * The second part of the animated drag to desktop transition, called after
     * [startDragToDesktop].
     */
    fun cancelDragToDesktop(task: RunningTaskInfo) {
        logV("cancelDragToDesktop taskId=%d", task.taskId)
        dragToDesktopTransitionHandler.cancelDragToDesktopTransition(
            DragToDesktopTransitionHandler.CancelState.STANDARD_CANCEL
        )
    }

    private fun moveToFullscreenWithAnimation(
        task: RunningTaskInfo,
        position: Point,
        transitionSource: DesktopModeTransitionSource,
    ) {
        logV("moveToFullscreenWithAnimation taskId=%d", task.taskId)
        val wct = WindowContainerTransaction()
        val willExitDesktop = willExitDesktop(task.taskId, task.displayId, forceExitDesktop = true)
        val deactivationRunnable = addMoveToFullscreenChanges(wct, task, willExitDesktop)

        // We are moving a freeform task to fullscreen, put the home task under the fullscreen task.
        if (!forceEnterDesktop(task.displayId)) {
            moveHomeTask(task.displayId, wct)
            wct.reorder(task.token, /* onTop= */ true)
        }

        val transition =
            exitDesktopTaskTransitionHandler.startTransition(
                transitionSource,
                wct,
                position,
                mOnAnimationFinishedCallback,
            )
        deactivationRunnable?.invoke(transition)

        // handles case where we are moving to full screen without closing all DW tasks.
        if (
            !taskRepository.isOnlyVisibleNonClosingTask(task.taskId)
            // This callback is already invoked by |addMoveToFullscreenChanges| when this flag is
            // enabled.
            && !DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue
        ) {
            desktopModeEnterExitTransitionListener?.onExitDesktopModeTransitionStarted(
                FULLSCREEN_ANIMATION_DURATION
            )
        }
    }

    /**
     * Move a task to the front, using [remoteTransition].
     *
     * Note: beyond moving a task to the front, this method will minimize a task if we reach the
     * Desktop task limit, so [remoteTransition] should also handle any such minimize change.
     */
    @JvmOverloads
    fun moveTaskToFront(
        taskId: Int,
        remoteTransition: RemoteTransition? = null,
        unminimizeReason: UnminimizeReason,
    ) {
        val task = shellTaskOrganizer.getRunningTaskInfo(taskId)
        if (task == null) {
            moveBackgroundTaskToFront(taskId, remoteTransition, unminimizeReason)
        } else {
            moveTaskToFront(task, remoteTransition, unminimizeReason)
        }
    }

    /**
     * Launch a background task in desktop. Note that this should be used when we are already in
     * desktop. If outside of desktop and want to launch a background task in desktop, use
     * [moveBackgroundTaskToDesktop] instead.
     */
    private fun moveBackgroundTaskToFront(
        taskId: Int,
        remoteTransition: RemoteTransition?,
        unminimizeReason: UnminimizeReason,
    ) {
        logV("moveBackgroundTaskToFront taskId=%s", taskId)
        val wct = WindowContainerTransaction()
        wct.startTask(
            taskId,
            ActivityOptions.makeBasic()
                .apply { launchWindowingMode = WINDOWING_MODE_FREEFORM }
                .toBundle(),
        )
        val deskId = taskRepository.getDeskIdForTask(taskId) ?: getDefaultDeskId(DEFAULT_DISPLAY)
        startLaunchTransition(
            TRANSIT_OPEN,
            wct,
            taskId,
            deskId = deskId,
            displayId = DEFAULT_DISPLAY,
            remoteTransition = remoteTransition,
            unminimizeReason = unminimizeReason,
        )
    }

    /**
     * Move a task to the front, using [remoteTransition].
     *
     * Note: beyond moving a task to the front, this method will minimize a task if we reach the
     * Desktop task limit, so [remoteTransition] should also handle any such minimize change.
     */
    @JvmOverloads
    fun moveTaskToFront(
        taskInfo: RunningTaskInfo,
        remoteTransition: RemoteTransition? = null,
        unminimizeReason: UnminimizeReason = UnminimizeReason.UNKNOWN,
    ) {
        val deskId =
            taskRepository.getDeskIdForTask(taskInfo.taskId) ?: getDefaultDeskId(taskInfo.displayId)
        logV("moveTaskToFront taskId=%s deskId=%s", taskInfo.taskId, deskId)
        // If a task is tiled, another task should be brought to foreground with it so let
        // tiling controller handle the request.
        if (snapEventHandler.moveTaskToFrontIfTiled(taskInfo)) {
            return
        }
        val wct = WindowContainerTransaction()
        if (DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
            desksOrganizer.reorderTaskToFront(wct, deskId, taskInfo)
        } else {
            wct.reorder(taskInfo.token, /* onTop= */ true, /* includingParents= */ true)
        }
        startLaunchTransition(
            transitionType = TRANSIT_TO_FRONT,
            wct = wct,
            launchingTaskId = taskInfo.taskId,
            remoteTransition = remoteTransition,
            deskId = deskId,
            displayId = taskInfo.displayId,
            unminimizeReason = unminimizeReason,
        )
    }

    @VisibleForTesting
    fun startLaunchTransition(
        transitionType: Int,
        wct: WindowContainerTransaction,
        launchingTaskId: Int?,
        remoteTransition: RemoteTransition? = null,
        deskId: Int,
        displayId: Int,
        unminimizeReason: UnminimizeReason = UnminimizeReason.UNKNOWN,
    ): IBinder {
        logV(
            "startLaunchTransition type=%s launchingTaskId=%d deskId=%d displayId=%d",
            WindowManager.transitTypeToString(transitionType),
            launchingTaskId,
            deskId,
            displayId,
        )
        // TODO: b/397619806 - Consolidate sharable logic with [handleFreeformTaskLaunch].
        var launchTransaction = wct
        val taskIdToMinimize =
            addAndGetMinimizeChanges(
                deskId,
                launchTransaction,
                newTaskId = launchingTaskId,
                launchingNewIntent = launchingTaskId == null,
            )
        val exitImmersiveResult =
            desktopImmersiveController.exitImmersiveIfApplicable(
                wct = launchTransaction,
                displayId = displayId,
                excludeTaskId = launchingTaskId,
                reason = DesktopImmersiveController.ExitReason.TASK_LAUNCH,
            )
        var activationRunOnTransitStart: RunOnTransitStart? = null
        val shouldActivateDesk =
            when {
                DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue ->
                    !taskRepository.isDeskActive(deskId)
                DesktopExperienceFlags.ENABLE_DISPLAY_WINDOWING_MODE_SWITCHING.isTrue -> {
                    !isDesktopModeShowing(displayId)
                }
                else -> false
            }
        if (shouldActivateDesk) {
            val activateDeskWct = WindowContainerTransaction()
            // TODO: b/391485148 - pass in the launching task here to apply task-limit policy,
            //  but make sure to not do it twice since it is also done at the start of this
            //  function.
            activationRunOnTransitStart = addDeskActivationChanges(deskId, activateDeskWct)
            // Desk activation must be handled before app launch-related transactions.
            activateDeskWct.merge(launchTransaction, /* transfer= */ true)
            launchTransaction = activateDeskWct
            desktopModeEnterExitTransitionListener?.onEnterDesktopModeTransitionStarted(
                FREEFORM_ANIMATION_DURATION
            )
        }
        val t =
            if (remoteTransition == null) {
                logV("startLaunchTransition -- no remoteTransition -- wct = $launchTransaction")
                desktopMixedTransitionHandler.startLaunchTransition(
                    transitionType = transitionType,
                    wct = launchTransaction,
                    taskId = launchingTaskId,
                    minimizingTaskId = taskIdToMinimize,
                    exitingImmersiveTask = exitImmersiveResult.asExit()?.exitingTask,
                )
            } else if (taskIdToMinimize == null) {
                val remoteTransitionHandler = OneShotRemoteHandler(mainExecutor, remoteTransition)
                transitions
                    .startTransition(transitionType, launchTransaction, remoteTransitionHandler)
                    .also { remoteTransitionHandler.setTransition(it) }
            } else {
                val remoteTransitionHandler =
                    DesktopWindowLimitRemoteHandler(
                        mainExecutor,
                        rootTaskDisplayAreaOrganizer,
                        remoteTransition,
                        taskIdToMinimize,
                    )
                transitions
                    .startTransition(transitionType, launchTransaction, remoteTransitionHandler)
                    .also { remoteTransitionHandler.setTransition(it) }
            }
        if (taskIdToMinimize != null) {
            addPendingMinimizeTransition(t, taskIdToMinimize, MinimizeReason.TASK_LIMIT)
        }
        if (launchingTaskId != null && taskRepository.isMinimizedTask(launchingTaskId)) {
            addPendingUnminimizeTransition(t, displayId, launchingTaskId, unminimizeReason)
        }
        activationRunOnTransitStart?.invoke(t)
        exitImmersiveResult.asExit()?.runOnTransitionStart?.invoke(t)
        return t
    }

    /**
     * Move task to the next display.
     *
     * Queries all current known display ids and sorts them in ascending order. Then iterates
     * through the list and looks for the display id that is larger than the display id for the
     * passed in task. If a display with a higher id is not found, iterates through the list and
     * finds the first display id that is not the display id for the passed in task.
     *
     * If a display matching the above criteria is found, re-parents the task to that display. No-op
     * if no such display is found.
     */
    fun moveToNextDisplay(taskId: Int) {
        val task = shellTaskOrganizer.getRunningTaskInfo(taskId)
        if (task == null) {
            logW("moveToNextDisplay: taskId=%d not found", taskId)
            return
        }
        logV("moveToNextDisplay: taskId=%d displayId=%d", taskId, task.displayId)

        val displayIds = rootTaskDisplayAreaOrganizer.displayIds.sorted()
        // Get the first display id that is higher than current task display id
        var newDisplayId = displayIds.firstOrNull { displayId -> displayId > task.displayId }
        if (newDisplayId == null) {
            // No display with a higher id, get the first display id that is not the task display id
            newDisplayId = displayIds.firstOrNull { displayId -> displayId < task.displayId }
        }
        if (newDisplayId == null) {
            logW("moveToNextDisplay: next display not found")
            return
        }
        moveToDisplay(task, newDisplayId)
    }

    /**
     * Start an intent through a launch transition for starting tasks whose transition does not get
     * handled by [handleRequest]
     */
    fun startLaunchIntentTransition(intent: Intent, options: Bundle, displayId: Int) {
        val wct = WindowContainerTransaction()
        val displayLayout = displayController.getDisplayLayout(displayId) ?: return
        val bounds = calculateDefaultDesktopTaskBounds(displayLayout)
        if (DesktopModeFlags.ENABLE_CASCADING_WINDOWS.isTrue) {
            cascadeWindow(bounds, displayLayout, displayId)
        }
        val pendingIntent =
            PendingIntent.getActivityAsUser(
                context,
                /* requestCode= */ 0,
                intent,
                PendingIntent.FLAG_IMMUTABLE,
                /* options= */ null,
                UserHandle.of(userId),
            )
        val ops =
            ActivityOptions.fromBundle(options).apply {
                launchWindowingMode = WINDOWING_MODE_FREEFORM
                pendingIntentBackgroundActivityStartMode =
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                launchBounds = bounds
                launchDisplayId = displayId
                if (DesktopModeFlags.ENABLE_SHELL_INITIAL_BOUNDS_REGRESSION_BUG_FIX.isTrue) {
                    // Sets launch bounds size as flexible so core can recalculate.
                    flexibleLaunchSize = true
                }
            }

        wct.sendPendingIntent(pendingIntent, intent, ops.toBundle())
        val deskId = getDefaultDeskId(displayId)
        startLaunchTransition(
            TRANSIT_OPEN,
            wct,
            launchingTaskId = null,
            deskId = deskId,
            displayId = displayId,
        )
    }

    /**
     * Move [task] to display with [displayId].
     *
     * No-op if task is already on that display per [RunningTaskInfo.displayId].
     *
     * TODO: b/399411604 - split this up into smaller functions.
     */
    private fun moveToDisplay(task: RunningTaskInfo, displayId: Int) {
        logV("moveToDisplay: taskId=%d displayId=%d", task.taskId, displayId)
        if (task.displayId == displayId) {
            logD("moveToDisplay: task already on display %d", displayId)
            return
        }

        if (splitScreenController.isTaskInSplitScreen(task.taskId)) {
            moveSplitPairToDisplay(task, displayId)
            return
        }

        val wct = WindowContainerTransaction()
        val displayAreaInfo = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(displayId)
        if (displayAreaInfo == null) {
            logW("moveToDisplay: display not found")
            return
        }

        val destinationDeskId = taskRepository.getDefaultDeskId(displayId)
        if (destinationDeskId == null) {
            logW("moveToDisplay: desk not found for display: $displayId")
            return
        }

        // TODO: b/393977830 and b/397437641 - do not assume that freeform==desktop.
        if (!task.isFreeform) {
            addMoveToDeskTaskChanges(wct = wct, task = task, deskId = destinationDeskId)
        } else {
            if (DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
                desksOrganizer.moveTaskToDesk(wct, destinationDeskId, task)
            }
            if (Flags.enableMoveToNextDisplayShortcut()) {
                applyFreeformDisplayChange(wct, task, displayId)
            }
        }

        if (!DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
            wct.reparent(task.token, displayAreaInfo.token, /* onTop= */ true)
        }

        val activationRunnable = addDeskActivationChanges(destinationDeskId, wct, task)

        if (Flags.enableDisplayFocusInShellTransitions()) {
            // Bring the destination display to top with includingParents=true, so that the
            // destination display gains the display focus, which makes the top task in the display
            // gains the global focus.
            wct.reorder(task.token, /* onTop= */ true, /* includingParents= */ true)
        }

        val sourceDisplayId = task.displayId
        val sourceDeskId = taskRepository.getDeskIdForTask(task.taskId)
        val shouldExitDesktopIfNeeded =
            Flags.enablePerDisplayDesktopWallpaperActivity() ||
                DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue
        val deactivationRunnable =
            if (shouldExitDesktopIfNeeded) {
                performDesktopExitCleanupIfNeeded(
                    taskId = task.taskId,
                    deskId = sourceDeskId,
                    displayId = sourceDisplayId,
                    wct = wct,
                    forceToFullscreen = false,
                    // TODO: b/371096166 - Temporary turing home relaunch off to prevent home
                    // stealing
                    // display focus. Remove shouldEndUpAtHome = false when home focus handling
                    // with connected display is implemented in wm core.
                    shouldEndUpAtHome = false,
                )
            } else {
                null
            }
        val transition =
            transitions.startTransition(TRANSIT_CHANGE, wct, moveToDisplayTransitionHandler)
        deactivationRunnable?.invoke(transition)
        activationRunnable?.invoke(transition)
    }

    /**
     * Move split pair associated with the [task] to display with [displayId].
     *
     * No-op if task is already on that display per [RunningTaskInfo.displayId].
     */
    private fun moveSplitPairToDisplay(task: RunningTaskInfo, displayId: Int) {
        if (!splitScreenController.isTaskInSplitScreen(task.taskId)) {
            return
        }

        if (!Flags.enableNonDefaultDisplaySplit() || !Flags.enableMoveToNextDisplayShortcut()) {
            return
        }

        val wct = WindowContainerTransaction()
        val displayAreaInfo = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(displayId)
        if (displayAreaInfo == null) {
            logW("moveSplitPairToDisplay: display not found")
            return
        }

        val activeDeskId = taskRepository.getActiveDeskId(displayId)
        logV("moveSplitPairToDisplay: moving split root to displayId=%d", displayId)

        val stageCoordinatorRootTaskToken =
            splitScreenController.multiDisplayProvider.getDisplayRootForDisplayId(DEFAULT_DISPLAY)
        if (stageCoordinatorRootTaskToken == null) {
            return
        }
        wct.reparent(stageCoordinatorRootTaskToken, displayAreaInfo.token, true /* onTop */)

        val deactivationRunnable =
            if (activeDeskId != null) {
                // Split is being placed on top of an existing desk in the target display. Make
                // sure it is cleaned up.
                performDesktopExitCleanUp(
                    wct = wct,
                    deskId = activeDeskId,
                    displayId = displayId,
                    willExitDesktop = true,
                    shouldEndUpAtHome = false,
                )
            } else {
                null
            }
        val transition = transitions.startTransition(TRANSIT_CHANGE, wct, /* handler= */ null)
        deactivationRunnable?.invoke(transition)
        return
    }

    /**
     * Quick-resizes a desktop task, toggling between a fullscreen state (represented by the stable
     * bounds) and a free floating state (either the last saved bounds if available or the default
     * bounds otherwise).
     */
    fun toggleDesktopTaskSize(taskInfo: RunningTaskInfo, interaction: ToggleTaskSizeInteraction) {
        val currentTaskBounds = taskInfo.configuration.windowConfiguration.bounds
        desktopModeEventLogger.logTaskResizingStarted(
            interaction.resizeTrigger,
            interaction.inputMethod,
            taskInfo,
            currentTaskBounds.width(),
            currentTaskBounds.height(),
            displayController,
        )
        val displayLayout = displayController.getDisplayLayout(taskInfo.displayId) ?: return
        val destinationBounds = Rect()
        val isMaximized = interaction.direction == ToggleTaskSizeInteraction.Direction.RESTORE
        // If the task is currently maximized, we will toggle it not to be and vice versa. This is
        // helpful to eliminate the current task from logic to calculate taskbar corner rounding.
        val willMaximize = interaction.direction == ToggleTaskSizeInteraction.Direction.MAXIMIZE
        if (isMaximized) {
            // The desktop task is at the maximized width and/or height of the stable bounds.
            // If the task's pre-maximize stable bounds were saved, toggle the task to those bounds.
            // Otherwise, toggle to the default bounds.
            val taskBoundsBeforeMaximize =
                taskRepository.removeBoundsBeforeMaximize(taskInfo.taskId)
            if (taskBoundsBeforeMaximize != null) {
                destinationBounds.set(taskBoundsBeforeMaximize)
            } else {
                if (ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS.isTrue()) {
                    destinationBounds.set(calculateInitialBounds(displayLayout, taskInfo))
                } else {
                    destinationBounds.set(calculateDefaultDesktopTaskBounds(displayLayout))
                }
            }
        } else {
            // Save current bounds so that task can be restored back to original bounds if necessary
            // and toggle to the stable bounds.
            snapEventHandler.removeTaskIfTiled(taskInfo.displayId, taskInfo.taskId)
            taskRepository.saveBoundsBeforeMaximize(taskInfo.taskId, currentTaskBounds)
            destinationBounds.set(calculateMaximizeBounds(displayLayout, taskInfo))
        }

        val shouldRestoreToSnap =
            isMaximized && isTaskSnappedToHalfScreen(taskInfo, destinationBounds)

        logD("willMaximize = %s", willMaximize)
        logD("shouldRestoreToSnap = %s", shouldRestoreToSnap)

        val doesAnyTaskRequireTaskbarRounding =
            willMaximize ||
                shouldRestoreToSnap ||
                doesAnyTaskRequireTaskbarRounding(taskInfo.displayId, taskInfo.taskId)

        taskbarDesktopTaskListener?.onTaskbarCornerRoundingUpdate(doesAnyTaskRequireTaskbarRounding)
        val wct = WindowContainerTransaction().setBounds(taskInfo.token, destinationBounds)
        interaction.uiEvent?.let { uiEvent -> desktopModeUiEventLogger.log(taskInfo, uiEvent) }
        desktopModeEventLogger.logTaskResizingEnded(
            interaction.resizeTrigger,
            interaction.inputMethod,
            taskInfo,
            destinationBounds.width(),
            destinationBounds.height(),
            displayController,
        )
        toggleResizeDesktopTaskTransitionHandler.startTransition(
            wct,
            interaction.animationStartBounds,
        )
    }

    private fun dragToMaximizeDesktopTask(
        taskInfo: RunningTaskInfo,
        taskSurface: SurfaceControl,
        currentDragBounds: Rect,
        motionEvent: MotionEvent,
    ) {
        if (isTaskMaximized(taskInfo, displayController)) {
            // Handle the case where we attempt to drag-to-maximize when already maximized: the task
            // position won't need to change but we want to animate the surface going back to the
            // maximized position.
            val containerBounds = taskInfo.configuration.windowConfiguration.bounds
            if (containerBounds != currentDragBounds) {
                returnToDragStartAnimator.start(
                    taskInfo.taskId,
                    taskSurface,
                    startBounds = currentDragBounds,
                    endBounds = containerBounds,
                )
            }
            return
        }

        toggleDesktopTaskSize(
            taskInfo,
            ToggleTaskSizeInteraction(
                direction = ToggleTaskSizeInteraction.Direction.MAXIMIZE,
                source = ToggleTaskSizeInteraction.Source.HEADER_DRAG_TO_TOP,
                inputMethod = DesktopModeEventLogger.getInputMethodFromMotionEvent(motionEvent),
                animationStartBounds = currentDragBounds,
            ),
        )
    }

    private fun isMaximizedToStableBoundsEdges(
        taskInfo: RunningTaskInfo,
        stableBounds: Rect,
    ): Boolean {
        val currentTaskBounds = taskInfo.configuration.windowConfiguration.bounds
        return isTaskBoundsEqual(currentTaskBounds, stableBounds)
    }

    /** Returns if current task bound is snapped to half screen */
    private fun isTaskSnappedToHalfScreen(
        taskInfo: RunningTaskInfo,
        taskBounds: Rect = taskInfo.configuration.windowConfiguration.bounds,
    ): Boolean =
        getSnapBounds(taskInfo, SnapPosition.LEFT) == taskBounds ||
            getSnapBounds(taskInfo, SnapPosition.RIGHT) == taskBounds

    @VisibleForTesting
    fun doesAnyTaskRequireTaskbarRounding(displayId: Int, excludeTaskId: Int? = null): Boolean {
        val doesAnyTaskRequireTaskbarRounding =
            taskRepository
                .getExpandedTasksOrdered(displayId)
                // exclude current task since maximize/restore transition has not taken place yet.
                .filterNot { taskId -> taskId == excludeTaskId }
                .any { taskId ->
                    val taskInfo = shellTaskOrganizer.getRunningTaskInfo(taskId) ?: return false
                    val displayLayout = displayController.getDisplayLayout(taskInfo.displayId)
                    val stableBounds = Rect().apply { displayLayout?.getStableBounds(this) }
                    logD("taskInfo = %s", taskInfo)
                    logD(
                        "isTaskSnappedToHalfScreen(taskInfo) = %s",
                        isTaskSnappedToHalfScreen(taskInfo),
                    )
                    logD(
                        "isMaximizedToStableBoundsEdges(taskInfo, stableBounds) = %s",
                        isMaximizedToStableBoundsEdges(taskInfo, stableBounds),
                    )
                    isTaskSnappedToHalfScreen(taskInfo) ||
                        isMaximizedToStableBoundsEdges(taskInfo, stableBounds)
                }

        logD("doesAnyTaskRequireTaskbarRounding = %s", doesAnyTaskRequireTaskbarRounding)
        return doesAnyTaskRequireTaskbarRounding
    }

    /**
     * Quick-resize to the right or left half of the stable bounds.
     *
     * @param taskInfo current task that is being snap-resized via dragging or maximize menu button
     * @param taskSurface the leash of the task being dragged
     * @param currentDragBounds current position of the task leash being dragged (or current task
     *   bounds if being snapped resize via maximize menu button)
     * @param position the portion of the screen (RIGHT or LEFT) we want to snap the task to.
     */
    fun snapToHalfScreen(
        taskInfo: RunningTaskInfo,
        taskSurface: SurfaceControl?,
        currentDragBounds: Rect,
        position: SnapPosition,
        resizeTrigger: ResizeTrigger,
        inputMethod: InputMethod,
    ) {
        desktopModeEventLogger.logTaskResizingStarted(
            resizeTrigger,
            inputMethod,
            taskInfo,
            currentDragBounds.width(),
            currentDragBounds.height(),
            displayController,
        )

        val destinationBounds = getSnapBounds(taskInfo, position)
        desktopModeEventLogger.logTaskResizingEnded(
            resizeTrigger,
            inputMethod,
            taskInfo,
            destinationBounds.width(),
            destinationBounds.height(),
            displayController,
        )

        if (DesktopModeFlags.ENABLE_TILE_RESIZING.isTrue()) {
            val isTiled = snapEventHandler.snapToHalfScreen(taskInfo, currentDragBounds, position)
            if (isTiled) {
                taskbarDesktopTaskListener?.onTaskbarCornerRoundingUpdate(true)
            }
            return
        }

        if (destinationBounds == taskInfo.configuration.windowConfiguration.bounds) {
            // Handle the case where we attempt to snap resize when already snap resized: the task
            // position won't need to change but we want to animate the surface going back to the
            // snapped position from the "dragged-to-the-edge" position.
            if (destinationBounds != currentDragBounds && taskSurface != null) {
                returnToDragStartAnimator.start(
                    taskInfo.taskId,
                    taskSurface,
                    startBounds = currentDragBounds,
                    endBounds = destinationBounds,
                )
            }
            return
        }

        taskbarDesktopTaskListener?.onTaskbarCornerRoundingUpdate(true)
        val wct = WindowContainerTransaction().setBounds(taskInfo.token, destinationBounds)

        toggleResizeDesktopTaskTransitionHandler.startTransition(wct, currentDragBounds)
    }

    /**
     * Handles snap resizing a [taskInfo] to [position] instantaneously, for example when the
     * [resizeTrigger] is the snap resize menu using any [motionEvent] or a keyboard shortcut.
     */
    fun handleInstantSnapResizingTask(
        taskInfo: RunningTaskInfo,
        position: SnapPosition,
        resizeTrigger: ResizeTrigger,
        inputMethod: InputMethod,
    ) {
        if (!isSnapResizingAllowed(taskInfo)) {
            Toast.makeText(
                    getContext(),
                    R.string.desktop_mode_non_resizable_snap_text,
                    Toast.LENGTH_SHORT,
                )
                .show()
            return
        }

        snapToHalfScreen(
            taskInfo,
            null,
            taskInfo.configuration.windowConfiguration.bounds,
            position,
            resizeTrigger,
            inputMethod,
        )
    }

    @VisibleForTesting
    fun handleSnapResizingTaskOnDrag(
        taskInfo: RunningTaskInfo,
        position: SnapPosition,
        taskSurface: SurfaceControl,
        currentDragBounds: Rect,
        dragStartBounds: Rect,
        motionEvent: MotionEvent,
    ) {
        releaseVisualIndicator()
        if (!isSnapResizingAllowed(taskInfo)) {
            interactionJankMonitor.begin(
                taskSurface,
                context,
                handler,
                CUJ_DESKTOP_MODE_SNAP_RESIZE,
                "drag_non_resizable",
            )

            // reposition non-resizable app back to its original position before being dragged
            returnToDragStartAnimator.start(
                taskInfo.taskId,
                taskSurface,
                startBounds = currentDragBounds,
                endBounds = dragStartBounds,
                doOnEnd = {
                    Toast.makeText(
                            context,
                            com.android.wm.shell.R.string.desktop_mode_non_resizable_snap_text,
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                },
            )
        } else {
            val resizeTrigger =
                if (position == SnapPosition.LEFT) {
                    ResizeTrigger.DRAG_LEFT
                } else {
                    ResizeTrigger.DRAG_RIGHT
                }
            interactionJankMonitor.begin(
                taskSurface,
                context,
                handler,
                CUJ_DESKTOP_MODE_SNAP_RESIZE,
                "drag_resizable",
            )
            snapToHalfScreen(
                taskInfo,
                taskSurface,
                currentDragBounds,
                position,
                resizeTrigger,
                DesktopModeEventLogger.getInputMethodFromMotionEvent(motionEvent),
            )
        }
    }

    private fun isSnapResizingAllowed(taskInfo: RunningTaskInfo) =
        taskInfo.isResizeable || !DISABLE_NON_RESIZABLE_APP_SNAP_RESIZE.isTrue()

    private fun getSnapBounds(taskInfo: RunningTaskInfo, position: SnapPosition): Rect {
        val displayLayout = displayController.getDisplayLayout(taskInfo.displayId) ?: return Rect()

        val stableBounds = Rect()
        displayLayout.getStableBounds(stableBounds)

        val destinationWidth = stableBounds.width() / 2
        return when (position) {
            SnapPosition.LEFT -> {
                Rect(
                    stableBounds.left,
                    stableBounds.top,
                    stableBounds.left + destinationWidth,
                    stableBounds.bottom,
                )
            }
            SnapPosition.RIGHT -> {
                Rect(
                    stableBounds.right - destinationWidth,
                    stableBounds.top,
                    stableBounds.right,
                    stableBounds.bottom,
                )
            }
        }
    }

    /**
     * Get windowing move for a given `taskId`
     *
     * @return [WindowingMode] for the task or [WINDOWING_MODE_UNDEFINED] if task is not found
     */
    @WindowingMode
    fun getTaskWindowingMode(taskId: Int): Int {
        return shellTaskOrganizer.getRunningTaskInfo(taskId)?.windowingMode
            ?: WINDOWING_MODE_UNDEFINED
    }

    private fun prepareForDeskActivation(displayId: Int, wct: WindowContainerTransaction) {
        // Move home to front, ensures that we go back home when all desktop windows are closed
        val useParamDisplayId =
            DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue ||
                Flags.enablePerDisplayDesktopWallpaperActivity()
        moveHomeTask(displayId = if (useParamDisplayId) displayId else context.displayId, wct = wct)
        // Currently, we only handle the desktop on the default display really.
        if (
            (displayId == DEFAULT_DISPLAY || Flags.enablePerDisplayDesktopWallpaperActivity()) &&
                ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY.isTrue()
        ) {
            // Add translucent wallpaper activity to show the wallpaper underneath.
            addWallpaperActivity(displayId, wct)
        }
    }

    @Deprecated(
        "Use addDeskActivationChanges() instead.",
        ReplaceWith("addDeskActivationChanges()"),
    )
    private fun bringDesktopAppsToFront(
        displayId: Int,
        wct: WindowContainerTransaction,
        newTaskIdInFront: Int? = null,
    ): Int? {
        logV("bringDesktopAppsToFront, newTaskId=%d", newTaskIdInFront)
        prepareForDeskActivation(displayId, wct)

        val expandedTasksOrderedFrontToBack = taskRepository.getExpandedTasksOrdered(displayId)
        // If we're adding a new Task we might need to minimize an old one
        // TODO(b/365725441): Handle non running task minimization
        val taskIdToMinimize: Int? =
            if (newTaskIdInFront != null && desktopTasksLimiter.isPresent) {
                desktopTasksLimiter
                    .get()
                    .getTaskIdToMinimize(expandedTasksOrderedFrontToBack, newTaskIdInFront)
            } else {
                null
            }

        expandedTasksOrderedFrontToBack
            // If there is a Task to minimize, let it stay behind the Home Task
            .filter { taskId -> taskId != taskIdToMinimize }
            .reversed() // Start from the back so the front task is brought forward last
            .forEach { taskId ->
                val runningTaskInfo = shellTaskOrganizer.getRunningTaskInfo(taskId)
                if (runningTaskInfo != null) {
                    // Task is already running, reorder it to the front
                    wct.reorder(runningTaskInfo.token, /* onTop= */ true)
                } else if (DesktopModeFlags.ENABLE_DESKTOP_WINDOWING_PERSISTENCE.isTrue()) {
                    // Task is not running, start it
                    wct.startTask(taskId, createActivityOptionsForStartTask().toBundle())
                }
            }

        taskbarDesktopTaskListener?.onTaskbarCornerRoundingUpdate(
            doesAnyTaskRequireTaskbarRounding(displayId)
        )

        return taskIdToMinimize
    }

    private fun moveHomeTask(displayId: Int, wct: WindowContainerTransaction) {
        shellTaskOrganizer
            .getRunningTasks(displayId)
            .firstOrNull { task -> task.activityType == ACTIVITY_TYPE_HOME }
            ?.let { homeTask -> wct.reorder(homeTask.getToken(), /* onTop= */ true) }
    }

    private fun addLaunchHomePendingIntent(wct: WindowContainerTransaction, displayId: Int) {
        homeIntentProvider.addLaunchHomePendingIntent(wct, displayId, userId)
    }

    private fun addWallpaperActivity(displayId: Int, wct: WindowContainerTransaction) {
        logV("addWallpaperActivity")
        if (ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER.isTrue()) {

            // If the wallpaper activity for this display already exists, let's reorder it to top.
            val wallpaperActivityToken = desktopWallpaperActivityTokenProvider.getToken(displayId)
            if (wallpaperActivityToken != null) {
                wct.reorder(wallpaperActivityToken, /* onTop= */ true)
                return
            }

            val intent = Intent(context, DesktopWallpaperActivity::class.java)
            if (Flags.enablePerDisplayDesktopWallpaperActivity()) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            val options =
                ActivityOptions.makeBasic().apply {
                    launchWindowingMode = WINDOWING_MODE_FULLSCREEN
                    pendingIntentBackgroundActivityStartMode =
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                    if (Flags.enablePerDisplayDesktopWallpaperActivity()) {
                        launchDisplayId = displayId
                    }
                }
            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    /* requestCode = */ 0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE,
                )
            wct.sendPendingIntent(pendingIntent, intent, options.toBundle())
        } else {
            val userHandle = UserHandle.of(userId)
            val userContext = context.createContextAsUser(userHandle, /* flags= */ 0)
            val intent = Intent(userContext, DesktopWallpaperActivity::class.java)
            if (
                desktopWallpaperActivityTokenProvider.getToken(displayId) == null &&
                    Flags.enablePerDisplayDesktopWallpaperActivity()
            ) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            intent.putExtra(Intent.EXTRA_USER_HANDLE, userId)
            val options =
                ActivityOptions.makeBasic().apply {
                    launchWindowingMode = WINDOWING_MODE_FULLSCREEN
                    pendingIntentBackgroundActivityStartMode =
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                    if (Flags.enablePerDisplayDesktopWallpaperActivity()) {
                        launchDisplayId = displayId
                    }
                }
            val pendingIntent =
                PendingIntent.getActivityAsUser(
                    userContext,
                    /* requestCode= */ 0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE,
                    /* options= */ null,
                    userHandle,
                )
            wct.sendPendingIntent(pendingIntent, intent, options.toBundle())
        }
    }

    private fun removeWallpaperActivity(wct: WindowContainerTransaction, displayId: Int) {
        desktopWallpaperActivityTokenProvider.getToken(displayId)?.let { token ->
            logV("removeWallpaperActivity")
            if (ENABLE_DESKTOP_WALLPAPER_ACTIVITY_FOR_SYSTEM_USER.isTrue()) {
                wct.reorder(token, /* onTop= */ false)
            } else {
                wct.removeTask(token)
            }
        }
    }

    private fun willExitDesktop(
        triggerTaskId: Int,
        displayId: Int,
        forceExitDesktop: Boolean,
    ): Boolean {
        if (forceExitDesktop && DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
            // |forceExitDesktop| is true when the callers knows we'll exit desktop, such as when
            // explicitly going fullscreen, so there's no point in checking the desktop state.
            return true
        }
        if (Flags.enablePerDisplayDesktopWallpaperActivity()) {
            if (!taskRepository.isOnlyVisibleNonClosingTask(triggerTaskId, displayId)) {
                return false
            }
        } else {
            if (!taskRepository.isOnlyVisibleNonClosingTask(triggerTaskId)) {
                return false
            }
        }
        return true
    }

    private fun performDesktopExitCleanupIfNeeded(
        taskId: Int,
        deskId: Int? = null,
        displayId: Int,
        wct: WindowContainerTransaction,
        forceToFullscreen: Boolean,
        shouldEndUpAtHome: Boolean = true,
    ): RunOnTransitStart? {
        if (!willExitDesktop(taskId, displayId, forceToFullscreen)) {
            return null
        }
        // TODO: b/394268248 - update remaining callers to pass in a |deskId| and apply the
        //  |RunOnTransitStart| when the transition is started.
        return performDesktopExitCleanUp(
            wct = wct,
            deskId = deskId,
            displayId = displayId,
            willExitDesktop = true,
            shouldEndUpAtHome = shouldEndUpAtHome,
        )
    }

    /** TODO: b/394268248 - update [deskId] to be non-null. */
    fun performDesktopExitCleanUp(
        wct: WindowContainerTransaction,
        deskId: Int?,
        displayId: Int,
        willExitDesktop: Boolean,
        shouldEndUpAtHome: Boolean = true,
        fromRecentsTransition: Boolean = false,
    ): RunOnTransitStart? {
        if (!willExitDesktop) return null
        desktopModeEnterExitTransitionListener?.onExitDesktopModeTransitionStarted(
            FULLSCREEN_ANIMATION_DURATION
        )
        // No need to clean up the wallpaper / reorder home when coming from a recents transition.
        if (
            !fromRecentsTransition ||
                !DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue
        ) {
            removeWallpaperActivity(wct, displayId)
            if (shouldEndUpAtHome) {
                // If the transition should end up with user going to home, launch home with a
                // pending intent.
                addLaunchHomePendingIntent(wct, displayId)
            }
        }
        return prepareDeskDeactivationIfNeeded(wct, deskId)
    }

    fun releaseVisualIndicator() {
        visualIndicator?.releaseVisualIndicator()
        visualIndicator = null
    }

    override fun getContext(): Context = context

    override fun getRemoteCallExecutor(): ShellExecutor = mainExecutor

    override fun startAnimation(
        transition: IBinder,
        info: TransitionInfo,
        startTransaction: SurfaceControl.Transaction,
        finishTransaction: SurfaceControl.Transaction,
        finishCallback: Transitions.TransitionFinishCallback,
    ): Boolean {
        // This handler should never be the sole handler, so should not animate anything.
        return false
    }

    override fun handleRequest(
        transition: IBinder,
        request: TransitionRequestInfo,
    ): WindowContainerTransaction? {
        logV("handleRequest request=%s", request)
        // Check if we should skip handling this transition
        var reason = ""
        val triggerTask = request.triggerTask
        val recentsAnimationRunning =
            RecentsTransitionStateListener.isAnimating(recentsTransitionState)
        var shouldHandleMidRecentsFreeformLaunch =
            recentsAnimationRunning && isFreeformRelaunch(triggerTask, request)
        val isDragAndDropFullscreenTransition = taskContainsDragAndDropCookie(triggerTask)
        val shouldHandleRequest =
            when {
                // Handle freeform relaunch during recents animation
                shouldHandleMidRecentsFreeformLaunch -> true
                recentsAnimationRunning -> {
                    reason = "recents animation is running"
                    false
                }
                // Don't handle request if this was a tear to fullscreen transition.
                // handleFullscreenTaskLaunch moves fullscreen intents to freeform;
                // this is an exception to the rule
                isDragAndDropFullscreenTransition -> {
                    dragAndDropFullscreenCookie = null
                    false
                }
                // Handle task closing for the last window if wallpaper is available
                shouldHandleTaskClosing(request) -> true
                // Only handle open or to front transitions
                request.type != TRANSIT_OPEN && request.type != TRANSIT_TO_FRONT -> {
                    reason = "transition type not handled (${request.type})"
                    false
                }
                // Only handle when it is a task transition
                triggerTask == null -> {
                    reason = "triggerTask is null"
                    false
                }
                // Only handle standard type tasks
                triggerTask.activityType != ACTIVITY_TYPE_STANDARD -> {
                    reason = "activityType not handled (${triggerTask.activityType})"
                    false
                }
                // Only handle fullscreen or freeform tasks
                !triggerTask.isFullscreen && !triggerTask.isFreeform -> {
                    reason = "windowingMode not handled (${triggerTask.windowingMode})"
                    false
                }
                // Otherwise process it
                else -> true
            }

        if (!shouldHandleRequest) {
            logV("skipping handleRequest reason=%s", reason)
            return null
        }

        val result =
            triggerTask?.let { task ->
                when {
                    // Check if freeform task launch during recents should be handled
                    shouldHandleMidRecentsFreeformLaunch ->
                        handleMidRecentsFreeformTaskLaunch(task, transition)
                    // Check if the closing task needs to be handled
                    TransitionUtil.isClosingType(request.type) ->
                        handleTaskClosing(task, transition, request.type)
                    // Check if the top task shouldn't be allowed to enter desktop mode
                    isIncompatibleTask(task) -> handleIncompatibleTaskLaunch(task, transition)
                    // Check if fullscreen task should be updated
                    task.isFullscreen -> handleFullscreenTaskLaunch(task, transition)
                    // Check if freeform task should be updated
                    task.isFreeform -> handleFreeformTaskLaunch(task, transition)
                    else -> {
                        null
                    }
                }
            }
        logV("handleRequest result=%s", result)
        return result
    }

    /** Whether the given [change] in the [transition] is a known desktop change. */
    fun isDesktopChange(transition: IBinder, change: TransitionInfo.Change): Boolean {
        // Only the immersive controller is currently involved in mixed transitions.
        return DesktopModeFlags.ENABLE_FULLY_IMMERSIVE_IN_DESKTOP.isTrue &&
            desktopImmersiveController.isImmersiveChange(transition, change)
    }

    /**
     * Whether the given transition [info] will potentially include a desktop change, in which case
     * the transition should be treated as mixed so that the change is in part animated by one of
     * the desktop transition handlers.
     */
    fun shouldPlayDesktopAnimation(info: TransitionRequestInfo): Boolean {
        // Only immersive mixed transition are currently supported.
        if (!DesktopModeFlags.ENABLE_FULLY_IMMERSIVE_IN_DESKTOP.isTrue) return false
        val triggerTask = info.triggerTask ?: return false
        if (!isDesktopModeShowing(triggerTask.displayId)) {
            return false
        }
        if (!TransitionUtil.isOpeningType(info.type)) {
            return false
        }
        taskRepository.getTaskInFullImmersiveState(displayId = triggerTask.displayId)
            ?: return false
        return when {
            triggerTask.isFullscreen -> {
                // Trigger fullscreen task will enter desktop, so any existing immersive task
                // should exit.
                shouldFullscreenTaskLaunchSwitchToDesktop(triggerTask)
            }
            triggerTask.isFreeform -> {
                // Trigger freeform task will enter desktop, so any existing immersive task should
                // exit.
                !shouldFreeformTaskLaunchSwitchToFullscreen(triggerTask)
            }
            else -> false
        }
    }

    /** Animate a desktop change found in a mixed transitions. */
    fun animateDesktopChange(
        transition: IBinder,
        change: Change,
        startTransaction: Transaction,
        finishTransaction: Transaction,
        finishCallback: TransitionFinishCallback,
    ) {
        if (!desktopImmersiveController.isImmersiveChange(transition, change)) {
            throw IllegalStateException("Only immersive changes support desktop mixed transitions")
        }
        desktopImmersiveController.animateResizeChange(
            change,
            startTransaction,
            finishTransaction,
            finishCallback,
        )
    }

    private fun taskContainsDragAndDropCookie(taskInfo: RunningTaskInfo?) =
        taskInfo?.launchCookies?.any { it == dragAndDropFullscreenCookie } ?: false

    /**
     * Applies the proper surface states (rounded corners) to tasks when desktop mode is active.
     * This is intended to be used when desktop mode is part of another animation but isn't, itself,
     * animating.
     */
    fun syncSurfaceState(info: TransitionInfo, finishTransaction: SurfaceControl.Transaction) {
        // Add rounded corners to freeform windows
        if (!DesktopModeStatus.useRoundedCorners()) {
            return
        }
        val cornerRadius =
            context.resources
                .getDimensionPixelSize(
                    SharedR.dimen.desktop_windowing_freeform_rounded_corner_radius
                )
                .toFloat()
        info.changes
            .filter { it.taskInfo?.windowingMode == WINDOWING_MODE_FREEFORM }
            .forEach { finishTransaction.setCornerRadius(it.leash, cornerRadius) }
    }

    /** Returns whether an existing desktop task is being relaunched in freeform or not. */
    private fun isFreeformRelaunch(triggerTask: RunningTaskInfo?, request: TransitionRequestInfo) =
        (triggerTask != null &&
            triggerTask.windowingMode == WINDOWING_MODE_FREEFORM &&
            TransitionUtil.isOpeningType(request.type) &&
            taskRepository.isActiveTask(triggerTask.taskId))

    private fun isIncompatibleTask(task: RunningTaskInfo) =
        desktopModeCompatPolicy.isTopActivityExemptFromDesktopWindowing(task)

    private fun shouldHandleTaskClosing(request: TransitionRequestInfo): Boolean =
        ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY.isTrue() &&
            TransitionUtil.isClosingType(request.type) &&
            request.triggerTask != null

    /** Open an existing instance of an app. */
    fun openInstance(callingTask: RunningTaskInfo, requestedTaskId: Int) {
        if (callingTask.isFreeform) {
            val requestedTaskInfo = shellTaskOrganizer.getRunningTaskInfo(requestedTaskId)
            if (requestedTaskInfo?.isFreeform == true) {
                // If requested task is an already open freeform task, just move it to front.
                moveTaskToFront(
                    requestedTaskId,
                    unminimizeReason = UnminimizeReason.APP_HANDLE_MENU_BUTTON,
                )
            } else {
                val deskId = getDefaultDeskId(callingTask.displayId)
                moveTaskToDesk(
                    requestedTaskId,
                    deskId,
                    WindowContainerTransaction(),
                    DesktopModeTransitionSource.APP_HANDLE_MENU_BUTTON,
                )
            }
        } else {
            val options = createNewWindowOptions(callingTask)
            val splitPosition = splitScreenController.determineNewInstancePosition(callingTask)
            splitScreenController.startTask(
                requestedTaskId,
                splitPosition,
                options.toBundle(),
                /* hideTaskToken= */ null,
                if (enableFlexibleSplit())
                    splitScreenController.determineNewInstanceIndex(callingTask)
                else SPLIT_INDEX_UNDEFINED,
            )
        }
    }

    /** Create an Intent to open a new window of a task. */
    fun openNewWindow(callingTaskInfo: RunningTaskInfo) {
        // TODO(b/337915660): Add a transition handler for these; animations
        //  need updates in some cases.
        val baseActivity = callingTaskInfo.baseActivity ?: return
        val userHandle = UserHandle.of(callingTaskInfo.userId)
        val fillIn: Intent =
            userProfileContexts
                .getOrCreate(callingTaskInfo.userId)
                .packageManager
                .getLaunchIntentForPackage(baseActivity.packageName) ?: return
        fillIn.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        val launchIntent =
            PendingIntent.getActivityAsUser(
                context,
                /* requestCode= */ 0,
                fillIn,
                PendingIntent.FLAG_IMMUTABLE,
                /* options= */ null,
                userHandle,
            )
        val options = createNewWindowOptions(callingTaskInfo)
        when (options.launchWindowingMode) {
            WINDOWING_MODE_MULTI_WINDOW -> {
                val splitPosition =
                    splitScreenController.determineNewInstancePosition(callingTaskInfo)
                // TODO(b/349828130) currently pass in index_undefined until we can revisit these
                //  specific cases in the future.
                val splitIndex =
                    if (enableFlexibleSplit())
                        splitScreenController.determineNewInstanceIndex(callingTaskInfo)
                    else SPLIT_INDEX_UNDEFINED
                splitScreenController.startIntent(
                    launchIntent,
                    context.userId,
                    fillIn,
                    splitPosition,
                    options.toBundle(),
                    /* hideTaskToken= */ null,
                    /* forceLaunchNewTask= */ true,
                    splitIndex,
                )
            }
            WINDOWING_MODE_FREEFORM -> {
                val wct = WindowContainerTransaction()
                wct.sendPendingIntent(launchIntent, fillIn, options.toBundle())
                val deskId =
                    taskRepository.getDeskIdForTask(callingTaskInfo.taskId)
                        ?: getDefaultDeskId(callingTaskInfo.displayId)
                startLaunchTransition(
                    transitionType = TRANSIT_OPEN,
                    wct = wct,
                    launchingTaskId = null,
                    deskId = deskId,
                    displayId = callingTaskInfo.displayId,
                )
            }
        }
    }

    private fun createNewWindowOptions(callingTask: RunningTaskInfo): ActivityOptions {
        val newTaskWindowingMode =
            when {
                callingTask.isFreeform -> {
                    WINDOWING_MODE_FREEFORM
                }
                callingTask.isFullscreen || callingTask.isMultiWindow -> {
                    WINDOWING_MODE_MULTI_WINDOW
                }
                else -> {
                    error("Invalid windowing mode: ${callingTask.windowingMode}")
                }
            }
        val bounds =
            when (newTaskWindowingMode) {
                WINDOWING_MODE_FREEFORM -> {
                    displayController.getDisplayLayout(callingTask.displayId)?.let {
                        getInitialBounds(it, callingTask, callingTask.displayId)
                    }
                }
                WINDOWING_MODE_MULTI_WINDOW -> {
                    Rect()
                }
                else -> {
                    error("Invalid windowing mode: $newTaskWindowingMode")
                }
            }
        return ActivityOptions.makeBasic().apply {
            launchWindowingMode = newTaskWindowingMode
            pendingIntentBackgroundActivityStartMode =
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
            launchBounds = bounds
        }
    }

    /**
     * Handles the case where a freeform task is launched from recents.
     *
     * This is a special case where we want to launch the task in fullscreen instead of freeform.
     */
    private fun handleMidRecentsFreeformTaskLaunch(
        task: RunningTaskInfo,
        transition: IBinder,
    ): WindowContainerTransaction? {
        logV("DesktopTasksController: handleMidRecentsFreeformTaskLaunch")
        val wct = WindowContainerTransaction()
        val runOnTransitStart =
            addMoveToFullscreenChanges(
                wct = wct,
                taskInfo = task,
                willExitDesktop =
                    willExitDesktop(
                        triggerTaskId = task.taskId,
                        displayId = task.displayId,
                        forceExitDesktop = true,
                    ),
            )
        runOnTransitStart?.invoke(transition)
        wct.reorder(task.token, true)
        return wct
    }

    private fun handleFreeformTaskLaunch(
        task: RunningTaskInfo,
        transition: IBinder,
    ): WindowContainerTransaction? {
        logV("handleFreeformTaskLaunch")
        if (keyguardManager.isKeyguardLocked) {
            // Do NOT handle freeform task launch when locked.
            // It will be launched in fullscreen windowing mode (Details: b/160925539)
            logV("skip keyguard is locked")
            return null
        }
        val deskId = getDefaultDeskId(task.displayId)
        val wct = WindowContainerTransaction()
        if (shouldFreeformTaskLaunchSwitchToFullscreen(task)) {
            logD("Bring desktop tasks to front on transition=taskId=%d", task.taskId)
            if (taskRepository.isActiveTask(task.taskId) && !forceEnterDesktop(task.displayId)) {
                // We are outside of desktop mode and already existing desktop task is being
                // launched. We should make this task go to fullscreen instead of freeform. Note
                // that this means any re-launch of a freeform window outside of desktop will be in
                // fullscreen as long as default-desktop flag is disabled.
                val runOnTransitStart =
                    addMoveToFullscreenChanges(
                        wct = wct,
                        taskInfo = task,
                        willExitDesktop =
                            willExitDesktop(
                                triggerTaskId = task.taskId,
                                displayId = task.displayId,
                                forceExitDesktop = true,
                            ),
                    )
                runOnTransitStart?.invoke(transition)
                return wct
            }
            val runOnTransitStart = addDeskActivationChanges(deskId, wct, task)
            runOnTransitStart?.invoke(transition)
            wct.reorder(task.token, true)
            return wct
        }
        val inheritedTaskBounds =
            getInheritedExistingTaskBounds(taskRepository, shellTaskOrganizer, task, deskId)
        if (!taskRepository.isActiveTask(task.taskId) && inheritedTaskBounds != null) {
            // Inherit bounds from closing task instance to prevent application jumping different
            // cascading positions.
            wct.setBounds(task.token, inheritedTaskBounds)
        }
        // TODO(b/365723620): Handle non running tasks that were launched after reboot.
        // If task is already visible, it must have been handled already and added to desktop mode.
        // Cascade task only if it's not visible yet and has no inherited bounds.
        if (
            inheritedTaskBounds == null &&
                DesktopModeFlags.ENABLE_CASCADING_WINDOWS.isTrue() &&
                !taskRepository.isVisibleTask(task.taskId)
        ) {
            val displayLayout = displayController.getDisplayLayout(task.displayId)
            if (displayLayout != null) {
                val initialBounds = Rect(task.configuration.windowConfiguration.bounds)
                cascadeWindow(initialBounds, displayLayout, task.displayId)
                wct.setBounds(task.token, initialBounds)
            }
        }
        if (useDesktopOverrideDensity()) {
            wct.setDensityDpi(task.token, DESKTOP_DENSITY_OVERRIDE)
        }
        // The task that is launching might have been minimized before - in which case this is an
        // unminimize action.
        if (taskRepository.isMinimizedTask(task.taskId)) {
            addPendingUnminimizeTransition(
                transition,
                task.displayId,
                task.taskId,
                UnminimizeReason.TASK_LAUNCH,
            )
        }
        // Desktop Mode is showing and we're launching a new Task:
        // 1) Exit immersive if needed.
        desktopImmersiveController.exitImmersiveIfApplicable(
            transition = transition,
            wct = wct,
            displayId = task.displayId,
            reason = DesktopImmersiveController.ExitReason.TASK_LAUNCH,
        )
        // 2) minimize a Task if needed.
        val taskIdToMinimize = addAndGetMinimizeChanges(deskId, wct, task.taskId)
        addPendingAppLaunchTransition(transition, task.taskId, taskIdToMinimize)
        if (taskIdToMinimize != null) {
            addPendingMinimizeTransition(transition, taskIdToMinimize, MinimizeReason.TASK_LIMIT)
            return wct
        }
        if (!wct.isEmpty) {
            snapEventHandler.removeTaskIfTiled(task.displayId, task.taskId)
            return wct
        }
        return null
    }

    private fun handleFullscreenTaskLaunch(
        task: RunningTaskInfo,
        transition: IBinder,
    ): WindowContainerTransaction? {
        logV("handleFullscreenTaskLaunch")
        if (shouldFullscreenTaskLaunchSwitchToDesktop(task)) {
            logD("Switch fullscreen task to freeform on transition: taskId=%d", task.taskId)
            return WindowContainerTransaction().also { wct ->
                val deskId = getDefaultDeskId(task.displayId)
                addMoveToDeskTaskChanges(wct = wct, task = task, deskId = deskId)
                val runOnTransitStart: RunOnTransitStart? =
                    if (
                        task.baseIntent.flags.and(Intent.FLAG_ACTIVITY_TASK_ON_HOME) != 0 ||
                            !isDesktopModeShowing(task.displayId)
                    ) {
                        // In some launches home task is moved behind new task being launched. Make
                        // sure that's not the case for launches in desktop. Also, if this launch is
                        // the first one to trigger the desktop mode (e.g., when
                        // [forceEnterDesktop()]), activate the desk here.
                        val activationRunnable =
                            addDeskActivationChanges(
                                deskId = deskId,
                                wct = wct,
                                newTask = task,
                                addPendingLaunchTransition = true,
                            )
                        wct.reorder(task.token, true)
                        activationRunnable
                    } else {
                        { transition: IBinder ->
                            // The desk was already showing and we're launching a new Task - we
                            // might need to minimize another Task.
                            val taskIdToMinimize =
                                addAndGetMinimizeChanges(deskId, wct, task.taskId)
                            taskIdToMinimize?.let { minimizingTaskId ->
                                addPendingMinimizeTransition(
                                    transition,
                                    minimizingTaskId,
                                    MinimizeReason.TASK_LIMIT,
                                )
                            }
                            // Also track the pending launching task.
                            addPendingAppLaunchTransition(transition, task.taskId, taskIdToMinimize)
                        }
                    }
                runOnTransitStart?.invoke(transition)
                desktopImmersiveController.exitImmersiveIfApplicable(
                    transition,
                    wct,
                    task.displayId,
                    reason = DesktopImmersiveController.ExitReason.TASK_LAUNCH,
                )
            }
        } else if (taskRepository.isActiveTask(task.taskId)) {
            // If a freeform task receives a request for a fullscreen launch, apply the same
            // changes we do for similar transitions. The task not having WINDOWING_MODE_UNDEFINED
            // set when needed can interfere with future split / multi-instance transitions.
            val wct = WindowContainerTransaction()
            val runOnTransitStart =
                addMoveToFullscreenChanges(
                    wct = wct,
                    taskInfo = task,
                    willExitDesktop =
                        willExitDesktop(
                            triggerTaskId = task.taskId,
                            displayId = task.displayId,
                            forceExitDesktop = true,
                        ),
                )
            runOnTransitStart?.invoke(transition)
            return wct
        }
        return null
    }

    private fun shouldFreeformTaskLaunchSwitchToFullscreen(task: RunningTaskInfo): Boolean =
        !isDesktopModeShowing(task.displayId)

    private fun shouldFullscreenTaskLaunchSwitchToDesktop(task: RunningTaskInfo): Boolean =
        isDesktopModeShowing(task.displayId) || forceEnterDesktop(task.displayId)

    /**
     * If a task is not compatible with desktop mode freeform, it should always be launched in
     * fullscreen.
     */
    private fun handleIncompatibleTaskLaunch(
        task: RunningTaskInfo,
        transition: IBinder,
    ): WindowContainerTransaction? {
        logV("handleIncompatibleTaskLaunch")
        if (!isDesktopModeShowing(task.displayId) && !forceEnterDesktop(task.displayId)) return null
        // Only update task repository for transparent task.
        if (
            DesktopModeFlags.INCLUDE_TOP_TRANSPARENT_FULLSCREEN_TASK_IN_DESKTOP_HEURISTIC
                .isTrue() && desktopModeCompatPolicy.isTransparentTask(task)
        ) {
            taskRepository.setTopTransparentFullscreenTaskId(task.displayId, task.taskId)
        }
        // Already fullscreen, no-op.
        if (task.isFullscreen) return null
        val wct = WindowContainerTransaction()
        val runOnTransitStart =
            addMoveToFullscreenChanges(
                wct = wct,
                taskInfo = task,
                willExitDesktop =
                    willExitDesktop(
                        triggerTaskId = task.taskId,
                        displayId = task.displayId,
                        forceExitDesktop = true,
                    ),
            )
        runOnTransitStart?.invoke(transition)
        return wct
    }

    /**
     * Handle task closing by removing wallpaper activity if it's the last active task.
     *
     * TODO: b/394268248 - desk needs to be deactivated.
     */
    private fun handleTaskClosing(
        task: RunningTaskInfo,
        transition: IBinder,
        requestType: Int,
    ): WindowContainerTransaction? {
        logV("handleTaskClosing")
        if (!isDesktopModeShowing(task.displayId)) return null
        val deskId = taskRepository.getDeskIdForTask(task.taskId)
        if (deskId == null && DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
            return null
        }

        val wct = WindowContainerTransaction()
        val deactivationRunnable =
            performDesktopExitCleanupIfNeeded(
                taskId = task.taskId,
                deskId = deskId,
                displayId = task.displayId,
                wct = wct,
                forceToFullscreen = false,
            )
        deactivationRunnable?.invoke(transition)

        if (!DesktopModeFlags.ENABLE_DESKTOP_WINDOWING_BACK_NAVIGATION.isTrue()) {
            taskRepository.addClosingTask(
                displayId = task.displayId,
                deskId = deskId,
                taskId = task.taskId,
            )
            snapEventHandler.removeTaskIfTiled(task.displayId, task.taskId)
        }

        taskbarDesktopTaskListener?.onTaskbarCornerRoundingUpdate(
            doesAnyTaskRequireTaskbarRounding(task.displayId, task.taskId)
        )
        return if (wct.isEmpty) null else wct
    }

    /**
     * Applies the [wct] changes need when a task is first moving to a desk and the desk needs to be
     * activated.
     */
    private fun addDeskActivationWithMovingTaskChanges(
        deskId: Int,
        wct: WindowContainerTransaction,
        task: RunningTaskInfo,
    ): RunOnTransitStart? {
        val runOnTransitStart = addDeskActivationChanges(deskId, wct, task)
        addMoveToDeskTaskChanges(wct = wct, task = task, deskId = deskId)
        return runOnTransitStart
    }

    /**
     * Applies the [wct] changes needed when a task is first moving to a desk.
     *
     * Note that this recalculates the initial bounds of the task, so it should not be used when
     * transferring a task between desks.
     *
     * TODO: b/362720497 - this should be improved to be reusable by desk-to-desk CUJs where
     *   [DesksOrganizer.moveTaskToDesk] needs to be called and even cross-display CUJs where
     *   [applyFreeformDisplayChange] needs to be called. Potentially by comparing source vs
     *   destination desk ids and display ids, or adding extra arguments to the function.
     */
    fun addMoveToDeskTaskChanges(
        wct: WindowContainerTransaction,
        task: RunningTaskInfo,
        deskId: Int,
    ) {
        val targetDisplayId = taskRepository.getDisplayForDesk(deskId)
        val displayLayout = displayController.getDisplayLayout(targetDisplayId) ?: return
        val inheritedTaskBounds =
            getInheritedExistingTaskBounds(taskRepository, shellTaskOrganizer, task, deskId)
        if (inheritedTaskBounds != null) {
            // Inherit bounds from closing task instance to prevent application jumping different
            // cascading positions.
            wct.setBounds(task.token, inheritedTaskBounds)
        } else {
            val initialBounds = getInitialBounds(displayLayout, task, targetDisplayId)
            if (canChangeTaskPosition(task)) {
                wct.setBounds(task.token, initialBounds)
            }
        }
        if (DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
            desksOrganizer.moveTaskToDesk(wct = wct, deskId = deskId, task = task)
        } else {
            val tdaInfo = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(targetDisplayId)!!
            val tdaWindowingMode = tdaInfo.configuration.windowConfiguration.windowingMode
            val targetWindowingMode =
                if (tdaWindowingMode == WINDOWING_MODE_FREEFORM) {
                    // Display windowing is freeform, set to undefined and inherit it
                    WINDOWING_MODE_UNDEFINED
                } else {
                    WINDOWING_MODE_FREEFORM
                }
            wct.setWindowingMode(task.token, targetWindowingMode)
            wct.reorder(task.token, /* onTop= */ true)
        }
        if (useDesktopOverrideDensity()) {
            wct.setDensityDpi(task.token, DESKTOP_DENSITY_OVERRIDE)
        }
    }

    /**
     * Apply changes to move a freeform task from one display to another, which includes handling
     * density changes between displays.
     */
    private fun applyFreeformDisplayChange(
        wct: WindowContainerTransaction,
        taskInfo: RunningTaskInfo,
        destDisplayId: Int,
    ) {
        val sourceLayout = displayController.getDisplayLayout(taskInfo.displayId) ?: return
        val destLayout = displayController.getDisplayLayout(destDisplayId) ?: return
        val bounds = taskInfo.configuration.windowConfiguration.bounds
        val scaledWidth = bounds.width() * destLayout.densityDpi() / sourceLayout.densityDpi()
        val scaledHeight = bounds.height() * destLayout.densityDpi() / sourceLayout.densityDpi()
        val sourceWidthMargin = sourceLayout.width() - bounds.width()
        val sourceHeightMargin = sourceLayout.height() - bounds.height()
        val destWidthMargin = destLayout.width() - scaledWidth
        val destHeightMargin = destLayout.height() - scaledHeight
        val scaledLeft =
            if (sourceWidthMargin != 0) {
                bounds.left * destWidthMargin / sourceWidthMargin
            } else {
                destWidthMargin / 2
            }
        val scaledTop =
            if (sourceHeightMargin != 0) {
                bounds.top * destHeightMargin / sourceHeightMargin
            } else {
                destHeightMargin / 2
            }
        val boundsWithinDisplay =
            if (destWidthMargin >= 0 && destHeightMargin >= 0) {
                Rect(0, 0, scaledWidth, scaledHeight).apply {
                    offsetTo(
                        scaledLeft.coerceIn(0, destWidthMargin),
                        scaledTop.coerceIn(0, destHeightMargin),
                    )
                }
            } else {
                getInitialBounds(destLayout, taskInfo, destDisplayId)
            }
        wct.setBounds(taskInfo.token, boundsWithinDisplay)
    }

    private fun getInitialBounds(
        displayLayout: DisplayLayout,
        taskInfo: RunningTaskInfo,
        displayId: Int,
    ): Rect {
        val bounds =
            if (ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS.isTrue) {
                // If caption insets should be excluded from app bounds, ensure caption insets
                // are excluded from the ideal initial bounds when scaling non-resizeable apps.
                // Caption insets stay fixed and don't scale with bounds.
                val captionInsets =
                    if (desktopModeCompatPolicy.shouldExcludeCaptionFromAppBounds(taskInfo)) {
                        getDesktopViewAppHeaderHeightPx(context)
                    } else {
                        0
                    }
                calculateInitialBounds(displayLayout, taskInfo, captionInsets = captionInsets)
            } else {
                calculateDefaultDesktopTaskBounds(displayLayout)
            }

        if (DesktopModeFlags.ENABLE_CASCADING_WINDOWS.isTrue) {
            cascadeWindow(bounds, displayLayout, displayId)
        }
        return bounds
    }

    /**
     * Applies the changes needed to enter fullscreen and returns the id of the desk that needs to
     * be deactivated.
     */
    private fun addMoveToFullscreenChanges(
        wct: WindowContainerTransaction,
        taskInfo: RunningTaskInfo,
        willExitDesktop: Boolean,
    ): RunOnTransitStart? {
        val tdaInfo = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(taskInfo.displayId)!!
        val tdaWindowingMode = tdaInfo.configuration.windowConfiguration.windowingMode
        val targetWindowingMode =
            if (tdaWindowingMode == WINDOWING_MODE_FULLSCREEN) {
                // Display windowing is fullscreen, set to undefined and inherit it
                WINDOWING_MODE_UNDEFINED
            } else {
                WINDOWING_MODE_FULLSCREEN
            }
        wct.setWindowingMode(taskInfo.token, targetWindowingMode)
        wct.setBounds(taskInfo.token, Rect())
        if (useDesktopOverrideDensity()) {
            wct.setDensityDpi(taskInfo.token, getDefaultDensityDpi())
        }
        if (DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
            wct.reparent(taskInfo.token, tdaInfo.token, /* onTop= */ true)
        }
        val deskId = taskRepository.getDeskIdForTask(taskInfo.taskId)
        return performDesktopExitCleanUp(
            wct = wct,
            deskId = deskId,
            displayId = taskInfo.displayId,
            willExitDesktop = willExitDesktop,
            shouldEndUpAtHome = false,
        )
    }

    private fun cascadeWindow(bounds: Rect, displayLayout: DisplayLayout, displayId: Int) {
        val stableBounds = Rect()
        displayLayout.getStableBoundsForDesktopMode(stableBounds)

        val activeTasks = taskRepository.getExpandedTasksOrdered(displayId)
        activeTasks.firstOrNull()?.let { activeTask ->
            shellTaskOrganizer.getRunningTaskInfo(activeTask)?.let {
                cascadeWindow(
                    context.resources,
                    stableBounds,
                    it.configuration.windowConfiguration.bounds,
                    bounds,
                )
            }
        }
    }

    /**
     * Adds split screen changes to a transaction. Note that bounds are not reset here due to
     * animation; see {@link onDesktopSplitSelectAnimComplete}
     */
    private fun addMoveToSplitChanges(
        wct: WindowContainerTransaction,
        taskInfo: RunningTaskInfo,
        deskId: Int?,
    ): RunOnTransitStart? {
        if (!DesktopModeFlags.ENABLE_INPUT_LAYER_TRANSITION_FIX.isTrue) {
            // This windowing mode is to get the transition animation started; once we complete
            // split select, we will change windowing mode to undefined and inherit from split
            // stage.
            // Going to undefined here causes task to flicker to the top left.
            // Cancelling the split select flow will revert it to fullscreen.
            wct.setWindowingMode(taskInfo.token, WINDOWING_MODE_MULTI_WINDOW)
        }
        // The task's density may have been overridden in freeform; revert it here as we don't
        // want it overridden in multi-window.
        wct.setDensityDpi(taskInfo.token, getDefaultDensityDpi())

        return performDesktopExitCleanupIfNeeded(
            taskId = taskInfo.taskId,
            displayId = taskInfo.displayId,
            deskId = deskId,
            wct = wct,
            forceToFullscreen = true,
            shouldEndUpAtHome = false,
        )
    }

    /** Returns the ID of the Task that will be minimized, or null if no task will be minimized. */
    private fun addAndGetMinimizeChanges(
        deskId: Int,
        wct: WindowContainerTransaction,
        newTaskId: Int?,
        launchingNewIntent: Boolean = false,
    ): Int? {
        if (!desktopTasksLimiter.isPresent) return null
        require(newTaskId == null || !launchingNewIntent)
        return desktopTasksLimiter
            .get()
            .addAndGetMinimizeTaskChanges(deskId, wct, newTaskId, launchingNewIntent)
    }

    private fun addPendingMinimizeTransition(
        transition: IBinder,
        taskIdToMinimize: Int,
        minimizeReason: MinimizeReason,
    ) {
        val taskToMinimize = shellTaskOrganizer.getRunningTaskInfo(taskIdToMinimize)
        desktopTasksLimiter.ifPresent {
            it.addPendingMinimizeChange(
                transition = transition,
                displayId = taskToMinimize?.displayId ?: DEFAULT_DISPLAY,
                taskId = taskIdToMinimize,
                minimizeReason = minimizeReason,
            )
        }
    }

    private fun addPendingUnminimizeTransition(
        transition: IBinder,
        displayId: Int,
        taskIdToUnminimize: Int,
        unminimizeReason: UnminimizeReason,
    ) {
        desktopTasksLimiter.ifPresent {
            it.addPendingUnminimizeChange(
                transition,
                displayId = displayId,
                taskId = taskIdToUnminimize,
                unminimizeReason,
            )
        }
    }

    private fun addPendingAppLaunchTransition(
        transition: IBinder,
        launchTaskId: Int,
        minimizeTaskId: Int?,
    ) {
        if (!DesktopModeFlags.ENABLE_DESKTOP_APP_LAUNCH_TRANSITIONS_BUGFIX.isTrue) {
            return
        }
        // TODO b/359523924: pass immersive task here?
        desktopMixedTransitionHandler.addPendingMixedTransition(
            DesktopMixedTransitionHandler.PendingMixedTransition.Launch(
                transition,
                launchTaskId,
                minimizeTaskId,
                /* exitingImmersiveTask= */ null,
            )
        )
    }

    private fun activateDefaultDeskInDisplay(
        displayId: Int,
        remoteTransition: RemoteTransition? = null,
    ) {
        val deskId = getDefaultDeskId(displayId)
        activateDesk(deskId, remoteTransition)
    }

    /**
     * Applies the necessary [wct] changes to activate the given desk.
     *
     * When a task is being brought into a desk together with the activation, then [newTask] is not
     * null and may be used to run other desktop policies, such as minimizing another task if the
     * task limit has been exceeded.
     */
    private fun addDeskActivationChanges(
        deskId: Int,
        wct: WindowContainerTransaction,
        newTask: TaskInfo? = null,
        // TODO: b/362720497 - should this be true in other places? Can it be calculated locally
        //  without having to specify the value?
        addPendingLaunchTransition: Boolean = false,
    ): RunOnTransitStart? {
        logV("addDeskActivationChanges newTaskId=%d deskId=%d", newTask?.taskId, deskId)
        val newTaskIdInFront = newTask?.taskId
        val displayId = taskRepository.getDisplayForDesk(deskId)
        if (!DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
            val taskIdToMinimize = bringDesktopAppsToFront(displayId, wct, newTask?.taskId)
            return { transition ->
                taskIdToMinimize?.let { minimizingTaskId ->
                    addPendingMinimizeTransition(
                        transition = transition,
                        taskIdToMinimize = minimizingTaskId,
                        minimizeReason = MinimizeReason.TASK_LIMIT,
                    )
                }
                if (newTask != null && addPendingLaunchTransition) {
                    addPendingAppLaunchTransition(transition, newTask.taskId, taskIdToMinimize)
                }
            }
        }
        prepareForDeskActivation(displayId, wct)
        desksOrganizer.activateDesk(wct, deskId)
        taskbarDesktopTaskListener?.onTaskbarCornerRoundingUpdate(
            doesAnyTaskRequireTaskbarRounding(displayId)
        )
        val expandedTasksOrderedFrontToBack =
            taskRepository.getExpandedTasksIdsInDeskOrdered(deskId = deskId)
        // If we're adding a new Task we might need to minimize an old one
        val taskIdToMinimize =
            desktopTasksLimiter
                .getOrNull()
                ?.getTaskIdToMinimize(expandedTasksOrderedFrontToBack, newTaskIdInFront)
        if (taskIdToMinimize != null) {
            val taskToMinimize = shellTaskOrganizer.getRunningTaskInfo(taskIdToMinimize)
            // TODO(b/365725441): Handle non running task minimization
            if (taskToMinimize != null) {
                desksOrganizer.minimizeTask(wct, deskId, taskToMinimize)
            }
        }
        if (DesktopModeFlags.ENABLE_DESKTOP_WINDOWING_PERSISTENCE.isTrue) {
            expandedTasksOrderedFrontToBack
                .filter { taskId -> taskId != taskIdToMinimize }
                .reversed()
                .forEach { taskId ->
                    val runningTaskInfo = shellTaskOrganizer.getRunningTaskInfo(taskId)
                    if (runningTaskInfo == null) {
                        wct.startTask(taskId, createActivityOptionsForStartTask().toBundle())
                    } else {
                        desksOrganizer.reorderTaskToFront(wct, deskId, runningTaskInfo)
                    }
                }
        }
        val deactivatingDesk = taskRepository.getActiveDeskId(displayId)?.takeIf { it != deskId }
        val deactivationRunnable = prepareDeskDeactivationIfNeeded(wct, deactivatingDesk)
        return { transition ->
            val activateDeskTransition =
                if (newTaskIdInFront != null) {
                    DeskTransition.ActiveDeskWithTask(
                        token = transition,
                        displayId = displayId,
                        deskId = deskId,
                        enterTaskId = newTaskIdInFront,
                    )
                } else {
                    DeskTransition.ActivateDesk(
                        token = transition,
                        displayId = displayId,
                        deskId = deskId,
                    )
                }
            desksTransitionObserver.addPendingTransition(activateDeskTransition)
            taskIdToMinimize?.let { minimizingTask ->
                addPendingMinimizeTransition(transition, minimizingTask, MinimizeReason.TASK_LIMIT)
            }
            deactivationRunnable?.invoke(transition)
        }
    }

    /** Activates the given desk. */
    fun activateDesk(deskId: Int, remoteTransition: RemoteTransition? = null) {
        val wct = WindowContainerTransaction()
        val runOnTransitStart = addDeskActivationChanges(deskId, wct)

        val transitionType = transitionType(remoteTransition)
        val handler =
            remoteTransition?.let {
                OneShotRemoteHandler(transitions.mainExecutor, remoteTransition)
            }

        val transition = transitions.startTransition(transitionType, wct, handler)
        handler?.setTransition(transition)
        runOnTransitStart?.invoke(transition)

        desktopModeEnterExitTransitionListener?.onEnterDesktopModeTransitionStarted(
            FREEFORM_ANIMATION_DURATION
        )
    }

    /**
     * TODO: b/393978539 - Deactivation should not happen in desktop-first devices when going home.
     */
    private fun prepareDeskDeactivationIfNeeded(
        wct: WindowContainerTransaction,
        deskId: Int?,
    ): RunOnTransitStart? {
        if (!DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) return null
        if (deskId == null) return null
        desksOrganizer.deactivateDesk(wct, deskId)
        return { transition ->
            desksTransitionObserver.addPendingTransition(
                DeskTransition.DeactivateDesk(token = transition, deskId = deskId)
            )
        }
    }

    /** Removes the default desk in the given display. */
    @Deprecated("Deprecated with multi-desks.", ReplaceWith("removeDesk()"))
    fun removeDefaultDeskInDisplay(displayId: Int) {
        val deskId = getDefaultDeskId(displayId)
        removeDesk(displayId = displayId, deskId = deskId)
    }

    private fun getDefaultDeskId(displayId: Int) =
        checkNotNull(taskRepository.getDefaultDeskId(displayId)) {
            "Expected a default desk to exist in display: $displayId"
        }

    /** Removes the given desk. */
    fun removeDesk(deskId: Int) {
        val displayId = taskRepository.getDisplayForDesk(deskId)
        removeDesk(displayId = displayId, deskId = deskId)
    }

    /** Removes all the available desks on all displays. */
    fun removeAllDesks() {
        taskRepository.getAllDeskIds().forEach { deskId -> removeDesk(deskId) }
    }

    private fun removeDesk(displayId: Int, deskId: Int) {
        if (!DesktopModeFlags.ENABLE_DESKTOP_WINDOWING_BACK_NAVIGATION.isTrue()) return
        logV("removeDesk deskId=%d from displayId=%d", deskId, displayId)

        val tasksToRemove =
            if (DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
                taskRepository.getActiveTaskIdsInDesk(deskId)
            } else {
                // TODO: 362720497 - make sure minimized windows are also removed in WM
                //  and the repository.
                taskRepository.removeDesk(deskId)
            }

        val wct = WindowContainerTransaction()
        tasksToRemove.forEach {
            // TODO: b/404595635 - consider moving this block into [DesksOrganizer].
            val task = shellTaskOrganizer.getRunningTaskInfo(it)
            if (task != null) {
                wct.removeTask(task.token)
            } else {
                recentTasksController?.removeBackgroundTask(it)
            }
        }
        if (DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
            desksOrganizer.removeDesk(wct, deskId, userId)
        }
        if (!DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue && wct.isEmpty) return
        val transition = transitions.startTransition(TRANSIT_CLOSE, wct, /* handler= */ null)
        if (DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
            desksTransitionObserver.addPendingTransition(
                DeskTransition.RemoveDesk(
                    token = transition,
                    displayId = displayId,
                    deskId = deskId,
                    tasks = tasksToRemove,
                    onDeskRemovedListener = onDeskRemovedListener,
                )
            )
        }
    }

    /** Enter split by using the focused desktop task in given `displayId`. */
    fun enterSplit(displayId: Int, leftOrTop: Boolean) {
        getFocusedFreeformTask(displayId)?.let { requestSplit(it, leftOrTop) }
    }

    private fun getFocusedFreeformTask(displayId: Int): RunningTaskInfo? =
        shellTaskOrganizer.getRunningTasks(displayId).find { taskInfo ->
            taskInfo.isFocused && taskInfo.windowingMode == WINDOWING_MODE_FREEFORM
        }

    /**
     * Requests a task be transitioned from desktop to split select. Applies needed windowing
     * changes if this transition is enabled.
     */
    @JvmOverloads
    fun requestSplit(taskInfo: RunningTaskInfo, leftOrTop: Boolean = false) {
        // If a drag to desktop is in progress, we want to enter split select
        // even if the requesting task is already in split.
        val isDragging = dragToDesktopTransitionHandler.inProgress
        val shouldRequestSplit = taskInfo.isFullscreen || taskInfo.isFreeform || isDragging
        if (shouldRequestSplit) {
            if (isDragging) {
                releaseVisualIndicator()
                val cancelState =
                    if (leftOrTop) {
                        DragToDesktopTransitionHandler.CancelState.CANCEL_SPLIT_LEFT
                    } else {
                        DragToDesktopTransitionHandler.CancelState.CANCEL_SPLIT_RIGHT
                    }
                dragToDesktopTransitionHandler.cancelDragToDesktopTransition(cancelState)
            } else {
                val deskId = taskRepository.getDeskIdForTask(taskInfo.taskId)
                logV("Split requested for task=%d in desk=%d", taskInfo.taskId, deskId)
                val wct = WindowContainerTransaction()
                val runOnTransitStart = addMoveToSplitChanges(wct, taskInfo, deskId)
                val transition =
                    splitScreenController.requestEnterSplitSelect(
                        taskInfo,
                        wct,
                        if (leftOrTop) SPLIT_POSITION_TOP_OR_LEFT
                        else SPLIT_POSITION_BOTTOM_OR_RIGHT,
                        taskInfo.configuration.windowConfiguration.bounds,
                    )
                if (transition != null) {
                    runOnTransitStart?.invoke(transition)
                }
            }
        }
    }

    /** Requests a task be transitioned from whatever mode it's in to a bubble. */
    @JvmOverloads
    fun requestFloat(taskInfo: RunningTaskInfo, left: Boolean? = null) {
        val isDragging = dragToDesktopTransitionHandler.inProgress
        val shouldRequestFloat =
            taskInfo.isFullscreen || taskInfo.isFreeform || isDragging || taskInfo.isMultiWindow
        if (!shouldRequestFloat) return
        if (isDragging) {
            releaseVisualIndicator()
            val cancelState =
                if (left == true) DragToDesktopTransitionHandler.CancelState.CANCEL_BUBBLE_LEFT
                else DragToDesktopTransitionHandler.CancelState.CANCEL_BUBBLE_RIGHT
            dragToDesktopTransitionHandler.cancelDragToDesktopTransition(cancelState)
        } else {
            bubbleController.ifPresent {
                it.expandStackAndSelectBubble(taskInfo, /* dragData= */ null)
            }
        }
    }

    private fun getDefaultDensityDpi(): Int = context.resources.displayMetrics.densityDpi

    /** Creates a new instance of the external interface to pass to another process. */
    private fun createExternalInterface(): ExternalInterfaceBinder = IDesktopModeImpl(this)

    /** Get connection interface between sysui and shell */
    fun asDesktopMode(): DesktopMode {
        return desktopMode
    }

    /**
     * Perform checks required on drag move. Create/release fullscreen indicator as needed.
     * Different sources for x and y coordinates are used due to different needs for each: We want
     * split transitions to be based on input coordinates but fullscreen transition to be based on
     * task edge coordinate.
     *
     * @param taskInfo the task being dragged.
     * @param taskSurface SurfaceControl of dragged task.
     * @param inputX x coordinate of input. Used for checks against left/right edge of screen.
     * @param taskBounds bounds of dragged task. Used for checks against status bar height.
     */
    fun onDragPositioningMove(
        taskInfo: RunningTaskInfo,
        taskSurface: SurfaceControl,
        inputX: Float,
        taskBounds: Rect,
    ) {
        if (taskInfo.windowingMode != WINDOWING_MODE_FREEFORM) return
        snapEventHandler.removeTaskIfTiled(taskInfo.displayId, taskInfo.taskId)
        updateVisualIndicator(
            taskInfo,
            taskSurface,
            inputX,
            taskBounds.top.toFloat(),
            DragStartState.FROM_FREEFORM,
        )
    }

    fun updateVisualIndicator(
        taskInfo: RunningTaskInfo,
        taskSurface: SurfaceControl?,
        inputX: Float,
        taskTop: Float,
        dragStartState: DragStartState,
    ): DesktopModeVisualIndicator.IndicatorType {
        // If the visual indicator has the wrong start state, it was never cleared from a previous
        // drag event and needs to be cleared
        if (visualIndicator != null && visualIndicator?.dragStartState != dragStartState) {
            Slog.e(TAG, "Visual indicator from previous motion event was never released")
            releaseVisualIndicator()
        }
        // If the visual indicator does not exist, create it.
        val indicator =
            visualIndicator
                ?: DesktopModeVisualIndicator(
                    desktopExecutor,
                    mainExecutor,
                    syncQueue,
                    taskInfo,
                    displayController,
                    if (Flags.enableBugFixesForSecondaryDisplay()) {
                        displayController.getDisplayContext(taskInfo.displayId)
                    } else {
                        context
                    },
                    taskSurface,
                    rootTaskDisplayAreaOrganizer,
                    dragStartState,
                    bubbleController.getOrNull()?.bubbleDropTargetBoundsProvider,
                    snapEventHandler,
                )
        if (visualIndicator == null) visualIndicator = indicator
        return indicator.updateIndicatorType(PointF(inputX, taskTop))
    }

    /**
     * Perform checks required on drag end. If indicator indicates a windowing mode change, perform
     * that change. Otherwise, ensure bounds are up to date.
     *
     * @param taskInfo the task being dragged.
     * @param taskSurface the leash of the task being dragged.
     * @param inputCoordinate the coordinates of the motion event
     * @param currentDragBounds the current bounds of where the visible task is (might be actual
     *   task bounds or just task leash)
     * @param validDragArea the bounds of where the task can be dragged within the display.
     * @param dragStartBounds the bounds of the task before starting dragging.
     */
    fun onDragPositioningEnd(
        taskInfo: RunningTaskInfo,
        taskSurface: SurfaceControl,
        inputCoordinate: PointF,
        currentDragBounds: Rect,
        validDragArea: Rect,
        dragStartBounds: Rect,
        motionEvent: MotionEvent,
    ) {
        if (taskInfo.configuration.windowConfiguration.windowingMode != WINDOWING_MODE_FREEFORM) {
            return
        }

        val indicator = getVisualIndicator() ?: return
        val indicatorType =
            indicator.updateIndicatorType(
                PointF(inputCoordinate.x, currentDragBounds.top.toFloat())
            )
        when (indicatorType) {
            IndicatorType.TO_FULLSCREEN_INDICATOR -> {
                if (DesktopModeStatus.shouldMaximizeWhenDragToTopEdge(context)) {
                    dragToMaximizeDesktopTask(taskInfo, taskSurface, currentDragBounds, motionEvent)
                } else {
                    desktopModeUiEventLogger.log(
                        taskInfo,
                        DesktopUiEventEnum.DESKTOP_WINDOW_APP_HEADER_DRAG_TO_FULL_SCREEN,
                    )
                    moveToFullscreenWithAnimation(
                        taskInfo,
                        Point(currentDragBounds.left, currentDragBounds.top),
                        DesktopModeTransitionSource.TASK_DRAG,
                    )
                }
            }
            IndicatorType.TO_SPLIT_LEFT_INDICATOR -> {
                desktopModeUiEventLogger.log(
                    taskInfo,
                    DesktopUiEventEnum.DESKTOP_WINDOW_APP_HEADER_DRAG_TO_TILE_TO_LEFT,
                )
                handleSnapResizingTaskOnDrag(
                    taskInfo,
                    SnapPosition.LEFT,
                    taskSurface,
                    currentDragBounds,
                    dragStartBounds,
                    motionEvent,
                )
            }
            IndicatorType.TO_SPLIT_RIGHT_INDICATOR -> {
                desktopModeUiEventLogger.log(
                    taskInfo,
                    DesktopUiEventEnum.DESKTOP_WINDOW_APP_HEADER_DRAG_TO_TILE_TO_RIGHT,
                )
                handleSnapResizingTaskOnDrag(
                    taskInfo,
                    SnapPosition.RIGHT,
                    taskSurface,
                    currentDragBounds,
                    dragStartBounds,
                    motionEvent,
                )
            }
            IndicatorType.NO_INDICATOR,
            IndicatorType.TO_BUBBLE_LEFT_INDICATOR,
            IndicatorType.TO_BUBBLE_RIGHT_INDICATOR -> {
                // TODO(b/391928049): add support fof dragging desktop apps to a bubble

                // Create a copy so that we can animate from the current bounds if we end up having
                // to snap the surface back without a WCT change.
                val destinationBounds = Rect(currentDragBounds)
                // If task bounds are outside valid drag area, snap them inward
                DragPositioningCallbackUtility.snapTaskBoundsIfNecessary(
                    destinationBounds,
                    validDragArea,
                )

                if (destinationBounds == dragStartBounds) {
                    // There's no actual difference between the start and end bounds, so while a
                    // WCT change isn't needed, the dragged surface still needs to be snapped back
                    // to its original location.
                    releaseVisualIndicator()
                    returnToDragStartAnimator.start(
                        taskInfo.taskId,
                        taskSurface,
                        startBounds = currentDragBounds,
                        endBounds = dragStartBounds,
                    )
                    return
                }

                // Update task bounds so that the task position will match the position of its leash
                val wct = WindowContainerTransaction()
                wct.setBounds(taskInfo.token, destinationBounds)

                val newDisplayId = motionEvent.getDisplayId()
                val displayAreaInfo = rootTaskDisplayAreaOrganizer.getDisplayAreaInfo(newDisplayId)
                val isCrossDisplayDrag =
                    Flags.enableConnectedDisplaysWindowDrag() &&
                        newDisplayId != taskInfo.getDisplayId() &&
                        displayAreaInfo != null
                val handler =
                    if (isCrossDisplayDrag) {
                        dragToDisplayTransitionHandler
                    } else {
                        null
                    }
                if (isCrossDisplayDrag) {
                    // TODO: b/362720497 - reparent to a specific desk within the target display.
                    wct.reparent(taskInfo.token, displayAreaInfo.token, /* onTop= */ true)
                }

                transitions.startTransition(TRANSIT_CHANGE, wct, handler)

                releaseVisualIndicator()
            }
            IndicatorType.TO_DESKTOP_INDICATOR -> {
                throw IllegalArgumentException(
                    "Should not be receiving TO_DESKTOP_INDICATOR for " + "a freeform task."
                )
            }
        }
        // A freeform drag-move ended, remove the indicator immediately.
        releaseVisualIndicator()
        taskbarDesktopTaskListener?.onTaskbarCornerRoundingUpdate(
            doesAnyTaskRequireTaskbarRounding(taskInfo.displayId)
        )
    }

    /**
     * Cancel the drag-to-desktop transition.
     *
     * @param taskInfo the task being dragged.
     */
    fun onDragPositioningCancelThroughStatusBar(taskInfo: RunningTaskInfo) {
        interactionJankMonitor.cancel(CUJ_DESKTOP_MODE_ENTER_APP_HANDLE_DRAG_HOLD)
        cancelDragToDesktop(taskInfo)
    }

    /**
     * Perform checks required when drag ends under status bar area.
     *
     * @param taskInfo the task being dragged.
     * @param y height of drag, to be checked against status bar height.
     * @return the [IndicatorType] used for the resulting transition
     */
    fun onDragPositioningEndThroughStatusBar(
        inputCoordinates: PointF,
        taskInfo: RunningTaskInfo,
        taskSurface: SurfaceControl,
    ): IndicatorType {
        // End the drag_hold CUJ interaction.
        interactionJankMonitor.end(CUJ_DESKTOP_MODE_ENTER_APP_HANDLE_DRAG_HOLD)
        val indicator = getVisualIndicator() ?: return IndicatorType.NO_INDICATOR
        val indicatorType = indicator.updateIndicatorType(inputCoordinates)
        when (indicatorType) {
            IndicatorType.TO_DESKTOP_INDICATOR -> {
                LatencyTracker.getInstance(context)
                    .onActionStart(LatencyTracker.ACTION_DESKTOP_MODE_ENTER_APP_HANDLE_DRAG)
                // Start a new jank interaction for the drag release to desktop window animation.
                interactionJankMonitor.begin(
                    taskSurface,
                    context,
                    handler,
                    CUJ_DESKTOP_MODE_ENTER_APP_HANDLE_DRAG_RELEASE,
                    "to_desktop",
                )
                desktopModeUiEventLogger.log(
                    taskInfo,
                    DesktopUiEventEnum.DESKTOP_WINDOW_APP_HANDLE_DRAG_TO_DESKTOP_MODE,
                )
                finalizeDragToDesktop(taskInfo)
            }
            IndicatorType.NO_INDICATOR,
            IndicatorType.TO_FULLSCREEN_INDICATOR -> {
                desktopModeUiEventLogger.log(
                    taskInfo,
                    DesktopUiEventEnum.DESKTOP_WINDOW_APP_HANDLE_DRAG_TO_FULL_SCREEN,
                )
                cancelDragToDesktop(taskInfo)
            }
            IndicatorType.TO_SPLIT_LEFT_INDICATOR -> {
                desktopModeUiEventLogger.log(
                    taskInfo,
                    DesktopUiEventEnum.DESKTOP_WINDOW_APP_HANDLE_DRAG_TO_SPLIT_SCREEN,
                )
                requestSplit(taskInfo, leftOrTop = true)
            }
            IndicatorType.TO_SPLIT_RIGHT_INDICATOR -> {
                desktopModeUiEventLogger.log(
                    taskInfo,
                    DesktopUiEventEnum.DESKTOP_WINDOW_APP_HANDLE_DRAG_TO_SPLIT_SCREEN,
                )
                requestSplit(taskInfo, leftOrTop = false)
            }
            IndicatorType.TO_BUBBLE_LEFT_INDICATOR -> {
                requestFloat(taskInfo, left = true)
            }
            IndicatorType.TO_BUBBLE_RIGHT_INDICATOR -> {
                requestFloat(taskInfo, left = false)
            }
        }
        return indicatorType
    }

    /** Update the exclusion region for a specified task */
    fun onExclusionRegionChanged(taskId: Int, exclusionRegion: Region) {
        taskRepository.updateTaskExclusionRegions(taskId, exclusionRegion)
    }

    /** Remove a previously tracked exclusion region for a specified task. */
    fun removeExclusionRegionForTask(taskId: Int) {
        taskRepository.removeExclusionRegion(taskId)
    }

    /**
     * Adds a listener to find out about changes in the visibility of freeform tasks.
     *
     * @param listener the listener to add.
     * @param callbackExecutor the executor to call the listener on.
     */
    fun addVisibleTasksListener(listener: VisibleTasksListener, callbackExecutor: Executor) {
        taskRepository.addVisibleTasksListener(listener, callbackExecutor)
    }

    /**
     * Adds a listener to track changes to desktop task gesture exclusion regions
     *
     * @param listener the listener to add.
     * @param callbackExecutor the executor to call the listener on.
     */
    fun setTaskRegionListener(listener: Consumer<Region>, callbackExecutor: Executor) {
        taskRepository.setExclusionRegionListener(listener, callbackExecutor)
    }

    // TODO(b/358114479): Move this implementation into a separate class.
    override fun onUnhandledDrag(
        launchIntent: PendingIntent,
        @UserIdInt userId: Int,
        dragEvent: DragEvent,
        onFinishCallback: Consumer<Boolean>,
    ): Boolean {
        // TODO(b/320797628): Pass through which display we are dropping onto
        if (!isDesktopModeShowing(DEFAULT_DISPLAY)) {
            // Not currently in desktop mode, ignore the drop
            return false
        }

        // TODO:
        val launchComponent = getComponent(launchIntent)
        if (!multiInstanceHelper.supportsMultiInstanceSplit(launchComponent, userId)) {
            // TODO(b/320797628): Should only return early if there is an existing running task, and
            //                    notify the user as well. But for now, just ignore the drop.
            logV("Dropped intent does not support multi-instance")
            return false
        }
        val taskInfo = getFocusedFreeformTask(DEFAULT_DISPLAY) ?: return false
        // TODO(b/358114479): Update drag and drop handling to give us visibility into when another
        //  window will accept a drag event. This way, we can hide the indicator when we won't
        //  be handling the transition here, allowing us to display the indicator accurately.
        //  For now, we create the indicator only on drag end and immediately dispose it.
        val indicatorType =
            updateVisualIndicator(
                taskInfo,
                dragEvent.dragSurface,
                dragEvent.x,
                dragEvent.y,
                DragStartState.DRAGGED_INTENT,
            )
        releaseVisualIndicator()
        val windowingMode =
            when (indicatorType) {
                IndicatorType.TO_FULLSCREEN_INDICATOR -> {
                    WINDOWING_MODE_FULLSCREEN
                }
                IndicatorType.TO_SPLIT_LEFT_INDICATOR,
                IndicatorType.TO_SPLIT_RIGHT_INDICATOR,
                IndicatorType.TO_DESKTOP_INDICATOR -> {
                    WINDOWING_MODE_FREEFORM
                }
                else -> error("Invalid indicator type: $indicatorType")
            }
        val displayLayout = displayController.getDisplayLayout(DEFAULT_DISPLAY) ?: return false
        val newWindowBounds = Rect()
        when (indicatorType) {
            IndicatorType.TO_DESKTOP_INDICATOR -> {
                // Use default bounds, but with the top-center at the drop point.
                newWindowBounds.set(calculateDefaultDesktopTaskBounds(displayLayout))
                newWindowBounds.offsetTo(
                    dragEvent.x.toInt() - (newWindowBounds.width() / 2),
                    dragEvent.y.toInt(),
                )
            }
            IndicatorType.TO_SPLIT_RIGHT_INDICATOR -> {
                newWindowBounds.set(getSnapBounds(taskInfo, SnapPosition.RIGHT))
            }
            IndicatorType.TO_SPLIT_LEFT_INDICATOR -> {
                newWindowBounds.set(getSnapBounds(taskInfo, SnapPosition.LEFT))
            }
            else -> {
                // Use empty bounds for the fullscreen case.
            }
        }
        // Start a new transition to launch the app
        val opts =
            ActivityOptions.makeBasic().apply {
                launchWindowingMode = windowingMode
                launchBounds = newWindowBounds
                pendingIntentBackgroundActivityStartMode =
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                pendingIntentLaunchFlags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                splashScreenStyle = SPLASH_SCREEN_STYLE_ICON
            }
        if (windowingMode == WINDOWING_MODE_FULLSCREEN) {
            dragAndDropFullscreenCookie = Binder()
            opts.launchCookie = dragAndDropFullscreenCookie
        }
        val wct = WindowContainerTransaction()
        wct.sendPendingIntent(launchIntent, null, opts.toBundle())
        if (windowingMode == WINDOWING_MODE_FREEFORM) {
            if (DesktopModeFlags.ENABLE_DESKTOP_TAB_TEARING_MINIMIZE_ANIMATION_BUGFIX.isTrue()) {
                // TODO b/376389593: Use a custom tab tearing transition/animation
                val deskId = getDefaultDeskId(DEFAULT_DISPLAY)
                startLaunchTransition(
                    TRANSIT_OPEN,
                    wct,
                    launchingTaskId = null,
                    deskId = deskId,
                    displayId = DEFAULT_DISPLAY,
                )
            } else {
                desktopModeDragAndDropTransitionHandler.handleDropEvent(wct)
            }
        } else {
            transitions.startTransition(TRANSIT_OPEN, wct, null)
        }

        // Report that this is handled by the listener
        onFinishCallback.accept(true)

        // We've assumed responsibility of cleaning up the drag surface, so do that now
        // TODO(b/320797628): Do an actual animation here for the drag surface
        val t = SurfaceControl.Transaction()
        t.remove(dragEvent.dragSurface)
        t.apply()
        return true
    }

    // TODO(b/366397912): Support full multi-user mode in Windowing.
    override fun onUserChanged(newUserId: Int, userContext: Context) {
        logV("onUserChanged previousUserId=%d, newUserId=%d", userId, newUserId)
        updateCurrentUser(newUserId)
    }

    private fun updateCurrentUser(newUserId: Int) {
        userId = newUserId
        taskRepository = userRepositories.getProfile(userId)
        if (this::snapEventHandler.isInitialized) {
            snapEventHandler.onUserChange()
        }
    }

    /** Called when a task's info changes. */
    fun onTaskInfoChanged(taskInfo: RunningTaskInfo) {
        if (!DesktopModeFlags.ENABLE_FULLY_IMMERSIVE_IN_DESKTOP.isTrue) return
        val inImmersive = taskRepository.isTaskInFullImmersiveState(taskInfo.taskId)
        val requestingImmersive = taskInfo.requestingImmersive
        if (
            inImmersive &&
                !requestingImmersive &&
                !RecentsTransitionStateListener.isRunning(recentsTransitionState)
        ) {
            // Exit immersive if the app is no longer requesting it.
            desktopImmersiveController.moveTaskToNonImmersive(
                taskInfo,
                DesktopImmersiveController.ExitReason.APP_NOT_IMMERSIVE,
            )
        }
    }

    private fun createActivityOptionsForStartTask(): ActivityOptions {
        return ActivityOptions.makeBasic().apply {
            launchWindowingMode = WINDOWING_MODE_FREEFORM
            splashScreenStyle = SPLASH_SCREEN_STYLE_ICON
        }
    }

    private fun dump(pw: PrintWriter, prefix: String) {
        val innerPrefix = "$prefix  "
        pw.println("${prefix}DesktopTasksController")
        DesktopModeStatus.dump(pw, innerPrefix, context)
        userRepositories.dump(pw, innerPrefix)
        focusTransitionObserver.dump(pw, innerPrefix)
    }

    /** The interface for calls from outside the shell, within the host process. */
    @ExternalThread
    private inner class DesktopModeImpl : DesktopMode {
        override fun addVisibleTasksListener(
            listener: VisibleTasksListener,
            callbackExecutor: Executor,
        ) {
            mainExecutor.execute {
                this@DesktopTasksController.addVisibleTasksListener(listener, callbackExecutor)
            }
        }

        override fun addDesktopGestureExclusionRegionListener(
            listener: Consumer<Region>,
            callbackExecutor: Executor,
        ) {
            mainExecutor.execute {
                this@DesktopTasksController.setTaskRegionListener(listener, callbackExecutor)
            }
        }

        override fun moveFocusedTaskToDesktop(
            displayId: Int,
            transitionSource: DesktopModeTransitionSource,
        ) {
            logV("moveFocusedTaskToDesktop")
            mainExecutor.execute {
                this@DesktopTasksController.moveFocusedTaskToDesktop(displayId, transitionSource)
            }
        }

        override fun moveFocusedTaskToFullscreen(
            displayId: Int,
            transitionSource: DesktopModeTransitionSource,
        ) {
            logV("moveFocusedTaskToFullscreen")
            mainExecutor.execute {
                this@DesktopTasksController.enterFullscreen(displayId, transitionSource)
            }
        }

        override fun moveFocusedTaskToStageSplit(displayId: Int, leftOrTop: Boolean) {
            logV("moveFocusedTaskToStageSplit")
            mainExecutor.execute { this@DesktopTasksController.enterSplit(displayId, leftOrTop) }
        }
    }

    /** The interface for calls from outside the host process. */
    @BinderThread
    private class IDesktopModeImpl(private var controller: DesktopTasksController?) :
        IDesktopMode.Stub(), ExternalInterfaceBinder {

        private lateinit var remoteListener:
            SingleInstanceRemoteListener<DesktopTasksController, IDesktopTaskListener>

        private val deskChangeListener: DeskChangeListener =
            object : DeskChangeListener {
                override fun onDeskAdded(displayId: Int, deskId: Int) {
                    ProtoLog.v(
                        WM_SHELL_DESKTOP_MODE,
                        "IDesktopModeImpl: onDeskAdded display=%d deskId=%d",
                        displayId,
                        deskId,
                    )
                    remoteListener.call { l -> l.onDeskAdded(displayId, deskId) }
                }

                override fun onDeskRemoved(displayId: Int, deskId: Int) {
                    ProtoLog.v(
                        WM_SHELL_DESKTOP_MODE,
                        "IDesktopModeImpl: onDeskRemoved display=%d deskId=%d",
                        displayId,
                        deskId,
                    )
                    remoteListener.call { l -> l.onDeskRemoved(displayId, deskId) }
                }

                override fun onActiveDeskChanged(
                    displayId: Int,
                    newActiveDeskId: Int,
                    oldActiveDeskId: Int,
                ) {
                    ProtoLog.v(
                        WM_SHELL_DESKTOP_MODE,
                        "IDesktopModeImpl: onActiveDeskChanged display=%d new=%d old=%d",
                        displayId,
                        newActiveDeskId,
                        oldActiveDeskId,
                    )
                    remoteListener.call { l ->
                        l.onActiveDeskChanged(displayId, newActiveDeskId, oldActiveDeskId)
                    }
                }
            }

        private val visibleTasksListener: VisibleTasksListener =
            object : VisibleTasksListener {
                override fun onTasksVisibilityChanged(displayId: Int, visibleTasksCount: Int) {
                    ProtoLog.v(
                        WM_SHELL_DESKTOP_MODE,
                        "IDesktopModeImpl: onVisibilityChanged display=%d visible=%d",
                        displayId,
                        visibleTasksCount,
                    )
                    remoteListener.call { l ->
                        l.onTasksVisibilityChanged(displayId, visibleTasksCount)
                    }
                }
            }

        private val taskbarDesktopTaskListener: TaskbarDesktopTaskListener =
            object : TaskbarDesktopTaskListener {
                override fun onTaskbarCornerRoundingUpdate(
                    hasTasksRequiringTaskbarRounding: Boolean
                ) {
                    ProtoLog.v(
                        WM_SHELL_DESKTOP_MODE,
                        "IDesktopModeImpl: onTaskbarCornerRoundingUpdate " +
                            "doesAnyTaskRequireTaskbarRounding=%s",
                        hasTasksRequiringTaskbarRounding,
                    )

                    remoteListener.call { l ->
                        l.onTaskbarCornerRoundingUpdate(hasTasksRequiringTaskbarRounding)
                    }
                }
            }

        private val desktopModeEntryExitTransitionListener: DesktopModeEntryExitTransitionListener =
            object : DesktopModeEntryExitTransitionListener {
                override fun onEnterDesktopModeTransitionStarted(transitionDuration: Int) {
                    ProtoLog.v(
                        WM_SHELL_DESKTOP_MODE,
                        "IDesktopModeImpl: onEnterDesktopModeTransitionStarted transitionTime=%s",
                        transitionDuration,
                    )
                    remoteListener.call { l ->
                        l.onEnterDesktopModeTransitionStarted(transitionDuration)
                    }
                }

                override fun onExitDesktopModeTransitionStarted(transitionDuration: Int) {
                    ProtoLog.v(
                        WM_SHELL_DESKTOP_MODE,
                        "IDesktopModeImpl: onExitDesktopModeTransitionStarted transitionTime=%s",
                        transitionDuration,
                    )
                    remoteListener.call { l ->
                        l.onExitDesktopModeTransitionStarted(transitionDuration)
                    }
                }
            }

        init {
            remoteListener =
                SingleInstanceRemoteListener<DesktopTasksController, IDesktopTaskListener>(
                    controller,
                    { c ->
                        run {
                            syncInitialState(c)
                            registerListeners(c)
                        }
                    },
                    { c -> run { unregisterListeners(c) } },
                )
        }

        /** Invalidates this instance, preventing future calls from updating the controller. */
        override fun invalidate() {
            remoteListener.unregister()
            controller = null
        }

        override fun createDesk(displayId: Int) {
            executeRemoteCallWithTaskPermission(controller, "createDesk") { c ->
                c.createDesk(displayId)
            }
        }

        override fun removeDesk(deskId: Int) {
            executeRemoteCallWithTaskPermission(controller, "removeDesk") { c ->
                c.removeDesk(deskId)
            }
        }

        override fun removeAllDesks() {
            executeRemoteCallWithTaskPermission(controller, "removeAllDesks") { c ->
                c.removeAllDesks()
            }
        }

        override fun activateDesk(deskId: Int, remoteTransition: RemoteTransition?) {
            executeRemoteCallWithTaskPermission(controller, "activateDesk") { c ->
                c.activateDesk(deskId, remoteTransition)
            }
        }

        override fun showDesktopApps(displayId: Int, remoteTransition: RemoteTransition?) {
            executeRemoteCallWithTaskPermission(controller, "showDesktopApps") { c ->
                c.showDesktopApps(displayId, remoteTransition)
            }
        }

        override fun showDesktopApp(
            taskId: Int,
            remoteTransition: RemoteTransition?,
            toFrontReason: DesktopTaskToFrontReason,
        ) {
            executeRemoteCallWithTaskPermission(controller, "showDesktopApp") { c ->
                c.moveTaskToFront(taskId, remoteTransition, toFrontReason.toUnminimizeReason())
            }
        }

        override fun stashDesktopApps(displayId: Int) {
            ProtoLog.w(WM_SHELL_DESKTOP_MODE, "IDesktopModeImpl: stashDesktopApps is deprecated")
        }

        override fun hideStashedDesktopApps(displayId: Int) {
            ProtoLog.w(
                WM_SHELL_DESKTOP_MODE,
                "IDesktopModeImpl: hideStashedDesktopApps is deprecated",
            )
        }

        override fun onDesktopSplitSelectAnimComplete(taskInfo: RunningTaskInfo) {
            executeRemoteCallWithTaskPermission(controller, "onDesktopSplitSelectAnimComplete") { c
                ->
                c.onDesktopSplitSelectAnimComplete(taskInfo)
            }
        }

        override fun setTaskListener(listener: IDesktopTaskListener?) {
            ProtoLog.v(WM_SHELL_DESKTOP_MODE, "IDesktopModeImpl: set task listener=%s", listener)
            executeRemoteCallWithTaskPermission(controller, "setTaskListener") { _ ->
                listener?.let { remoteListener.register(it) } ?: remoteListener.unregister()
            }
        }

        override fun moveToDesktop(
            taskId: Int,
            transitionSource: DesktopModeTransitionSource,
            remoteTransition: RemoteTransition?,
            callback: IMoveToDesktopCallback?,
        ) {
            executeRemoteCallWithTaskPermission(controller, "moveTaskToDesktop") { c ->
                c.moveTaskToDefaultDeskAndActivate(
                    taskId,
                    transitionSource = transitionSource,
                    remoteTransition = remoteTransition,
                    callback = callback,
                )
            }
        }

        override fun removeDefaultDeskInDisplay(displayId: Int) {
            executeRemoteCallWithTaskPermission(controller, "removeDefaultDeskInDisplay") { c ->
                c.removeDefaultDeskInDisplay(displayId)
            }
        }

        override fun moveToExternalDisplay(taskId: Int) {
            executeRemoteCallWithTaskPermission(controller, "moveTaskToExternalDisplay") { c ->
                c.moveToNextDisplay(taskId)
            }
        }

        override fun startLaunchIntentTransition(intent: Intent, options: Bundle, displayId: Int) {
            executeRemoteCallWithTaskPermission(controller, "startLaunchIntentTransition") { c ->
                c.startLaunchIntentTransition(intent, options, displayId)
            }
        }

        private fun syncInitialState(c: DesktopTasksController) {
            remoteListener.call { l ->
                // TODO: b/393962589 - implement desks limit.
                val canCreateDesks = true
                l.onListenerConnected(
                    c.taskRepository.getDeskDisplayStateForRemote(),
                    canCreateDesks,
                )
            }
        }

        private fun registerListeners(c: DesktopTasksController) {
            c.taskRepository.addDeskChangeListener(deskChangeListener, c.mainExecutor)
            c.taskRepository.addVisibleTasksListener(visibleTasksListener, c.mainExecutor)
            c.taskbarDesktopTaskListener = taskbarDesktopTaskListener
            c.desktopModeEnterExitTransitionListener = desktopModeEntryExitTransitionListener
        }

        private fun unregisterListeners(c: DesktopTasksController) {
            c.taskRepository.removeDeskChangeListener(deskChangeListener)
            c.taskRepository.removeVisibleTasksListener(visibleTasksListener)
            c.taskbarDesktopTaskListener = null
            c.desktopModeEnterExitTransitionListener = null
        }
    }

    private fun logV(msg: String, vararg arguments: Any?) {
        ProtoLog.v(WM_SHELL_DESKTOP_MODE, "%s: $msg", TAG, *arguments)
    }

    private fun logD(msg: String, vararg arguments: Any?) {
        ProtoLog.d(WM_SHELL_DESKTOP_MODE, "%s: $msg", TAG, *arguments)
    }

    private fun logW(msg: String, vararg arguments: Any?) {
        ProtoLog.w(WM_SHELL_DESKTOP_MODE, "%s: $msg", TAG, *arguments)
    }

    companion object {
        @JvmField
        val DESKTOP_MODE_INITIAL_BOUNDS_SCALE =
            SystemProperties.getInt("persist.wm.debug.desktop_mode_initial_bounds_scale", 75) / 100f

        // Timeout used for CUJ_DESKTOP_MODE_ENTER_APP_HANDLE_DRAG_HOLD, this is longer than the
        // default timeout to avoid timing out in the middle of a drag action.
        private val APP_HANDLE_DRAG_HOLD_CUJ_TIMEOUT_MS: Long = TimeUnit.SECONDS.toMillis(10L)

        private const val TAG = "DesktopTasksController"

        private fun DesktopTaskToFrontReason.toUnminimizeReason(): UnminimizeReason =
            when (this) {
                DesktopTaskToFrontReason.UNKNOWN -> UnminimizeReason.UNKNOWN
                DesktopTaskToFrontReason.TASKBAR_TAP -> UnminimizeReason.TASKBAR_TAP
                DesktopTaskToFrontReason.ALT_TAB -> UnminimizeReason.ALT_TAB
                DesktopTaskToFrontReason.TASKBAR_MANAGE_WINDOW ->
                    UnminimizeReason.TASKBAR_MANAGE_WINDOW
            }

        @JvmField
        /**
         * A placeholder for a synthetic transition that isn't backed by a true system transition.
         */
        val SYNTHETIC_TRANSITION: IBinder = Binder()
    }

    /** Defines interface for classes that can listen to changes for task resize. */
    // TODO(b/343931111): Migrate to using TransitionObservers when ready
    interface TaskbarDesktopTaskListener {
        /**
         * [hasTasksRequiringTaskbarRounding] is true when a task is either maximized or snapped
         * left/right and rounded corners are enabled.
         */
        fun onTaskbarCornerRoundingUpdate(hasTasksRequiringTaskbarRounding: Boolean)
    }

    /** Defines interface for entering and exiting desktop windowing mode. */
    interface DesktopModeEntryExitTransitionListener {
        /** [transitionDuration] time it takes to run enter desktop mode transition */
        fun onEnterDesktopModeTransitionStarted(transitionDuration: Int)

        /** [transitionDuration] time it takes to run exit desktop mode transition */
        fun onExitDesktopModeTransitionStarted(transitionDuration: Int)
    }

    /** The positions on a screen that a task can snap to. */
    enum class SnapPosition {
        RIGHT,
        LEFT,
    }
}
