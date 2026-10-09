
package com.example.carelink.data

import com.example.carelink.model.RefillRequest
import com.example.carelink.model.RefillRequestStatus
import com.google.firebase.firestore.FirebaseFirestore

class RefillRequestRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {
    fun save(
        request: RefillRequest,
        onResult: (Result<Unit>) -> Unit
    ) {
        firestore.collection("users")
            .document(request.patientId)
            .collection("refillRequests")
            .document(request.id)
            .set(request)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }

    fun load(
        patientId: String,
        onResult: (Result<List<RefillRequest>>) -> Unit
    ) {
        firestore.collection("users")
            .document(patientId)
            .collection("refillRequests")
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    val requests = snapshot.documents.map { doc ->
                        RefillRequest(
                            id = doc.id,
                            patientId = patientId,
                            medicationId = doc.getString("medicationId").orEmpty(),
                            requestedById = doc.getString("requestedById").orEmpty(),
                            note = doc.getString("note").orEmpty(),
                            status = runCatching {
                                RefillRequestStatus.valueOf(
                                    doc.getString("status") ?: "REQUESTED"
                                )
                            }.getOrDefault(RefillRequestStatus.REQUESTED),
                            requestedAt = doc.getLong("requestedAt") ?: 0L
                        )
                    }
                    onResult(Result.success(
                        requests.sortedByDescending { it.requestedAt }
                    ))
                } catch (e: Exception) {
                    onResult(Result.failure(e))
                }
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }
}
