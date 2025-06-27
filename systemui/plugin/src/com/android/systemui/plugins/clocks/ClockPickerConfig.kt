/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.systemui.plugins.clocks

import android.graphics.drawable.Drawable

data class ClockPickerConfig
@JvmOverloads
constructor(
    val id: String,

    /** Localized name of the clock */
    val name: String,

    /** Localized accessibility description for the clock */
    val description: String,

    /* Static & lightweight thumbnail version of the clock */
    val thumbnail: Drawable,

    /** True if the clock will react to tone changes in the seed color */
    val isReactiveToTone: Boolean = true,

    /** Font axes that can be modified on this clock */
    val axes: List<ClockFontAxis> = listOf(),

    /** Presets for this clock. Null indicates the preset list should be disabled. */
    val presetConfig: AxisPresetConfig? = null,
)

data class AxisPresetConfig(
    /** Groups of Presets. Each group can be used together in a single control. */
    val groups: List<Group>,

    /** Preset item currently being used, null when the current style is not a preset */
    val current: IndexedStyle? = null,
) {
    /** The selected clock axis style, and its indices */
    data class IndexedStyle(
        /** Index of the group that this clock axis style appears in */
        val groupIndex: Int,

        /** Index of the preset within the group */
        val presetIndex: Int,

        /** Reference to the style in question */
        val style: ClockAxisStyle,
    )

    /** A group of preset styles */
    data class Group(
        /* List of preset styles in this group */
        val presets: List<ClockAxisStyle>,

        /* Icon to use when this preset-group is active */
        val icon: Drawable,
    )

    fun findStyle(style: ClockAxisStyle): IndexedStyle? {
        groups.forEachIndexed { groupIndex, group ->
            group.presets.forEachIndexed { presetIndex, preset ->
                if (preset == style) {
                    return@findStyle IndexedStyle(
                        groupIndex = groupIndex,
                        presetIndex = presetIndex,
                        style = preset,
                    )
                }
            }
        }
        return null
    }
}

/** Represents an Axis that can be modified */
data class ClockFontAxis(
    /** Axis key, not user renderable */
    val key: String,

    /** Intended mode of user interaction */
    val type: AxisType,

    /** Maximum value the axis supports */
    val maxValue: Float,

    /** Minimum value the axis supports */
    val minValue: Float,

    /** Current value the axis is set to */
    val currentValue: Float,

    /** User-renderable name of the axis */
    val name: String,

    /** Description of the axis */
    val description: String,
) {
    companion object {
        fun List<ClockFontAxis>.merge(axisStyle: ClockAxisStyle): List<ClockFontAxis> {
            return this.map { axis ->
                    axisStyle.get(axis.key)?.let { axis.copy(currentValue = it) } ?: axis
                }
                .toList()
        }
    }
}

/** Axis user interaction modes */
enum class AxisType {
    /** Continuous range between minValue & maxValue. */
    Float,

    /** Only minValue & maxValue are valid. No intermediate values between them are allowed. */
    Boolean,
}
