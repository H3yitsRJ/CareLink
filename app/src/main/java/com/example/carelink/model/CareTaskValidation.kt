package com.example.carelink.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.text.ParsePosition

fun CareTask.validationErrors(now: Date = Date()): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    if (title.isBlank()) errors["title"] = "Enter a task title"
    val date = dueDate.trim()
    val clock = time.trim()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    val validDate = Regex("""\d{4}-\d{2}-\d{2}""").matches(date) &&
        dateFormat.parse(date, ParsePosition(0)) != null
    val validTime = Regex("""([01]\d|2[0-3]):[0-5]\d""").matches(clock)
    if (!validDate) errors["dueDate"] = "Use a valid date in YYYY-MM-DD format"
    if (!validTime) errors["time"] = "Use 24-hour time in HH:MM format"
    if (validDate && validTime) {
        val deadline = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }
            .parse("$date $clock", ParsePosition(0))
        if (deadline == null || !deadline.after(now)) errors["dueDate"] = "Choose a future due date and time"
    }
    return errors
}
