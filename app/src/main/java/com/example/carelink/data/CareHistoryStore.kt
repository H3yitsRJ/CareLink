package com.example.carelink.data

import com.example.carelink.model.CareActivityType
import com.example.carelink.model.CareHistoryEntry
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

/** Combines existing dose records and patient-owned caregiver access events. */
class CareHistoryStore(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    fun load(patientId: String, completed: (Result<List<CareHistoryEntry>>) -> Unit) {
        DoseHistoryStore(firestore).load(patientId) { doses ->
            doses.fold(onSuccess = { items ->
                firestore.collection("users").document(patientId).collection("careHistory").get()
                    .addOnSuccessListener { snapshot ->
                        val entries = items.map { item ->
                            CareHistoryEntry(
                                "dose:${item.record.id}", patientId, item.record.scheduledTimeMillis,
                                CareActivityType.MEDICATION,
                                "${item.medicationName} • ${item.dosage} • " +
                                    item.record.status.name.lowercase().replaceFirstChar(Char::uppercase)
                            )
                        } + snapshot.documents.mapNotNull { document ->
                            caregiverHistoryEntry(document.id, patientId, document.data.orEmpty())
                        }
                        completed(Result.success(entries.sortedByDescending { it.occurredAtMillis }))
                    }
                    .addOnFailureListener { completed(Result.failure(it)) }
            }, onFailure = { completed(Result.failure(it)) })
        }
    }
}

internal fun caregiverHistoryEntry(id: String, patientId: String, data: Map<String, Any>): CareHistoryEntry? {
    if (data["patientId"] != patientId || data["type"] != "CAREGIVER_ACCESS") return null
    val title = when (data["action"]) {
        "GRANTED" -> "Caregiver access granted"
        "UPDATED" -> "Caregiver access updated"
        "REVOKED" -> "Caregiver access revoked"
        else -> return null
    }
    val occurredAt = data["occurredAt"] as? Timestamp ?: return null
    return CareHistoryEntry("access:$id", patientId, occurredAt.toDate().time, CareActivityType.CAREGIVER_ACCESS, title)
}
