package com.example.carelink.data

import com.example.carelink.model.CareTask

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
