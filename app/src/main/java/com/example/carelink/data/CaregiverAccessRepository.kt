// Firestore caregiver-access writes under patients/{patientId}/caregiverAccess. Revocation batches the
// access change with a care-history entry. Not connected to MainActivity yet.

package com.example.carelink.data

import com.example.carelink.model.CareActivityType
import com.example.carelink.model.CareHistoryEntry
import com.example.carelink.model.CaregiverAccess
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore

class CaregiverAccessRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun save(access: CaregiverAccess): Task<Void> = accessDocument(access)
        .set(access.copy(revoked = false).toFirestore())

    fun revoke(access: CaregiverAccess, changedById: String): Task<Void> {
        val revoked = access.copy(revoked = true)
        val history = CareHistoryEntry(
            id = "caregiver-access-${System.currentTimeMillis()}",
            patientId = access.patientId,
            occurredAtMillis = System.currentTimeMillis(),
            type = CareActivityType.CAREGIVER_ACCESS,
            summary = "Caregiver access revoked",
            changedById = changedById
        )
        return firestore.runBatch { batch ->
            batch.set(accessDocument(revoked), revoked.toFirestore())
            batch.set(
                patientDocument(access.patientId).collection("careHistory").document(history.id),
                history.toFirestore()
            )
        }
    }

    private fun accessDocument(access: CaregiverAccess) = patientDocument(access.patientId)
        .collection("caregiverAccess").document(access.caregiverId)

    private fun patientDocument(patientId: String) = firestore.collection("patients").document(patientId)
}
