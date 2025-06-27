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

import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private val X_MASK: ULong = 0xFFFFFFFF00000000U
private val Y_MASK: ULong = 0x00000000FFFFFFFFU

private fun unpackX(data: ULong): Int = ((data and X_MASK) shr 32).toInt()

private fun unpackY(data: ULong): Int = ((data and Y_MASK) shr 0).toInt()

private fun pack(x: Int, y: Int): ULong {
    return ((x.toULong() shl 32) and X_MASK) or ((y.toULong() shl 0) and Y_MASK)
}

@JvmInline
value class VPointF(val data: ULong) {
    val x: Float
        get() = Float.fromBits(unpackX(data))

    val y: Float
        get() = Float.fromBits(unpackY(data))

    constructor(pt: PointF) : this(pt.x, pt.y)

    constructor(x: Int, y: Int) : this(x.toFloat(), y.toFloat())

    constructor(x: Int, y: Float) : this(x.toFloat(), y)

    constructor(x: Float, y: Int) : this(x, y.toFloat())

    constructor(x: Float, y: Float) : this(pack(x.toBits(), y.toBits()))

    fun toPointF() = PointF(x, y)

    fun toLong(): Long = data.toLong()

    fun lengthSq(): Float = x * x + y * y

    fun length(): Float = sqrt(lengthSq())

    fun abs() = VPointF(abs(x), abs(y))

    fun dot(pt: VPointF): Float = x * pt.x + y * pt.y

    fun normalize(): VPointF {
        val length = this.length()
        return VPointF(x / length, y / length)
    }

    operator fun component1(): Float = x

    operator fun component2(): Float = y

    override fun toString() = "($x, $y)"

    operator fun plus(pt: VPoint) = VPointF(x + pt.x, y + pt.y)

    operator fun plus(pt: VPointF) = VPointF(x + pt.x, y + pt.y)

    operator fun plus(value: Int) = VPointF(x + value, y + value)

    operator fun plus(value: Float) = VPointF(x + value, y + value)

    operator fun minus(pt: VPoint) = VPointF(x - pt.x, y - pt.y)

    operator fun minus(pt: VPointF) = VPointF(x - pt.x, y - pt.y)

    operator fun minus(value: Int) = VPointF(x - value, y - value)

    operator fun minus(value: Float) = VPointF(x - value, y - value)

    operator fun times(pt: VPoint) = VPointF(x * pt.x, y * pt.y)

    operator fun times(pt: VPointF) = VPointF(x * pt.x, y * pt.y)

    operator fun times(value: Int) = VPointF(x * value, y * value)

    operator fun times(value: Float) = VPointF(x * value, y * value)

    operator fun div(pt: VPoint) = VPointF(x / pt.x, y / pt.y)

    operator fun div(pt: VPointF) = VPointF(x / pt.x, y / pt.y)

    operator fun div(value: Int) = VPointF(x / value, y / value)

    operator fun div(value: Float) = VPointF(x / value, y / value)

    companion object {
        val ZERO = VPointF(0, 0)

        fun fromLong(data: Long) = VPointF(data.toULong())

        fun max(lhs: VPointF, rhs: VPointF) = VPointF(max(lhs.x, rhs.x), max(lhs.y, rhs.y))

        fun min(lhs: VPointF, rhs: VPointF) = VPointF(min(lhs.x, rhs.x), min(lhs.y, rhs.y))

        operator fun Float.plus(value: VPointF) = VPointF(this + value.x, this + value.y)

        operator fun Int.minus(value: VPointF) = VPointF(this - value.x, this - value.y)

        operator fun Float.minus(value: VPointF) = VPointF(this - value.x, this - value.y)

        operator fun Int.times(value: VPointF) = VPointF(this * value.x, this * value.y)

        operator fun Float.times(value: VPointF) = VPointF(this * value.x, this * value.y)

        operator fun Int.div(value: VPointF) = VPointF(this / value.x, this / value.y)

        operator fun Float.div(value: VPointF) = VPointF(this / value.x, this / value.y)

        val RectF.center: VPointF
            get() = VPointF(centerX(), centerY())

        val RectF.size: VPointF
            get() = VPointF(width(), height())
    }
}

@JvmInline
value class VPoint(val data: ULong) {
    val x: Int
        get() = unpackX(data)

    val y: Int
        get() = unpackY(data)

    constructor(x: Int, y: Int) : this(pack(x, y))

    fun toPoint() = Point(x, y)

    fun toLong(): Long = data.toLong()

    fun abs() = VPoint(abs(x), abs(y))

    operator fun component1(): Int = x

    operator fun component2(): Int = y

    override fun toString() = "($x, $y)"

    operator fun plus(pt: VPoint) = VPoint(x + pt.x, y + pt.y)

    operator fun plus(pt: VPointF) = VPointF(x + pt.x, y + pt.y)

    operator fun plus(value: Int) = VPoint(x + value, y + value)

    operator fun plus(value: Float) = VPointF(x + value, y + value)

    operator fun minus(pt: VPoint) = VPoint(x - pt.x, y - pt.y)

    operator fun minus(pt: VPointF) = VPointF(x - pt.x, y - pt.y)

    operator fun minus(value: Int) = VPoint(x - value, y - value)

    operator fun minus(value: Float) = VPointF(x - value, y - value)

    operator fun times(pt: VPoint) = VPoint(x * pt.x, y * pt.y)

    operator fun times(pt: VPointF) = VPointF(x * pt.x, y * pt.y)

    operator fun times(value: Int) = VPoint(x * value, y * value)

    operator fun times(value: Float) = VPointF(x * value, y * value)

    operator fun div(pt: VPoint) = VPoint(x / pt.x, y / pt.y)

    operator fun div(pt: VPointF) = VPointF(x / pt.x, y / pt.y)

    operator fun div(value: Int) = VPoint(x / value, y / value)

    operator fun div(value: Float) = VPointF(x / value, y / value)

    companion object {
        val ZERO = VPoint(0, 0)

        fun fromLong(data: Long) = VPoint(data.toULong())

        fun max(lhs: VPoint, rhs: VPoint) = VPoint(max(lhs.x, rhs.x), max(lhs.y, rhs.y))

        fun min(lhs: VPoint, rhs: VPoint) = VPoint(min(lhs.x, rhs.x), min(lhs.y, rhs.y))

        operator fun Int.plus(value: VPoint) = VPoint(this + value.x, this + value.y)

        operator fun Float.plus(value: VPoint) = VPointF(this + value.x, this + value.y)

        operator fun Int.minus(value: VPoint) = VPoint(this - value.x, this - value.y)

        operator fun Float.minus(value: VPoint) = VPointF(this - value.x, this - value.y)

        operator fun Int.times(value: VPoint) = VPoint(this * value.x, this * value.y)

        operator fun Float.times(value: VPoint) = VPointF(this * value.x, this * value.y)

        operator fun Int.div(value: VPoint) = VPoint(this / value.x, this / value.y)

        operator fun Float.div(value: VPoint) = VPointF(this / value.x, this / value.y)

        val Rect.center: VPoint
            get() = VPoint(centerX(), centerY())

        val Rect.size: VPoint
            get() = VPoint(width(), height())
    }
}
