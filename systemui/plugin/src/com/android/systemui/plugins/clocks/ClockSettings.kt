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

import com.android.internal.annotations.Keep
import org.json.JSONArray
import org.json.JSONObject

@Keep
/** Structure for keeping clock-specific settings */
data class ClockSettings(
    val clockId: ClockId? = null,
    val seedColor: Int? = null,
    val axes: ClockAxisStyle = ClockAxisStyle(),
) {
    // Exclude metadata from equality checks
    var metadata: JSONObject = JSONObject()

    companion object {
        private val KEY_CLOCK_ID = "clockId"
        private val KEY_SEED_COLOR = "seedColor"
        private val KEY_METADATA = "metadata"
        private val KEY_AXIS_LIST = "axes"

        fun toJson(setting: ClockSettings): JSONObject {
            return JSONObject().apply {
                put(KEY_CLOCK_ID, setting.clockId)
                put(KEY_SEED_COLOR, setting.seedColor)
                put(KEY_METADATA, setting.metadata)
                put(KEY_AXIS_LIST, ClockAxisStyle.toJson(setting.axes))
            }
        }

        fun fromJson(json: JSONObject): ClockSettings {
            val clockId = if (!json.isNull(KEY_CLOCK_ID)) json.getString(KEY_CLOCK_ID) else null
            val seedColor = if (!json.isNull(KEY_SEED_COLOR)) json.getInt(KEY_SEED_COLOR) else null
            val axisList = json.optJSONArray(KEY_AXIS_LIST)?.let(ClockAxisStyle::fromJson)
            return ClockSettings(clockId, seedColor, axisList ?: ClockAxisStyle()).apply {
                metadata = json.optJSONObject(KEY_METADATA) ?: JSONObject()
            }
        }
    }
}

@Keep
class ClockAxisStyle {
    private val settings: MutableMap<String, Float>

    // Iterable would be implemented on ClockAxisStyle directly,
    // but that doesn't appear to work with plugins/dynamic libs.
    val items: Iterable<Map.Entry<String, Float>>
        get() = settings.asIterable()

    val isEmpty: Boolean
        get() = settings.isEmpty()

    constructor(initialize: ClockAxisStyle.() -> Unit = {}) {
        settings = mutableMapOf()
        this.initialize()
    }

    constructor(style: ClockAxisStyle) {
        settings = style.settings.toMutableMap()
    }

    constructor(items: Map<String, Float>) {
        settings = items.toMutableMap()
    }

    constructor(key: String, value: Float) {
        settings = mutableMapOf(key to value)
    }

    constructor(items: List<ClockFontAxis>) {
        settings = items.associate { it.key to it.currentValue }.toMutableMap()
    }

    fun copy(initialize: ClockAxisStyle.() -> Unit): ClockAxisStyle {
        return ClockAxisStyle(this).apply { initialize() }
    }

    operator fun get(key: String): Float? = settings[key]

    operator fun set(key: String, value: Float) = put(key, value)

    fun put(key: String, value: Float) {
        settings.put(key, value)
    }

    fun toFVar(): String {
        val sb = StringBuilder()
        for (axis in settings) {
            if (sb.length > 0) sb.append(", ")
            sb.append("'${axis.key}' ${axis.value.toInt()}")
        }
        return sb.toString()
    }

    fun copyWith(replacements: ClockAxisStyle): ClockAxisStyle {
        val result = ClockAxisStyle(this)
        for ((key, value) in replacements.settings) {
            result[key] = value
        }
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClockAxisStyle) return false
        return settings == other.settings
    }

    companion object {
        private val KEY_AXIS_KEY = "key"
        private val KEY_AXIS_VALUE = "value"

        fun fromJson(jsonArray: JSONArray): ClockAxisStyle {
            val result = ClockAxisStyle()
            for (i in 0..jsonArray.length() - 1) {
                val obj = jsonArray.getJSONObject(i)
                if (obj == null) continue

                result.put(
                    key = obj.getString(KEY_AXIS_KEY),
                    value = obj.getDouble(KEY_AXIS_VALUE).toFloat(),
                )
            }
            return result
        }

        fun toJson(style: ClockAxisStyle): JSONArray {
            return JSONArray().apply {
                for ((key, value) in style.settings) {
                    put(
                        JSONObject().apply {
                            put(KEY_AXIS_KEY, key)
                            put(KEY_AXIS_VALUE, value)
                        }
                    )
                }
            }
        }
    }
}
