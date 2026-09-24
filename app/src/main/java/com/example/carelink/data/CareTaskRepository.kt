// Care-task storage contracts and implementations.

package com.example.carelink.data

import com.example.carelink.model.CareTask
import com.example.carelink.model.isValidCareDocumentId
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore

// The repository keeps storage details out of the care-task screens.
interface CareTaskRepository {
    fun create(task: CareTask): Result<CareTask>
    fun list(patientId: String): List<CareTask>
    fun update(task: CareTask): Result<CareTask>
    fun setCompleted(id: String, completed: Boolean): Result<CareTask>
}

// A small in-memory version is enough to verify repository rules in unit tests.
class InMemoryCareTaskRepository : CareTaskRepository {
    private val tasks = linkedMapOf<String, CareTask>()

    override fun create(task: CareTask): Result<CareTask> {
        task.validate()?.let { return Result.failure(IllegalArgumentException(it)) }
        if (tasks.containsKey(task.id)) return Result.failure(IllegalStateException("Care task already exists"))
        tasks[task.id] = task
        return Result.success(task)
    }

    override fun list(patientId: String) = tasks.values.filter { it.patientId == patientId }

    override fun update(task: CareTask): Result<CareTask> {
        if (!tasks.containsKey(task.id)) return Result.failure(NoSuchElementException("Care task not found"))
        task.validate()?.let { return Result.failure(IllegalArgumentException(it)) }
        if (tasks[task.id]?.patientId != task.patientId) return Result.failure(IllegalArgumentException("Cannot change task owner"))
        tasks[task.id] = task
        return Result.success(task)
    }

    override fun setCompleted(id: String, completed: Boolean): Result<CareTask> {
        val task = tasks[id] ?: return Result.failure(NoSuchElementException("Care task not found"))
        val updated = task.copy(completed = completed)
        tasks[id] = updated
        return Result.success(updated)
    }
}

/** Durable storage. Transactions distinguish create from update and fail without a connection. */
class FirestoreCareTaskRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun create(task: CareTask): Task<CareTask> = write(task, create = true)
    fun update(task: CareTask): Task<CareTask> = write(task, create = false)

    private fun write(task: CareTask, create: Boolean): Task<CareTask> {
        task.validate()?.let { return Tasks.forException(IllegalArgumentException(it)) }
        val reference = collection(task.patientId).document(task.id)
        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(reference)
            if (create) check(!snapshot.exists()) { "Care task already exists" }
            else {
                check(snapshot.exists()) { "Care task not found" }
                check(snapshot.getString("patientId") == task.patientId) { "Cannot change task owner" }
            }
            if (create) transaction.set(reference, task.toFirestore())
            else transaction.update(reference, task.toFirestore())
            task
        }
    }

    fun list(patientId: String): Task<List<CareTask>> {
        if (!isValidCareDocumentId(patientId)) return Tasks.forException(IllegalArgumentException("Patient ID is required"))
        return collection(patientId).get(com.google.firebase.firestore.Source.SERVER).continueWith { result ->
            if (!result.isSuccessful) throw result.exception ?: IllegalStateException("Care tasks could not be loaded")
            result.result.documents.mapNotNull { document ->
                CareTask.fromFirestore(document.id, document.data.orEmpty())?.takeIf { it.patientId == patientId }
            }
        }
    }

    fun setCompleted(patientId: String, id: String, completed: Boolean): Task<Unit> {
        if (!isValidCareDocumentId(patientId) || !isValidCareDocumentId(id)) return Tasks.forException(IllegalArgumentException("Patient and task IDs are required"))
        val reference = collection(patientId).document(id)
        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(reference)
            check(snapshot.exists() && snapshot.getString("patientId") == patientId) { "Care task not found" }
            transaction.update(reference, "completed", completed)
            Unit
        }
    }

    private fun collection(patientId: String) = firestore.collection("patients").document(patientId).collection("careTasks")
}
