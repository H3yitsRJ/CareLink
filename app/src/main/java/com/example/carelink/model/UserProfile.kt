package com.example.carelink.model

/**
 * User profile data stored in Firestore.
 *
 * Required fields:
 * - userId
 * - fullName
 * - email
 *
 * Optional fields:
 * - dateOfBirth
 * - phoneNumber
 * - primaryPhysician
 * - preferredPharmacy
 */
data class UserProfile(
    val userId: String,
    val fullName: String,
    val email: String,
    val dateOfBirth: String? = null,
    val phoneNumber: String? = null,
    val primaryPhysician: String? = null,
    val preferredPharmacy: String? = null
) {
    fun toFirestore(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "fullName" to fullName,
        "email" to email,
        "dateOfBirth" to dateOfBirth,
        "phoneNumber" to phoneNumber,
        "primaryPhysician" to primaryPhysician,
        "preferredPharmacy" to preferredPharmacy
    )

    companion object {
        /**
         * Converts Firestore data back into a UserProfile object.
         *
         * Optional fields use safe casts, so missing or incorrectly typed
         * values become null rather than causing a crash.
        */
        fun fromFirestore(data: Map<String, Any?>): UserProfile? {
            // Read the required fields. Return null if any are missing
            // or cannot be converted to a String.
            val userId = data["userId"] as? String ?: return null
            val fullName = data["fullName"] as? String ?: return null
            val email = data["email"] as? String ?: return null

            // Create the profile using the required values and any
            // optional information that is available in Firestore.
            return UserProfile(
                userId = userId,
                fullName = fullName,
                email = email,
                dateOfBirth = data["dateOfBirth"] as? String,
                phoneNumber = data["phoneNumber"] as? String,
                primaryPhysician = data["primaryPhysician"] as? String,
                preferredPharmacy = data["preferredPharmacy"] as? String
            )
        }
    }
}