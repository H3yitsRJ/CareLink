// Care-task storage contracts and implementations. MainActivity currently uses
// InMemoryCareTaskRepository; the separate asynchronous Firestore implementation is available but not
// wired into navigation.

package com.example.carelink.data

import com.example.carelink.model.CareTask
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
        if (task.patientId.isBlank() || task.title.isBlank()) return Result.failure(IllegalArgumentException("Patient and title are required"))
        if (tasks.containsKey(task.id)) return Result.failure(IllegalStateException("Care task already exists"))
        tasks[task.id] = task
        return Result.success(task)
    }

    override fun list(patientId: String) = tasks.values.filter { it.patientId == patientId }

    override fun update(task: CareTask): Result<CareTask> {
        if (!tasks.containsKey(task.id)) return Result.failure(NoSuchElementException("Care task not found"))
        if (task.title.isBlank()) return Result.failure(IllegalArgumentException("Title is required"))
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

/** Firestore-backed care-task storage; MainActivity still uses the in-memory implementation. */
class FirestoreCareTaskRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun create(task: CareTask): Task<CareTask> {
        val error = validate(task)
        if (error != null) return Tasks.forException(IllegalArgumentException(error))
        return collection(task.patientId).document(task.id).set(task.toFirestore())
            .continueWith { result ->
                if (!result.isSuccessful) throw result.exception ?: IllegalStateException("Care task was not saved")
                task
            }
    }

    fun list(patientId: String): Task<List<CareTask>> = collection(patientId).get()
        .continueWith { result ->
            if (!result.isSuccessful) throw result.exception ?: IllegalStateException("Care tasks could not be loaded")
            result.result.documents.mapNotNull { document ->
                CareTask.fromFirestore(document.id, document.data.orEmpty())
            }
        }

    fun update(task: CareTask): Task<CareTask> {
        val error = validate(task)
        if (error != null) return Tasks.forException(IllegalArgumentException(error))
        return collection(task.patientId).document(task.id).set(task.toFirestore())
            .continueWith { result ->
                if (!result.isSuccessful) throw result.exception ?: IllegalStateException("Care task was not updated")
                task
            }
    }

    fun setCompleted(patientId: String, id: String, completed: Boolean): Task<Unit> =
        collection(patientId).document(id).update("completed", completed)
            .continueWith { result ->
                if (!result.isSuccessful) throw result.exception ?: IllegalStateException("Care task status was not updated")
                Unit
            }

    private fun collection(patientId: String) = firestore.collection("patients")
        .document(patientId).collection("careTasks")

    private fun validate(task: CareTask): String? = when {
        task.id.isBlank() -> "Care task ID is required"
        task.patientId.isBlank() -> "Patient ID is required"
        task.title.isBlank() -> "Title is required"
        else -> null
    }
}
