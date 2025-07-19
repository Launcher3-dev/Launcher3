/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.wm.shell.recents;

import static android.app.WindowConfiguration.ACTIVITY_TYPE_HOME;
import static android.app.WindowConfiguration.ACTIVITY_TYPE_STANDARD;
import static android.app.WindowConfiguration.WINDOWING_MODE_FREEFORM;
import static android.view.WindowManager.TRANSIT_CLOSE;
import static android.view.WindowManager.TRANSIT_OPEN;
import static android.view.WindowManager.TRANSIT_SLEEP;
import static android.view.WindowManager.TRANSIT_TO_FRONT;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession;
import static com.android.window.flags.Flags.FLAG_ENABLE_DESKTOP_RECENTS_TRANSITIONS_CORNERS_BUGFIX;
import static com.android.wm.shell.Flags.FLAG_ENABLE_RECENTS_BOOKEND_TRANSITION;
import static com.android.wm.shell.recents.RecentsTransitionStateListener.TRANSITION_STATE_ANIMATING;
import static com.android.wm.shell.recents.RecentsTransitionStateListener.TRANSITION_STATE_NOT_RUNNING;
import static com.android.wm.shell.recents.RecentsTransitionStateListener.TRANSITION_STATE_REQUESTED;
import static com.android.wm.shell.transition.Transitions.TRANSIT_END_RECENTS_TRANSITION;
import static com.android.wm.shell.transition.Transitions.TRANSIT_START_RECENTS_TRANSITION;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.IApplicationThread;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.platform.test.annotations.EnableFlags;
import android.view.SurfaceControl;
import android.window.TransitionInfo;

import androidx.annotation.NonNull;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.dx.mockito.inline.extended.StaticMockitoSession;
import com.android.internal.os.IResultReceiver;
import com.android.wm.shell.ShellTaskOrganizer;
import com.android.wm.shell.ShellTestCase;
import com.android.wm.shell.TestRunningTaskInfoBuilder;
import com.android.wm.shell.TestShellExecutor;
import com.android.wm.shell.common.DisplayInsetsController;
import com.android.wm.shell.common.TaskStackListenerImpl;
import com.android.wm.shell.desktopmode.DesktopRepository;
import com.android.wm.shell.desktopmode.DesktopUserRepositories;
import com.android.wm.shell.shared.R;
import com.android.wm.shell.shared.desktopmode.DesktopModeStatus;
import com.android.wm.shell.sysui.ShellCommandHandler;
import com.android.wm.shell.sysui.ShellController;
import com.android.wm.shell.sysui.ShellInit;
import com.android.wm.shell.transition.HomeTransitionObserver;
import com.android.wm.shell.transition.TransitionInfoBuilder;
import com.android.wm.shell.transition.Transitions;
import com.android.wm.shell.util.StubTransaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.quality.Strictness;

import java.util.Optional;

/**
 * Tests for {@link RecentTasksController}
 *
 * Usage: atest WMShellUnitTests:RecentsTransitionHandlerTest
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class RecentsTransitionHandlerTest extends ShellTestCase {

    private static final int FREEFORM_TASK_CORNER_RADIUS = 32;

    @Mock
    private Context mContext;
    @Mock
    private Resources mResources;
    @Mock
    private TaskStackListenerImpl mTaskStackListener;
    @Mock
    private ShellCommandHandler mShellCommandHandler;
    @Mock
    private DesktopUserRepositories mDesktopUserRepositories;
    @Mock
    private ActivityTaskManager mActivityTaskManager;
    @Mock
    private DisplayInsetsController mDisplayInsetsController;
    @Mock
    private IRecentTasksListener mRecentTasksListener;
    @Mock
    private TaskStackTransitionObserver mTaskStackTransitionObserver;
    @Mock
    private Transitions mTransitions;

    @Mock private DesktopRepository mDesktopRepository;

    private ShellTaskOrganizer mShellTaskOrganizer;
    private RecentTasksController mRecentTasksController;
    private RecentTasksController mRecentTasksControllerReal;
    private RecentsTransitionHandler mRecentsTransitionHandler;
    private ShellInit mShellInit;
    private ShellController mShellController;
    private TestShellExecutor mMainExecutor;
    private static StaticMockitoSession sMockitoSession;

    @Before
    public void setUp() {
        sMockitoSession = mockitoSession().initMocks(this).strictness(Strictness.LENIENT)
                .mockStatic(DesktopModeStatus.class).startMocking();
        ExtendedMockito.doReturn(true)
                .when(() -> DesktopModeStatus.canEnterDesktopMode(any()));

        when(mDesktopUserRepositories.getCurrent()).thenReturn(mDesktopRepository);
        mMainExecutor = new TestShellExecutor();
        when(mContext.getPackageManager()).thenReturn(mock(PackageManager.class));
        when(mContext.getSystemService(KeyguardManager.class))
                .thenReturn(mock(KeyguardManager.class));
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getDimensionPixelSize(
                R.dimen.desktop_windowing_freeform_rounded_corner_radius)
        ).thenReturn(FREEFORM_TASK_CORNER_RADIUS);
        mShellInit = spy(new ShellInit(mMainExecutor));
        mShellController = spy(new ShellController(mContext, mShellInit, mShellCommandHandler,
                mDisplayInsetsController, mMainExecutor));
        mRecentTasksControllerReal = new RecentTasksController(mContext, mShellInit,
                mShellController, mShellCommandHandler, mTaskStackListener, mActivityTaskManager,
                Optional.of(mDesktopUserRepositories), mTaskStackTransitionObserver,
                mMainExecutor);
        mRecentTasksController = spy(mRecentTasksControllerReal);
        mShellTaskOrganizer = new ShellTaskOrganizer(mShellInit, mShellCommandHandler,
                null /* sizeCompatUI */, Optional.empty(), Optional.of(mRecentTasksController),
                mMainExecutor);

        doReturn(mMainExecutor).when(mTransitions).getMainExecutor();
        mRecentsTransitionHandler = new RecentsTransitionHandler(mShellInit, mShellTaskOrganizer,
                mTransitions, mRecentTasksController, mock(HomeTransitionObserver.class));

        mShellInit.init();
    }

    @After
    public void tearDown() {
        sMockitoSession.finishMocking();
    }

    @Test
    public void testStartSyntheticRecentsTransition_callsOnAnimationStartAndFinishCallback() throws Exception {
        final IRecentsAnimationRunner runner = mock(IRecentsAnimationRunner.class);
        final IResultReceiver finishCallback = mock(IResultReceiver.class);

        final IBinder transition = startRecentsTransition(/* synthetic= */ true, runner);
        verify(runner).onAnimationStart(any(), any(), any(), any(), any(), any(), any());

        // Finish and verify no transition remains and that the provided finish callback is called
        mRecentsTransitionHandler.findController(transition).finish(true /* toHome */,
                false /* sendUserLeaveHint */, finishCallback);
        mMainExecutor.flushAll();
        verify(finishCallback).send(anyInt(), any());
        assertNull(mRecentsTransitionHandler.findController(transition));
    }

    @Test
    public void testStartSyntheticRecentsTransition_callsOnAnimationCancel() throws Exception {
        final IRecentsAnimationRunner runner = mock(IRecentsAnimationRunner.class);

        final IBinder transition = startRecentsTransition(/* synthetic= */ true, runner);
        verify(runner).onAnimationStart(any(), any(), any(), any(), any(), any(), any());

        mRecentsTransitionHandler.findController(transition).cancel("test");
        mMainExecutor.flushAll();
        verify(runner).onAnimationCanceled(any(), any());
        assertNull(mRecentsTransitionHandler.findController(transition));
    }

    @Test
    public void testStartTransition_updatesStateListeners() {
        final TestTransitionStateListener listener = new TestTransitionStateListener();
        mRecentsTransitionHandler.addTransitionStateListener(listener);

        startRecentsTransition(/* synthetic= */ false);
        mMainExecutor.flushAll();

        assertThat(listener.getState()).isEqualTo(TRANSITION_STATE_REQUESTED);
    }

    @Test
    public void testStartAnimation_updatesStateListeners() {
        final TestTransitionStateListener listener = new TestTransitionStateListener();
        mRecentsTransitionHandler.addTransitionStateListener(listener);

        final IBinder transition = startRecentsTransition(/* synthetic= */ false);
        mRecentsTransitionHandler.startAnimation(
                transition, createTransitionInfo(), new StubTransaction(), new StubTransaction(),
                mock(Transitions.TransitionFinishCallback.class));
        mMainExecutor.flushAll();

        assertThat(listener.getState()).isEqualTo(TRANSITION_STATE_ANIMATING);
    }

    @Test
    public void testFinishTransition_updatesStateListeners() {
        final TestTransitionStateListener listener = new TestTransitionStateListener();
        mRecentsTransitionHandler.addTransitionStateListener(listener);

        final IBinder transition = startRecentsTransition(/* synthetic= */ false);
        mRecentsTransitionHandler.startAnimation(
                transition, createTransitionInfo(), new StubTransaction(), new StubTransaction(),
                mock(Transitions.TransitionFinishCallback.class));
        mRecentsTransitionHandler.findController(transition).finish(true /* toHome */,
                false /* sendUserLeaveHint */, mock(IResultReceiver.class));
        mMainExecutor.flushAll();

        assertThat(listener.getState()).isEqualTo(TRANSITION_STATE_NOT_RUNNING);
    }

    @Test
    public void testCancelTransition_updatesStateListeners() {
        final TestTransitionStateListener listener = new TestTransitionStateListener();
        mRecentsTransitionHandler.addTransitionStateListener(listener);

        final IBinder transition = startRecentsTransition(/* synthetic= */ false);
        mRecentsTransitionHandler.findController(transition).cancel("test");
        mMainExecutor.flushAll();

        assertThat(listener.getState()).isEqualTo(TRANSITION_STATE_NOT_RUNNING);
    }

    @Test
    public void testStartAnimation_synthetic_updatesStateListeners() {
        final TestTransitionStateListener listener = new TestTransitionStateListener();
        mRecentsTransitionHandler.addTransitionStateListener(listener);

        startRecentsTransition(/* synthetic= */ true);
        mMainExecutor.flushAll();

        assertThat(listener.getState()).isEqualTo(TRANSITION_STATE_ANIMATING);
    }

    @Test
    public void testFinishTransition_synthetic_updatesStateListeners() {
        final TestTransitionStateListener listener = new TestTransitionStateListener();
        mRecentsTransitionHandler.addTransitionStateListener(listener);

        final IBinder transition = startRecentsTransition(/* synthetic= */ true);
        mRecentsTransitionHandler.findController(transition).finish(true /* toHome */,
                false /* sendUserLeaveHint */, mock(IResultReceiver.class));
        mMainExecutor.flushAll();

        assertThat(listener.getState()).isEqualTo(TRANSITION_STATE_NOT_RUNNING);
    }

    @Test
    public void testCancelTransition_synthetic_updatesStateListeners() {
        final TestTransitionStateListener listener = new TestTransitionStateListener();
        mRecentsTransitionHandler.addTransitionStateListener(listener);

        final IBinder transition = startRecentsTransition(/* synthetic= */ true);
        mRecentsTransitionHandler.findController(transition).cancel("test");
        mMainExecutor.flushAll();

        assertThat(listener.getState()).isEqualTo(TRANSITION_STATE_NOT_RUNNING);
    }

    @Test
    @EnableFlags(FLAG_ENABLE_DESKTOP_RECENTS_TRANSITIONS_CORNERS_BUGFIX)
    public void testMerge_openingTasks_callsOnTasksAppeared() throws Exception {
        final IRecentsAnimationRunner animationRunner = mock(IRecentsAnimationRunner.class);
        TransitionInfo mergeTransitionInfo = new TransitionInfoBuilder(TRANSIT_OPEN)
                .addChange(TRANSIT_OPEN, new TestRunningTaskInfoBuilder().build())
                .build();
        final IBinder transition = startRecentsTransition(/* synthetic= */ false, animationRunner);
        SurfaceControl.Transaction finishT = mock(SurfaceControl.Transaction.class);
        mRecentsTransitionHandler.startAnimation(
                transition, createTransitionInfo(), new StubTransaction(), new StubTransaction(),
                mock(Transitions.TransitionFinishCallback.class));

        mRecentsTransitionHandler.findController(transition).merge(
                mergeTransitionInfo,
                new StubTransaction(),
                new StubTransaction(),
                mock(Transitions.TransitionFinishCallback.class));
        mMainExecutor.flushAll();

        verify(animationRunner).onTasksAppeared(
                /* appearedTargets= */ any(), eq(mergeTransitionInfo));
    }

    @Test
    @EnableFlags(FLAG_ENABLE_RECENTS_BOOKEND_TRANSITION)
    public void testMerge_consumeBookendTransition() throws Exception {
        // Start and finish the transition
        final IRecentsAnimationRunner animationRunner = mock(IRecentsAnimationRunner.class);
        final IBinder transition = startRecentsTransition(/* synthetic= */ false, animationRunner);
        mRecentsTransitionHandler.startAnimation(
                transition, createTransitionInfo(), new StubTransaction(), new StubTransaction(),
                mock(Transitions.TransitionFinishCallback.class));
        mRecentsTransitionHandler.findController(transition).finish(/* toHome= */ false,
                false /* sendUserLeaveHint */, mock(IResultReceiver.class));
        mMainExecutor.flushAll();

        // Merge the bookend transition
        TransitionInfo mergeTransitionInfo =
                new TransitionInfoBuilder(TRANSIT_END_RECENTS_TRANSITION)
                        .addChange(TRANSIT_OPEN, new TestRunningTaskInfoBuilder().build())
                        .build();
        SurfaceControl.Transaction finishT = mock(SurfaceControl.Transaction.class);
        Transitions.TransitionFinishCallback finishCallback
                = mock(Transitions.TransitionFinishCallback.class);
        mRecentsTransitionHandler.findController(transition).merge(
                mergeTransitionInfo,
                new StubTransaction(),
                finishT,
                finishCallback);
        mMainExecutor.flushAll();

        // Verify that we've merged
        verify(finishCallback).onTransitionFinished(any());
    }

    @Test
    @EnableFlags(FLAG_ENABLE_RECENTS_BOOKEND_TRANSITION)
    public void testMerge_pendingBookendTransition_mergesTransition() throws Exception {
        // Start and finish the transition
        final IRecentsAnimationRunner animationRunner = mock(IRecentsAnimationRunner.class);
        final IBinder transition = startRecentsTransition(/* synthetic= */ false, animationRunner);
        mRecentsTransitionHandler.startAnimation(
                transition, createTransitionInfo(), new StubTransaction(), new StubTransaction(),
                mock(Transitions.TransitionFinishCallback.class));
        mRecentsTransitionHandler.findController(transition).finish(/* toHome= */ false,
                false /* sendUserLeaveHint */, mock(IResultReceiver.class));
        mMainExecutor.flushAll();

        // Merge a new transition while we have a pending finish
        TransitionInfo mergeTransitionInfo = new TransitionInfoBuilder(TRANSIT_OPEN)
                .addChange(TRANSIT_OPEN, new TestRunningTaskInfoBuilder().build())
                .build();
        SurfaceControl.Transaction finishT = mock(SurfaceControl.Transaction.class);
        Transitions.TransitionFinishCallback finishCallback
                = mock(Transitions.TransitionFinishCallback.class);
        mRecentsTransitionHandler.findController(transition).merge(
                mergeTransitionInfo,
                new StubTransaction(),
                finishT,
                finishCallback);
        mMainExecutor.flushAll();

        // Verify that we've cleaned up the original transition
        assertNull(mRecentsTransitionHandler.findController(transition));
    }

    @Test
    @EnableFlags(FLAG_ENABLE_DESKTOP_RECENTS_TRANSITIONS_CORNERS_BUGFIX)
    public void testMergeAndFinish_openingFreeformTasks_setsCornerRadius() {
        ActivityManager.RunningTaskInfo freeformTask =
                new TestRunningTaskInfoBuilder().setWindowingMode(WINDOWING_MODE_FREEFORM).build();
        TransitionInfo mergeTransitionInfo = new TransitionInfoBuilder(TRANSIT_OPEN)
                .addChange(TRANSIT_OPEN, freeformTask)
                .build();
        SurfaceControl leash = mergeTransitionInfo.getChanges().get(0).getLeash();
        final IBinder transition = startRecentsTransition(/* synthetic= */ false);
        SurfaceControl.Transaction finishT = mock(SurfaceControl.Transaction.class);
        mRecentsTransitionHandler.startAnimation(
                transition, createTransitionInfo(), new StubTransaction(), new StubTransaction(),
                mock(Transitions.TransitionFinishCallback.class));

        mRecentsTransitionHandler.findController(transition).merge(
                mergeTransitionInfo,
                new StubTransaction(),
                finishT,
                mock(Transitions.TransitionFinishCallback.class));
        mRecentsTransitionHandler.findController(transition).finish(/* toHome= */ false,
                false /* sendUserLeaveHint */, mock(IResultReceiver.class));
        mMainExecutor.flushAll();

        verify(finishT).setCornerRadius(leash, FREEFORM_TASK_CORNER_RADIUS);
    }

    @Test
    @EnableFlags(FLAG_ENABLE_DESKTOP_RECENTS_TRANSITIONS_CORNERS_BUGFIX)
    public void testFinish_returningToFreeformTasks_setsCornerRadius() {
        ActivityManager.RunningTaskInfo freeformTask =
                new TestRunningTaskInfoBuilder().setWindowingMode(WINDOWING_MODE_FREEFORM).build();
        TransitionInfo transitionInfo = new TransitionInfoBuilder(TRANSIT_CLOSE)
                .addChange(TRANSIT_CLOSE, freeformTask)
                .build();
        SurfaceControl leash = transitionInfo.getChanges().get(0).getLeash();
        final IBinder transition = startRecentsTransition(/* synthetic= */ false);
        SurfaceControl.Transaction finishT = mock(SurfaceControl.Transaction.class);
        mRecentsTransitionHandler.startAnimation(
                transition, transitionInfo, new StubTransaction(), finishT,
                mock(Transitions.TransitionFinishCallback.class));

        mRecentsTransitionHandler.findController(transition).finish(/* toHome= */ false,
                false /* sendUserLeaveHint */, mock(IResultReceiver.class));
        mMainExecutor.flushAll();


        verify(finishT).setCornerRadius(leash, FREEFORM_TASK_CORNER_RADIUS);
    }

    private IBinder startRecentsTransition(boolean synthetic) {
        return startRecentsTransition(synthetic, mock(IRecentsAnimationRunner.class));
    }

    private IBinder startRecentsTransition(boolean synthetic,
            @NonNull IRecentsAnimationRunner runner) {
        doReturn(new Binder()).when(runner).asBinder();
        final Bundle options = new Bundle();
        options.putBoolean("is_synthetic_recents_transition", synthetic);
        final IBinder transition = new Binder();
        when(mTransitions.startTransition(anyInt(), any(), any())).thenReturn(transition);
        return mRecentsTransitionHandler.startRecentsTransition(
                mock(PendingIntent.class), new Intent(), options, mock(IApplicationThread.class),
                runner);
    }

    private TransitionInfo createTransitionInfo() {
        final ActivityManager.RunningTaskInfo homeTask = new TestRunningTaskInfoBuilder()
                .setTopActivityType(ACTIVITY_TYPE_HOME)
                .build();
        final ActivityManager.RunningTaskInfo appTask = new TestRunningTaskInfoBuilder()
                .setTopActivityType(ACTIVITY_TYPE_STANDARD)
                .build();
        final TransitionInfo.Change homeChange = new TransitionInfo.Change(
                homeTask.token, new SurfaceControl());
        homeChange.setMode(TRANSIT_TO_FRONT);
        homeChange.setTaskInfo(homeTask);
        final TransitionInfo.Change appChange = new TransitionInfo.Change(
                appTask.token, new SurfaceControl());
        appChange.setMode(TRANSIT_TO_FRONT);
        appChange.setTaskInfo(appTask);
        return new TransitionInfoBuilder(TRANSIT_START_RECENTS_TRANSITION)
                .addChange(homeChange)
                .addChange(appChange)
                .build();
    }

    private static class TestTransitionStateListener implements RecentsTransitionStateListener {
        @RecentsTransitionState
        private int mState = TRANSITION_STATE_NOT_RUNNING;

        @Override
        public void onTransitionStateChanged(int state) {
            mState = state;
        }

        @RecentsTransitionState
        int getState() {
            return mState;
        }
    }
}
