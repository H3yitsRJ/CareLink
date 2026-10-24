package repositories

import com.example.carelink.data.refillRequestFromFirestore
import com.example.carelink.model.RefillRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class RefillRequestRepository(
    private val firestore: FirebaseFirestore
) {
    fun getRefillRequests(
        patientId: String,
        connectionAvailable: Boolean,
        onSuccess: (List<RefillRequest>) -> Unit,
        onError: (String) -> Unit
    ) {
        firestore
            .collection("users")
            .document(patientId)
            .collection("refillRequests")
            .get(
                if(connectionAvailable)
                    Source.DEFAULT
                else
                    Source.CACHE
            )
            .addOnSuccessListener { snapshot ->
                val requests =
                    snapshot.documents.map { document ->
                        refillRequestFromFirestore(
                            document.id,
                            patientId,
                            document.data.orEmpty()
                        )
                    }.sortedByDescending {
                        it.requestedAt
                    }

                onSuccess(requests)
            }
            .addOnFailureListener {
                onError("Couldn't load your refill requests. Please try again.")
            }
    }

    fun createRefillRequest(
        request: RefillRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val requestRef =
            firestore
                .collection("users")
                .document(request.patientId)
                .collection("refillRequests")
                .document(request.medicationId)

        requestRef
            .set(
                request.toFirestore() +
                        (
                                "requestedAt" to
                                        FieldValue.serverTimestamp()
                                )
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError("Couldn't submit refill request. Please try again.")
            }
    }
}