package com.example.carelink.data

import com.example.carelink.model.DoseRecord
import com.example.carelink.model.DoseStatus
import com.example.carelink.model.Medication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// The in-memory repository verifies the same workflow expected from remote storage.
class MedicationRepositoryTest {
    private lateinit var repository: InMemoryMedicationRepository
    private lateinit var medication: Medication

    @Before
    fun setUp() {
        repository = InMemoryMedicationRepository()
        medication = Medication("med-1", "patient-1", "Metformin", "500 mg", "1 tablet", "Daily", listOf("08:00"))
    }

    @Test
    fun `creates retrieves and updates medication`() {
        assertTrue(repository.create(medication).isSuccess)
        assertEquals(medication, repository.get("med-1"))
        assertEquals("Metformin", repository.list("patient-1").single().name)

        val updated = medication.copy(dose = "2 tablets")
        assertTrue(repository.update(updated).isSuccess)
        assertEquals("2 tablets", repository.get("med-1")?.dose)
    }

    @Test
    fun `rejects invalid medication`() {
        assertTrue(repository.create(medication.copy(name = "")).isFailure)
        assertNull(repository.get("med-1"))
    }

    @Test
    fun `removes medication after confirmation is supplied by caller`() {
        repository.create(medication)
        assertTrue(repository.remove("med-1").isSuccess)
        assertTrue(repository.list("patient-1").isEmpty())
        assertFalse(repository.get("med-1")!!.active)
    }

    @Test
    fun `records taken missed and delayed doses`() {
        repository.create(medication)
        listOf(DoseStatus.TAKEN, DoseStatus.MISSED, DoseStatus.DELAYED).forEachIndexed { index, status ->
            assertTrue(repository.recordDose(DoseRecord("dose-$index", "med-1", 10_000L + index, status, 20_000L + index)).isSuccess)
        }
        assertEquals(setOf(DoseStatus.TAKEN, DoseStatus.MISSED, DoseStatus.DELAYED), repository.doseRecords("med-1").map { it.status }.toSet())
    }
}
