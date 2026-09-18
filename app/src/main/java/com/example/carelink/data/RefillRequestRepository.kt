// Firestore refill-request creation, not yet connected to MainActivity. Checks existing active requests
// before writing; this query-then-write sequence is not an atomic duplicate guarantee.

package com.example.carelink.data

import com.example.carelink.model.RefillRequest
import com.example.carelink.model.RefillRequestStatus
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore

class RefillRequestRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun create(request: RefillRequest): Task<RefillRequest> {
        if (request.patientId.isBlank() || request.medicationId.isBlank() || request.requestedById.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Patient, medication, and requester are required"))
        }
        val requests = collection(request.patientId)
        return requests.whereEqualTo("medicationId", request.medicationId).get()
            .continueWithTask { query ->
                if (!query.isSuccessful) throw query.exception ?: IllegalStateException("Refill requests could not be checked")
                val hasActiveRequest = query.result.documents.any { document ->
                    document.getString("status") in setOf(
                        RefillRequestStatus.REQUESTED.name,
                        RefillRequestStatus.PROCESSING.name
                    )
                }
                if (hasActiveRequest) {
                    Tasks.forException(IllegalStateException("A refill request is already active for this medication"))
                } else {
                    requests.document(request.id).set(
                        mapOf(
                            "patientId" to request.patientId,
                            "medicationId" to request.medicationId,
                            "requestedById" to request.requestedById,
                            "note" to request.note,
                            "status" to request.status.name
                        )
                    ).continueWith { result ->
                        if (!result.isSuccessful) throw result.exception ?: IllegalStateException("Refill request was not saved")
                        request
                    }
                }
            }
    }

    private fun collection(patientId: String) = firestore.collection("patients")
        .document(patientId).collection("refillRequests")
}
