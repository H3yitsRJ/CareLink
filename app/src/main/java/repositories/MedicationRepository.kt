package repositories

import com.example.carelink.model.Medication
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class MedicationRepository(
    private val firestore: FirebaseFirestore
) {
    fun getMedications(
        userId: String,
        connectionAvailable: Boolean,
        onSuccess: (List<Medication>) -> Unit,
        onError: (String) -> Unit
    ) {
        firestore
            .collection("users")
            .document(userId)
            .collection("medications")
            .get(
                if(connectionAvailable) {
                    Source.DEFAULT
                } else {
                    Source.CACHE
                }
            )
            .addOnSuccessListener { result ->
                val medications =
                    result.documents.mapNotNull { document ->
                        Medication.fromFirestore(
                            document.id,
                            document.data.orEmpty() +
                                    ("patientId" to userId)
                        )
                    }

                onSuccess(medications)
            }
            .addOnFailureListener {
                onError("We couldn't load your medications. Please try again.")
            }
    }

    fun saveMedication(
        userId: String,
        medication: Medication,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val medicationData =
            medication.copy(
                patientId = userId,
                updatedById = userId
            )
                .toFirestore()
                .toMutableMap()

        medicationData["updatedAt"] =
            FieldValue.serverTimestamp()

        firestore
            .collection("users")
            .document(userId)
            .collection("medications")
            .document(medication.id)
            .set(medicationData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError("We couldn't save the medication. Please try again.")
            }
    }

    fun deleteMedication(
        userId: String,
        medicationId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firestore
            .collection("users")
            .document(userId)
            .collection("medications")
            .document(medicationId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError("We couldn't remove the medication. Please try again.")
            }
    }
}