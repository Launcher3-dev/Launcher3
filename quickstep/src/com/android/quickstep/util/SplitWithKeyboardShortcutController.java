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

package com.android.quickstep.util;

import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_KEYBOARD_SHORTCUT_SPLIT_LEFT_TOP;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_KEYBOARD_SHORTCUT_SPLIT_RIGHT_BOTTOM;
import static com.android.launcher3.util.Executors.MAIN_EXECUTOR;
import static com.android.launcher3.util.Executors.UI_HELPER_EXECUTOR;
import static com.android.launcher3.util.SplitConfigurationOptions.STAGE_POSITION_BOTTOM_OR_RIGHT;
import static com.android.launcher3.util.SplitConfigurationOptions.STAGE_POSITION_TOP_OR_LEFT;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.graphics.Rect;
import android.graphics.RectF;
import android.window.TransitionInfo;

import androidx.annotation.BinderThread;

import com.android.launcher3.R;
import com.android.launcher3.anim.PendingAnimation;
import com.android.launcher3.taskbar.LauncherTaskbarUIController;
import com.android.launcher3.uioverrides.QuickstepLauncher;
import com.android.quickstep.OverviewComponentObserver;
import com.android.quickstep.RecentsAnimationCallbacks;
import com.android.quickstep.RecentsAnimationController;
import com.android.quickstep.RecentsAnimationTargets;
import com.android.quickstep.RecentsModel;
import com.android.quickstep.SystemUiProxy;
import com.android.quickstep.TopTaskTracker;
import com.android.quickstep.views.FloatingTaskView;
import com.android.quickstep.views.RecentsView;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.system.ActivityManagerWrapper;

/** Transitions app from fullscreen to stage split when triggered from keyboard shortcuts. */
public class SplitWithKeyboardShortcutController {

    private final QuickstepLauncher mLauncher;
    private final SplitSelectStateController mController;
    private final OverviewComponentObserver mOverviewComponentObserver;

    private final int mSplitPlaceholderSize;
    private final int mSplitPlaceholderInset;

    public SplitWithKeyboardShortcutController(
            QuickstepLauncher launcher, SplitSelectStateController controller) {
        mLauncher = launcher;
        mController = controller;
        mOverviewComponentObserver = OverviewComponentObserver.INSTANCE.get(launcher);

        mSplitPlaceholderSize = mLauncher.getResources().getDimensionPixelSize(
                R.dimen.split_placeholder_size);
        mSplitPlaceholderInset = mLauncher.getResources().getDimensionPixelSize(
                R.dimen.split_placeholder_inset);
    }

    @BinderThread
    public void enterStageSplit(boolean leftOrTop) {
        if (TopTaskTracker.INSTANCE.get(mLauncher).getRunningSplitTaskIds().length == 2) {
            // Do not enter stage split from keyboard shortcuts if the user is already in split
            return;
        }
        RecentsAnimationCallbacks callbacks = new RecentsAnimationCallbacks(
                SystemUiProxy.INSTANCE.get(mLauncher.getApplicationContext()));
        SplitWithKeyboardShortcutRecentsAnimationListener listener =
                new SplitWithKeyboardShortcutRecentsAnimationListener(leftOrTop);

        MAIN_EXECUTOR.execute(() -> {
            callbacks.addListener(listener);
            UI_HELPER_EXECUTOR.execute(() -> {
                // Transition from fullscreen app to enter stage split in launcher with
                // recents animation
                final ActivityOptions options = ActivityOptions.makeBasic();
                options.setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS);
                options.setTransientLaunch();
                SystemUiProxy.INSTANCE.get(mLauncher.getApplicationContext())
                        .startRecentsActivity(mOverviewComponentObserver.getOverviewIntent(),
                                ActivityOptions.makeBasic(), callbacks,
                                false /* useSyntheticRecentsTransition */);
            });
        });
    }

    private class SplitWithKeyboardShortcutRecentsAnimationListener implements
            RecentsAnimationCallbacks.RecentsAnimationListener {

        private final boolean mLeftOrTop;
        private final Rect mTempRect = new Rect();
        private final ActivityManager.RunningTaskInfo mRunningTaskInfo;

        private SplitWithKeyboardShortcutRecentsAnimationListener(boolean leftOrTop) {
            mLeftOrTop = leftOrTop;
            mRunningTaskInfo = ActivityManagerWrapper.getInstance().getRunningTask();
        }

        @Override
        public void onRecentsAnimationStart(RecentsAnimationController controller,
                RecentsAnimationTargets targets, TransitionInfo transitionInfo) {
            mController.setInitialTaskSelect(mRunningTaskInfo,
                    mLeftOrTop ? STAGE_POSITION_TOP_OR_LEFT : STAGE_POSITION_BOTTOM_OR_RIGHT,
                    null /* itemInfo */,
                    mLeftOrTop ? LAUNCHER_KEYBOARD_SHORTCUT_SPLIT_LEFT_TOP
                            : LAUNCHER_KEYBOARD_SHORTCUT_SPLIT_RIGHT_BOTTOM);

            RecentsView recentsView = mLauncher.getOverviewPanel();
            recentsView.getPagedOrientationHandler().getInitialSplitPlaceholderBounds(
                    mSplitPlaceholderSize, mSplitPlaceholderInset, mLauncher.getDeviceProfile(),
                    mController.getActiveSplitStagePosition(), mTempRect);

            PendingAnimation anim = new PendingAnimation(
                    SplitAnimationTimings.TABLET_HOME_TO_SPLIT.getDuration());
            RectF startingTaskRect = new RectF();
            final FloatingTaskView floatingTaskView = FloatingTaskView.getFloatingTaskView(
                    mLauncher, mLauncher.getDragLayer(),
                    controller.screenshotTask(mRunningTaskInfo.taskId).getThumbnail(),
                    null /* icon */, startingTaskRect);
            Task task = Task.from(new Task.TaskKey(mRunningTaskInfo), mRunningTaskInfo,
                    false /* isLocked */);
            RecentsModel.INSTANCE.get(mLauncher.getApplicationContext())
                    .getIconCache()
                    .getIconInBackground(
                            task,
                            (icon, contentDescription, title) -> floatingTaskView.setIcon(icon));
            floatingTaskView.setAlpha(1);
            floatingTaskView.addStagingAnimation(anim, startingTaskRect, mTempRect,
                    false /* fadeWithThumbnail */, true /* isStagedTask */);
            mController.setFirstFloatingTaskView(floatingTaskView);

            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    controller.finish(
                            true /* toRecents */,
                            () -> {
                                LauncherTaskbarUIController controller =
                                        mLauncher.getTaskbarUIController();
                                if (controller != null) {
                                    controller.updateTaskbarLauncherStateGoingHome();
                                }

                            },
                            false /* sendUserLeaveHint */);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mLauncher.getDragLayer().removeView(floatingTaskView);
                    mController.getSplitAnimationController()
                            .removeSplitInstructionsView(mLauncher);
                    mController.resetState();
                }
            });
            anim.add(mController.getSplitAnimationController()
                    .getShowSplitInstructionsAnim(mLauncher).buildAnim());
            anim.buildAnim().start();
        }
    };
}
