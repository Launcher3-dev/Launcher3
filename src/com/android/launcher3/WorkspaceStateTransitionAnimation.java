/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.launcher3;

import android.view.View;
import android.view.animation.Interpolator;

import com.android.launcher3.LauncherState.PageAlphaProvider;
import com.android.launcher3.LauncherStateManager.AnimationConfig;
import com.android.launcher3.anim.AnimatorSetBuilder;
import com.android.launcher3.anim.PropertySetter;
import com.android.launcher3.graphics.WorkspaceAndHotseatScrim;
import com.android.mxlibrary.util.XLog;

import static com.android.launcher3.LauncherAnimUtils.DRAWABLE_ALPHA;
import static com.android.launcher3.LauncherAnimUtils.SCALE_PROPERTY;
import static com.android.launcher3.LauncherState.HOTSEAT_ICONS;
import static com.android.launcher3.LauncherState.HOTSEAT_SEARCH_BOX;
import static com.android.launcher3.anim.AnimatorSetBuilder.ANIM_WORKSPACE_FADE;
import static com.android.launcher3.anim.AnimatorSetBuilder.ANIM_WORKSPACE_SCALE;
import static com.android.launcher3.anim.Interpolators.LINEAR;
import static com.android.launcher3.anim.Interpolators.ZOOM_OUT;
import static com.android.launcher3.anim.PropertySetter.NO_ANIM_PROPERTY_SETTER;
import static com.android.launcher3.graphics.WorkspaceAndHotseatScrim.SCRIM_PROGRESS;
import static com.android.launcher3.graphics.WorkspaceAndHotseatScrim.SYSUI_PROGRESS;

/**
 * Manages the animations between each of the workspace states.
 */
public class WorkspaceStateTransitionAnimation {

    private final Launcher mLauncher;
    private final Workspace mWorkspace;

    public final int mOverviewTransitionTime = 300;

    private float mNewScale;

    public WorkspaceStateTransitionAnimation(Launcher launcher, Workspace workspace) {
        mLauncher = launcher;
        mWorkspace = workspace;
    }

    public void setState(LauncherState toState) {
        setWorkspaceProperty(toState, NO_ANIM_PROPERTY_SETTER, new AnimatorSetBuilder(),
                new AnimationConfig());
    }

    public void setStateWithAnimation(LauncherState toState, AnimatorSetBuilder builder,
                                      AnimationConfig config) {
        setWorkspaceProperty(toState, config.getPropertySetter(builder), builder, config);
    }

    public float getFinalScale() {
        return mNewScale;
    }

    /**
     * Starts a transition animation for the workspace.
     */
    private void setWorkspaceProperty(LauncherState state, PropertySetter propertySetter,
                                      AnimatorSetBuilder builder, AnimationConfig config) {
        float[] scaleAndTranslation = state.getWorkspaceScaleAndTranslation(mLauncher);
        mNewScale = scaleAndTranslation[0];
        // modify by comdex.cn ---20181027-------start
//        PageAlphaProvider pageAlphaProvider = state.getWorkspacePageAlphaProvider(mLauncher);
        LauncherState.PageScaleProvider pageScaleProvider = state.getWorkspacePageScaleProvider(mLauncher);
        final int childCount = mWorkspace.getChildCount();
        for (int i = 0; i < childCount; i++) {
//            applyChildStateAlpha(state, (CellLayout) mWorkspace.getChildAt(i), i, pageAlphaProvider,
//                    propertySetter, builder, config);
            applyChildStateScale(state, (CellLayout) mWorkspace.getChildAt(i), i, pageScaleProvider,
                    propertySetter, builder, config);
        }
        // modify by comdex.cn ---20181027-------end

        int elements = state.getVisibleElements(mLauncher);
        Interpolator fadeInterpolator = builder.getInterpolator(ANIM_WORKSPACE_FADE,
                pageScaleProvider.interpolator);
        boolean playAtomicComponent = config.playAtomicComponent();
        if (playAtomicComponent) {
            Interpolator scaleInterpolator = builder.getInterpolator(ANIM_WORKSPACE_SCALE, ZOOM_OUT);
            propertySetter.setFloat(mWorkspace, SCALE_PROPERTY, mNewScale, scaleInterpolator);
            float hotseatIconsAlpha = (elements & HOTSEAT_ICONS) != 0 ? 1 : 0;
            propertySetter.setViewAlpha(mLauncher.getHotseat().getLayout(), hotseatIconsAlpha,
                    fadeInterpolator);
            propertySetter.setViewAlpha(mLauncher.getWorkspace().getPageIndicator(),
                    hotseatIconsAlpha, fadeInterpolator);
        }

        if (!config.playNonAtomicComponent()) {
            // Only the alpha and scale, handled above, are included in the atomic animation.
            return;
        }

        Interpolator translationInterpolator = !playAtomicComponent ? LINEAR : ZOOM_OUT;
        propertySetter.setFloat(mWorkspace, View.TRANSLATION_X,
                scaleAndTranslation[1], translationInterpolator);
        propertySetter.setFloat(mWorkspace, View.TRANSLATION_Y,
                scaleAndTranslation[2], translationInterpolator);

        propertySetter.setViewAlpha(mLauncher.getHotseatSearchBox(),
                (elements & HOTSEAT_SEARCH_BOX) != 0 ? 1 : 0, fadeInterpolator);

        // Set scrim
        WorkspaceAndHotseatScrim scrim = mLauncher.getDragLayer().getScrim();
        propertySetter.setFloat(scrim, SCRIM_PROGRESS, state.getWorkspaceScrimAlpha(mLauncher),
                LINEAR);
        propertySetter.setFloat(scrim, SYSUI_PROGRESS, state.hasSysUiScrim ? 1 : 0, LINEAR);
    }

    public void applyChildStateAlpha(LauncherState state, CellLayout cl, int childIndex) {
        applyChildStateAlpha(state, cl, childIndex, state.getWorkspacePageAlphaProvider(mLauncher),
                NO_ANIM_PROPERTY_SETTER, new AnimatorSetBuilder(), new AnimationConfig());
    }

    /**
     * 单个CellLayout的Alpha动画
     *
     * @param state             目标状态
     * @param cl                CellLayout
     * @param childIndex        CellLayout的Index
     * @param pageAlphaProvider Alpha值速度值
     * @param propertySetter    Property设置值
     * @param builder           动画集合构造器
     * @param config            动画配置
     */
    private void applyChildStateAlpha(LauncherState state, CellLayout cl, int childIndex,
                                      PageAlphaProvider pageAlphaProvider, PropertySetter propertySetter,
                                      AnimatorSetBuilder builder, AnimationConfig config) {
        float pageAlpha = pageAlphaProvider.getPageAlpha(childIndex);
        int drawableAlpha = Math.round(pageAlpha * (state.hasWorkspacePageBackground ? 255 : 0));

        if (config.playNonAtomicComponent()) {
            propertySetter.setInt(cl.getScrimBackground(),
                    DRAWABLE_ALPHA, drawableAlpha, ZOOM_OUT);
        }
        if (config.playAtomicComponent()) {
            Interpolator fadeInterpolator = builder.getInterpolator(ANIM_WORKSPACE_FADE,
                    pageAlphaProvider.interpolator);
            propertySetter.setFloat(cl.getShortcutsAndWidgets(), View.ALPHA,
                    pageAlpha, fadeInterpolator);
        }
    }

    /**
     * 单个CellLayout的Scale动画
     *
     * @param state             目标状态
     * @param cl                CellLayout
     * @param childIndex        CellLayout的Index
     * @param pageScaleProvider Alpha值速度值
     * @param propertySetter    Property设置值
     * @param builder           动画集合构造器
     * @param config            动画配置
     */
    private void applyChildStateScale(LauncherState state, CellLayout cl, int childIndex,
                                      LauncherState.PageScaleProvider pageScaleProvider, PropertySetter propertySetter,
                                      AnimatorSetBuilder builder, AnimationConfig config) {
        float pageScale;
        if (state == LauncherState.NORMAL) {
            pageScale = 1.0f;
        } else {
            pageScale = pageScaleProvider.getPageScale(childIndex);
        }
        if (config.playNonAtomicComponent()) {
            propertySetter.setFloat(cl, SCALE_PROPERTY, pageScale, ZOOM_OUT);
        }
        if (config.playAtomicComponent()) {
            Interpolator scaleInterpolator = builder.getInterpolator(ANIM_WORKSPACE_SCALE,
                    pageScaleProvider.interpolator);
            propertySetter.setFloat(cl.getShortcutsAndWidgets(), View.SCALE_X,
                    pageScale, scaleInterpolator);
            propertySetter.setFloat(cl.getShortcutsAndWidgets(), View.SCALE_Y,
                    pageScale, scaleInterpolator);
        }
    }
}

/**
 * Stores the transition states for convenience.
 */
class TransitionStates {

    // Raw states
    final boolean oldStateIsNormal;
    final boolean oldStateIsEditing;
    final boolean oldStateIsSpringLoaded;
    final boolean oldStateIsOverview;

    final boolean stateIsNormal;
    final boolean stateIsEditing;
    final boolean stateIsSpringLoaded;
    final boolean stateIsOverview;

    // Convenience members
    final boolean workspaceToOverview;
    final boolean overviewToWorkspace;
    final boolean workspaceToEditing;
    final boolean editingToWorkspace;
    final boolean workspaceToSpringLoaded;
    final boolean springLoadedToEditing;


    public TransitionStates(final LauncherState fromState, final LauncherState toState) {
        XLog.e(XLog.getTag(), XLog.TAG_GU + "fromState:  " + fromState.containerType + "  ---   toState:  " + toState.containerType);
        oldStateIsNormal = (fromState == LauncherState.NORMAL);
        oldStateIsEditing = (fromState == LauncherState.EDITING);
        oldStateIsSpringLoaded = (fromState == LauncherState.SPRING_LOADED);
        oldStateIsOverview = (fromState == LauncherState.OVERVIEW);

        stateIsNormal = (toState == LauncherState.NORMAL);
        stateIsEditing = (toState == LauncherState.EDITING);
        stateIsSpringLoaded = (toState == LauncherState.SPRING_LOADED);
        stateIsOverview = (toState == LauncherState.OVERVIEW);

        workspaceToOverview = (oldStateIsNormal && stateIsOverview);
        overviewToWorkspace = (oldStateIsOverview && stateIsNormal);
        workspaceToEditing = (oldStateIsNormal && stateIsEditing);
        editingToWorkspace = (oldStateIsEditing && stateIsNormal);
        workspaceToSpringLoaded = (oldStateIsNormal && stateIsSpringLoaded);
        springLoadedToEditing = (oldStateIsSpringLoaded && stateIsEditing);
    }
}