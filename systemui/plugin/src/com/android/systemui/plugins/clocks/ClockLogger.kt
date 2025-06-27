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

import android.view.View
import android.view.View.MeasureSpec
import com.android.systemui.log.core.LogLevel
import com.android.systemui.log.core.LogcatOnlyMessageBuffer
import com.android.systemui.log.core.Logger
import com.android.systemui.log.core.MessageBuffer
import kotlin.math.abs

class ClockLogger(private val view: View?, buffer: MessageBuffer, tag: String) :
    Logger(buffer, tag) {

    private var loggedAlpha = 1000f
    private val isDrawn: Boolean
        get() = ((view?.mPrivateFlags ?: 0x0) and 0x20 /* PFLAG_DRAWN */) > 0

    fun invalidate() {
        if (isDrawn && view?.visibility == View.VISIBLE) {
            d("invalidate()")
        }
    }

    fun refreshTime() {
        d("refreshTime()")
    }

    fun requestLayout() {
        if (view?.isLayoutRequested() == false) {
            d("requestLayout()")
        }
    }

    fun onMeasure(widthSpec: Int, heightSpec: Int) {
        d({ "onMeasure(${getSpecText(int1)}, ${getSpecText(int2)})" }) {
            int1 = widthSpec
            int2 = heightSpec
        }
    }

    fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        d({ "onLayout($bool1, ${VRect.fromLong(long1)})" }) {
            bool1 = changed
            long1 = VRect(left, top, right, bottom).toLong()
        }
    }

    fun onDraw() {
        d("onDraw()")
    }

    fun onDraw(str: String?) {
        d({ "onDraw(${escapeTime(str1)})" }) { str1 = str ?: "" }
    }

    fun onDraw(lsStr: String?, aodStr: String?) {
        d({ "onDraw(ls = ${escapeTime(str1)}, aod = ${escapeTime(str2)}" }) {
            str1 = lsStr
            str2 = aodStr
        }
    }

    fun setVisibility(visibility: Int) {
        if (visibility != view?.visibility) {
            d({ "setVisibility(${getVisText(int1)})" }) { int1 = visibility }
        }
    }

    fun setAlpha(alpha: Float) {
        val delta = if (alpha <= 0f || alpha >= 1f) 0.001f else 0.5f
        if (abs(loggedAlpha - alpha) >= delta) {
            loggedAlpha = alpha
            d({ "setAlpha($double1)" }) { double1 = alpha.toDouble() }
        }
    }

    fun updateAxes(lsFVar: String, aodFVar: String, isAnimated: Boolean) {
        i({ "updateAxes(LS = $str1, AOD = $str2, isAnimated=$bool1)" }) {
            str1 = lsFVar
            str2 = aodFVar
            bool1 = isAnimated
        }
    }

    fun onViewAdded(child: View) {
        d({ "onViewAdded($str1 @$int1)" }) {
            str1 = child::class.simpleName!!
            int1 = child.id
        }
    }

    fun animateDoze(isDozing: Boolean, isAnimated: Boolean) {
        d({ "animateDoze(isDozing=$bool1, isAnimated=$bool2)" }) {
            bool1 = isDozing
            bool2 = isAnimated
        }
    }

    fun animateCharge() {
        d("animateCharge()")
    }

    fun animateFidget(x: Float, y: Float) {
        d({ "animateFidget(${VPointF.fromLong(long1)})" }) { long1 = VPointF(x, y).toLong() }
    }

    companion object {
        // Used when MessageBuffers are not provided by the host application
        val DEFAULT_MESSAGE_BUFFER = LogcatOnlyMessageBuffer(LogLevel.INFO)

        // Debug is primarially used for tests, but can also be used for tracking down hard issues.
        val DEBUG_MESSAGE_BUFFER = LogcatOnlyMessageBuffer(LogLevel.DEBUG)

        // Only intended for use during initialization steps before the logger is initialized
        val INIT_LOGGER = ClockLogger(null, LogcatOnlyMessageBuffer(LogLevel.ERROR), "CLOCK_INIT")

        @JvmStatic
        fun getVisText(visibility: Int): String {
            return when (visibility) {
                View.GONE -> "GONE"
                View.INVISIBLE -> "INVISIBLE"
                View.VISIBLE -> "VISIBLE"
                else -> "$visibility"
            }
        }

        @JvmStatic
        fun getSpecText(spec: Int): String {
            val size = MeasureSpec.getSize(spec)
            val mode = MeasureSpec.getMode(spec)
            val modeText =
                when (mode) {
                    MeasureSpec.EXACTLY -> "EXACTLY"
                    MeasureSpec.AT_MOST -> "AT MOST"
                    MeasureSpec.UNSPECIFIED -> "UNSPECIFIED"
                    else -> "$mode"
                }
            return "($size, $modeText)"
        }

        @JvmStatic
        fun escapeTime(timeStr: String?): String? {
            return timeStr?.replace("\n", "\\n")
        }
    }
}
