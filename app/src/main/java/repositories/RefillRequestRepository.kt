package repositories

import com.example.carelink.data.FirestoreRefillRequestRepository
import com.example.carelink.data.RefillRequestAlreadyActiveException
import com.example.carelink.model.RefillRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class RefillRequestRepository(
    firestore: FirebaseFirestore,
    readSource: () -> Source = { Source.DEFAULT }
) {
    private val storage = FirestoreRefillRequestRepository(firestore, readSource)

    fun getRefillRequests(
        patientId: String,
        onSuccess: (List<RefillRequest>) -> Unit,
        onError: (String) -> Unit
    ) {
        storage.load(patientId) { result ->
            result.onSuccess(onSuccess).onFailure {
                onError("Couldn't load your refill requests. Please try again.")
            }
        }
    }

    fun createRefillRequest(
        request: RefillRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        storage.create(request) { result ->
            result.onSuccess { onSuccess() }.onFailure { error ->
                onError(if (generateSequence(error) { it.cause }.any { it is RefillRequestAlreadyActiveException })
                    "A refill request is already active for this medication."
                else "Couldn't submit refill request. Please try again.")
            }
        }
    }
}
