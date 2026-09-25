package com.example.carelink.data

import com.example.carelink.model.CareTask
import com.example.carelink.model.validationErrors
import com.google.firebase.firestore.FirebaseFirestore

interface CareTaskRepository {
    fun create(task: CareTask, completed: (Result<CareTask>) -> Unit)
    fun list(patientId: String, completed: (Result<List<CareTask>>) -> Unit)
    fun setCompleted(patientId: String, id: String, completed: Boolean, result: (Result<Unit>) -> Unit)
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
        tasks(patientId).get().addOnSuccessListener { snapshot ->
            completed(Result.success(snapshot.documents.mapNotNull { CareTask.fromFirestore(it.id, it.data.orEmpty()) }))
        }.addOnFailureListener { completed(Result.failure(it)) }
    }

    override fun setCompleted(patientId: String, id: String, completed: Boolean, result: (Result<Unit>) -> Unit) {
        tasks(patientId).document(id).update("completed", completed)
            .addOnSuccessListener { result(Result.success(Unit)) }
            .addOnFailureListener { result(Result.failure(it)) }
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
        completed(Result.success(tasks.values.filter { it.patientId == patientId }))
    }
    override fun setCompleted(patientId: String, id: String, completed: Boolean, result: (Result<Unit>) -> Unit) {
        val task = tasks[id]?.takeIf { it.patientId == patientId }
        if (task == null) result(Result.failure(NoSuchElementException("Care task not found")))
        else { tasks[id] = task.copy(completed = completed); result(Result.success(Unit)) }
    }
}

private fun creationError(task: CareTask): Exception? {
    if (task.id.isBlank() || task.patientId.isBlank()) return IllegalArgumentException("Patient and task ID are required")
    val errors = task.validationErrors()
    return errors.values.firstOrNull()?.let { IllegalArgumentException(it) }
}
