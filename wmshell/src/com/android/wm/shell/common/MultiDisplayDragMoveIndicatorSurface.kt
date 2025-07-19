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
package com.android.wm.shell.common

import android.app.ActivityManager.RunningTaskInfo
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Trace
import android.view.Display
import android.view.SurfaceControl
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb
import com.android.wm.shell.RootTaskDisplayAreaOrganizer
import com.android.wm.shell.windowdecor.common.DecorThemeUtil
import com.android.wm.shell.windowdecor.common.Theme

/**
 * Represents the indicator surface that visualizes the current position of a dragged window during
 * a multi-display drag operation.
 *
 * This class manages the creation, display, and manipulation of the [SurfaceControl]s that act as a
 * visual indicator, providing feedback to the user about the dragged window's location.
 */
class MultiDisplayDragMoveIndicatorSurface(
    context: Context,
    taskInfo: RunningTaskInfo,
    display: Display,
    surfaceControlBuilderFactory: Factory.SurfaceControlBuilderFactory,
) {
    private var isVisible = false

    // A container surface to host the veil background
    private var veilSurface: SurfaceControl? = null

    private val decorThemeUtil = DecorThemeUtil(context)
    private val lightColors = dynamicLightColorScheme(context)
    private val darkColors = dynamicDarkColorScheme(context)

    init {
        Trace.beginSection("DragIndicatorSurface#init")

        val displayId = display.displayId
        veilSurface =
            surfaceControlBuilderFactory
                .create("Drag indicator veil of Task=${taskInfo.taskId} Display=$displayId")
                .setColorLayer()
                .setCallsite("DragIndicatorSurface#init")
                .setHidden(true)
                .build()

        // TODO: b/383069173 - Add icon for the surface.

        Trace.endSection()
    }

    /**
     * Disposes the indicator surface using the provided [transaction].
     */
    fun disposeSurface(transaction: SurfaceControl.Transaction) {
        veilSurface?.let { veil -> transaction.remove(veil) }
        veilSurface = null
    }

    /**
     * Shows the indicator surface at [bounds] on the specified display ([displayId]),
     * visualizing the drag of the [taskInfo]. The indicator surface is shown using [transaction],
     * and the [rootTaskDisplayAreaOrganizer] is used to reparent the surfaces.
     */
    fun show(
        transaction: SurfaceControl.Transaction,
        taskInfo: RunningTaskInfo,
        rootTaskDisplayAreaOrganizer: RootTaskDisplayAreaOrganizer,
        displayId: Int,
        bounds: Rect,
    ) {
        val backgroundColor =
            when (decorThemeUtil.getAppTheme(taskInfo)) {
                Theme.LIGHT -> lightColors.surfaceContainer
                Theme.DARK -> darkColors.surfaceContainer
            }
        val veil = veilSurface ?: return
        isVisible = true

        rootTaskDisplayAreaOrganizer.reparentToDisplayArea(displayId, veil, transaction)
        relayout(bounds, transaction, shouldBeVisible = true)
        transaction.show(veil).setColor(veil, Color.valueOf(backgroundColor.toArgb()).components)
        transaction.apply()
    }

    /**
     * Repositions and resizes the indicator surface based on [bounds] using [transaction]. The
     * [shouldBeVisible] flag indicates whether the indicator is within the display after relayout.
     */
    fun relayout(bounds: Rect, transaction: SurfaceControl.Transaction, shouldBeVisible: Boolean) {
        if (!isVisible && !shouldBeVisible) {
            // No need to relayout if the surface is already invisible and should not be visible.
            return
        }
        isVisible = shouldBeVisible
        val veil = veilSurface ?: return
        transaction.setCrop(veil, bounds)
    }

    /**
     * Factory for creating [MultiDisplayDragMoveIndicatorSurface] instances with the [context].
     */
    class Factory(private val context: Context) {
        private val surfaceControlBuilderFactory: SurfaceControlBuilderFactory =
            object : SurfaceControlBuilderFactory {}

        /**
         * Creates a new [MultiDisplayDragMoveIndicatorSurface] instance to visualize the drag
         * operation of the [taskInfo] on the given [display].
         */
        fun create(
            taskInfo: RunningTaskInfo,
            display: Display,
        ) = MultiDisplayDragMoveIndicatorSurface(
            context,
            taskInfo,
            display,
            surfaceControlBuilderFactory,
        )

        /**
         * Interface for creating [SurfaceControl.Builder] instances.
         *
         * This provides an abstraction over [SurfaceControl.Builder] creation for testing purposes.
         */
        interface SurfaceControlBuilderFactory {
            fun create(name: String): SurfaceControl.Builder {
                return SurfaceControl.Builder().setName(name)
            }
        }
    }
}
