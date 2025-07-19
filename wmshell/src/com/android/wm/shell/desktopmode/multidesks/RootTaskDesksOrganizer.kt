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
package com.android.wm.shell.desktopmode.multidesks

import android.annotation.SuppressLint
import android.app.ActivityManager.RunningTaskInfo
import android.app.ActivityTaskManager.INVALID_TASK_ID
import android.app.WindowConfiguration.ACTIVITY_TYPE_STANDARD
import android.app.WindowConfiguration.ACTIVITY_TYPE_UNDEFINED
import android.app.WindowConfiguration.WINDOWING_MODE_FREEFORM
import android.app.WindowConfiguration.WINDOWING_MODE_UNDEFINED
import android.util.SparseArray
import android.view.SurfaceControl
import android.view.WindowManager.TRANSIT_TO_FRONT
import android.window.DesktopExperienceFlags
import android.window.TransitionInfo
import android.window.WindowContainerToken
import android.window.WindowContainerTransaction
import androidx.core.util.forEach
import androidx.core.util.valueIterator
import com.android.internal.annotations.VisibleForTesting
import com.android.internal.protolog.ProtoLog
import com.android.wm.shell.ShellTaskOrganizer
import com.android.wm.shell.common.LaunchAdjacentController
import com.android.wm.shell.desktopmode.multidesks.DesksOrganizer.OnCreateCallback
import com.android.wm.shell.protolog.ShellProtoLogGroup.WM_SHELL_DESKTOP_MODE
import com.android.wm.shell.sysui.ShellCommandHandler
import com.android.wm.shell.sysui.ShellInit
import java.io.PrintWriter

/**
 * A [DesksOrganizer] that uses root tasks as the container of each desk.
 *
 * Note that root tasks are reusable between multiple users at the same time, and may also be
 * pre-created to have one ready for the first entry to the default desk, so root-task existence
 * does not imply a formal desk exists to the user.
 */
class RootTaskDesksOrganizer(
    shellInit: ShellInit,
    shellCommandHandler: ShellCommandHandler,
    private val shellTaskOrganizer: ShellTaskOrganizer,
    private val launchAdjacentController: LaunchAdjacentController,
) : DesksOrganizer, ShellTaskOrganizer.TaskListener {

    private val createDeskRootRequests = mutableListOf<CreateDeskRequest>()
    @VisibleForTesting val deskRootsByDeskId = SparseArray<DeskRoot>()
    private val createDeskMinimizationRootRequests =
        mutableListOf<CreateDeskMinimizationRootRequest>()
    @VisibleForTesting
    val deskMinimizationRootsByDeskId: MutableMap<Int, DeskMinimizationRoot> = mutableMapOf()
    private var onTaskInfoChangedListener: ((RunningTaskInfo) -> Unit)? = null

    init {
        if (DesktopExperienceFlags.ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue) {
            shellInit.addInitCallback(
                { shellCommandHandler.addDumpCallback(this::dump, this) },
                this,
            )
        }
    }

    override fun createDesk(displayId: Int, userId: Int, callback: OnCreateCallback) {
        logV("createDesk in displayId=%d userId=%s", displayId, userId)
        // Find an existing desk that is not yet used by this user.
        val unassignedDesk =
            deskRootsByDeskId
                .valueIterator()
                .asSequence()
                .filterNot { desk -> userId in desk.users }
                .firstOrNull()
        if (unassignedDesk != null) {
            unassignedDesk.users.add(userId)
            callback.onCreated(unassignedDesk.deskId)
            return
        }
        createDeskRoot(displayId, userId, callback)
    }

    private fun createDeskRoot(displayId: Int, userId: Int, callback: OnCreateCallback) {
        logV("createDeskRoot in display: %d for user: %d", displayId, userId)
        createDeskRootRequests += CreateDeskRequest(displayId, userId, callback)
        shellTaskOrganizer.createRootTask(
            displayId,
            WINDOWING_MODE_FREEFORM,
            /* listener = */ this,
            /* removeWithTaskOrganizer = */ true,
        )
    }

    override fun removeDesk(wct: WindowContainerTransaction, deskId: Int, userId: Int) {
        logV("removeDesk %d for userId=%d", deskId, userId)
        val deskRoot = deskRootsByDeskId[deskId]
        if (deskRoot == null) {
            logW("removeDesk attempted to remove non-existent desk=%d", deskId)
            return
        }
        updateLaunchRoot(wct, deskId, enabled = false)
        deskRoot.users.remove(userId)
        if (deskRoot.users.isEmpty()) {
            // No longer in use by any users, remove it completely.
            logD("removeDesk %d is no longer used by any users, removing it completely", deskId)
            wct.removeRootTask(deskRoot.token)
            deskMinimizationRootsByDeskId[deskId]?.let { root -> wct.removeRootTask(root.token) }
        }
    }

    override fun activateDesk(wct: WindowContainerTransaction, deskId: Int) {
        logV("activateDesk %d", deskId)
        val root = checkNotNull(deskRootsByDeskId[deskId]) { "Root not found for desk: $deskId" }
        wct.reorder(root.token, /* onTop= */ true)
        updateLaunchRoot(wct, deskId, enabled = true)
    }

    override fun deactivateDesk(wct: WindowContainerTransaction, deskId: Int) {
        logV("deactivateDesk %d", deskId)
        updateLaunchRoot(wct, deskId, enabled = false)
    }

    private fun updateLaunchRoot(wct: WindowContainerTransaction, deskId: Int, enabled: Boolean) {
        val root = checkNotNull(deskRootsByDeskId[deskId]) { "Root not found for desk: $deskId" }
        root.isLaunchRootRequested = enabled
        logD("updateLaunchRoot deskId=%d enabled=%b", deskId, enabled)
        if (enabled) {
            wct.setLaunchRoot(
                /* container= */ root.taskInfo.token,
                /* windowingModes= */ intArrayOf(WINDOWING_MODE_FREEFORM, WINDOWING_MODE_UNDEFINED),
                /* activityTypes= */ intArrayOf(ACTIVITY_TYPE_UNDEFINED, ACTIVITY_TYPE_STANDARD),
            )
        } else {
            wct.setLaunchRoot(
                /* container= */ root.taskInfo.token,
                /* windowingModes= */ null,
                /* activityTypes= */ null,
            )
        }
    }

    override fun moveTaskToDesk(
        wct: WindowContainerTransaction,
        deskId: Int,
        task: RunningTaskInfo,
    ) {
        val root = deskRootsByDeskId[deskId] ?: error("Root not found for desk: $deskId")
        wct.setWindowingMode(task.token, WINDOWING_MODE_UNDEFINED)
        wct.reparent(task.token, root.taskInfo.token, /* onTop= */ true)
    }

    override fun reorderTaskToFront(
        wct: WindowContainerTransaction,
        deskId: Int,
        task: RunningTaskInfo,
    ) {
        logV("reorderTaskToFront task=${task.taskId} desk=$deskId")
        val root = deskRootsByDeskId[deskId] ?: error("Root not found for desk: $deskId")
        if (task.taskId in root.children) {
            wct.reorder(task.token, /* onTop= */ true, /* includingParents= */ true)
            return
        }
        val minimizationRoot =
            checkNotNull(deskMinimizationRootsByDeskId[deskId]) {
                "Minimization root not found for desk: $deskId"
            }
        if (task.taskId in minimizationRoot.children) {
            unminimizeTask(wct, deskId, task)
            wct.reorder(task.token, /* onTop= */ true, /* includingParents= */ true)
            return
        }
        logE("Attempted to reorder task=${task.taskId} in desk=$deskId but it was not a child")
    }

    override fun minimizeTask(wct: WindowContainerTransaction, deskId: Int, task: RunningTaskInfo) {
        logV("minimizeTask task=${task.taskId} desk=$deskId")
        val deskRoot =
            checkNotNull(deskRootsByDeskId[deskId]) { "Root not found for desk: $deskId" }
        val minimizationRoot =
            checkNotNull(deskMinimizationRootsByDeskId[deskId]) {
                "Minimization root not found for desk: $deskId"
            }
        val taskId = task.taskId
        if (taskId in minimizationRoot.children) {
            logV("Task #$taskId is already minimized in desk #$deskId")
            return
        }
        if (taskId !in deskRoot.children) {
            logE("Attempted to minimize task=${task.taskId} in desk=$deskId but it was not a child")
            return
        }
        wct.reparent(task.token, minimizationRoot.token, /* onTop= */ true)
    }

    override fun unminimizeTask(
        wct: WindowContainerTransaction,
        deskId: Int,
        task: RunningTaskInfo,
    ) {
        val taskId = task.taskId
        logV("unminimizeTask task=$taskId desk=$deskId")
        val deskRoot =
            checkNotNull(deskRootsByDeskId[deskId]) { "Root not found for desk: $deskId" }
        val minimizationRoot =
            checkNotNull(deskMinimizationRootsByDeskId[deskId]) {
                "Minimization root not found for desk: $deskId"
            }
        if (taskId in deskRoot.children) {
            logV("Task #$taskId is already unminimized in desk=$deskId")
            return
        }
        if (taskId !in minimizationRoot.children) {
            logE("Attempted to unminimize task=$taskId in desk=$deskId but it was not a child")
            return
        }
        wct.reparent(task.token, deskRoot.token, /* onTop= */ true)
    }

    override fun isDeskChange(change: TransitionInfo.Change, deskId: Int): Boolean =
        (isDeskRootChange(change) && change.taskId == deskId) ||
            (getDeskMinimizationRootInChange(change)?.deskId == deskId)

    override fun isDeskChange(change: TransitionInfo.Change): Boolean =
        isDeskRootChange(change) || getDeskMinimizationRootInChange(change) != null

    private fun isDeskRootChange(change: TransitionInfo.Change): Boolean =
        change.taskId in deskRootsByDeskId

    private fun getDeskMinimizationRootInChange(
        change: TransitionInfo.Change
    ): DeskMinimizationRoot? =
        deskMinimizationRootsByDeskId.values.find { it.rootId == change.taskId }

    private val TransitionInfo.Change.taskId: Int
        get() = taskInfo?.taskId ?: INVALID_TASK_ID

    override fun getDeskAtEnd(change: TransitionInfo.Change): Int? {
        val parentTaskId = change.taskInfo?.parentTaskId ?: return null
        if (parentTaskId in deskRootsByDeskId) {
            return parentTaskId
        }
        val deskMinimizationRoot =
            deskMinimizationRootsByDeskId.values.find { root -> root.rootId == parentTaskId }
                ?: return null
        return deskMinimizationRoot.deskId
    }

    override fun isDeskActiveAtEnd(change: TransitionInfo.Change, deskId: Int): Boolean =
        change.taskInfo?.taskId == deskId &&
            change.taskInfo?.isVisibleRequested == true &&
            change.mode == TRANSIT_TO_FRONT

    override fun setOnDesktopTaskInfoChangedListener(listener: (RunningTaskInfo) -> Unit) {
        onTaskInfoChangedListener = listener
    }

    override fun onTaskAppeared(taskInfo: RunningTaskInfo, leash: SurfaceControl) {
        handleTaskAppeared(taskInfo, leash)
        updateLaunchAdjacentController()
    }

    override fun onTaskInfoChanged(taskInfo: RunningTaskInfo) {
        handleTaskInfoChanged(taskInfo)
        if (
            taskInfo.taskId !in deskRootsByDeskId &&
                deskMinimizationRootsByDeskId.values.none { it.rootId == taskInfo.taskId }
        ) {
            onTaskInfoChangedListener?.invoke(taskInfo)
        }
        updateLaunchAdjacentController()
    }

    override fun onTaskVanished(taskInfo: RunningTaskInfo) {
        handleTaskVanished(taskInfo)
        updateLaunchAdjacentController()
    }

    private fun handleTaskAppeared(taskInfo: RunningTaskInfo, leash: SurfaceControl) {
        // Check whether this task is appearing inside a desk.
        if (taskInfo.parentTaskId in deskRootsByDeskId) {
            val deskId = taskInfo.parentTaskId
            val taskId = taskInfo.taskId
            logV("Task #$taskId appeared in desk #$deskId")
            addChildToDesk(taskId = taskId, deskId = deskId)
            return
        }
        // Check whether this task is appearing in a minimization root.
        val minimizationRoot =
            deskMinimizationRootsByDeskId.values.singleOrNull { it.rootId == taskInfo.parentTaskId }
        if (minimizationRoot != null) {
            val deskId = minimizationRoot.deskId
            val taskId = taskInfo.taskId
            logV("Task #$taskId was minimized in desk #$deskId ")
            addChildToMinimizationRoot(taskId = taskId, deskId = deskId)
            return
        }
        // The appearing task is a root (either a desk or a minimization root), it should not exist
        // already.
        check(taskInfo.taskId !in deskRootsByDeskId) {
            "A root already exists for desk: ${taskInfo.taskId}"
        }
        check(deskMinimizationRootsByDeskId.values.none { it.rootId == taskInfo.taskId }) {
            "A minimization root already exists with rootId: ${taskInfo.taskId}"
        }

        val appearingInDisplayId = taskInfo.displayId
        // Check if there's any pending desk creation requests under this display.
        val deskRequest =
            createDeskRootRequests.firstOrNull { it.displayId == appearingInDisplayId }
        if (deskRequest != null) {
            // Appearing root matches desk request.
            val deskId = taskInfo.taskId
            logV("Desk #$deskId appeared")
            deskRootsByDeskId[deskId] =
                DeskRoot(
                    deskId = deskId,
                    taskInfo = taskInfo,
                    leash = leash,
                    users = mutableSetOf(deskRequest.userId),
                )
            createDeskRootRequests.remove(deskRequest)
            deskRequest.onCreateCallback.onCreated(deskId)
            createDeskMinimizationRoot(displayId = appearingInDisplayId, deskId = deskId)
            return
        }
        // Check if there's any pending minimization container creation requests under this display.
        val deskMinimizationRootRequest =
            createDeskMinimizationRootRequests.first { it.displayId == appearingInDisplayId }
        val deskId = deskMinimizationRootRequest.deskId
        logV("Minimization container for desk #$deskId appeared with id=${taskInfo.taskId}")
        val deskMinimizationRoot = DeskMinimizationRoot(deskId, taskInfo, leash)
        deskMinimizationRootsByDeskId[deskId] = deskMinimizationRoot
        createDeskMinimizationRootRequests.remove(deskMinimizationRootRequest)
        hideMinimizationRoot(deskMinimizationRoot)
    }

    private fun handleTaskInfoChanged(taskInfo: RunningTaskInfo) {
        if (deskRootsByDeskId.contains(taskInfo.taskId)) {
            val deskId = taskInfo.taskId
            deskRootsByDeskId[deskId] = deskRootsByDeskId[deskId].copy(taskInfo = taskInfo)
            logV("Desk #$deskId's task info changed")
            return
        }
        val minimizationRoot =
            deskMinimizationRootsByDeskId.values.find { root -> root.rootId == taskInfo.taskId }
        if (minimizationRoot != null) {
            deskMinimizationRootsByDeskId.remove(minimizationRoot.deskId)
            deskMinimizationRootsByDeskId[minimizationRoot.deskId] =
                minimizationRoot.copy(taskInfo = taskInfo)
            logV("Minimization root for desk#${minimizationRoot.deskId} task info changed")
            return
        }

        val parentTaskId = taskInfo.parentTaskId
        if (parentTaskId in deskRootsByDeskId) {
            val deskId = taskInfo.parentTaskId
            val taskId = taskInfo.taskId
            logV("onTaskInfoChanged: Task #$taskId appeared in desk #$deskId")
            addChildToDesk(taskId = taskId, deskId = deskId)
            return
        }
        // Check whether this task is appearing in a minimization root.
        val parentMinimizationRoot =
            deskMinimizationRootsByDeskId.values.singleOrNull { it.rootId == parentTaskId }
        if (parentMinimizationRoot != null) {
            val deskId = parentMinimizationRoot.deskId
            val taskId = taskInfo.taskId
            logV("onTaskInfoChanged: Task #$taskId was minimized in desk #$deskId ")
            addChildToMinimizationRoot(taskId = taskId, deskId = deskId)
            return
        }
        logE("onTaskInfoChanged: unknown task: ${taskInfo.taskId}")
    }

    private fun handleTaskVanished(taskInfo: RunningTaskInfo) {
        if (deskRootsByDeskId.contains(taskInfo.taskId)) {
            val deskId = taskInfo.taskId
            val deskRoot = deskRootsByDeskId[deskId]
            // Use the last saved taskInfo to obtain the displayId. Using the local one here will
            // return -1 since the task is not unassociated with a display.
            val displayId = deskRoot.taskInfo.displayId
            logV("Desk #$deskId vanished from display #$displayId")
            deskRootsByDeskId.remove(deskId)
            return
        }
        val deskMinimizationRoot =
            deskMinimizationRootsByDeskId.values.singleOrNull { it.rootId == taskInfo.taskId }
        if (deskMinimizationRoot != null) {
            logV("Minimization root for desk ${deskMinimizationRoot.deskId} vanished")
            deskMinimizationRootsByDeskId.remove(deskMinimizationRoot.deskId)
            return
        }

        // Check whether the vanishing task was a child of any desk.
        // At this point, [parentTaskId] may be unset even if this is a task vanishing from a desk,
        // so search through each root to remove this if it's a child.
        deskRootsByDeskId.forEach { deskId, deskRoot ->
            if (deskRoot.children.remove(taskInfo.taskId)) {
                logV("Task #${taskInfo.taskId} vanished from desk #$deskId")
                return
            }
        }
        // Check whether the vanishing task was a child of the minimized root and remove it.
        deskMinimizationRootsByDeskId.values.forEach { root ->
            val taskId = taskInfo.taskId
            if (root.children.remove(taskId)) {
                logV("Task #$taskId vanished from minimization root of desk #${root.deskId}")
                return
            }
        }
    }

    private fun createDeskMinimizationRoot(displayId: Int, deskId: Int) {
        createDeskMinimizationRootRequests +=
            CreateDeskMinimizationRootRequest(displayId = displayId, deskId = deskId)
        shellTaskOrganizer.createRootTask(
            displayId,
            WINDOWING_MODE_FREEFORM,
            /* listener = */ this,
            /* removeWithTaskOrganizer = */ true,
        )
    }

    @SuppressLint("MissingPermission")
    private fun hideMinimizationRoot(root: DeskMinimizationRoot) {
        shellTaskOrganizer.applyTransaction(
            WindowContainerTransaction().apply { setHidden(root.token, /* hidden= */ true) }
        )
    }

    private fun addChildToDesk(taskId: Int, deskId: Int) {
        deskRootsByDeskId.forEach { _, deskRoot ->
            if (deskRoot.deskId == deskId) {
                deskRoot.children.add(taskId)
            } else {
                deskRoot.children.remove(taskId)
            }
        }
        // A task cannot be in both a desk root and a minimization root at the same time, so make
        // sure to remove them if needed.
        deskMinimizationRootsByDeskId.values.forEach { root -> root.children.remove(taskId) }
    }

    private fun addChildToMinimizationRoot(taskId: Int, deskId: Int) {
        deskMinimizationRootsByDeskId.forEach { _, minimizationRoot ->
            if (minimizationRoot.deskId == deskId) {
                minimizationRoot.children += taskId
            } else {
                minimizationRoot.children -= taskId
            }
        }
        // A task cannot be in both a desk root and a minimization root at the same time, so make
        // sure to remove them if needed.
        deskRootsByDeskId.forEach { _, deskRoot -> deskRoot.children -= taskId }
    }

    private fun updateLaunchAdjacentController() {
        deskRootsByDeskId.forEach { deskId, root ->
            if (root.taskInfo.isVisible) {
                // Disable launch adjacent handling if any desk is active, otherwise the split
                // launch root and the desk root will both be eligible to take launching tasks.
                launchAdjacentController.launchAdjacentEnabled = false
                return
            }
        }
        launchAdjacentController.launchAdjacentEnabled = true
    }

    @VisibleForTesting
    data class DeskRoot(
        val deskId: Int,
        val taskInfo: RunningTaskInfo,
        val leash: SurfaceControl,
        val children: MutableSet<Int> = mutableSetOf(),
        val users: MutableSet<Int> = mutableSetOf(),
        var isLaunchRootRequested: Boolean = false,
    ) {
        val token: WindowContainerToken = taskInfo.token
    }

    @VisibleForTesting
    data class DeskMinimizationRoot(
        val deskId: Int,
        val taskInfo: RunningTaskInfo,
        val leash: SurfaceControl,
        val children: MutableSet<Int> = mutableSetOf(),
    ) {
        val rootId: Int
            get() = taskInfo.taskId

        val token: WindowContainerToken = taskInfo.token
    }

    private data class CreateDeskRequest(
        val displayId: Int,
        val userId: Int,
        val onCreateCallback: OnCreateCallback,
    )

    private data class CreateDeskMinimizationRootRequest(val displayId: Int, val deskId: Int)

    private fun logD(msg: String, vararg arguments: Any?) {
        ProtoLog.d(WM_SHELL_DESKTOP_MODE, "%s: $msg", TAG, *arguments)
    }

    private fun logV(msg: String, vararg arguments: Any?) {
        ProtoLog.v(WM_SHELL_DESKTOP_MODE, "%s: $msg", TAG, *arguments)
    }

    private fun logW(msg: String, vararg arguments: Any?) {
        ProtoLog.w(WM_SHELL_DESKTOP_MODE, "%s: $msg", TAG, *arguments)
    }

    private fun logE(msg: String, vararg arguments: Any?) {
        ProtoLog.e(WM_SHELL_DESKTOP_MODE, "%s: $msg", TAG, *arguments)
    }

    override fun dump(pw: PrintWriter, prefix: String) {
        val innerPrefix = "$prefix  "
        pw.println("$prefix$TAG")
        pw.println(
            "${innerPrefix}launchAdjacentEnabled=" + launchAdjacentController.launchAdjacentEnabled
        )
        pw.println("${innerPrefix}Desk Roots:")
        deskRootsByDeskId.forEach { deskId, root ->
            val minimizationRoot = deskMinimizationRootsByDeskId[deskId]
            pw.println("$innerPrefix  #$deskId visible=${root.taskInfo.isVisible}")
            pw.println("$innerPrefix    displayId=${root.taskInfo.displayId}")
            pw.println("$innerPrefix    isLaunchRootRequested=${root.isLaunchRootRequested}")
            pw.println("$innerPrefix    children=${root.children}")
            pw.println("$innerPrefix    users=${root.users}")
            pw.println("$innerPrefix    minimization root:")
            pw.println("$innerPrefix      rootId=${minimizationRoot?.rootId}")
            if (minimizationRoot != null) {
                pw.println("$innerPrefix      children=${minimizationRoot.children}")
            }
        }
    }

    companion object {
        private const val TAG = "RootTaskDesksOrganizer"
    }
}
