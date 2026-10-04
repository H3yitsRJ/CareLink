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
    override fun watch(
        caregiverId: String,
        changed: (Result<List<CareRecipient>>) -> Unit
    ): () -> Unit {

        var active = true

        val listener = db.collection("caregiverAccess")
            .document(caregiverId)
            .collection("recipients")
            .addSnapshotListener { snapshot, error ->

                if (!active) return@addSnapshotListener

                if (error != null) {
                    changed(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    changed(Result.success(emptyList()))
                    return@addSnapshotListener
                }

                val recipients = snapshot.documents.mapNotNull { document ->

                    val patientId =
                        document.getString("patientId")
                            ?: document.id

                    val patientName =
                        document.getString("patientName")
                            ?: patientId

                    CareRecipient(
                        patientId = patientId,
                        name = patientName
                    )
                }

                changed(
                    Result.success(
                        recipients.sortedBy {
                            it.name.lowercase()
                        }
                    )
                )
            }

        return {
            active = false
            listener.remove()
        }
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
