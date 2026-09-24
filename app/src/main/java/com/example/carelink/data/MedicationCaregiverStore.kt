package com.example.carelink.data

import com.example.carelink.model.CarePermission
import com.example.carelink.model.CaregiverAccess
import com.example.carelink.model.Medication
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges

interface CaregiverMedicationData {
    fun watchAccess(patientId: String, caregiverId: String, changed: (CaregiverAccess?, String, Boolean) -> Unit): () -> Unit
    fun watchMedications(patientId: String, changed: (List<Medication>?, String?) -> Unit): () -> Unit
    fun edit(original: Medication, edited: Medication, actorId: String, completed: (Result<Medication>) -> Unit)
}

/** Medication-only delegation. The patient owns grants; caregivers cannot grant themselves access. */
class MedicationCaregiverStore(private val db: FirebaseFirestore) : CaregiverMedicationData {
    fun grants(patientId: String) = db.collection("users").document(patientId).collection("medicationCaregivers")
    fun medications(patientId: String) = db.collection("users").document(patientId).collection("medications")

    fun saveAccess(access: CaregiverAccess, patientName: String) = db.runTransaction { transaction ->
        val reference = grants(access.patientId).document(access.caregiverId)
        transaction.get(reference)
        transaction.set(reference, mapOf("patientId" to access.patientId, "caregiverId" to access.caregiverId,
            "patientName" to patientName.take(200), "permissions" to access.permissions.map { it.name }, "revoked" to access.revoked))
    }

    override fun watchAccess(patientId: String, caregiverId: String, changed: (CaregiverAccess?, String, Boolean) -> Unit): () -> Unit {
        var active = true
        val listener = grants(patientId).document(caregiverId).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, failure ->
            if (active) changed(snapshot?.data?.let { access(patientId, caregiverId, it) },
                snapshot?.getString("patientName").orEmpty(), failure == null && snapshot != null && !snapshot.metadata.isFromCache)
        }
        return { active = false; listener.remove() }
    }

    override fun watchMedications(patientId: String, changed: (List<Medication>?, String?) -> Unit): () -> Unit {
        var active = true
        val listener = medications(patientId).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, failure ->
            if (active) {
                if (failure != null || snapshot == null || snapshot.metadata.isFromCache)
                    changed(null, "Connect to the internet and verify your access to load medications.")
                else changed(snapshot.documents.mapNotNull { Medication.fromFirestore(it.id, it.data.orEmpty()) }, null)
            }
        }
        return { active = false; listener.remove() }
    }

    // Transactions require a connection and rules recheck access at commit, including revocation races.
    override fun edit(original: Medication, edited: Medication, actorId: String, completed: (Result<Medication>) -> Unit) {
        db.runTransaction { transaction ->
            val reference = medications(original.patientId).document(original.id)
            val snapshot = transaction.get(reference)
            val current = Medication.fromFirestore(snapshot.id, snapshot.data.orEmpty())
            val fields = editFields(original, current, edited) + mapOf("updatedById" to actorId, "updatedAt" to FieldValue.serverTimestamp())
            transaction.update(reference, fields)
            edited.copy(updatedById = actorId)
        }.addOnSuccessListener { completed(Result.success(it)) }
            .addOnFailureListener { completed(Result.failure(it)) }
    }

    companion object {
        internal fun editFields(original: Medication, current: Medication?, edited: Medication): Map<String, Any> {
            require(original.id == edited.id && original.patientId == edited.patientId)
            require(edited.validate().isEmpty())
            check(current == original) { "Medication changed. Reopen it before saving." }
            return editableFields(edited)
        }
        fun validId(id: String) = id.isNotBlank() && id.length <= 128 && '/' !in id && id != "." && id != ".."
        fun access(patientId: String, caregiverId: String, data: Map<String, Any?>): CaregiverAccess? {
            if (data["patientId"] != patientId || data["caregiverId"] != caregiverId) return null
            val permissions = (data["permissions"] as? List<*>)?.mapNotNull { value ->
                CarePermission.entries.find { it.name == value }
            }?.toSet() ?: return null
            return CaregiverAccess(caregiverId, patientId, caregiverId, permissions, data["revoked"] != false)
        }
        fun editableFields(medication: Medication): Map<String, Any> = medication.toFirestore().filterKeys {
            it in setOf("name", "strength", "dose", "frequency", "reminderTimes", "instructions")
        }
    }
}
