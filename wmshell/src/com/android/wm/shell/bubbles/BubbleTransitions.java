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

import static android.app.ActivityTaskManager.INVALID_TASK_ID;
import static android.app.WindowConfiguration.WINDOWING_MODE_MULTI_WINDOW;
import static android.app.WindowConfiguration.WINDOWING_MODE_UNDEFINED;
import static android.view.View.INVISIBLE;
import static android.view.WindowManager.TRANSIT_CHANGE;
import static android.view.WindowManager.TRANSIT_TO_FRONT;

import static com.android.wm.shell.transition.Transitions.TRANSIT_CONVERT_TO_BUBBLE;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.TaskInfo;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.IBinder;
import android.util.Slog;
import android.view.SurfaceControl;
import android.view.SurfaceView;
import android.view.View;
import android.window.TransitionInfo;
import android.window.TransitionRequestInfo;
import android.window.WindowContainerToken;
import android.window.WindowContainerTransaction;

import androidx.core.animation.Animator;
import androidx.core.animation.Animator.AnimatorUpdateListener;
import androidx.core.animation.AnimatorListenerAdapter;
import androidx.core.animation.ValueAnimator;

import com.android.internal.annotations.VisibleForTesting;
import com.android.launcher3.icons.BubbleIconFactory;
import com.android.wm.shell.ShellTaskOrganizer;
import com.android.wm.shell.bubbles.bar.BubbleBarExpandedView;
import com.android.wm.shell.bubbles.bar.BubbleBarLayerView;
import com.android.wm.shell.taskview.TaskView;
import com.android.wm.shell.taskview.TaskViewRepository;
import com.android.wm.shell.taskview.TaskViewTaskController;
import com.android.wm.shell.taskview.TaskViewTransitions;
import com.android.wm.shell.transition.Transitions;

import java.util.concurrent.Executor;

/**
 * Implements transition coordination for bubble operations.
 */
public class BubbleTransitions {
    private static final String TAG = "BubbleTransitions";

    /**
     * Multiplier used to convert a view elevation to an "equivalent" shadow-radius. This is the
     * same multiple used by skia and surface-outsets in WMS.
     */
    private static final float ELEVATION_TO_RADIUS = 2;

    @NonNull final Transitions mTransitions;
    @NonNull final ShellTaskOrganizer mTaskOrganizer;
    @NonNull final TaskViewRepository mRepository;
    @NonNull final Executor mMainExecutor;
    @NonNull final BubbleData mBubbleData;
    @NonNull final TaskViewTransitions mTaskViewTransitions;
    @NonNull final Context mContext;

    BubbleTransitions(@NonNull Transitions transitions, @NonNull ShellTaskOrganizer organizer,
            @NonNull TaskViewRepository repository, @NonNull BubbleData bubbleData,
            @NonNull TaskViewTransitions taskViewTransitions, Context context) {
        mTransitions = transitions;
        mTaskOrganizer = organizer;
        mRepository = repository;
        mMainExecutor = transitions.getMainExecutor();
        mBubbleData = bubbleData;
        mTaskViewTransitions = taskViewTransitions;
        mContext = context;
    }

    /**
     * Starts a convert-to-bubble transition.
     *
     * @see ConvertToBubble
     */
    public BubbleTransition startConvertToBubble(Bubble bubble, TaskInfo taskInfo,
            BubbleExpandedViewManager expandedViewManager, BubbleTaskViewFactory factory,
            BubblePositioner positioner, BubbleStackView stackView,
            BubbleBarLayerView layerView, BubbleIconFactory iconFactory,
            DragData dragData, boolean inflateSync) {
        return new ConvertToBubble(bubble, taskInfo, mContext,
                expandedViewManager, factory, positioner, stackView, layerView, iconFactory,
                dragData, inflateSync);
    }

    /**
     * Starts a convert-from-bubble transition.
     *
     * @see ConvertFromBubble
     */
    public BubbleTransition startConvertFromBubble(Bubble bubble,
            TaskInfo taskInfo) {
        ConvertFromBubble convert = new ConvertFromBubble(bubble, taskInfo);
        return convert;
    }

    /** Starts a transition that converts a dragged bubble icon to a full screen task. */
    public BubbleTransition startDraggedBubbleIconToFullscreen(Bubble bubble, Point dropLocation) {
        return new DraggedBubbleIconToFullscreen(bubble, dropLocation);
    }

    /**
     * Plucks the task-surface out of an ancestor view while making the view invisible. This helper
     * attempts to do this seamlessly (ie. view becomes invisible in sync with task reparent).
     */
    private void pluck(SurfaceControl taskLeash, View fromView, SurfaceControl dest,
            float destX, float destY, float cornerRadius, SurfaceControl.Transaction t,
            Runnable onPlucked) {
        SurfaceControl.Transaction pluckT = new SurfaceControl.Transaction();
        pluckT.reparent(taskLeash, dest);
        t.reparent(taskLeash, dest);
        pluckT.setPosition(taskLeash, destX, destY);
        t.setPosition(taskLeash, destX, destY);
        pluckT.show(taskLeash);
        pluckT.setAlpha(taskLeash, 1.f);
        float shadowRadius = fromView.getElevation() * ELEVATION_TO_RADIUS;
        pluckT.setShadowRadius(taskLeash, shadowRadius);
        pluckT.setCornerRadius(taskLeash, cornerRadius);
        t.setShadowRadius(taskLeash, shadowRadius);
        t.setCornerRadius(taskLeash, cornerRadius);

        // Need to remove the taskview AFTER applying the startTransaction because it isn't
        // synchronized.
        pluckT.addTransactionCommittedListener(mMainExecutor, onPlucked::run);
        fromView.getViewRootImpl().applyTransactionOnDraw(pluckT);
        fromView.setVisibility(INVISIBLE);
    }

    /**
     * Interface to a bubble-specific transition. Bubble transitions have a multi-step lifecycle
     * in order to coordinate with the bubble view logic. These steps are communicated on this
     * interface.
     */
    interface BubbleTransition {
        default void surfaceCreated() {}
        default void continueExpand() {}
        void skip();
        default void continueCollapse() {}
    }

    /**
     * Information about the task when it is being dragged to a bubble
     */
    public static class DragData {
        private final WindowContainerTransaction mPendingWct;
        private final boolean mReleasedOnLeft;
        private final float mTaskScale;
        private final float mCornerRadius;
        private final PointF mDragPosition;

        /**
         * @param releasedOnLeft true if the bubble was released in the left drop target
         * @param taskScale      the scale of the task when it was dragged to bubble
         * @param cornerRadius   the corner radius of the task when it was dragged to bubble
         * @param dragPosition   the position of the task when it was dragged to bubble
         * @param wct            pending operations to be applied when finishing the drag
         */
        public DragData(boolean releasedOnLeft, float taskScale, float cornerRadius,
                @Nullable PointF dragPosition, @Nullable WindowContainerTransaction wct) {
            mPendingWct = wct;
            mReleasedOnLeft = releasedOnLeft;
            mTaskScale = taskScale;
            mCornerRadius = cornerRadius;
            mDragPosition = dragPosition != null ? dragPosition : new PointF(0, 0);
        }

        /**
         * @return pending operations to be applied when finishing the drag
         */
        @Nullable
        public WindowContainerTransaction getPendingWct() {
            return mPendingWct;
        }

        /**
         * @return true if the bubble was released in the left drop target
         */
        public boolean isReleasedOnLeft() {
            return mReleasedOnLeft;
        }

        /**
         * @return the scale of the task when it was dragged to bubble
         */
        public float getTaskScale() {
            return mTaskScale;
        }

        /**
         * @return the corner radius of the task when it was dragged to bubble
         */
        public float getCornerRadius() {
            return mCornerRadius;
        }

        /**
         * @return position of the task when it was dragged to bubble
         */
        public PointF getDragPosition() {
            return mDragPosition;
        }
    }

    /**
     * BubbleTransition that coordinates the process of a non-bubble task becoming a bubble. The
     * steps are as follows:
     *
     * 1. Start inflating the bubble view
     * 2. Once inflated (but not-yet visible), tell WM to do the shell-transition.
     * 3. When the transition becomes ready, notify Launcher in parallel
     * 4. Wait for surface to be created
     * 5. Once surface is ready, animate the task to a bubble
     *
     * While the animation is pending, we keep a reference to the pending transition in the bubble.
     * This allows us to check in other parts of the code that this bubble will be shown via the
     * transition animation.
     *
     * startAnimation, continueExpand and surfaceCreated are set-up to happen in either order,
     * to support UX/timing adjustments.
     */
    @VisibleForTesting
    class ConvertToBubble implements Transitions.TransitionHandler, BubbleTransition {
        final BubbleBarLayerView mLayerView;
        Bubble mBubble;
        @Nullable DragData mDragData;
        IBinder mTransition;
        Transitions.TransitionFinishCallback mFinishCb;
        WindowContainerTransaction mFinishWct = null;
        final Rect mStartBounds = new Rect();
        SurfaceControl mSnapshot = null;
        TaskInfo mTaskInfo;
        BubbleViewProvider mPriorBubble = null;

        private final TransitionProgress mTransitionProgress = new TransitionProgress();
        private SurfaceControl.Transaction mFinishT;
        private SurfaceControl mTaskLeash;

        ConvertToBubble(Bubble bubble, TaskInfo taskInfo, Context context,
                BubbleExpandedViewManager expandedViewManager, BubbleTaskViewFactory factory,
                BubblePositioner positioner, BubbleStackView stackView,
                BubbleBarLayerView layerView, BubbleIconFactory iconFactory,
                @Nullable DragData dragData, boolean inflateSync) {
            mBubble = bubble;
            mTaskInfo = taskInfo;
            mLayerView = layerView;
            mDragData = dragData;
            mBubble.setInflateSynchronously(inflateSync);
            mBubble.setPreparingTransition(this);
            mBubble.inflate(
                    this::onInflated,
                    context,
                    expandedViewManager,
                    factory,
                    positioner,
                    stackView,
                    layerView,
                    iconFactory,
                    false /* skipInflation */);
        }

        @VisibleForTesting
        void onInflated(Bubble b) {
            if (b != mBubble) {
                throw new IllegalArgumentException("inflate callback doesn't match bubble");
            }
            final Rect launchBounds = new Rect();
            mLayerView.getExpandedViewRestBounds(launchBounds);
            WindowContainerTransaction wct = new WindowContainerTransaction();
            if (mDragData != null && mDragData.getPendingWct() != null) {
                wct.merge(mDragData.getPendingWct(), true);
            }
            if (mTaskInfo.getWindowingMode() == WINDOWING_MODE_MULTI_WINDOW) {
                if (mTaskInfo.getParentTaskId() != INVALID_TASK_ID) {
                    wct.reparent(mTaskInfo.token, null, true);
                }
            }

            wct.setAlwaysOnTop(mTaskInfo.token, true);
            wct.setWindowingMode(mTaskInfo.token, WINDOWING_MODE_MULTI_WINDOW);
            wct.setBounds(mTaskInfo.token, launchBounds);

            final TaskView tv = b.getTaskView();
            tv.setSurfaceLifecycle(SurfaceView.SURFACE_LIFECYCLE_FOLLOWS_ATTACHMENT);
            final TaskViewRepository.TaskViewState state = mRepository.byTaskView(
                    tv.getController());
            if (state != null) {
                state.mVisible = true;
            }
            mTaskViewTransitions.enqueueExternal(tv.getController(), () -> {
                mTransition = mTransitions.startTransition(TRANSIT_CONVERT_TO_BUBBLE, wct, this);
                return mTransition;
            });
        }

        @Override
        public void skip() {
            mBubble.setPreparingTransition(null);
            mFinishCb.onTransitionFinished(mFinishWct);
            mFinishCb = null;
        }

        @Override
        public WindowContainerTransaction handleRequest(@NonNull IBinder transition,
                @Nullable TransitionRequestInfo request) {
            return null;
        }

        @Override
        public void mergeAnimation(@NonNull IBinder transition, @NonNull TransitionInfo info,
                @NonNull SurfaceControl.Transaction startT,
                @NonNull SurfaceControl.Transaction finishT,
                @NonNull IBinder mergeTarget,
                @NonNull Transitions.TransitionFinishCallback finishCallback) {
        }

        @Override
        public void onTransitionConsumed(@NonNull IBinder transition, boolean aborted,
                @NonNull SurfaceControl.Transaction finishTransaction) {
            if (!aborted) return;
            mTransition = null;
            mTaskViewTransitions.onExternalDone(transition);
        }

        @Override
        public boolean startAnimation(@NonNull IBinder transition,
                @NonNull TransitionInfo info,
                @NonNull SurfaceControl.Transaction startTransaction,
                @NonNull SurfaceControl.Transaction finishTransaction,
                @NonNull Transitions.TransitionFinishCallback finishCallback) {
            if (mTransition != transition) return false;
            boolean found = false;
            for (int i = 0; i < info.getChanges().size(); ++i) {
                final TransitionInfo.Change chg = info.getChanges().get(i);
                if (chg.getTaskInfo() == null) continue;
                if (chg.getMode() != TRANSIT_CHANGE && chg.getMode() != TRANSIT_TO_FRONT) continue;
                if (!mTaskInfo.token.equals(chg.getTaskInfo().token)) continue;
                mStartBounds.set(chg.getStartAbsBounds());
                // Converting a task into taskview, so treat as "new"
                mFinishWct = new WindowContainerTransaction();
                mTaskInfo = chg.getTaskInfo();
                mFinishT = finishTransaction;
                mTaskLeash = chg.getLeash();
                found = true;
                mSnapshot = chg.getSnapshot();
                break;
            }
            if (!found) {
                Slog.w(TAG, "Expected a TaskView conversion in this transition but didn't get "
                        + "one, cleaning up the task view");
                mBubble.getTaskView().getController().setTaskNotFound();
                mTaskViewTransitions.onExternalDone(transition);
                return false;
            }
            mFinishCb = finishCallback;

            if (mDragData != null) {
                mStartBounds.offsetTo((int) mDragData.getDragPosition().x,
                        (int) mDragData.getDragPosition().y);
                startTransaction.setScale(mSnapshot, mDragData.getTaskScale(),
                        mDragData.getTaskScale());
                startTransaction.setCornerRadius(mSnapshot, mDragData.getCornerRadius());
            }

            // Now update state (and talk to launcher) in parallel with snapshot stuff
            mBubbleData.notificationEntryUpdated(mBubble, /* suppressFlyout= */ true,
                    /* showInShade= */ false);

            final int left = mStartBounds.left - info.getRoot(0).getOffset().x;
            final int top = mStartBounds.top - info.getRoot(0).getOffset().y;
            startTransaction.setPosition(mTaskLeash, left, top);
            startTransaction.show(mSnapshot);
            // Move snapshot to root so that it remains visible while task is moved to taskview
            startTransaction.reparent(mSnapshot, info.getRoot(0).getLeash());
            startTransaction.setPosition(mSnapshot, left, top);
            startTransaction.setLayer(mSnapshot, Integer.MAX_VALUE);

            startTransaction.apply();

            mTaskViewTransitions.onExternalDone(transition);
            mTransitionProgress.setTransitionReady();
            startExpandAnim();
            return true;
        }

        private void startExpandAnim() {
            final boolean animate = mLayerView.canExpandView(mBubble);
            if (animate) {
                mPriorBubble = mLayerView.prepareConvertedView(mBubble);
            }
            if (mPriorBubble != null) {
                // TODO: an animation. For now though, just remove it.
                final BubbleBarExpandedView priorView = mPriorBubble.getBubbleBarExpandedView();
                mLayerView.removeView(priorView);
                mPriorBubble = null;
            }
            if (!animate || mTransitionProgress.isReadyToAnimate()) {
                playAnimation(animate);
            }
        }

        @Override
        public void continueExpand() {
            mTransitionProgress.setReadyToExpand();
        }

        @Override
        public void surfaceCreated() {
            mTransitionProgress.setSurfaceReady();
            mMainExecutor.execute(() -> {
                final TaskViewTaskController tvc = mBubble.getTaskView().getController();
                final TaskViewRepository.TaskViewState state = mRepository.byTaskView(tvc);
                if (state == null) return;
                state.mVisible = true;
                if (mTransitionProgress.isReadyToAnimate()) {
                    playAnimation(true /* animate */);
                }
            });
        }

        private void playAnimation(boolean animate) {
            final TaskViewTaskController tv = mBubble.getTaskView().getController();
            final SurfaceControl.Transaction startT = new SurfaceControl.Transaction();
            // Set task position to 0,0 as it will be placed inside the TaskView
            startT.setPosition(mTaskLeash, 0, 0);
            mTaskViewTransitions.prepareOpenAnimation(tv, true /* new */, startT, mFinishT,
                    (ActivityManager.RunningTaskInfo) mTaskInfo, mTaskLeash, mFinishWct);

            if (mFinishWct.isEmpty()) {
                mFinishWct = null;
            }

            if (animate) {
                float startScale = mDragData != null ? mDragData.getTaskScale() : 1f;
                mLayerView.animateConvert(startT, mStartBounds, startScale, mSnapshot, mTaskLeash,
                        () -> {
                            mFinishCb.onTransitionFinished(mFinishWct);
                            mFinishCb = null;
                        });
            } else {
                startT.apply();
                mFinishCb.onTransitionFinished(mFinishWct);
                mFinishCb = null;
            }
        }

        /**
         * Keeps track of internal state of different steps of this BubbleTransition.
         */
        private class TransitionProgress {
            private boolean mTransitionReady;
            private boolean mReadyToExpand;
            private boolean mSurfaceReady;

            void setTransitionReady() {
                mTransitionReady = true;
                onUpdate();
            }

            void setReadyToExpand() {
                mReadyToExpand = true;
                onUpdate();
            }

            void setSurfaceReady() {
                mSurfaceReady = true;
                onUpdate();
            }

            boolean isReadyToAnimate() {
                // Animation only depends on transition and surface state
                return mTransitionReady && mSurfaceReady;
            }

            private void onUpdate() {
                if (mTransitionReady && mReadyToExpand && mSurfaceReady) {
                    // Clear the transition from bubble when all the steps are ready
                    mBubble.setPreparingTransition(null);
                }
            }
        }
    }

    /**
     * BubbleTransition that coordinates the setup for moving a task out of a bubble. The actual
     * animation is owned by the "receiver" of the task; however, because Bubbles uses TaskView,
     * we need to do some extra coordination work to get the task surface out of the view
     * "seamlessly".
     *
     * The process here looks like:
     * 1. Send transition to WM for leaving bubbles mode
     * 2. in startAnimation, set-up a "pluck" operation to pull the task surface out of taskview
     * 3. Once "plucked", remove the view (calls continueCollapse when surfaces can be cleaned-up)
     * 4. Then re-dispatch the transition animation so that the "receiver" can animate it.
     *
     * So, constructor -> startAnimation -> continueCollapse -> re-dispatch.
     */
    @VisibleForTesting
    class ConvertFromBubble implements Transitions.TransitionHandler, BubbleTransition {
        @NonNull final Bubble mBubble;
        IBinder mTransition;
        TaskInfo mTaskInfo;
        SurfaceControl mTaskLeash;
        SurfaceControl mRootLeash;

        ConvertFromBubble(@NonNull Bubble bubble, TaskInfo taskInfo) {
            mBubble = bubble;
            mTaskInfo = taskInfo;

            mBubble.setPreparingTransition(this);
            WindowContainerTransaction wct = new WindowContainerTransaction();
            WindowContainerToken token = mTaskInfo.getToken();
            wct.setWindowingMode(token, WINDOWING_MODE_UNDEFINED);
            wct.setAlwaysOnTop(token, false);
            mTaskOrganizer.setInterceptBackPressedOnTaskRoot(token, false);
            mTaskViewTransitions.enqueueExternal(
                    mBubble.getTaskView().getController(),
                    () -> {
                        mTransition = mTransitions.startTransition(TRANSIT_CHANGE, wct, this);
                        return mTransition;
                    });
        }

        @Override
        public void skip() {
            mBubble.setPreparingTransition(null);
            final TaskViewTaskController tv =
                    mBubble.getTaskView().getController();
            tv.notifyTaskRemovalStarted(tv.getTaskInfo());
            mTaskLeash = null;
        }

        @Override
        public WindowContainerTransaction handleRequest(@NonNull IBinder transition,
                @android.annotation.Nullable TransitionRequestInfo request) {
            return null;
        }

        @Override
        public void mergeAnimation(@NonNull IBinder transition, @NonNull TransitionInfo info,
                @NonNull SurfaceControl.Transaction startT,
                @NonNull SurfaceControl.Transaction finishT,
                @NonNull IBinder mergeTarget,
                @NonNull Transitions.TransitionFinishCallback finishCallback) {
        }

        @Override
        public void onTransitionConsumed(@NonNull IBinder transition, boolean aborted,
                @NonNull SurfaceControl.Transaction finishTransaction) {
            if (!aborted) return;
            mTransition = null;
            skip();
            mTaskViewTransitions.onExternalDone(transition);
        }

        @Override
        public boolean startAnimation(@NonNull IBinder transition,
                @NonNull TransitionInfo info,
                @NonNull SurfaceControl.Transaction startTransaction,
                @NonNull SurfaceControl.Transaction finishTransaction,
                @NonNull Transitions.TransitionFinishCallback finishCallback) {
            if (mTransition != transition) return false;

            final TaskViewTaskController tv =
                    mBubble.getTaskView().getController();
            if (tv == null) {
                mTaskViewTransitions.onExternalDone(transition);
                return false;
            }

            TransitionInfo.Change taskChg = null;

            boolean found = false;
            for (int i = 0; i < info.getChanges().size(); ++i) {
                final TransitionInfo.Change chg = info.getChanges().get(i);
                if (chg.getTaskInfo() == null) continue;
                if (chg.getMode() != TRANSIT_CHANGE) continue;
                if (!mTaskInfo.token.equals(chg.getTaskInfo().token)) continue;
                found = true;
                mRepository.remove(tv);
                taskChg = chg;
                break;
            }

            if (!found) {
                Slog.w(TAG, "Expected a TaskView conversion in this transition but didn't get "
                        + "one, cleaning up the task view");
                tv.setTaskNotFound();
                skip();
                mTaskViewTransitions.onExternalDone(transition);
                return false;
            }

            mTaskLeash = taskChg.getLeash();
            mRootLeash = info.getRoot(0).getLeash();

            SurfaceControl dest = getExpandedView(mBubble).getViewRootImpl().getSurfaceControl();
            final Runnable onPlucked = () -> {
                // Need to remove the taskview AFTER applying the startTransaction because
                // it isn't synchronized.
                tv.notifyTaskRemovalStarted(tv.getTaskInfo());
                // Unset after removeView so it can be used to pick a different animation.
                mBubble.setPreparingTransition(null);
                mBubbleData.setExpanded(false /* expanded */);
            };
            if (dest != null) {
                pluck(mTaskLeash, getExpandedView(mBubble), dest,
                        taskChg.getStartAbsBounds().left - info.getRoot(0).getOffset().x,
                        taskChg.getStartAbsBounds().top - info.getRoot(0).getOffset().y,
                        getCornerRadius(mBubble), startTransaction,
                        onPlucked);
                getExpandedView(mBubble).post(() -> mTransitions.dispatchTransition(
                        mTransition, info, startTransaction, finishTransaction, finishCallback,
                        null));
            } else {
                onPlucked.run();
                mTransitions.dispatchTransition(mTransition, info, startTransaction,
                        finishTransaction, finishCallback, null);
            }

            mTaskViewTransitions.onExternalDone(transition);
            return true;
        }

        @Override
        public void continueCollapse() {
            mBubble.cleanupTaskView();
            if (mTaskLeash == null || !mTaskLeash.isValid() || !mRootLeash.isValid()) return;
            SurfaceControl.Transaction t = new SurfaceControl.Transaction();
            t.reparent(mTaskLeash, mRootLeash);
            t.apply();
        }

        private View getExpandedView(@NonNull Bubble bubble) {
            if (bubble.getBubbleBarExpandedView() != null) {
                return bubble.getBubbleBarExpandedView();
            }
            return bubble.getExpandedView();
        }

        private float getCornerRadius(@NonNull Bubble bubble) {
            if (bubble.getBubbleBarExpandedView() != null) {
                return bubble.getBubbleBarExpandedView().getCornerRadius();
            }
            return bubble.getExpandedView().getCornerRadius();
        }
    }

    /**
     * A transition that converts a dragged bubble icon to a full screen window.
     *
     * <p>This transition assumes that the bubble is invisible so it is simply sent to front.
     */
    class DraggedBubbleIconToFullscreen implements Transitions.TransitionHandler, BubbleTransition {

        IBinder mTransition;
        final Bubble mBubble;
        final Point mDropLocation;
        final TransactionProvider mTransactionProvider;

        DraggedBubbleIconToFullscreen(Bubble bubble, Point dropLocation) {
            this(bubble, dropLocation, SurfaceControl.Transaction::new);
        }

        @VisibleForTesting
        DraggedBubbleIconToFullscreen(Bubble bubble, Point dropLocation,
                TransactionProvider transactionProvider) {
            mBubble = bubble;
            mDropLocation = dropLocation;
            mTransactionProvider = transactionProvider;
            bubble.setPreparingTransition(this);
            WindowContainerToken token = bubble.getTaskView().getTaskInfo().getToken();
            WindowContainerTransaction wct = new WindowContainerTransaction();
            wct.setAlwaysOnTop(token, false);
            wct.setWindowingMode(token, WINDOWING_MODE_UNDEFINED);
            wct.reorder(token, /* onTop= */ true);
            wct.setHidden(token, false);
            mTaskOrganizer.setInterceptBackPressedOnTaskRoot(token, false);
            mTaskViewTransitions.enqueueExternal(bubble.getTaskView().getController(), () -> {
                mTransition = mTransitions.startTransition(TRANSIT_TO_FRONT, wct, this);
                return mTransition;
            });
        }

        @Override
        public void skip() {
            mBubble.setPreparingTransition(null);
        }

        @Override
        public boolean startAnimation(@NonNull IBinder transition, @NonNull TransitionInfo info,
                @NonNull SurfaceControl.Transaction startTransaction,
                @NonNull SurfaceControl.Transaction finishTransaction,
                @NonNull Transitions.TransitionFinishCallback finishCallback) {
            if (mTransition != transition) {
                return false;
            }

            final TaskViewTaskController taskViewTaskController =
                    mBubble.getTaskView().getController();
            if (taskViewTaskController == null) {
                mTaskViewTransitions.onExternalDone(transition);
                finishCallback.onTransitionFinished(null);
                return true;
            }

            TransitionInfo.Change change = findTransitionChange(info);
            if (change == null) {
                Slog.w(TAG, "Expected a TaskView transition to front but didn't find "
                        + "one, cleaning up the task view");
                taskViewTaskController.setTaskNotFound();
                mTaskViewTransitions.onExternalDone(transition);
                finishCallback.onTransitionFinished(null);
                return true;
            }
            mRepository.remove(taskViewTaskController);

            final SurfaceControl taskLeash = change.getLeash();
            // set the initial position of the task with 0 scale
            startTransaction.setPosition(taskLeash, mDropLocation.x, mDropLocation.y);
            startTransaction.setScale(taskLeash, 0, 0);
            startTransaction.apply();

            final SurfaceControl.Transaction animT = mTransactionProvider.get();
            ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
            animator.setDuration(250);
            animator.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(@NonNull Animator animation) {
                    float progress = animator.getAnimatedFraction();
                    float x = mDropLocation.x * (1 - progress);
                    float y = mDropLocation.y * (1 - progress);
                    animT.setPosition(taskLeash, x, y);
                    animT.setScale(taskLeash, progress, progress);
                    animT.apply();
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(@NonNull Animator animation) {
                    animT.close();
                    finishCallback.onTransitionFinished(null);
                }
            });
            animator.start();
            taskViewTaskController.notifyTaskRemovalStarted(mBubble.getTaskView().getTaskInfo());
            mTaskViewTransitions.onExternalDone(transition);
            return true;
        }

        private TransitionInfo.Change findTransitionChange(TransitionInfo info) {
            TransitionInfo.Change result = null;
            WindowContainerToken token = mBubble.getTaskView().getTaskInfo().getToken();
            for (int i = 0; i < info.getChanges().size(); ++i) {
                final TransitionInfo.Change change = info.getChanges().get(i);
                if (change.getTaskInfo() == null) {
                    continue;
                }
                if (change.getMode() != TRANSIT_TO_FRONT) {
                    continue;
                }
                if (!token.equals(change.getTaskInfo().token)) {
                    continue;
                }
                result = change;
                break;
            }
            return result;
        }

        @Override
        public void mergeAnimation(@NonNull IBinder transition, @NonNull TransitionInfo info,
                @NonNull SurfaceControl.Transaction startTransaction,
                @NonNull SurfaceControl.Transaction finishTransaction,
                @NonNull IBinder mergeTarget,
                @NonNull Transitions.TransitionFinishCallback finishCallback) {
        }

        @Override
        public WindowContainerTransaction handleRequest(@NonNull IBinder transition,
                @NonNull TransitionRequestInfo request) {
            return null;
        }

        @Override
        public void onTransitionConsumed(@NonNull IBinder transition, boolean aborted,
                @Nullable SurfaceControl.Transaction finishTransaction) {
            if (!aborted) {
                return;
            }
            mTransition = null;
            mTaskViewTransitions.onExternalDone(transition);
        }
    }

    interface TransactionProvider {
        SurfaceControl.Transaction get();
    }
}
