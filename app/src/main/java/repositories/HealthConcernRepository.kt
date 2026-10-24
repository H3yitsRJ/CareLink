package repositories

import com.example.carelink.model.ConcernStatus
import com.example.carelink.model.HealthConcern
import com.google.firebase.firestore.FirebaseFirestore

class HealthConcernRepository(
    private val firestore: FirebaseFirestore
) {

    fun getHealthConcerns(
        patientId: String,
        onSuccess: (List<HealthConcern>) -> Unit,
        onError: (String) -> Unit
    ) {
        firestore
            .collection("users")
            .document(patientId)
            .collection("healthConcerns")
            .get()
            .addOnSuccessListener { snapshot ->
                val concerns =
                    snapshot.documents.mapNotNull { document ->
                        HealthConcern.fromFirestore(
                            document.id,
                            document.data.orEmpty()
                        )
                    }

                onSuccess(concerns)
            }
            .addOnFailureListener {
                onError("We couldn't load your health concerns. Please try again.")
            }
    }

    fun getSingleConcern(
        patientId: String,
        concernId: String,
        onSuccess: (HealthConcern?) -> Unit,
        onError: (String) -> Unit
    ) {
        firestore
            .collection("users")
            .document(patientId)
            .collection("healthConcerns")
            .document(concernId)
            .get()
            .addOnSuccessListener { document ->
                val concern =
                    if(document.exists()) {
                        HealthConcern.fromFirestore(
                            document.id,
                            document.data.orEmpty()
                        )
                    } else {
                        null
                    }
                onSuccess(concern)
            }
            .addOnFailureListener {
                onError("We couldn't load this health concern. Please try again.")
            }
    }

    fun saveHealthConcern(
        patientId: String,
        concern: HealthConcern,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val concernsRef =
            firestore
                .collection("users")
                .document(patientId)
                .collection("healthConcerns")

        val concernId =
            concern.id.ifBlank {
                concernsRef.document().id
            }

        val concernToSave =
            concern.copy(
                id = concernId,
                patientId = patientId
            )

        concernsRef
            .document(concernId)
            .set(concernToSave.toFirestore())
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError("Unable to save health concern. Please try again.")
            }
    }

    fun updateConcernStatus(
        patientId: String,
        concernId: String,
        status: ConcernStatus,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firestore
            .collection("users")
            .document(patientId)
            .collection("healthConcerns")
            .document(concernId)
            .update(
                "status",
                status.name
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError("Unable to update concern status. Please try again.")
            }
    }

    fun linkAppointment(
        patientId: String,
        concernId: String,
        appointmentId: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firestore
            .collection("users")
            .document(patientId)
            .collection("healthConcerns")
            .document(concernId)
            .update(
                "appointmentId",
                appointmentId
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError("Unable to link appointment. Please try again.")
            }
    }
}