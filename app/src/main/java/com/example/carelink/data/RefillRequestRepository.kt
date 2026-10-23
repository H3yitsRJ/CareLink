package com.example.carelink.data

import com.example.carelink.model.Medication
import com.example.carelink.model.RefillRequest
import com.example.carelink.model.RefillRequestStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

interface RefillRequestRepository {
    fun create(request: RefillRequest, completed: (Result<RefillRequest>) -> Unit)
}

class RefillRequestAlreadyActiveException : IllegalStateException("A refill request is already active for this medication.")

class FirestoreRefillRequestRepository(
    private val db: FirebaseFirestore,
    private val readSource: () -> Source = { Source.DEFAULT }
) : RefillRequestRepository {

    fun load(patientId: String, onResult: (Result<List<RefillRequest>>) -> Unit) {
        db.collection("users").document(patientId).collection("refillRequests")
            .get(readSource())
            .addOnSuccessListener { snapshot ->
                onResult(runCatching {
                    snapshot.documents.map { document ->
                        refillRequestFromFirestore(document.id, patientId, document.data.orEmpty())
                    }.sortedByDescending { it.requestedAt }
                })
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun create(request: RefillRequest, completed: (Result<RefillRequest>) -> Unit) {
        val error = request.validationErrors().values.firstOrNull()
        if (error != null) { completed(Result.failure(IllegalArgumentException(error))); return }
        val patient = db.collection("users").document(request.patientId)
        val reference = patient.collection("refillRequests").document(request.medicationId)
        db.runTransaction { transaction ->
            val existing = transaction.get(reference)
            val medication = transaction.get(patient.collection("medications").document(request.medicationId))
            if (existing.exists() && existing.getString("status") !in setOf("COMPLETED", "CANCELLED")) throw RefillRequestAlreadyActiveException()
            check(medication.exists() && medication.getString("patientId") == request.patientId && medication.getBoolean("active") != false) {
                "This medication is no longer available for a refill request."
            }
            val saved = request.copy(medicationName = medication.getString("name").orEmpty())
            transaction.set(reference, saved.toFirestore() + ("requestedAt" to FieldValue.serverTimestamp()))
            saved
        }
            .addOnSuccessListener { completed(Result.success(it)) }
            .addOnFailureListener { completed(Result.failure(it)) }
    }

}

internal fun refillRequestFromFirestore(
    id: String,
    patientId: String,
    data: Map<String, Any>
): RefillRequest =
    RefillRequest.fromFirestore(id, patientId, data)

class InMemoryRefillRequestRepository(private val medications: () -> List<Medication>) : RefillRequestRepository {
    private val requests = mutableMapOf<Pair<String, String>, RefillRequest>()
    @Synchronized override fun create(request: RefillRequest, completed: (Result<RefillRequest>) -> Unit) {
        completed(runCatching {
            require(request.validationErrors().isEmpty()) { request.validationErrors().values.first() }
            check(medications().any { it.id == request.medicationId && it.patientId == request.patientId && it.active }) {
                "This medication is no longer available for a refill request."
            }
            val key = request.patientId to request.medicationId
            if (key in requests) throw RefillRequestAlreadyActiveException()
            requests[key] = request
            request
        })
    }
}
