package com.example.carelink

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.carelink.data.CaregiverAccessData
import com.example.carelink.model.CarePermission
import com.example.carelink.model.CaregiverAccess
import com.example.carelink.screens.MedicationCaregiverAccessScreen
import com.example.carelink.ui.theme.CareLinkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CaregiverAccessManagementTest {
    @get:Rule val compose = createComposeRule()
    private val data = FakeAccessData()
    private fun open() = compose.setContent { CareLinkTheme { MedicationCaregiverAccessScreen("patient", "Patient", {}, data) } }

    @Test fun cancelRevocationLeavesAccessUnchanged() {
        open()
        compose.onNodeWithText("Revoke access").performScrollTo().performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(0, data.saves); assertFalse(data.grant.revoked) }
        compose.onNodeWithText("Edit access").assertExists()
    }

    @Test fun confirmRevocationRemovesCaregiverAndReportsSuccess() {
        open()
        compose.onNodeWithText("Revoke access").performScrollTo().performClick()
        compose.onNodeWithText("Revoke").performClick()
        compose.onNodeWithText("Access revoked.").assertExists()
        compose.onNodeWithText("Edit access").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, data.saves); assertTrue(data.grant.revoked) }
    }

    @Test fun failedRevocationKeepsCaregiverAvailableForRetry() {
        data.fail = true
        open()
        compose.onNodeWithText("Revoke access").performScrollTo().performClick()
        compose.onNodeWithText("Revoke").performClick()
        compose.onNodeWithText("Revoke access").assertExists()
        compose.runOnIdle { assertFalse(data.grant.revoked); assertEquals(0, data.saves) }
    }
    @Test fun staleEditorCannotRestoreRevokedAccess() {
        open()
        compose.onNodeWithText("Edit access").performScrollTo().performClick()
        compose.runOnIdle { data.revokeElsewhere() }
        compose.onNodeWithText("Save access").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(data.grant.revoked); assertEquals(0, data.saves) }
        compose.onNodeWithText("Couldn't save access. It may have changed on another device. Cancel and reopen it, or check your connection and the caregiver's profile.").assertExists()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNodeWithText("Edit access").assertDoesNotExist()
    }

    private class FakeAccessData : CaregiverAccessData {
        var grant = CaregiverAccess("caregiver", "patient", "caregiver", setOf(CarePermission.VIEW, CarePermission.EDIT))
        var saves = 0
        var fail = false
        private var changed: ((List<CaregiverAccess>, Boolean) -> Unit)? = null
        fun revokeElsewhere() { grant = grant.copy(revoked = true); changed?.invoke(listOf(grant), true) }
        override fun watchGrants(patientId: String, changed: (List<CaregiverAccess>, Boolean) -> Unit): () -> Unit {
            this.changed = changed
            changed(listOf(grant), true)
            return { this.changed = null }
        }
        override fun save(access: CaregiverAccess, patientName: String, expected: CaregiverAccess?, completed: (Result<Unit>) -> Unit) {
            if (fail || expected != grant) completed(Result.failure(IllegalStateException("Access changed or offline")))
            else { grant = access; saves++; changed?.invoke(listOf(grant), true); completed(Result.success(Unit)) }
        }
    }
}
