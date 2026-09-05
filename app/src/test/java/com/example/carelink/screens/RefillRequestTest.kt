package com.example.carelink.screens

import com.example.carelink.model.RefillRequest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Duplicate-request prevention is health workflow behavior, not just a UI detail.
class RefillRequestTest {
    @Test fun `prevents duplicate active refill request`() {
        val active = RefillRequest("1", "patient-1", "med-1", "caregiver-1")
        assertFalse(canCreateRefill("med-1", listOf(active)))
        assertTrue(canCreateRefill("med-2", listOf(active)))
    }
}
