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

package com.android.wm.shell.shared.bubbles

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.graphics.toColorInt
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.android.wm.shell.shared.bubbles.DragZoneFactory.DesktopWindowModeChecker
import com.android.wm.shell.shared.bubbles.DragZoneFactory.SplitScreenModeChecker
import com.android.wm.shell.shared.bubbles.DragZoneFactory.SplitScreenModeChecker.SplitScreenMode
import com.android.wm.shell.testing.goldenpathmanager.WMShellGoldenPathManager
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.runner.parameterized.ParameterizedAndroidJunit4
import platform.test.runner.parameterized.Parameters
import platform.test.screenshot.DeviceEmulationSpec
import platform.test.screenshot.Displays
import platform.test.screenshot.ViewScreenshotTestRule
import platform.test.screenshot.ViewScreenshotTestRule.Mode
import platform.test.screenshot.getEmulatedDevicePathConfig

@RunWith(ParameterizedAndroidJunit4::class)
class DragZoneFactoryScreenshotTest(private val param: Param) {
    companion object {
        @Parameters(name = "{0}")
        @JvmStatic
        fun getTestSpecs(): List<Param> {
            val params = mutableListOf<Param>()
            val draggedObjects =
                listOf(
                    DraggedObject.Bubble(BubbleBarLocation.LEFT),
                    DraggedObject.BubbleBar(BubbleBarLocation.LEFT),
                    DraggedObject.ExpandedView(BubbleBarLocation.LEFT),
                )
            DeviceEmulationSpec.forDisplays(Displays.Tablet, isDarkTheme = false).forEach { tablet
                ->
                draggedObjects.forEach { draggedObject ->
                    params.add(Param(tablet, draggedObject, SplitScreenMode.NONE))
                }
            }
            DeviceEmulationSpec.forDisplays(Displays.FoldableInner, isDarkTheme = false).forEach {
                foldable ->
                draggedObjects.forEach { draggedObject ->
                    params.add(Param(foldable, draggedObject, SplitScreenMode.NONE))
                    val isBubble = draggedObject is DraggedObject.Bubble
                    val isExpandedView = draggedObject is DraggedObject.ExpandedView
                    val addMoreSplitModes = isBubble || (isExpandedView && foldable.isLandscape)
                    if (addMoreSplitModes) {
                        params.add(Param(foldable, draggedObject, SplitScreenMode.SPLIT_10_90))
                        params.add(Param(foldable, draggedObject, SplitScreenMode.SPLIT_90_10))
                    }
                }
            }
            return params
        }
    }

    class Param(
        val emulationSpec: DeviceEmulationSpec,
        val draggedObject: DraggedObject,
        val splitScreenMode: SplitScreenMode
    ) {
        private val draggedObjectName =
            when (draggedObject) {
                is DraggedObject.Bubble -> "bubble"
                is DraggedObject.BubbleBar -> "bubbleBar"
                is DraggedObject.ExpandedView -> "expandedView"
            }

        private val splitScreenModeName =
            when (splitScreenMode) {
                SplitScreenMode.UNSUPPORTED -> "_split_unsupported"
                SplitScreenMode.NONE -> ""
                SplitScreenMode.SPLIT_50_50 -> "_split_50_50"
                SplitScreenMode.SPLIT_10_90 -> "_split_10_90"
                SplitScreenMode.SPLIT_90_10 -> "_split_90_10"
            }

        val testName = "$draggedObjectName$splitScreenModeName"

        override fun toString() = "${emulationSpec}_$testName"
    }

    @get:Rule
    val screenshotRule =
        ViewScreenshotTestRule(
            param.emulationSpec,
            WMShellGoldenPathManager(getEmulatedDevicePathConfig(param.emulationSpec))
        )

    private val context = getApplicationContext<Context>()

    @Test
    fun dragZones() {
        screenshotRule.screenshotTest("dragZones_${param.testName}", mode = Mode.MatchSize) {
            activity ->
            activity.actionBar?.hide()
            val dragZoneFactory = createDragZoneFactory()
            val dragZones = dragZoneFactory.createSortedDragZones(param.draggedObject)
            val container = FrameLayout(context)
            dragZones.forEach { zone -> container.addZoneView(zone) }
            container
        }
    }

    private fun createDragZoneFactory(): DragZoneFactory {
        val deviceConfig =
            DeviceConfig.create(context, context.getSystemService(WindowManager::class.java)!!)
        val splitScreenModeChecker = SplitScreenModeChecker { param.splitScreenMode }
        val desktopWindowModeChecker = DesktopWindowModeChecker { true }
        return DragZoneFactory(
            context,
            deviceConfig,
            splitScreenModeChecker,
            desktopWindowModeChecker
        )
    }

    private fun FrameLayout.addZoneView(zone: DragZone) {
        val view = View(context)
        this.addView(view, 0)
        view.layoutParams = FrameLayout.LayoutParams(zone.bounds.width(), zone.bounds.height())
        view.background = createZoneDrawable(zone.color)
        view.x = zone.bounds.left.toFloat()
        view.y = zone.bounds.top.toFloat()
    }

    private fun createZoneDrawable(@ColorInt color: Int): Drawable {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.setColor(Color.argb(128, color.red, color.green, color.blue))
        shape.setStroke(2, color)
        return shape
    }

    private val DragZone.color: Int
        @ColorInt
        get() =
            when (this) {
                is DragZone.Bubble -> "#3F5C8B".toColorInt()
                is DragZone.Dismiss -> "#8B3F3F".toColorInt()
                is DragZone.Split -> "#89B675".toColorInt()
                is DragZone.FullScreen -> "#4ED075".toColorInt()
                is DragZone.DesktopWindow -> "#EC928E".toColorInt()
            }
}
