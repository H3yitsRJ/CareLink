package com.example.carelink.model

data class Medication(
    val id: String,
    val patientId: String,
    val name: String,
    val strength: String,
    val dose: String,
    val frequency: String,
    val reminderTimes: List<String>,
    val instructions: String = "",
    val active: Boolean = true
) {
    // Validation lives on the model so screens and repositories enforce the same rules.
    fun validate(): Map<String, String> = buildMap {
        if (name.isBlank()) put("name", "Enter a medication name")
        if (strength.isBlank()) put("strength", "Enter the medication strength")
        if (dose.isBlank()) put("dose", "Enter the dose")
        if (frequency.isBlank()) put("frequency", "Choose a frequency")
        if (reminderTimes.isEmpty()) put("reminderTimes", "Add at least one reminder time")
        if (reminderTimes.any { !TIME_PATTERN.matches(it) }) {
            put("reminderTimes", "Use a 24-hour time such as 08:30")
        }
    }

    // Explicit maps keep Firestore field names stable if Kotlin property names change later.
    fun toFirestore(): Map<String, Any> = mapOf(
        "patientId" to patientId,
        "name" to name,
        "strength" to strength,
        "dose" to dose,
        "frequency" to frequency,
        "reminderTimes" to reminderTimes,
        "instructions" to instructions,
        "active" to active
    )

    companion object {
        private val TIME_PATTERN = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")

        // Bad remote data returns null instead of crashing the whole medication list.
        fun fromFirestore(id: String, data: Map<String, Any?>): Medication? {
            val patientId = data["patientId"] as? String ?: return null
            val name = data["name"] as? String ?: return null
            val reminderTimes = (data["reminderTimes"] as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()
            return Medication(
                id = id,
                patientId = patientId,
                name = name,
                strength = data["strength"] as? String ?: "",
                dose = data["dose"] as? String ?: "",
                frequency = data["frequency"] as? String ?: "",
                reminderTimes = reminderTimes,
                instructions = data["instructions"] as? String ?: "",
                active = data["active"] as? Boolean ?: true
            )
        }
    }
}
