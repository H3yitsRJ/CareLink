package com.example.carelink.data

import com.example.carelink.model.CarePermission
import com.example.carelink.model.CaregiverAccess
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges

data class CareRecipient(val patientId: String, val name: String)

interface CareRecipientDirectory {
    fun watch(caregiverId: String, changed: (Result<List<CareRecipient>>) -> Unit): () -> Unit
}

class FirestoreCareRecipientDirectory(private val db: FirebaseFirestore) : CareRecipientDirectory {
    override fun watch(caregiverId: String, changed: (Result<List<CareRecipient>>) -> Unit): () -> Unit {
        var active = true
        val listener = db.collectionGroup("medicationCaregivers").whereEqualTo("caregiverId", caregiverId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (active) {
                    if (error != null || snapshot == null || snapshot.metadata.isFromCache) {
                        changed(Result.failure(error ?: IllegalStateException("Connect to verify access")))
                    } else changed(Result.success(snapshot.documents.mapNotNull { document ->
                        recipient(caregiverId, document.reference.path, document.data.orEmpty())
                    }.distinctBy { it.patientId }.sortedBy { it.name.lowercase() }))
                }
            }
        return { active = false; listener.remove() }
    }

    companion object {
        internal fun recipient(actorId: String, path: String, data: Map<String, Any?>): CareRecipient? {
            val parts = path.split('/')
            if (parts.size != 4 || parts[0] != "users" || parts[2] != "medicationCaregivers" || parts[3] != actorId) return null
            val grant = CaregiverAccess.fromFirestore(parts[3], data) ?: return null
            if (grant.caregiverId != actorId || grant.patientId != parts[1] || grant.patientId == actorId || !grant.allows(CarePermission.VIEW)) return null
            return CareRecipient(grant.patientId, (data["patientName"] as? String)?.takeIf { it.isNotBlank() } ?: grant.patientId)
        }
    }
}
