package com.example.carelink.data

import com.example.carelink.model.DoseRecord
import com.example.carelink.model.DoseStatus
import com.example.carelink.model.Medication

// Screens depend on this contract instead of a specific local or Firebase implementation.
interface MedicationRepository {
    fun create(medication: Medication): Result<Medication>
    fun get(id: String): Medication?
    fun list(patientId: String): List<Medication>
    fun update(medication: Medication): Result<Medication>
    fun remove(id: String): Result<Unit>
    fun recordDose(record: DoseRecord): Result<DoseRecord>
    fun doseRecords(medicationId: String): List<DoseRecord>
}

// This implementation gives tests deterministic CRUD behavior without network access.
class InMemoryMedicationRepository : MedicationRepository {
    private val medications = linkedMapOf<String, Medication>()
    private val doses = linkedMapOf<String, DoseRecord>()

    override fun create(medication: Medication): Result<Medication> {
        val errors = medication.validate()
        if (errors.isNotEmpty()) return Result.failure(IllegalArgumentException(errors.values.first()))
        if (medications.containsKey(medication.id)) return Result.failure(IllegalStateException("Medication already exists"))
        medications[medication.id] = medication
        return Result.success(medication)
    }

    override fun get(id: String): Medication? = medications[id]

    override fun list(patientId: String): List<Medication> = medications.values.filter {
        it.patientId == patientId && it.active
    }

    override fun update(medication: Medication): Result<Medication> {
        if (!medications.containsKey(medication.id)) return Result.failure(NoSuchElementException("Medication not found"))
        val errors = medication.validate()
        if (errors.isNotEmpty()) return Result.failure(IllegalArgumentException(errors.values.first()))
        medications[medication.id] = medication
        return Result.success(medication)
    }

    override fun remove(id: String): Result<Unit> {
        val medication = medications[id] ?: return Result.failure(NoSuchElementException("Medication not found"))
        // Deactivation preserves old dose records while hiding the medication from active lists.
        medications[id] = medication.copy(active = false)
        return Result.success(Unit)
    }

    override fun recordDose(record: DoseRecord): Result<DoseRecord> {
        if (!medications.containsKey(record.medicationId)) {
            return Result.failure(NoSuchElementException("Medication not found"))
        }
        if (record.status == DoseStatus.SCHEDULED) {
            return Result.failure(IllegalArgumentException("Choose Taken, Missed, or Delayed"))
        }
        doses[record.id] = record
        return Result.success(record)
    }

    override fun doseRecords(medicationId: String): List<DoseRecord> = doses.values
        .filter { it.medicationId == medicationId }
        .sortedByDescending { it.scheduledTimeMillis }
}
