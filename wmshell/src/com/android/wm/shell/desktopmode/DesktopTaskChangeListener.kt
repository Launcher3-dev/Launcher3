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

import android.app.ActivityManager.RunningTaskInfo
import android.app.WindowConfiguration.WINDOWING_MODE_FREEFORM
import android.window.DesktopModeFlags
import com.android.internal.protolog.ProtoLog
import com.android.wm.shell.freeform.TaskChangeListener
import com.android.wm.shell.protolog.ShellProtoLogGroup.WM_SHELL_DESKTOP_MODE

/** Manages tasks handling specific to Android Desktop Mode. */
class DesktopTaskChangeListener(private val desktopUserRepositories: DesktopUserRepositories) :
    TaskChangeListener {

    override fun onTaskOpening(taskInfo: RunningTaskInfo) {
        logD("onTaskOpening for taskId=%d, displayId=%d", taskInfo.taskId, taskInfo.displayId)
        val desktopRepository: DesktopRepository =
            desktopUserRepositories.getProfile(taskInfo.userId)
        if (!isFreeformTask(taskInfo) && desktopRepository.isActiveTask(taskInfo.taskId)) {
            desktopRepository.removeTask(taskInfo.displayId, taskInfo.taskId)
            return
        }
        if (isFreeformTask(taskInfo) && !desktopRepository.isActiveTask(taskInfo.taskId)) {
            desktopRepository.addTask(taskInfo.displayId, taskInfo.taskId, taskInfo.isVisible)
        }
    }

    override fun onTaskChanging(taskInfo: RunningTaskInfo) {
        logD("onTaskChanging for taskId=%d, displayId=%d", taskInfo.taskId, taskInfo.displayId)
        val desktopRepository: DesktopRepository =
            desktopUserRepositories.getProfile(taskInfo.userId)
        // TODO: b/394281403 - with multiple desks, it's possible to have a non-freeform task
        //  inside a desk, so this should be decoupled from windowing mode.
        //  Also, changes in/out of desks are handled by the [DesksTransitionObserver], which has
        //  more specific information about the desk involved in the transition, which might be
        //  more accurate than assuming it's always the default/active desk in the display, as this
        //  method does.
        // Case 1: When the task change is from a task in the desktop repository which is now
        // fullscreen,
        // remove the task from the desktop repository since it is no longer a freeform task.
        if (!isFreeformTask(taskInfo) && desktopRepository.isActiveTask(taskInfo.taskId)) {
            desktopRepository.removeTask(taskInfo.displayId, taskInfo.taskId)
        } else if (isFreeformTask(taskInfo)) {
            // If the task is already active in the repository, then moves task to the front,
            // else adds the task.
            desktopRepository.addTask(taskInfo.displayId, taskInfo.taskId, taskInfo.isVisible)
        }
    }

    // This method should only be used for scenarios where the task info changes are not propagated
    // to
    // [DesktopTaskChangeListener#onTaskChanging] via [TransitionsObserver].
    // Any changes to [DesktopRepository] from this method should be made carefully to minimize risk
    // of race conditions and possible duplications with [onTaskChanging].
    override fun onNonTransitionTaskChanging(taskInfo: RunningTaskInfo) {
        // TODO: b/367268953 - Propagate usages from FreeformTaskListener to this method.
        logD(
            "onNonTransitionTaskChanging for taskId=%d, displayId=%d",
            taskInfo.taskId,
            taskInfo.displayId,
        )
    }

    override fun onTaskMovingToFront(taskInfo: RunningTaskInfo) {
        logD("onTaskMovingToFront for taskId=%d, displayId=%d", taskInfo.taskId, taskInfo.displayId)
        val desktopRepository: DesktopRepository =
            desktopUserRepositories.getProfile(taskInfo.userId)
        // When the task change is from a task in the desktop repository which is now fullscreen,
        // remove the task from the desktop repository since it is no longer a freeform task.
        if (!isFreeformTask(taskInfo) && desktopRepository.isActiveTask(taskInfo.taskId)) {
            desktopRepository.removeTask(taskInfo.displayId, taskInfo.taskId)
        }
        if (isFreeformTask(taskInfo)) {
            // If the task is already active in the repository, then it only moves the task to the
            // front.
            desktopRepository.addTask(taskInfo.displayId, taskInfo.taskId, taskInfo.isVisible)
        }
    }

    override fun onTaskMovingToBack(taskInfo: RunningTaskInfo) {
        val desktopRepository: DesktopRepository =
            desktopUserRepositories.getProfile(taskInfo.userId)
        if (!desktopRepository.isActiveTask(taskInfo.taskId)) return
        logD("onTaskMovingToBack for taskId=%d, displayId=%d", taskInfo.taskId, taskInfo.displayId)
        desktopRepository.updateTask(taskInfo.displayId, taskInfo.taskId, /* isVisible= */ false)
    }

    override fun onTaskClosing(taskInfo: RunningTaskInfo) {
        logD("onTaskClosing for taskId=%d, displayId=%d", taskInfo.taskId, taskInfo.displayId)
        val desktopRepository: DesktopRepository =
            desktopUserRepositories.getProfile(taskInfo.userId)
        if (!desktopRepository.isActiveTask(taskInfo.taskId)) return
        // TODO: b/370038902 - Handle Activity#finishAndRemoveTask.
        if (
            !DesktopModeFlags.ENABLE_DESKTOP_WINDOWING_BACK_NAVIGATION.isTrue ||
                desktopRepository.isClosingTask(taskInfo.taskId)
        ) {
            // A task that's vanishing should be removed:
            // - If it's closed by the X button which means it's marked as a closing task.
            desktopRepository.removeClosingTask(taskInfo.taskId)
            desktopRepository.removeTask(taskInfo.displayId, taskInfo.taskId)
        } else {
            desktopRepository.updateTask(taskInfo.displayId, taskInfo.taskId, isVisible = false)
            desktopRepository.minimizeTask(taskInfo.displayId, taskInfo.taskId)
        }
    }

    private fun isFreeformTask(taskInfo: RunningTaskInfo): Boolean =
        taskInfo.windowingMode == WINDOWING_MODE_FREEFORM

    private fun logD(msg: String, vararg arguments: Any?) {
        ProtoLog.d(WM_SHELL_DESKTOP_MODE, "%s: $msg", TAG, *arguments)
    }

    companion object {
        private const val TAG = "DesktopTaskChangeListener"
    }
}
