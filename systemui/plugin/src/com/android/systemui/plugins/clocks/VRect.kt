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

package com.android.systemui.plugins.clocks

import android.graphics.Rect
import android.graphics.RectF
import android.util.Half

private val LEFT_MASK: ULong = 0xFFFF000000000000U
private val TOP_MASK: ULong = 0x0000FFFF00000000U
private val RIGHT_MASK: ULong = 0x00000000FFFF0000U
private val BOTTOM_MASK: ULong = 0x000000000000FFFFU

private fun unpackLeft(data: ULong): Short = ((data and LEFT_MASK) shr 48).toShort()

private fun unpackTop(data: ULong): Short = ((data and TOP_MASK) shr 32).toShort()

private fun unpackRight(data: ULong): Short = ((data and RIGHT_MASK) shr 16).toShort()

private fun unpackBottom(data: ULong): Short = ((data and BOTTOM_MASK) shr 0).toShort()

private fun pack(left: Short, top: Short, right: Short, bottom: Short): ULong {
    return ((left.toULong() shl 48) and LEFT_MASK) or
        ((top.toULong() shl 32) and TOP_MASK) or
        ((right.toULong() shl 16) and RIGHT_MASK) or
        ((bottom.toULong() shl 0) and BOTTOM_MASK)
}

@JvmInline
value class VRectF(val data: ULong) {
    val left: Float
        get() = fromBits(unpackLeft(data))

    val top: Float
        get() = fromBits(unpackTop(data))

    val right: Float
        get() = fromBits(unpackRight(data))

    val bottom: Float
        get() = fromBits(unpackBottom(data))

    val width: Float
        get() = right - left

    val height: Float
        get() = bottom - top

    constructor(rect: RectF) : this(rect.left, rect.top, rect.right, rect.bottom)

    constructor(
        rect: Rect
    ) : this(
        left = rect.left.toFloat(),
        top = rect.top.toFloat(),
        right = rect.right.toFloat(),
        bottom = rect.bottom.toFloat(),
    )

    constructor(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) : this(pack(toBits(left), toBits(top), toBits(right), toBits(bottom)))

    val center: VPointF
        get() = VPointF(left, top) + size / 2f

    val size: VPointF
        get() = VPointF(width, height)

    fun toRectF(): RectF = RectF(left, top, right, bottom)

    fun toLong(): Long = data.toLong()

    override fun toString() = "($left, $top) -> ($right, $bottom)"

    companion object {
        private fun toBits(value: Float): Short = Half.halfToShortBits(Half.toHalf(value))

        private fun fromBits(value: Short): Float = Half.toFloat(Half.intBitsToHalf(value.toInt()))

        fun fromLong(data: Long) = VRectF(data.toULong())

        fun fromCenter(center: VPointF, size: VPointF): VRectF {
            return VRectF(
                center.x - size.x / 2,
                center.y - size.y / 2,
                center.x + size.x / 2,
                center.y + size.y / 2,
            )
        }

        fun fromTopLeft(pos: VPointF, size: VPointF): VRectF {
            return VRectF(pos.x, pos.y, pos.x + size.x, pos.y + size.y)
        }

        val ZERO = VRectF(0f, 0f, 0f, 0f)
    }
}

@JvmInline
value class VRect(val data: ULong) {
    val left: Int
        get() = unpackLeft(data).toInt()

    val top: Int
        get() = unpackTop(data).toInt()

    val right: Int
        get() = unpackRight(data).toInt()

    val bottom: Int
        get() = unpackBottom(data).toInt()

    val width: Int
        get() = right - left

    val height: Int
        get() = bottom - top

    constructor(
        rect: Rect
    ) : this(
        left = rect.left.toShort(),
        top = rect.top.toShort(),
        right = rect.right.toShort(),
        bottom = rect.bottom.toShort(),
    )

    constructor(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) : this(
        left = left.toShort(),
        top = top.toShort(),
        right = right.toShort(),
        bottom = bottom.toShort(),
    )

    constructor(
        left: Short,
        top: Short,
        right: Short,
        bottom: Short,
    ) : this(pack(left, top, right, bottom))

    val center: VPoint
        get() = VPoint(left, top) + size / 2

    val size: VPoint
        get() = VPoint(width, height)

    fun toRect(): Rect = Rect(left, top, right, bottom)

    fun toLong(): Long = data.toLong()

    override fun toString() = "($left, $top) -> ($right, $bottom)"

    companion object {
        val ZERO = VRect(0, 0, 0, 0)

        fun fromLong(data: Long) = VRect(data.toULong())

        fun fromCenter(center: VPoint, size: VPoint): VRect {
            return VRect(
                (center.x - size.x / 2).toShort(),
                (center.y - size.y / 2).toShort(),
                (center.x + size.x / 2).toShort(),
                (center.y + size.y / 2).toShort(),
            )
        }

        fun fromTopLeft(pos: VPoint, size: VPoint): VRect {
            return VRect(
                pos.x.toShort(),
                pos.y.toShort(),
                (pos.x + size.x).toShort(),
                (pos.y + size.y).toShort(),
            )
        }
    }
}
