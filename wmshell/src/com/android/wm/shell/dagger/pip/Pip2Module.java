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

package com.android.wm.shell.dagger.pip;

import android.annotation.NonNull;
import android.content.Context;
import android.os.Handler;
import android.window.DesktopModeFlags;

import com.android.internal.jank.InteractionJankMonitor;
import com.android.wm.shell.RootTaskDisplayAreaOrganizer;
import com.android.wm.shell.ShellTaskOrganizer;
import com.android.wm.shell.common.DisplayController;
import com.android.wm.shell.common.DisplayInsetsController;
import com.android.wm.shell.common.FloatingContentCoordinator;
import com.android.wm.shell.common.ShellExecutor;
import com.android.wm.shell.common.SystemWindows;
import com.android.wm.shell.common.TaskStackListenerImpl;
import com.android.wm.shell.common.pip.PipAppOpsListener;
import com.android.wm.shell.common.pip.PipBoundsAlgorithm;
import com.android.wm.shell.common.pip.PipBoundsState;
import com.android.wm.shell.common.pip.PipDesktopState;
import com.android.wm.shell.common.pip.PipDisplayLayoutState;
import com.android.wm.shell.common.pip.PipMediaController;
import com.android.wm.shell.common.pip.PipPerfHintController;
import com.android.wm.shell.common.pip.PipSnapAlgorithm;
import com.android.wm.shell.common.pip.PipUiEventLogger;
import com.android.wm.shell.common.pip.PipUtils;
import com.android.wm.shell.common.pip.SizeSpecSource;
import com.android.wm.shell.dagger.WMShellBaseModule;
import com.android.wm.shell.dagger.WMSingleton;
import com.android.wm.shell.desktopmode.DesktopPipTransitionController;
import com.android.wm.shell.desktopmode.DesktopTasksController;
import com.android.wm.shell.desktopmode.DesktopUserRepositories;
import com.android.wm.shell.desktopmode.DragToDesktopTransitionHandler;
import com.android.wm.shell.pip2.phone.PhonePipMenuController;
import com.android.wm.shell.pip2.phone.PipController;
import com.android.wm.shell.pip2.phone.PipInteractionHandler;
import com.android.wm.shell.pip2.phone.PipMotionHelper;
import com.android.wm.shell.pip2.phone.PipScheduler;
import com.android.wm.shell.pip2.phone.PipTaskListener;
import com.android.wm.shell.pip2.phone.PipTouchHandler;
import com.android.wm.shell.pip2.phone.PipTransition;
import com.android.wm.shell.pip2.phone.PipTransitionState;
import com.android.wm.shell.pip2.phone.PipUiStateChangeController;
import com.android.wm.shell.shared.annotations.ShellMainThread;
import com.android.wm.shell.shared.desktopmode.DesktopModeStatus;
import com.android.wm.shell.splitscreen.SplitScreenController;
import com.android.wm.shell.sysui.ShellCommandHandler;
import com.android.wm.shell.sysui.ShellController;
import com.android.wm.shell.sysui.ShellInit;
import com.android.wm.shell.transition.Transitions;

import dagger.BindsOptionalOf;
import dagger.Module;
import dagger.Provides;

import java.util.Optional;

/**
 * Provides dependencies from {@link com.android.wm.shell.pip2}, this implementation is meant to be
 * the successor of its sibling {@link Pip1Module}.
 */
@Module(includes = WMShellBaseModule.class)
public abstract class Pip2Module {
    @WMSingleton
    @Provides
    static PipTransition providePipTransition(Context context,
            @NonNull ShellInit shellInit,
            @NonNull ShellTaskOrganizer shellTaskOrganizer,
            @NonNull Transitions transitions,
            PipBoundsState pipBoundsState,
            PipBoundsAlgorithm pipBoundsAlgorithm,
            Optional<PipController> pipController,
            PipTouchHandler pipTouchHandler,
            PipTaskListener pipTaskListener,
            @NonNull PipScheduler pipScheduler,
            @NonNull PipTransitionState pipStackListenerController,
            @NonNull PipDisplayLayoutState pipDisplayLayoutState,
            @NonNull PipUiStateChangeController pipUiStateChangeController,
            DisplayController displayController,
            Optional<SplitScreenController> splitScreenControllerOptional,
            PipDesktopState pipDesktopState,
            Optional<DesktopPipTransitionController> desktopPipTransitionController,
            PipInteractionHandler pipInteractionHandler) {
        return new PipTransition(context, shellInit, shellTaskOrganizer, transitions,
                pipBoundsState, null, pipBoundsAlgorithm, pipTaskListener,
                pipScheduler, pipStackListenerController, pipDisplayLayoutState,
                pipUiStateChangeController, displayController, splitScreenControllerOptional,
                pipDesktopState, desktopPipTransitionController, pipInteractionHandler);
    }

    @WMSingleton
    @Provides
    static Optional<PipController.PipImpl> providePip2(Optional<PipController> pipController) {
        if (pipController.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(pipController.get().getPipImpl());
        }
    }

    @WMSingleton
    @Provides
    static Optional<PipController> providePipController(Context context,
            ShellInit shellInit,
            ShellCommandHandler shellCommandHandler,
            ShellController shellController,
            DisplayController displayController,
            DisplayInsetsController displayInsetsController,
            PipBoundsState pipBoundsState,
            PipBoundsAlgorithm pipBoundsAlgorithm,
            PipDisplayLayoutState pipDisplayLayoutState,
            PipScheduler pipScheduler,
            TaskStackListenerImpl taskStackListener,
            ShellTaskOrganizer shellTaskOrganizer,
            PipTransitionState pipTransitionState,
            PipTouchHandler pipTouchHandler,
            PipAppOpsListener pipAppOpsListener,
            PhonePipMenuController pipMenuController,
            PipUiEventLogger pipUiEventLogger,
            @ShellMainThread ShellExecutor mainExecutor) {
        if (!PipUtils.isPip2ExperimentEnabled()) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(PipController.create(
                    context, shellInit, shellCommandHandler, shellController, displayController,
                    displayInsetsController, pipBoundsState, pipBoundsAlgorithm,
                    pipDisplayLayoutState, pipScheduler, taskStackListener, shellTaskOrganizer,
                    pipTransitionState, pipTouchHandler, pipAppOpsListener, pipMenuController,
                    pipUiEventLogger, mainExecutor));
        }
    }

    @WMSingleton
    @Provides
    static PipScheduler providePipScheduler(Context context,
            PipBoundsState pipBoundsState,
            @ShellMainThread ShellExecutor mainExecutor,
            PipTransitionState pipTransitionState,
            Optional<SplitScreenController> splitScreenControllerOptional,
            PipDesktopState pipDesktopState) {
        return new PipScheduler(context, pipBoundsState, mainExecutor, pipTransitionState,
                splitScreenControllerOptional, pipDesktopState);
    }

    @WMSingleton
    @Provides
    static PhonePipMenuController providePipPhoneMenuController(Context context,
            PipBoundsState pipBoundsState, PipMediaController pipMediaController,
            SystemWindows systemWindows,
            PipUiEventLogger pipUiEventLogger,
            PipTaskListener pipTaskListener,
            @NonNull PipTransitionState pipTransitionState,
            @NonNull PipDisplayLayoutState pipDisplayLayoutState,
            @ShellMainThread ShellExecutor mainExecutor,
            @ShellMainThread Handler mainHandler) {
        return new PhonePipMenuController(context, pipBoundsState, pipMediaController,
                systemWindows, pipUiEventLogger, pipTaskListener, pipTransitionState,
                pipDisplayLayoutState, mainExecutor, mainHandler);
    }


    @WMSingleton
    @Provides
    static PipTouchHandler providePipTouchHandler(Context context,
            ShellInit shellInit,
            ShellCommandHandler shellCommandHandler,
            PhonePipMenuController menuPhoneController,
            PipBoundsAlgorithm pipBoundsAlgorithm,
            @NonNull PipBoundsState pipBoundsState,
            @NonNull PipTransitionState pipTransitionState,
            @NonNull PipScheduler pipScheduler,
            @NonNull SizeSpecSource sizeSpecSource,
            @NonNull PipDisplayLayoutState pipDisplayLayoutState,
            PipDesktopState pipDesktopState,
            DisplayController displayController,
            PipMotionHelper pipMotionHelper,
            FloatingContentCoordinator floatingContentCoordinator,
            PipUiEventLogger pipUiEventLogger,
            @ShellMainThread ShellExecutor mainExecutor,
            Optional<PipPerfHintController> pipPerfHintControllerOptional) {
        return new PipTouchHandler(context, shellInit, shellCommandHandler, menuPhoneController,
                pipBoundsAlgorithm, pipBoundsState, pipTransitionState, pipScheduler,
                sizeSpecSource, pipDisplayLayoutState, pipDesktopState, displayController,
                pipMotionHelper, floatingContentCoordinator, pipUiEventLogger, mainExecutor,
                pipPerfHintControllerOptional);
    }

    @WMSingleton
    @Provides
    static PipMotionHelper providePipMotionHelper(Context context,
            PipBoundsState pipBoundsState, PhonePipMenuController menuController,
            PipSnapAlgorithm pipSnapAlgorithm,
            FloatingContentCoordinator floatingContentCoordinator,
            PipScheduler pipScheduler,
            Optional<PipPerfHintController> pipPerfHintControllerOptional,
            PipTransitionState pipTransitionState,
            PipUiEventLogger pipUiEventLogger) {
        return new PipMotionHelper(context, pipBoundsState, menuController, pipSnapAlgorithm,
                floatingContentCoordinator, pipScheduler, pipPerfHintControllerOptional,
                pipTransitionState, pipUiEventLogger);
    }

    @WMSingleton
    @Provides
    static PipTransitionState providePipTransitionState(@ShellMainThread Handler handler,
            PipDesktopState pipDesktopState) {
        return new PipTransitionState(handler, pipDesktopState);
    }

    @WMSingleton
    @Provides
    static PipUiStateChangeController providePipUiStateChangeController(
            PipTransitionState pipTransitionState) {
        return new PipUiStateChangeController(pipTransitionState);
    }

    @WMSingleton
    @Provides
    static PipTaskListener providePipTaskListener(Context context,
            ShellTaskOrganizer shellTaskOrganizer,
            PipTransitionState pipTransitionState,
            PipScheduler pipScheduler,
            PipBoundsState pipBoundsState,
            PipBoundsAlgorithm pipBoundsAlgorithm,
            @ShellMainThread ShellExecutor mainExecutor) {
        return new PipTaskListener(context, shellTaskOrganizer, pipTransitionState,
                pipScheduler, pipBoundsState, pipBoundsAlgorithm, mainExecutor);
    }

    @WMSingleton
    @Provides
    static PipDesktopState providePipDesktopState(
            PipDisplayLayoutState pipDisplayLayoutState,
            Optional<DesktopUserRepositories> desktopUserRepositoriesOptional,
            Optional<DragToDesktopTransitionHandler> dragToDesktopTransitionHandlerOptional,
            RootTaskDisplayAreaOrganizer rootTaskDisplayAreaOrganizer
    ) {
        return new PipDesktopState(pipDisplayLayoutState, desktopUserRepositoriesOptional,
                dragToDesktopTransitionHandlerOptional, rootTaskDisplayAreaOrganizer);
    }

    @WMSingleton
    @Provides
    static Optional<DesktopPipTransitionController> provideDesktopPipTransitionController(
            Context context, Optional<DesktopTasksController> desktopTasksControllerOptional,
            Optional<DesktopUserRepositories> desktopUserRepositoriesOptional,
            PipDesktopState pipDesktopState
    ) {
        if (DesktopModeStatus.canEnterDesktopMode(context)
                && DesktopModeFlags.ENABLE_DESKTOP_WINDOWING_PIP.isTrue()) {
            return Optional.of(
                    new DesktopPipTransitionController(desktopTasksControllerOptional.get(),
                            desktopUserRepositoriesOptional.get(), pipDesktopState));
        }
        return Optional.empty();
    }

    @BindsOptionalOf
    abstract DragToDesktopTransitionHandler optionalDragToDesktopTransitionHandler();

    @WMSingleton
    @Provides
    static PipInteractionHandler providePipInteractionHandler(
            Context context,
            @ShellMainThread Handler mainHandler
    ) {
        return new PipInteractionHandler(context, mainHandler,
                InteractionJankMonitor.getInstance());
    }
}
