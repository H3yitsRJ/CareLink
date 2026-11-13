package com.example.carelink.screens

import androidx.compose.runtime.staticCompositionLocalOf
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal const val DATE_FIELD_PATTERN = "MM-dd-yyyy"
internal const val TIME_FIELD_PATTERN = "h:mm a"
internal const val DATE_FIELD_HINT = "MM-DD-YYYY"

private const val STORED_DATE_PATTERN = "yyyy-MM-dd"
private const val STORED_TIME_PATTERN = "HH:mm"

enum class ClockFormat(val label: String, val pattern: String, val hint: String) {
    TwelveHour("12-hour", TIME_FIELD_PATTERN, "h:mm AM/PM"),
    TwentyFourHour("24-hour", STORED_TIME_PATTERN, "HH:mm");

    val is24Hour get() = this == TwentyFourHour
    val timeError get() = if (is24Hour) "Enter a valid time in HH:mm format" else "Enter a valid time with AM or PM"

    fun displayTime(value: String) =
        if (is24Hour) value else reformatField(value, STORED_TIME_PATTERN, pattern) ?: value

    fun storedTime(value: String): String? =
        if (is24Hour) value.trim().takeIf { Regex("""([01]\d|2[0-3]):[0-5]\d""").matches(it) }
        else storedFieldTime(value)
}

internal val LocalClockFormat = staticCompositionLocalOf { ClockFormat.TwelveHour }

internal fun displayFieldDate(value: String) =
    reformatField(value, STORED_DATE_PATTERN, DATE_FIELD_PATTERN) ?: value

internal fun displayFieldTime(value: String) =
    ClockFormat.TwelveHour.displayTime(value)

internal fun storedFieldDate(value: String): String? =
    value.trim().takeIf { Regex("""\d{2}-\d{2}-\d{4}""").matches(it) }
        ?.let { reformatField(it, DATE_FIELD_PATTERN, STORED_DATE_PATTERN) }

internal fun storedFieldTime(value: String): String? =
    value.trim().uppercase(Locale.US)
        .takeIf { Regex("""(0?[1-9]|1[0-2]):[0-5]\d\s+[AP]M""").matches(it) }
        ?.let { reformatField(it, TIME_FIELD_PATTERN, STORED_TIME_PATTERN) }

private fun reformatField(value: String, from: String, to: String): String? {
    // A date or wall-clock time must survive formatting in every device time zone.
    val utc = TimeZone.getTimeZone("UTC")
    val parser = SimpleDateFormat(from, Locale.US).apply {
        isLenient = false
        timeZone = utc
    }
    val position = ParsePosition(0)
    val parsed = parser.parse(value, position) ?: return null
    if (position.index != value.length) return null
    return SimpleDateFormat(to, Locale.US).apply { timeZone = utc }.format(parsed)
}
