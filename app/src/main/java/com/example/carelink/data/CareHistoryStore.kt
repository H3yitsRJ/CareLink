package com.example.carelink.data

import com.example.carelink.model.CareActivityType
import com.example.carelink.model.CareHistoryEntry
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

/** Combines dose records, shared events, and legacy caregiver events. */
class CareHistoryStore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun load(
        patientId: String,
        completed: (Result<List<CareHistoryEntry>>) -> Unit
    ) {
        DoseHistoryStore(firestore).load(patientId) { doses ->
            doses.fold(
                onSuccess = { items ->
                    firestore.collection("users")
                        .document(patientId)
                        .collection("careHistory")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val entries = items.map { item ->
                                CareHistoryEntry(
                                    id = "dose:${item.record.id}",
                                    patientId = patientId,
                                    occurredAtMillis = item.record.scheduledTimeMillis,
                                    type = CareActivityType.MEDICATION,
                                    summary =
                                        "${item.medicationName} • ${item.dosage} • " +
                                                item.record.status.name.lowercase()
                                                    .replaceFirstChar(Char::uppercase),
                                    relatedRecordId = item.record.id
                                )
                            } + snapshot.documents.mapNotNull { document ->
                                val data = document.data.orEmpty()
                                CareHistoryEntry.fromFirestore(document.id, data)
                                    ?.takeIf { it.patientId == patientId }
                                    ?: caregiverHistoryEntry(
                                        document.id,
                                        patientId,
                                        data
                                    )
                            }

                            completed(
                                Result.success(
                                    CareHistoryEntry.newestFirst(entries)
                                )
                            )
                        }
                        .addOnFailureListener {
                            completed(Result.failure(it))
                        }
                },
                onFailure = {
                    completed(Result.failure(it))
                }
            )
        }
    }
}

internal fun caregiverHistoryEntry(
    id: String,
    patientId: String,
    data: Map<String, Any>
): CareHistoryEntry? {
    if (data["patientId"] != patientId ||
        data["type"] != "CAREGIVER_ACCESS"
    ) return null

    val title = when (data["action"]) {
        "GRANTED" -> "Caregiver access granted"
        "UPDATED" -> "Caregiver access updated"
        "REVOKED" -> "Caregiver access revoked"
        else -> return null
    }

    val occurredAt = data["occurredAt"] as? Timestamp ?: return null

    return CareHistoryEntry(
        id = "access:$id",
        patientId = patientId,
        occurredAtMillis = occurredAt.toDate().time,
        type = CareActivityType.CAREGIVER_ACCESS,
        summary = title,
        actorId = (data["actorId"] as? String)
            ?.takeIf { it.isNotBlank() },
        relatedRecordId = (data["caregiverId"] as? String)
            ?.takeIf { it.isNotBlank() }
    )
}