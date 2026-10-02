package com.example.carelink.data

import com.example.carelink.model.DoseRecord
import com.example.carelink.model.DoseStatus
import com.example.carelink.model.Medication
import com.google.firebase.firestore.FirebaseFirestore

/** A dose and the medication details as they were when the outcome was recorded. */
data class DoseHistoryItem(
    val record: DoseRecord,
    val medicationName: String,
    val dosage: String
)

class DoseHistoryStore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun collection(patientId: String) = firestore
        .collection("users")
        .document(patientId)
        .collection("doseRecords")

    /** Records an explicit patient action; reminders alone never imply a missed dose. */
    fun record(
        patientId: String,
        medication: Medication,
        scheduledTimeMillis: Long,
        status: DoseStatus,
        onComplete: (Result<DoseHistoryItem>) -> Unit
    ) {
        if (
            patientId.isBlank() ||
            medication.patientId != patientId ||
            scheduledTimeMillis <= 0 ||
            status == DoseStatus.SCHEDULED
        ) {
            onComplete(
                Result.failure(
                    IllegalArgumentException("Invalid dose record")
                )
            )
            return
        }

        val record = DoseRecord.completed(
            medication.id,
            scheduledTimeMillis,
            status
        )
        val item = DoseHistoryItem(
            record = record,
            medicationName = medication.name,
            dosage = medication.dose
        )

        collection(patientId)
            .document(record.id)
            .set(
                record.toFirestore() + mapOf(
                    "medicationName" to item.medicationName,
                    "dosage" to item.dosage
                )
            )
            .addOnSuccessListener {
                onComplete(Result.success(item))
            }
            .addOnFailureListener { exception ->
                onComplete(Result.failure(exception))
            }
    }

    /** Loads recorded outcomes, newest scheduled dose first. */
    fun load(
        patientId: String,
        onComplete: (Result<List<DoseHistoryItem>>) -> Unit
    ) {
        if (patientId.isBlank()) {
            onComplete(
                Result.failure(
                    IllegalArgumentException("Sign in to view dose history")
                )
            )
            return
        }

        collection(patientId)
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents
                    .mapNotNull { document ->
                        val data = document.data ?: return@mapNotNull null
                        val record = DoseRecord.fromFirestore(
                            document.id,
                            data
                        ) ?: return@mapNotNull null

                        if (record.status == DoseStatus.SCHEDULED) {
                            return@mapNotNull null
                        }

                        DoseHistoryItem(
                            record = record,
                            medicationName = data["medicationName"] as? String
                                ?: "Medication unavailable",
                            dosage = data["dosage"] as? String
                                ?: "Dosage unavailable"
                        )
                    }
                    .sortedWith(
                        compareByDescending<DoseHistoryItem> {
                            it.record.scheduledTimeMillis
                        }.thenByDescending {
                            it.record.completionTimeMillis ?: 0L
                        }
                    )

                onComplete(Result.success(items))
            }
            .addOnFailureListener { exception ->
                onComplete(Result.failure(exception))
            }
    }
}