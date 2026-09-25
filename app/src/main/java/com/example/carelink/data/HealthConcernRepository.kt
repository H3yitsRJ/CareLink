package com.example.carelink.data

import com.example.carelink.model.*
import com.google.firebase.firestore.FirebaseFirestore

interface HealthConcernRepository {
    fun watchList(patientId: String, changed: (Result<List<HealthConcern>>) -> Unit): () -> Unit
    fun watchConcern(patientId: String, id: String, changed: (Result<HealthConcern?>) -> Unit): () -> Unit
    fun watchAppointments(patientId: String, changed: (Result<List<Appointment>>) -> Unit): () -> Unit
    fun setStatus(patientId: String, id: String, status: ConcernStatus, completed: (Result<Unit>) -> Unit)
    fun linkAppointment(patientId: String, id: String, appointmentId: String?, completed: (Result<Unit>) -> Unit)
}

class FirestoreHealthConcernRepository(private val db: FirebaseFirestore) : HealthConcernRepository {
    private fun concerns(patientId: String) = db.collection("users").document(patientId).collection("healthConcerns")
    private fun appointments(patientId: String) = db.collection("users").document(patientId).collection("appointments")
    override fun watchList(patientId: String, changed: (Result<List<HealthConcern>>) -> Unit): () -> Unit {
        val listener = concerns(patientId).addSnapshotListener { snapshot, error ->
            if (error != null) changed(Result.failure(error))
            else if (snapshot != null) changed(Result.success(snapshot.documents.mapNotNull {
                HealthConcern.fromFirestore(it.id, it.data.orEmpty())
            }))
        }
        return { listener.remove() }
    }
    override fun watchConcern(patientId: String, id: String, changed: (Result<HealthConcern?>) -> Unit): () -> Unit {
        val listener = concerns(patientId).document(id).addSnapshotListener { snapshot, error ->
            if (error != null) changed(Result.failure(error))
            else if (snapshot != null) changed(Result.success(if (snapshot.exists())
                HealthConcern.fromFirestore(snapshot.id, snapshot.data.orEmpty()) else null))
        }
        return { listener.remove() }
    }
    override fun watchAppointments(patientId: String, changed: (Result<List<Appointment>>) -> Unit): () -> Unit {
        val listener = appointments(patientId).addSnapshotListener { snapshot, error ->
            if (error != null) changed(Result.failure(error))
            else if (snapshot != null) changed(Result.success(snapshot.documents.map { Appointment.fromFirestore(it.id, it.data.orEmpty()) }))
        }
        return { listener.remove() }
    }
    override fun setStatus(patientId: String, id: String, status: ConcernStatus, completed: (Result<Unit>) -> Unit) {
        concerns(patientId).document(id).update("status", status.name)
            .addOnSuccessListener { completed(Result.success(Unit)) }.addOnFailureListener { completed(Result.failure(it)) }
    }
    override fun linkAppointment(patientId: String, id: String, appointmentId: String?, completed: (Result<Unit>) -> Unit) {
        val reference = concerns(patientId).document(id)
        db.runTransaction { transaction ->
            check(transaction.get(reference).exists()) { "Concern no longer exists" }
            if (appointmentId != null) check(transaction.get(appointments(patientId).document(appointmentId)).exists()) {
                "Appointment no longer exists"
            }
            transaction.update(reference, "appointmentId", appointmentId)
        }.addOnSuccessListener { completed(Result.success(Unit)) }.addOnFailureListener { completed(Result.failure(it)) }
    }
}
