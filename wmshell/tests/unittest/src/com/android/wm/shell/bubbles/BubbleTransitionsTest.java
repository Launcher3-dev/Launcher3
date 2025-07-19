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
package com.android.wm.shell.bubbles;

import static android.view.WindowManager.TRANSIT_CHANGE;
import static android.view.WindowManager.TRANSIT_TO_FRONT;

import static com.android.wm.shell.transition.Transitions.TRANSIT_CONVERT_TO_BUBBLE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.IBinder;
import android.view.SurfaceControl;
import android.view.ViewRootImpl;
import android.window.IWindowContainerToken;
import android.window.TransitionInfo;
import android.window.WindowContainerToken;
import android.window.WindowContainerTransaction;

import androidx.core.animation.AnimatorTestRule;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.launcher3.icons.BubbleIconFactory;
import com.android.wm.shell.ShellTaskOrganizer;
import com.android.wm.shell.ShellTestCase;
import com.android.wm.shell.TestSyncExecutor;
import com.android.wm.shell.bubbles.BubbleTransitions.DraggedBubbleIconToFullscreen;
import com.android.wm.shell.bubbles.bar.BubbleBarExpandedView;
import com.android.wm.shell.bubbles.bar.BubbleBarLayerView;
import com.android.wm.shell.common.ShellExecutor;
import com.android.wm.shell.common.SyncTransactionQueue;
import com.android.wm.shell.taskview.TaskView;
import com.android.wm.shell.taskview.TaskViewRepository;
import com.android.wm.shell.taskview.TaskViewTaskController;
import com.android.wm.shell.taskview.TaskViewTransitions;
import com.android.wm.shell.transition.Transitions;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests of {@link BubbleTransitions}.
 */
@SmallTest
public class BubbleTransitionsTest extends ShellTestCase {

    @Rule public final AnimatorTestRule mAnimatorTestRule = new AnimatorTestRule();

    private static final int FULLSCREEN_TASK_WIDTH = 200;
    private static final int FULLSCREEN_TASK_HEIGHT = 100;

    @Mock
    private BubbleData mBubbleData;
    @Mock
    private Bubble mBubble;
    @Mock
    private Transitions mTransitions;
    @Mock
    private SyncTransactionQueue mSyncQueue;
    @Mock
    private BubbleExpandedViewManager mExpandedViewManager;
    @Mock
    private BubblePositioner mBubblePositioner;
    @Mock
    private BubbleStackView mStackView;
    @Mock
    private BubbleBarLayerView mLayerView;
    @Mock
    private BubbleIconFactory mIconFactory;

    @Mock private ShellTaskOrganizer mTaskOrganizer;
    private TaskViewTransitions mTaskViewTransitions;
    private TaskViewRepository mRepository;
    private BubbleTransitions mBubbleTransitions;
    private BubbleTaskViewFactory mTaskViewFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mRepository = new TaskViewRepository();
        ShellExecutor syncExecutor = new TestSyncExecutor();

        when(mTransitions.getMainExecutor()).thenReturn(syncExecutor);
        when(mTransitions.isRegistered()).thenReturn(true);
        mTaskViewTransitions = new TaskViewTransitions(mTransitions, mRepository, mTaskOrganizer,
                mSyncQueue);
        mBubbleTransitions = new BubbleTransitions(mTransitions, mTaskOrganizer, mRepository,
                mBubbleData, mTaskViewTransitions, mContext);
        mTaskViewFactory = () -> {
            TaskViewTaskController taskViewTaskController = new TaskViewTaskController(
                    mContext, mTaskOrganizer, mTaskViewTransitions, mSyncQueue);
            TaskView taskView = new TaskView(mContext, mTaskViewTransitions,
                    taskViewTaskController);
            return new BubbleTaskView(taskView, syncExecutor);
        };
        final BubbleBarExpandedView bbev = mock(BubbleBarExpandedView.class);
        final ViewRootImpl vri = mock(ViewRootImpl.class);
        when(bbev.getViewRootImpl()).thenReturn(vri);
        when(mBubble.getBubbleBarExpandedView()).thenReturn(bbev);
    }

    private ActivityManager.RunningTaskInfo setupBubble() {
        ActivityManager.RunningTaskInfo taskInfo = new ActivityManager.RunningTaskInfo();
        WindowContainerToken token = createMockToken();
        taskInfo.token = token;
        final TaskView tv = mock(TaskView.class);
        final TaskViewTaskController tvtc = mock(TaskViewTaskController.class);
        when(tvtc.getTaskInfo()).thenReturn(taskInfo);
        when(tv.getController()).thenReturn(tvtc);
        when(mBubble.getTaskView()).thenReturn(tv);
        when(tv.getTaskInfo()).thenReturn(taskInfo);
        mRepository.add(tvtc);
        return taskInfo;
    }

    private TransitionInfo setupFullscreenTaskTransition(ActivityManager.RunningTaskInfo taskInfo,
            SurfaceControl taskLeash, SurfaceControl snapshot) {
        final TransitionInfo info = new TransitionInfo(TRANSIT_CONVERT_TO_BUBBLE, 0);
        final TransitionInfo.Change chg = new TransitionInfo.Change(taskInfo.token, taskLeash);
        chg.setTaskInfo(taskInfo);
        chg.setMode(TRANSIT_CHANGE);
        chg.setStartAbsBounds(new Rect(0, 0, FULLSCREEN_TASK_WIDTH, FULLSCREEN_TASK_HEIGHT));
        chg.setSnapshot(snapshot, /* luma= */ 0f);
        info.addChange(chg);
        info.addRoot(new TransitionInfo.Root(0, mock(SurfaceControl.class), 0, 0));
        return info;
    }

    private WindowContainerToken createMockToken() {
        final IWindowContainerToken itoken = mock(IWindowContainerToken.class);
        final IBinder asBinder = mock(IBinder.class);
        when(itoken.asBinder()).thenReturn(asBinder);
        return new WindowContainerToken(itoken);
    }

    @Test
    public void testConvertToBubble() {
        // Basic walk-through of convert-to-bubble transition stages
        ActivityManager.RunningTaskInfo taskInfo = setupBubble();
        final BubbleTransitions.BubbleTransition bt = mBubbleTransitions.startConvertToBubble(
                mBubble, taskInfo, mExpandedViewManager, mTaskViewFactory, mBubblePositioner,
                mStackView, mLayerView, mIconFactory, null, false);
        final BubbleTransitions.ConvertToBubble ctb = (BubbleTransitions.ConvertToBubble) bt;
        ctb.onInflated(mBubble);
        when(mLayerView.canExpandView(any())).thenReturn(true);
        verify(mTransitions).startTransition(anyInt(), any(), eq(ctb));
        verify(mBubble).setPreparingTransition(eq(bt));
        // Ensure we are communicating with the taskviewtransitions queue
        assertTrue(mTaskViewTransitions.hasPending());

        SurfaceControl taskLeash = new SurfaceControl.Builder().setName("taskLeash").build();
        SurfaceControl snapshot = new SurfaceControl.Builder().setName("snapshot").build();
        final TransitionInfo info = setupFullscreenTaskTransition(taskInfo, taskLeash, snapshot);
        SurfaceControl.Transaction startT = mock(SurfaceControl.Transaction.class);
        SurfaceControl.Transaction finishT = mock(SurfaceControl.Transaction.class);
        final boolean[] finishCalled = new boolean[]{false};
        Transitions.TransitionFinishCallback finishCb = wct -> {
            assertFalse(finishCalled[0]);
            finishCalled[0] = true;
        };
        ctb.startAnimation(ctb.mTransition, info, startT, finishT, finishCb);
        assertFalse(mTaskViewTransitions.hasPending());

        verify(startT).setPosition(taskLeash, 0, 0);
        verify(startT).setPosition(snapshot, 0, 0);

        verify(mBubbleData).notificationEntryUpdated(eq(mBubble), anyBoolean(), anyBoolean());

        clearInvocations(mBubble);
        verify(mBubble, never()).setPreparingTransition(any());

        ctb.surfaceCreated();
        // Check that preparing transition is not reset before continueExpand is called
        verify(mBubble, never()).setPreparingTransition(any());
        ArgumentCaptor<Runnable> animCb = ArgumentCaptor.forClass(Runnable.class);
        verify(mLayerView).animateConvert(any(), any(), anyFloat(), any(), any(), animCb.capture());

        // continueExpand is now called, check that preparing transition is cleared
        ctb.continueExpand();
        verify(mBubble).setPreparingTransition(isNull());

        assertFalse(finishCalled[0]);
        animCb.getValue().run();
        assertTrue(finishCalled[0]);
    }

    @Test
    public void testConvertToBubble_drag() {
        ActivityManager.RunningTaskInfo taskInfo = setupBubble();

        WindowContainerTransaction pendingWct = new WindowContainerTransaction();
        WindowContainerToken pendingDragOpToken = createMockToken();
        pendingWct.reorder(pendingDragOpToken, /* onTop= */ false);

        PointF dragPosition = new PointF(10f, 20f);
        BubbleTransitions.DragData dragData = new BubbleTransitions.DragData(
                /* releasedOnLeft= */ false, /* taskScale= */ 0.5f, /* cornerRadius= */ 10f,
                dragPosition, pendingWct);

        final BubbleTransitions.BubbleTransition bt = mBubbleTransitions.startConvertToBubble(
                mBubble, taskInfo, mExpandedViewManager, mTaskViewFactory, mBubblePositioner,
                mStackView, mLayerView, mIconFactory, dragData, false);
        final BubbleTransitions.ConvertToBubble ctb = (BubbleTransitions.ConvertToBubble) bt;

        ArgumentCaptor<WindowContainerTransaction> wctCaptor = ArgumentCaptor.forClass(
                WindowContainerTransaction.class);
        ctb.onInflated(mBubble);
        verify(mTransitions).startTransition(anyInt(), wctCaptor.capture(), eq(ctb));

        // Verify that the WCT has the pending operation from drag data
        WindowContainerTransaction transitionWct = wctCaptor.getValue();
        assertThat(transitionWct.getHierarchyOps().stream().anyMatch(op -> op.getType()
                == WindowContainerTransaction.HierarchyOp.HIERARCHY_OP_TYPE_REORDER
                && op.getContainer() == pendingDragOpToken.asBinder())).isTrue();

        SurfaceControl taskLeash = new SurfaceControl.Builder().setName("taskLeash").build();
        SurfaceControl snapshot = new SurfaceControl.Builder().setName("snapshot").build();
        final TransitionInfo info = setupFullscreenTaskTransition(taskInfo, taskLeash, snapshot);
        SurfaceControl.Transaction startT = mock(SurfaceControl.Transaction.class);
        SurfaceControl.Transaction finishT = mock(SurfaceControl.Transaction.class);
        Transitions.TransitionFinishCallback finishCb = wct -> {};
        ctb.startAnimation(ctb.mTransition, info, startT, finishT, finishCb);

        // Verify that snapshot and task are placed at where the drag ended
        verify(startT).setPosition(taskLeash, dragPosition.x, dragPosition.y);
        verify(startT).setPosition(snapshot, dragPosition.x, dragPosition.y);
        // Snapshot has the scale of the dragged task
        verify(startT).setScale(snapshot, dragData.getTaskScale(), dragData.getTaskScale());
        // Snapshot has dragged task corner radius
        verify(startT).setCornerRadius(snapshot, dragData.getCornerRadius());
    }

    @Test
    public void testConvertFromBubble() {
        ActivityManager.RunningTaskInfo taskInfo = setupBubble();
        final BubbleTransitions.BubbleTransition bt = mBubbleTransitions.startConvertFromBubble(
                mBubble, taskInfo);
        final BubbleTransitions.ConvertFromBubble cfb = (BubbleTransitions.ConvertFromBubble) bt;
        verify(mTransitions).startTransition(anyInt(), any(), eq(cfb));
        verify(mBubble).setPreparingTransition(eq(bt));
        assertTrue(mTaskViewTransitions.hasPending());

        final TransitionInfo info = new TransitionInfo(TRANSIT_CHANGE, 0);
        final TransitionInfo.Change chg = new TransitionInfo.Change(taskInfo.token,
                mock(SurfaceControl.class));
        chg.setMode(TRANSIT_CHANGE);
        chg.setTaskInfo(taskInfo);
        info.addChange(chg);
        info.addRoot(new TransitionInfo.Root(0, mock(SurfaceControl.class), 0, 0));
        SurfaceControl.Transaction startT = mock(SurfaceControl.Transaction.class);
        SurfaceControl.Transaction finishT = mock(SurfaceControl.Transaction.class);
        Transitions.TransitionFinishCallback finishCb = wct -> {};
        cfb.startAnimation(cfb.mTransition, info, startT, finishT, finishCb);

        // Can really only verify that it interfaces with the taskViewTransitions queue.
        // The actual functioning of this is tightly-coupled with SurfaceFlinger and renderthread
        // in order to properly synchronize surface manipulation with drawing and thus can't be
        // directly tested.
        assertFalse(mTaskViewTransitions.hasPending());
    }

    @Test
    public void convertDraggedBubbleToFullscreen() {
        ActivityManager.RunningTaskInfo taskInfo = setupBubble();
        SurfaceControl.Transaction animT = mock(SurfaceControl.Transaction.class);
        BubbleTransitions.TransactionProvider transactionProvider = () -> animT;
        final DraggedBubbleIconToFullscreen bt =
                mBubbleTransitions.new DraggedBubbleIconToFullscreen(
                        mBubble, new Point(100, 50), transactionProvider);
        verify(mTransitions).startTransition(anyInt(), any(), eq(bt));

        SurfaceControl taskLeash = new SurfaceControl.Builder().setName("taskLeash").build();
        final TransitionInfo info = new TransitionInfo(TRANSIT_TO_FRONT, 0);
        final TransitionInfo.Change chg = new TransitionInfo.Change(taskInfo.token, taskLeash);
        chg.setMode(TRANSIT_TO_FRONT);
        chg.setTaskInfo(taskInfo);
        info.addChange(chg);
        info.addRoot(new TransitionInfo.Root(0, mock(SurfaceControl.class), 0, 0));
        SurfaceControl.Transaction startT = mock(SurfaceControl.Transaction.class);
        SurfaceControl.Transaction finishT = mock(SurfaceControl.Transaction.class);
        boolean[] transitionFinished = {false};
        Transitions.TransitionFinishCallback finishCb = wct -> transitionFinished[0] = true;
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            bt.startAnimation(bt.mTransition, info, startT, finishT, finishCb);
            mAnimatorTestRule.advanceTimeBy(250);
        });
        verify(startT).setScale(taskLeash, 0, 0);
        verify(startT).setPosition(taskLeash, 100, 50);
        verify(startT).apply();
        verify(animT).setScale(taskLeash, 1, 1);
        verify(animT).setPosition(taskLeash, 0, 0);
        verify(animT, atLeastOnce()).apply();
        verify(animT).close();
        assertFalse(mTaskViewTransitions.hasPending());
        assertTrue(transitionFinished[0]);
    }

    @Test
    public void convertFloatingBubbleToFullscreen() {
        final BubbleExpandedView bev = mock(BubbleExpandedView.class);
        final ViewRootImpl vri = mock(ViewRootImpl.class);
        when(bev.getViewRootImpl()).thenReturn(vri);
        when(mBubble.getBubbleBarExpandedView()).thenReturn(null);
        when(mBubble.getExpandedView()).thenReturn(bev);

        ActivityManager.RunningTaskInfo taskInfo = setupBubble();
        final BubbleTransitions.BubbleTransition bt = mBubbleTransitions.startConvertFromBubble(
                mBubble, taskInfo);
        final BubbleTransitions.ConvertFromBubble cfb = (BubbleTransitions.ConvertFromBubble) bt;
        verify(mTransitions).startTransition(anyInt(), any(), eq(cfb));
        verify(mBubble).setPreparingTransition(eq(bt));
        assertTrue(mTaskViewTransitions.hasPending());

        final TransitionInfo info = new TransitionInfo(TRANSIT_CHANGE, 0);
        final TransitionInfo.Change chg = new TransitionInfo.Change(taskInfo.token,
                mock(SurfaceControl.class));
        chg.setMode(TRANSIT_CHANGE);
        chg.setTaskInfo(taskInfo);
        info.addChange(chg);
        info.addRoot(new TransitionInfo.Root(0, mock(SurfaceControl.class), 0, 0));
        SurfaceControl.Transaction startT = mock(SurfaceControl.Transaction.class);
        SurfaceControl.Transaction finishT = mock(SurfaceControl.Transaction.class);
        Transitions.TransitionFinishCallback finishCb = wct -> {};
        cfb.startAnimation(cfb.mTransition, info, startT, finishT, finishCb);

        // Can really only verify that it interfaces with the taskViewTransitions queue.
        // The actual functioning of this is tightly-coupled with SurfaceFlinger and renderthread
        // in order to properly synchronize surface manipulation with drawing and thus can't be
        // directly tested.
        assertFalse(mTaskViewTransitions.hasPending());
    }
}
