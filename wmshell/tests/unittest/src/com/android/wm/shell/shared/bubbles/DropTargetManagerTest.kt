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
import android.graphics.Rect
import android.widget.FrameLayout
import androidx.core.animation.AnimatorTestRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFails
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for [DropTargetManager]. */
@SmallTest
@RunWith(AndroidJUnit4::class)
class DropTargetManagerTest {

    @get:Rule val animatorTestRule = AnimatorTestRule()

    private val context = getApplicationContext<Context>()
    private lateinit var dropTargetManager: DropTargetManager
    private lateinit var dragZoneChangedListener: FakeDragZoneChangedListener
    private lateinit var container: FrameLayout

    // create 3 drop zones that are horizontally next to each other
    // -------------------------------------------------
    // |               |               |               |
    // |    bubble     |               |    bubble     |
    // |               |    dismiss    |               |
    // |     left      |               |     right     |
    // |               |               |               |
    // -------------------------------------------------
    private val bubbleLeftDragZone =
        DragZone.Bubble.Left(bounds = Rect(0, 0, 100, 100), dropTarget = Rect(0, 0, 50, 200))
    private val dismissDragZone = DragZone.Dismiss(bounds = Rect(100, 0, 200, 100))
    private val bubbleRightDragZone =
        DragZone.Bubble.Right(bounds = Rect(200, 0, 300, 100), dropTarget = Rect(200, 0, 280, 150))

    private val dropTargetView: DropTargetView
        get() = container.getChildAt(0) as DropTargetView

    @Before
    fun setUp() {
        container = FrameLayout(context)
        dragZoneChangedListener = FakeDragZoneChangedListener()
        dropTargetManager =
            DropTargetManager(context, container, dragZoneChangedListener)
    }

    @Test
    fun onDragStarted_notifiesInitialDragZone() {
        dropTargetManager.onDragStarted(
            DraggedObject.Bubble(BubbleBarLocation.LEFT),
            listOf(bubbleLeftDragZone, bubbleRightDragZone)
        )
        assertThat(dragZoneChangedListener.initialDragZone).isEqualTo(bubbleLeftDragZone)
    }

    @Test
    fun onDragStarted_missingExpectedDragZone_fails() {
        assertFails {
            dropTargetManager.onDragStarted(
                DraggedObject.Bubble(BubbleBarLocation.RIGHT),
                listOf(bubbleLeftDragZone)
            )
        }
    }

    @Test
    fun onDragUpdated_notifiesDragZoneChanged() {
        dropTargetManager.onDragStarted(
            DraggedObject.Bubble(BubbleBarLocation.LEFT),
            listOf(bubbleLeftDragZone, bubbleRightDragZone, dismissDragZone)
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dropTargetManager.onDragUpdated(
                bubbleRightDragZone.bounds.centerX(),
                bubbleRightDragZone.bounds.centerY()
            )
        }
        assertThat(dragZoneChangedListener.fromDragZone).isEqualTo(bubbleLeftDragZone)
        assertThat(dragZoneChangedListener.toDragZone).isEqualTo(bubbleRightDragZone)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dropTargetManager.onDragUpdated(
                dismissDragZone.bounds.centerX(),
                dismissDragZone.bounds.centerY()
            )
        }
        assertThat(dragZoneChangedListener.fromDragZone).isEqualTo(bubbleRightDragZone)
        assertThat(dragZoneChangedListener.toDragZone).isEqualTo(dismissDragZone)
    }

    @Test
    fun onDragUpdated_withinSameZone_doesNotNotify() {
        dropTargetManager.onDragStarted(
            DraggedObject.Bubble(BubbleBarLocation.LEFT),
            listOf(bubbleLeftDragZone, bubbleRightDragZone, dismissDragZone)
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dropTargetManager.onDragUpdated(
                bubbleLeftDragZone.bounds.centerX(),
                bubbleLeftDragZone.bounds.centerY()
            )
        }
        assertThat(dragZoneChangedListener.fromDragZone).isNull()
        assertThat(dragZoneChangedListener.toDragZone).isNull()
    }

    @Test
    fun onDragUpdated_outsideAllZones_doesNotNotify() {
        dropTargetManager.onDragStarted(
            DraggedObject.Bubble(BubbleBarLocation.LEFT),
            listOf(bubbleLeftDragZone, bubbleRightDragZone)
        )
        val pointX = 200
        val pointY = 200
        assertThat(bubbleLeftDragZone.contains(pointX, pointY)).isFalse()
        assertThat(bubbleRightDragZone.contains(pointX, pointY)).isFalse()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dropTargetManager.onDragUpdated(pointX, pointY)
        }
        assertThat(dragZoneChangedListener.fromDragZone).isNull()
        assertThat(dragZoneChangedListener.toDragZone).isNull()
    }

    @Test
    fun onDragUpdated_hasOverlappingZones_notifiesFirstDragZoneChanged() {
        // create a drag zone that spans across the width of all 3 drag zones, but extends below
        // them
        val splitDragZone = DragZone.Split.Left(bounds = Rect(0, 0, 300, 200))
        dropTargetManager.onDragStarted(
            DraggedObject.Bubble(BubbleBarLocation.LEFT),
            listOf(bubbleLeftDragZone, bubbleRightDragZone, dismissDragZone, splitDragZone)
        )

        // drag to a point that is within both the bubble right zone and split zone
        val (pointX, pointY) =
            Pair(bubbleRightDragZone.bounds.centerX(), bubbleRightDragZone.bounds.centerY())
        assertThat(splitDragZone.contains(pointX, pointY)).isTrue()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dropTargetManager.onDragUpdated(pointX, pointY)
        }
        // verify we dragged to the bubble right zone because that has higher priority than split
        assertThat(dragZoneChangedListener.fromDragZone).isEqualTo(bubbleLeftDragZone)
        assertThat(dragZoneChangedListener.toDragZone).isEqualTo(bubbleRightDragZone)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dropTargetManager.onDragUpdated(
                bubbleRightDragZone.bounds.centerX(),
                150 // below the bubble and dismiss drag zones but within split
            )
        }
        assertThat(dragZoneChangedListener.fromDragZone).isEqualTo(bubbleRightDragZone)
        assertThat(dragZoneChangedListener.toDragZone).isEqualTo(splitDragZone)

        val (dismissPointX, dismissPointY) =
            Pair(dismissDragZone.bounds.centerX(), dismissDragZone.bounds.centerY())
        assertThat(splitDragZone.contains(dismissPointX, dismissPointY)).isTrue()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dropTargetManager.onDragUpdated(dismissPointX, dismissPointY)
        }
        assertThat(dragZoneChangedListener.fromDragZone).isEqualTo(splitDragZone)
        assertThat(dragZoneChangedListener.toDragZone).isEqualTo(dismissDragZone)
    }

    @Test
    fun onDragUpdated_afterDragEnded_doesNotNotify() {
        dropTargetManager.onDragStarted(
            DraggedObject.Bubble(BubbleBarLocation.LEFT),
            listOf(bubbleLeftDragZone, bubbleRightDragZone, dismissDragZone)
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dropTargetManager.onDragEnded()
        }
        dropTargetManager.onDragUpdated(
            bubbleRightDragZone.bounds.centerX(),
            bubbleRightDragZone.bounds.centerY()
        )
        assertThat(dragZoneChangedListener.fromDragZone).isNull()
        assertThat(dragZoneChangedListener.toDragZone).isNull()
    }

    @Test
    fun onDragStarted_dropTargetAddedToContainer() {
        dropTargetManager.onDragStarted(
            DraggedObject.Bubble(BubbleBarLocation.LEFT),
            listOf(bubbleLeftDragZone, bubbleRightDragZone)
        )
        assertThat(container.childCount).isEqualTo(1)
        assertThat(dropTargetView.alpha).isEqualTo(0)
    }

    @Test
    fun onDragEnded_dropTargetRemovedFromContainer() {
        dropTargetManager.onDragStarted(
            DraggedObject.Bubble(BubbleBarLocation.LEFT),
            listOf(bubbleLeftDragZone, bubbleRightDragZone)
        )
        assertThat(container.childCount).isEqualTo(1)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dropTargetManager.onDragEnded()
            animatorTestRule.advanceTimeBy(250)
        }
        assertThat(container.childCount).isEqualTo(0)
    }

    @Test
    fun onDragEnded_dropTargetNotifies() {
        dropTargetManager.onDragStarted(
            DraggedObject.Bubble(BubbleBarLocation.LEFT),
            listOf(bubbleLeftDragZone, bubbleRightDragZone, dismissDragZone)
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dropTargetManager.onDragUpdated(
                bubbleRightDragZone.bounds.centerX(),
                bubbleRightDragZone.bounds.centerY()
            )
            dropTargetManager.onDragEnded()
        }
        assertThat(dragZoneChangedListener.endedDragZone).isEqualTo(bubbleRightDragZone)
    }

    @Test
    fun startNewDrag_beforeDropTargetRemoved() {
        dropTargetManager.onDragStarted(
            DraggedObject.Bubble(BubbleBarLocation.LEFT),
            listOf(bubbleLeftDragZone, bubbleRightDragZone)
        )
        assertThat(container.childCount).isEqualTo(1)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dropTargetManager.onDragEnded()
            // advance the timer by 50ms so the animation doesn't complete
            // needs to be < DropTargetManager.DROP_TARGET_ALPHA_OUT_DURATION
            animatorTestRule.advanceTimeBy(50)
        }
        assertThat(container.childCount).isEqualTo(1)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dropTargetManager.onDragStarted(
                DraggedObject.Bubble(BubbleBarLocation.LEFT),
                listOf(bubbleLeftDragZone, bubbleRightDragZone)
            )
        }
        assertThat(container.childCount).isEqualTo(1)
    }

    @Test
    fun updateDragZone_withDropTarget_dropTargetUpdated() {
        dropTargetManager.onDragStarted(
            DraggedObject.Bubble(BubbleBarLocation.LEFT),
            listOf(dismissDragZone, bubbleLeftDragZone, bubbleRightDragZone)
        )

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dropTargetManager.onDragUpdated(
                bubbleRightDragZone.bounds.centerX(),
                bubbleRightDragZone.bounds.centerY()
            )
            animatorTestRule.advanceTimeBy(250)
        }

        assertThat(dropTargetView.alpha).isEqualTo(1)
        verifyDropTargetPosition(bubbleRightDragZone.dropTarget)
    }

    @Test
    fun updateDragZone_withoutDropTarget_dropTargetHidden() {
        dropTargetManager.onDragStarted(
            DraggedObject.Bubble(BubbleBarLocation.LEFT),
            listOf(dismissDragZone, bubbleLeftDragZone, bubbleRightDragZone)
        )

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dropTargetManager.onDragUpdated(
                dismissDragZone.bounds.centerX(),
                dismissDragZone.bounds.centerY()
            )
            animatorTestRule.advanceTimeBy(250)
        }

        assertThat(dropTargetView.alpha).isEqualTo(0)
    }

    @Test
    fun updateDragZone_betweenZonesWithDropTarget_dropTargetUpdated() {
        dropTargetManager.onDragStarted(
            DraggedObject.Bubble(BubbleBarLocation.LEFT),
            listOf(dismissDragZone, bubbleLeftDragZone, bubbleRightDragZone)
        )

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dropTargetManager.onDragUpdated(
                bubbleRightDragZone.bounds.centerX(),
                bubbleRightDragZone.bounds.centerY()
            )
            animatorTestRule.advanceTimeBy(250)
        }

        assertThat(dropTargetView.alpha).isEqualTo(1)
        verifyDropTargetPosition(bubbleRightDragZone.dropTarget)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dropTargetManager.onDragUpdated(
                bubbleLeftDragZone.bounds.centerX(),
                bubbleLeftDragZone.bounds.centerY()
            )
            animatorTestRule.advanceTimeBy(250)
        }

        assertThat(dropTargetView.alpha).isEqualTo(1)
        verifyDropTargetPosition(bubbleLeftDragZone.dropTarget)
    }

    private fun verifyDropTargetPosition(rect: Rect) {
        assertThat(dropTargetView.getRect().left).isEqualTo(rect.left)
        assertThat(dropTargetView.getRect().top).isEqualTo(rect.top)
        assertThat(dropTargetView.getRect().right).isEqualTo(rect.right)
        assertThat(dropTargetView.getRect().bottom).isEqualTo(rect.bottom)
    }

    private class FakeDragZoneChangedListener : DropTargetManager.DragZoneChangedListener {
        var initialDragZone: DragZone? = null
        var fromDragZone: DragZone? = null
        var toDragZone: DragZone? = null
        var endedDragZone: DragZone? = null

        override fun onInitialDragZoneSet(dragZone: DragZone) {
            initialDragZone = dragZone
        }

        override fun onDragZoneChanged(draggedObject: DraggedObject, from: DragZone, to: DragZone) {
            fromDragZone = from
            toDragZone = to
        }

        override fun onDragEnded(zone: DragZone) {
            endedDragZone = zone
        }
    }
}
