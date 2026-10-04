package com.example.carelink.data

import com.example.carelink.model.CareTask
import com.example.carelink.model.validationErrors
import com.example.carelink.model.validCareDocumentId as validDocumentId
import com.google.firebase.firestore.FirebaseFirestore

interface CareTaskRepository {
    fun create(task: CareTask, completed: (Result<CareTask>) -> Unit)
    fun update(task: CareTask, completed: (Result<CareTask>) -> Unit)
    fun list(patientId: String, completed: (Result<List<CareTask>>) -> Unit)
    fun setCompleted(patientId: String, id: String, completed: Boolean, result: (Result<Unit>) -> Unit)
    fun delete(patientId: String, id: String, result: (Result<Unit>) -> Unit)
}

class FirestoreCareTaskRepository(private val db: FirebaseFirestore) : CareTaskRepository {
    private fun tasks(patientId: String) = db.collection("users").document(patientId).collection("careTasks")

    override fun create(task: CareTask, completed: (Result<CareTask>) -> Unit) {
        val error = creationError(task)
        if (error != null) { completed(Result.failure(error)); return }
        val reference = tasks(task.patientId).document(task.id)
        db.runTransaction { transaction ->
            check(!transaction.get(reference).exists()) { "Care task already exists" }
            transaction.set(reference, task.toFirestore())
            task
        }.addOnSuccessListener { completed(Result.success(it)) }
            .addOnFailureListener { completed(Result.failure(it)) }
    }

    override fun list(patientId: String, completed: (Result<List<CareTask>>) -> Unit) {
        if (!validDocumentId(patientId)) { completed(Result.failure(IllegalArgumentException("Invalid patient ID"))); return }
        tasks(patientId).get().addOnSuccessListener { snapshot ->
            completed(Result.success(snapshot.documents.mapNotNull { CareTask.fromFirestore(it.id, it.data.orEmpty()) }))
        }.addOnFailureListener { completed(Result.failure(it)) }
    }

    override fun setCompleted(patientId: String, id: String, completed: Boolean, result: (Result<Unit>) -> Unit) {
        if (!validDocumentId(patientId) || !validDocumentId(id)) { result(Result.failure(IllegalArgumentException("Invalid task identity"))); return }
        tasks(patientId).document(id).update(mapOf("completed" to completed, "status" to if (completed) "COMPLETED" else "PENDING"))
            .addOnSuccessListener { result(Result.success(Unit)) }
            .addOnFailureListener { result(Result.failure(it)) }
    }

    override fun update(task: CareTask, completed: (Result<CareTask>) -> Unit) {
        val error = taskError(task, creating = false)
        if (error != null) { completed(Result.failure(error)); return }
        val reference = tasks(task.patientId).document(task.id)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(reference)
            check(snapshot.exists() && snapshot.getString("patientId") == task.patientId) { "Care task not found" }
            transaction.update(reference, task.toFirestore())
            task
        }.addOnSuccessListener { completed(Result.success(it)) }
            .addOnFailureListener { completed(Result.failure(it)) }
    }

    override fun delete(
        patientId: String,
        id: String,
        result: (Result<Unit>) -> Unit
    ) {
        if (!validDocumentId(patientId) || !validDocumentId(id)) {
            result(Result.failure(IllegalArgumentException("Invalid task identity")))
            return
        }

        val reference = tasks(patientId).document(id)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(reference)
            check(
                snapshot.exists() &&
                        snapshot.getString("patientId") == patientId
            ) {
                "Care task not found"
            }
            transaction.delete(reference)
            Unit
        }.addOnSuccessListener {
            result(Result.success(Unit))
        }.addOnFailureListener {
            result(Result.failure(it))
        }
    }

}

class InMemoryCareTaskRepository : CareTaskRepository {
    private val tasks = linkedMapOf<String, CareTask>()
    override fun create(task: CareTask, completed: (Result<CareTask>) -> Unit) {
        val error = creationError(task)
        when {
            error != null -> completed(Result.failure(error))
            tasks.containsKey(task.id) -> completed(Result.failure(IllegalStateException("Care task already exists")))
            else -> { tasks[task.id] = task; completed(Result.success(task)) }
        }
    }
    override fun list(patientId: String, completed: (Result<List<CareTask>>) -> Unit) {
        if (!validDocumentId(patientId)) { completed(Result.failure(IllegalArgumentException("Invalid patient ID"))); return }
        completed(Result.success(tasks.values.filter { it.patientId == patientId }))
    }
    override fun update(task: CareTask, completed: (Result<CareTask>) -> Unit) {
        val error = taskError(task, creating = false)
        when {
            error != null -> completed(Result.failure(error))
            tasks[task.id]?.patientId != task.patientId -> completed(Result.failure(NoSuchElementException("Care task not found")))
            else -> { tasks[task.id] = task; completed(Result.success(task)) }
        }
    }
    override fun setCompleted(patientId: String, id: String, completed: Boolean, result: (Result<Unit>) -> Unit) {
        val task = tasks[id]?.takeIf { it.patientId == patientId }
        if (task == null) result(Result.failure(NoSuchElementException("Care task not found")))
        else { tasks[id] = task.copy(completed = completed); result(Result.success(Unit)) }
    }
    override fun delete(patientId: String, id: String, result: (Result<Unit>) -> Unit) {
        if (!validDocumentId(patientId) || !validDocumentId(id)) {
            result(Result.failure(IllegalArgumentException("Invalid task identity")))
            return
        }

        val task = tasks[id]?.takeIf { it.patientId == patientId }

        if (task == null) {
            result(Result.failure(NoSuchElementException("Care task not found")))
        } else {
            tasks.remove(id)
            result(Result.success(Unit))
        }
    }

}

private fun creationError(task: CareTask) = taskError(task, creating = true)

private fun taskError(task: CareTask, creating: Boolean): Exception? {
    if (!validDocumentId(task.id) || !validDocumentId(task.patientId)) return IllegalArgumentException("Patient and task ID are required")
    if (task.appointmentId != null && !validDocumentId(task.appointmentId)) return IllegalArgumentException("Invalid appointment reference")
    val errors = task.validationErrors(requireFuture = creating)
    return errors.values.firstOrNull()?.let { IllegalArgumentException(it) }
}
