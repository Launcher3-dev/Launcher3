package com.android.systemui.plugins.clocks

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.text.util.LocalePreferences

typealias WeatherTouchAction = (View) -> Unit

data class WeatherData(
    val description: String,
    val state: WeatherStateIcon,
    val useCelsius: Boolean,
    val temperature: Int,
    val touchAction: WeatherTouchAction? = null,
) {
    companion object {
        const val DEBUG = true
        private const val TAG = "WeatherData"
        @VisibleForTesting const val DESCRIPTION_KEY = "description"
        @VisibleForTesting const val STATE_KEY = "state"
        @VisibleForTesting const val USE_CELSIUS_KEY = "use_celsius"
        @VisibleForTesting const val TEMPERATURE_KEY = "temperature"
        private const val INVALID_WEATHER_ICON_STATE = -1

        @JvmStatic
        @JvmOverloads
        fun fromBundle(extras: Bundle, touchAction: WeatherTouchAction? = null): WeatherData? {
            val description = extras.getString(DESCRIPTION_KEY)
            val state =
                WeatherStateIcon.fromInt(extras.getInt(STATE_KEY, INVALID_WEATHER_ICON_STATE))
            val temperature = readIntFromBundle(extras, TEMPERATURE_KEY)
            if (
                description == null ||
                    state == null ||
                    !extras.containsKey(USE_CELSIUS_KEY) ||
                    temperature == null
            ) {
                if (DEBUG) {
                    Log.w(TAG, "Weather data did not parse from $extras")
                }
                return null
            } else {
                val result =
                    WeatherData(
                        description = description,
                        state = state,
                        useCelsius = extras.getBoolean(USE_CELSIUS_KEY),
                        temperature = temperature,
                        touchAction = touchAction,
                    )
                if (DEBUG) {
                    Log.i(TAG, "Weather data parsed $result from $extras")
                }
                return result
            }
        }

        private fun readIntFromBundle(extras: Bundle, key: String): Int? {
            try {
                return extras.getString(key)?.toInt()
            } catch (e: Exception) {
                return null
            }
        }

        fun getPlaceholderWeatherData(): WeatherData {
            return getPlaceholderWeatherData(
                LocalePreferences.getTemperatureUnit() == LocalePreferences.TemperatureUnit.CELSIUS
            )
        }

        private const val DESCRIPTION_PLACEHODLER = ""
        private const val TEMPERATURE_FAHRENHEIT_PLACEHOLDER = 58
        private const val TEMPERATURE_CELSIUS_PLACEHOLDER = 21
        private val WEATHERICON_PLACEHOLDER = WeatherData.WeatherStateIcon.MOSTLY_SUNNY

        fun getPlaceholderWeatherData(useCelsius: Boolean): WeatherData {
            return WeatherData(
                description = DESCRIPTION_PLACEHODLER,
                state = WEATHERICON_PLACEHOLDER,
                temperature =
                    if (useCelsius) TEMPERATURE_CELSIUS_PLACEHOLDER
                    else TEMPERATURE_FAHRENHEIT_PLACEHOLDER,
                useCelsius = useCelsius,
            )
        }
    }

    // Values for WeatherStateIcon must stay in sync with go/g3-WeatherStateIcon
    enum class WeatherStateIcon(val id: Int, val icon: String) {
        UNKNOWN_ICON(0, ""),

        // Clear, day & night.
        SUNNY(1, "a"),
        CLEAR_NIGHT(2, "f"),

        // Mostly clear, day & night.
        MOSTLY_SUNNY(3, "b"),
        MOSTLY_CLEAR_NIGHT(4, "n"),

        // Partly cloudy, day & night.
        PARTLY_CLOUDY(5, "b"),
        PARTLY_CLOUDY_NIGHT(6, "n"),

        // Mostly cloudy, day & night.
        MOSTLY_CLOUDY_DAY(7, "e"),
        MOSTLY_CLOUDY_NIGHT(8, "e"),
        CLOUDY(9, "e"),
        HAZE_FOG_DUST_SMOKE(10, "d"),
        DRIZZLE(11, "c"),
        HEAVY_RAIN(12, "c"),
        SHOWERS_RAIN(13, "c"),

        // Scattered showers, day & night.
        SCATTERED_SHOWERS_DAY(14, "c"),
        SCATTERED_SHOWERS_NIGHT(15, "c"),

        // Isolated scattered thunderstorms, day & night.
        ISOLATED_SCATTERED_TSTORMS_DAY(16, "i"),
        ISOLATED_SCATTERED_TSTORMS_NIGHT(17, "i"),
        STRONG_TSTORMS(18, "i"),
        BLIZZARD(19, "j"),
        BLOWING_SNOW(20, "j"),
        FLURRIES(21, "h"),
        HEAVY_SNOW(22, "j"),

        // Scattered snow showers, day & night.
        SCATTERED_SNOW_SHOWERS_DAY(23, "h"),
        SCATTERED_SNOW_SHOWERS_NIGHT(24, "h"),
        SNOW_SHOWERS_SNOW(25, "g"),
        MIXED_RAIN_HAIL_RAIN_SLEET(26, "h"),
        SLEET_HAIL(27, "h"),
        TORNADO(28, "l"),
        TROPICAL_STORM_HURRICANE(29, "m"),
        WINDY_BREEZY(30, "k"),
        WINTRY_MIX_RAIN_SNOW(31, "h");

        companion object {
            fun fromInt(value: Int) = values().firstOrNull { it.id == value }
        }
    }

    override fun toString(): String {
        val unit = if (useCelsius) "C" else "F"
        return "$state (\"$description\") $temperature°$unit"
    }
}
