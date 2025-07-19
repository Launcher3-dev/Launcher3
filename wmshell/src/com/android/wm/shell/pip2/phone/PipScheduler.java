/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.wm.shell.pip2.phone;

import android.app.PictureInPictureParams;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.SurfaceControl;
import android.window.WindowContainerToken;
import android.window.WindowContainerTransaction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.protolog.ProtoLog;
import com.android.wm.shell.common.ScreenshotUtils;
import com.android.wm.shell.common.ShellExecutor;
import com.android.wm.shell.common.pip.PipBoundsState;
import com.android.wm.shell.common.pip.PipDesktopState;
import com.android.wm.shell.pip.PipTransitionController;
import com.android.wm.shell.pip2.PipSurfaceTransactionHelper;
import com.android.wm.shell.pip2.animation.PipAlphaAnimator;
import com.android.wm.shell.protolog.ShellProtoLogGroup;
import com.android.wm.shell.shared.split.SplitScreenConstants;
import com.android.wm.shell.splitscreen.SplitScreenController;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Scheduler for Shell initiated PiP transitions and animations.
 */
public class PipScheduler implements PipTransitionState.PipTransitionStateChangedListener {
    private static final String TAG = PipScheduler.class.getSimpleName();

    /**
     * The fixed start delay in ms when fading out the content overlay from bounds animation.
     * The fadeout animation is guaranteed to start after the client has drawn under the new config.
     */
    public static final int EXTRA_CONTENT_OVERLAY_FADE_OUT_DELAY_MS =
            SystemProperties.getInt(
                    "persist.wm.debug.extra_content_overlay_fade_out_delay_ms", 400);
    private static final int CONTENT_OVERLAY_FADE_OUT_DURATION_MS = 500;

    private final Context mContext;
    private final PipBoundsState mPipBoundsState;
    private final ShellExecutor mMainExecutor;
    private final PipTransitionState mPipTransitionState;
    private final PipDesktopState mPipDesktopState;
    private final Optional<SplitScreenController> mSplitScreenControllerOptional;
    private PipTransitionController mPipTransitionController;
    private PipSurfaceTransactionHelper.SurfaceControlTransactionFactory
            mSurfaceControlTransactionFactory;
    private final PipSurfaceTransactionHelper mPipSurfaceTransactionHelper;

    @Nullable private Runnable mUpdateMovementBoundsRunnable;
    @Nullable private PipAlphaAnimator mOverlayFadeoutAnimator;

    private PipAlphaAnimatorSupplier mPipAlphaAnimatorSupplier;
    private Supplier<PictureInPictureParams> mPipParamsSupplier;

    public PipScheduler(Context context,
            PipBoundsState pipBoundsState,
            ShellExecutor mainExecutor,
            PipTransitionState pipTransitionState,
            Optional<SplitScreenController> splitScreenControllerOptional,
            PipDesktopState pipDesktopState) {
        mContext = context;
        mPipBoundsState = pipBoundsState;
        mMainExecutor = mainExecutor;
        mPipTransitionState = pipTransitionState;
        mPipTransitionState.addPipTransitionStateChangedListener(this);
        mPipDesktopState = pipDesktopState;
        mSplitScreenControllerOptional = splitScreenControllerOptional;

        mSurfaceControlTransactionFactory =
                new PipSurfaceTransactionHelper.VsyncSurfaceControlTransactionFactory();
        mPipSurfaceTransactionHelper = new PipSurfaceTransactionHelper(mContext);
        mPipAlphaAnimatorSupplier = PipAlphaAnimator::new;
    }

    void setPipTransitionController(PipTransitionController pipTransitionController) {
        mPipTransitionController = pipTransitionController;
    }

    @Nullable
    private WindowContainerTransaction getExitPipViaExpandTransaction() {
        WindowContainerToken pipTaskToken = mPipTransitionState.getPipTaskToken();
        if (pipTaskToken == null) {
            return null;
        }
        WindowContainerTransaction wct = new WindowContainerTransaction();
        // final expanded bounds to be inherited from the parent
        wct.setBounds(pipTaskToken, null);
        // if we are hitting a multi-activity case
        // windowing mode change will reparent to original host task
        wct.setWindowingMode(pipTaskToken, mPipDesktopState.getOutPipWindowingMode());
        return wct;
    }

    /**
     * Schedules exit PiP via expand transition.
     */
    public void scheduleExitPipViaExpand() {
        mMainExecutor.execute(() -> {
            if (!mPipTransitionState.isInPip()) return;

            final WindowContainerTransaction expandWct = getExitPipViaExpandTransaction();
            if (expandWct == null) return;

            final WindowContainerTransaction wct = new WindowContainerTransaction();
            mSplitScreenControllerOptional.ifPresent(splitScreenController -> {
                int lastParentTaskId = mPipTransitionState.getPipTaskInfo()
                        .lastParentTaskIdBeforePip;
                if (splitScreenController.isTaskInSplitScreen(lastParentTaskId)) {
                    splitScreenController.prepareEnterSplitScreen(wct,
                            null /* taskInfo */, SplitScreenConstants.SPLIT_POSITION_UNDEFINED);
                }
            });

            boolean toSplit = !wct.isEmpty();
            wct.merge(expandWct, true /* transfer */);
            mPipTransitionController.startExpandTransition(wct, toSplit);
        });
    }

    /** Schedules remove PiP transition. */
    public void scheduleRemovePip(boolean withFadeout) {
        mMainExecutor.execute(() -> {
            if (!mPipTransitionState.isInPip()) return;
            mPipTransitionController.startRemoveTransition(withFadeout);
        });
    }

    /**
     * Animates resizing of the pinned stack given the duration.
     */
    public void scheduleAnimateResizePip(Rect toBounds) {
        scheduleAnimateResizePip(toBounds, false /* configAtEnd */);
    }

    /**
     * Animates resizing of the pinned stack given the duration.
     *
     * @param configAtEnd true if we are delaying config updates until the transition ends.
     */
    public void scheduleAnimateResizePip(Rect toBounds, boolean configAtEnd) {
        scheduleAnimateResizePip(toBounds, configAtEnd,
                PipTransition.BOUNDS_CHANGE_JUMPCUT_DURATION);
    }

    /**
     * Animates resizing of the pinned stack given the duration.
     *
     * @param configAtEnd true if we are delaying config updates until the transition ends.
     * @param duration    the suggested duration to run the animation; the component responsible
     *                    for running the animator will get this as an extra.
     */
    public void scheduleAnimateResizePip(Rect toBounds, boolean configAtEnd, int duration) {
        WindowContainerToken pipTaskToken = mPipTransitionState.getPipTaskToken();
        if (pipTaskToken == null || !mPipTransitionState.isInPip()) {
            return;
        }
        WindowContainerTransaction wct = new WindowContainerTransaction();
        if (configAtEnd) {
            wct.deferConfigToTransitionEnd(pipTaskToken);

            if (mPipBoundsState.getBounds().width() == toBounds.width()
                    && mPipBoundsState.getBounds().height() == toBounds.height()) {
                // TODO (b/393159816): Config-at-End causes a flicker without size change.
                // If PiP size isn't changing enforce a minimal one-pixel change as a workaround.
                --toBounds.bottom;
            }
        }
        wct.setBounds(pipTaskToken, toBounds);
        mPipTransitionController.startResizeTransition(wct, duration);
    }

    /**
     * Signals to Core to finish the PiP resize transition.
     * Note that we do not allow any actual WM Core changes at this point.
     *
     * @param toBounds destination bounds used only for internal state updates - not sent to Core.
     */
    public void scheduleFinishResizePip(Rect toBounds) {
        // Make updates to the internal state to reflect new bounds before updating any transitions
        // related state; transition state updates can trigger callbacks that use the cached bounds.
        onFinishingPipResize(toBounds);
        mPipTransitionController.finishTransition();
    }

    /**
     * Directly perform a scaled matrix transformation on the leash. This will not perform any
     * {@link WindowContainerTransaction}.
     */
    public void scheduleUserResizePip(Rect toBounds) {
        scheduleUserResizePip(toBounds, 0f /* degrees */);
    }

    /**
     * Directly perform a scaled matrix transformation on the leash. This will not perform any
     * {@link WindowContainerTransaction}.
     *
     * @param degrees the angle to rotate the bounds to.
     */
    public void scheduleUserResizePip(Rect toBounds, float degrees) {
        if (toBounds.isEmpty() || !mPipTransitionState.isInPip()) {
            ProtoLog.w(ShellProtoLogGroup.WM_SHELL_PICTURE_IN_PICTURE,
                    "%s: Attempted to user resize PIP in invalid state, aborting;"
                            + "toBounds=%s, mPipTransitionState=%s",
                    TAG, toBounds, mPipTransitionState);
            return;
        }
        SurfaceControl leash = mPipTransitionState.getPinnedTaskLeash();
        final SurfaceControl.Transaction tx = mSurfaceControlTransactionFactory.getTransaction();

        Matrix transformTensor = new Matrix();
        final float[] mMatrixTmp = new float[9];
        final float scale = (float) toBounds.width() / mPipBoundsState.getBounds().width();

        transformTensor.setScale(scale, scale);
        transformTensor.postTranslate(toBounds.left, toBounds.top);
        transformTensor.postRotate(degrees, toBounds.centerX(), toBounds.centerY());

        mPipSurfaceTransactionHelper.round(tx, leash, mPipBoundsState.getBounds(), toBounds);

        tx.setMatrix(leash, transformTensor, mMatrixTmp);
        tx.apply();
    }

    void startOverlayFadeoutAnimation(@NonNull SurfaceControl overlayLeash,
            boolean withStartDelay, @NonNull Runnable onAnimationEnd) {
        mOverlayFadeoutAnimator = mPipAlphaAnimatorSupplier.get(mContext, overlayLeash,
                null /* startTx */, null /* finishTx */, PipAlphaAnimator.FADE_OUT);
        mOverlayFadeoutAnimator.setDuration(CONTENT_OVERLAY_FADE_OUT_DURATION_MS);
        mOverlayFadeoutAnimator.setStartDelay(withStartDelay
                ? EXTRA_CONTENT_OVERLAY_FADE_OUT_DELAY_MS : 0);
        mOverlayFadeoutAnimator.setAnimationEndCallback(() -> {
            onAnimationEnd.run();
            mOverlayFadeoutAnimator = null;
        });
        mOverlayFadeoutAnimator.start();
    }

    void setUpdateMovementBoundsRunnable(@Nullable Runnable updateMovementBoundsRunnable) {
        mUpdateMovementBoundsRunnable = updateMovementBoundsRunnable;
    }

    private void maybeUpdateMovementBounds() {
        if (mUpdateMovementBoundsRunnable != null)  {
            mUpdateMovementBoundsRunnable.run();
        }
    }

    private void onFinishingPipResize(Rect newBounds) {
        if (mPipBoundsState.getBounds().equals(newBounds)) {
            return;
        }

        // Take a screenshot of PiP and fade it out after resize is finished if seamless resize
        // is off and if the PiP size is changing.
        boolean animateCrossFadeResize = !getPipParams().isSeamlessResizeEnabled()
                && !(mPipBoundsState.getBounds().width() == newBounds.width()
                && mPipBoundsState.getBounds().height() == newBounds.height());
        if (animateCrossFadeResize) {
            final Rect crop = new Rect(newBounds);
            crop.offsetTo(0, 0);
            // Note: Put this at layer=MAX_VALUE-2 since the input consumer for PIP is placed at
            //       MAX_VALUE-1
            final SurfaceControl snapshotSurface = ScreenshotUtils.takeScreenshot(
                    mSurfaceControlTransactionFactory.getTransaction(),
                    mPipTransitionState.getPinnedTaskLeash(), crop, Integer.MAX_VALUE - 2);
            startOverlayFadeoutAnimation(snapshotSurface, false /* withStartDelay */, () -> {
                mSurfaceControlTransactionFactory.getTransaction().remove(snapshotSurface).apply();
            });
        }

        mPipBoundsState.setBounds(newBounds);
        maybeUpdateMovementBounds();
    }

    @VisibleForTesting
    void setSurfaceControlTransactionFactory(
            @NonNull PipSurfaceTransactionHelper.SurfaceControlTransactionFactory factory) {
        mSurfaceControlTransactionFactory = factory;
    }

    @Override
    public void onPipTransitionStateChanged(@PipTransitionState.TransitionState int oldState,
            @PipTransitionState.TransitionState int newState,
            @android.annotation.Nullable Bundle extra) {
        switch (newState) {
            case PipTransitionState.EXITING_PIP:
            case PipTransitionState.SCHEDULED_BOUNDS_CHANGE:
                if (mOverlayFadeoutAnimator != null && mOverlayFadeoutAnimator.isStarted()) {
                    mOverlayFadeoutAnimator.end();
                    mOverlayFadeoutAnimator = null;
                }
                break;
        }
    }

    @VisibleForTesting
    interface PipAlphaAnimatorSupplier {
        PipAlphaAnimator get(@NonNull Context context,
                SurfaceControl leash,
                SurfaceControl.Transaction startTransaction,
                SurfaceControl.Transaction finishTransaction,
                @PipAlphaAnimator.Fade int direction);
    }

    @VisibleForTesting
    void setPipAlphaAnimatorSupplier(@NonNull PipAlphaAnimatorSupplier supplier) {
        mPipAlphaAnimatorSupplier = supplier;
    }

    @VisibleForTesting
    void setOverlayFadeoutAnimator(@NonNull PipAlphaAnimator animator) {
        mOverlayFadeoutAnimator = animator;
    }

    @VisibleForTesting
    @Nullable
    PipAlphaAnimator getOverlayFadeoutAnimator() {
        return mOverlayFadeoutAnimator;
    }

    void setPipParamsSupplier(@NonNull Supplier<PictureInPictureParams> pipParamsSupplier) {
        mPipParamsSupplier = pipParamsSupplier;
    }

    @NonNull
    private PictureInPictureParams getPipParams() {
        if (mPipParamsSupplier == null) return new PictureInPictureParams.Builder().build();
        return mPipParamsSupplier.get();
    }
}
