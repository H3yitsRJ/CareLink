package com.example.carelink

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.carelink.model.CarePermission
import com.example.carelink.model.CaregiverAccess
import com.example.carelink.screens.CaregiverAccessScreen
import com.example.carelink.ui.theme.CareLinkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CaregiverPermissionEditorTest {
    @get:Rule val compose = createComposeRule()
    private val grant = CaregiverAccess("caregiver", "patient", "caregiver", setOf(CarePermission.VIEW))

    @Test fun showsStoredPermissionsAndSavesOnlyTheSelectedGrant() {
        var saved: CaregiverAccess? = null
        compose.setContent { CareLinkTheme {
            CaregiverAccessScreen(grant, "Caregiver", true, onSave = { saved = it })
        } }
        compose.onNodeWithContentDescription("view access").assertIsOn()
        compose.onNodeWithContentDescription("edit access").assertIsOff().performClick()
        compose.runOnIdle { assertNull(saved); assertEquals(setOf(CarePermission.VIEW), grant.permissions) }
        compose.onNodeWithText("Save access").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(grant.copy(permissions = setOf(CarePermission.VIEW, CarePermission.EDIT)), saved)
        }
    }

    @Test fun ownerCanDisableEditWithoutRemovingViewAndCancelLeavesStoredAccessUnchanged() {
        val original = grant.copy(permissions = setOf(CarePermission.VIEW, CarePermission.EDIT))
        var saves = 0
        var cancels = 0
        compose.setContent { CareLinkTheme {
            CaregiverAccessScreen(original, "Caregiver", true, onSave = { saves++ }, onCancel = { cancels++ })
        } }
        compose.onNodeWithContentDescription("edit access").performClick().assertIsOff()
        compose.onNodeWithContentDescription("view access").assertIsOn()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(0, saves)
            assertEquals(1, cancels)
            assertEquals(setOf(CarePermission.VIEW, CarePermission.EDIT), original.permissions)
        }
    }

    @Test fun removingViewAlsoRemovesEditBeforeSaving() {
        var saved: CaregiverAccess? = null
        compose.setContent { CareLinkTheme {
            CaregiverAccessScreen(grant.copy(permissions = setOf(CarePermission.VIEW, CarePermission.EDIT)),
                "Caregiver", true, onSave = { saved = it })
        } }
        compose.onNodeWithContentDescription("view access").performClick().assertIsOff()
        compose.onNodeWithContentDescription("edit access").assertIsOff()
        compose.onNodeWithText("Save access").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(emptySet<CarePermission>(), saved?.permissions) }
    }

    @Test fun nonOwnerCannotChangeOrSavePermissions() {
        var saves = 0
        compose.setContent { CareLinkTheme {
            CaregiverAccessScreen(grant, "Caregiver", false, onSave = { saves++ })
        } }
        compose.onNodeWithContentDescription("view access").assertIsNotEnabled().assertIsOn()
        compose.onNodeWithContentDescription("edit access").assertIsNotEnabled().assertIsOff()
        compose.onNodeWithText("Save access").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Only the patient can change caregiver access.").assertExists()
        compose.runOnIdle { assertEquals(0, saves) }
    }

    @Test fun savingLocksControlsAndFailureKeepsDraftForRetry() {
        val saving = mutableStateOf(false)
        val error = mutableStateOf<String?>(null)
        val attempts = mutableListOf<CaregiverAccess>()
        compose.setContent { CareLinkTheme {
            CaregiverAccessScreen(grant, "Caregiver", true,
                onSave = { attempts += it; saving.value = true },
                isSaving = saving.value, error = error.value)
        } }
        compose.onNodeWithContentDescription("edit access").performClick()
        compose.onNodeWithText("Save access").performScrollTo().performClick()
        compose.onNodeWithText("Saving access").assertIsNotEnabled()
        compose.onNodeWithContentDescription("edit access").assertIsNotEnabled()
        compose.onNodeWithText("Cancel").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { saving.value = false; error.value = "Couldn't save access. Try again." }
        compose.onNodeWithContentDescription("edit access").assertIsOn().assertIsEnabled()
        compose.onNodeWithText("Couldn't save access. Try again.").assertExists()
        compose.onNodeWithText("Save access").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(2, attempts.size); assertEquals(attempts[0], attempts[1]) }
    }

    @Test fun revokedGrantCannotBeEditedAndMissingGrantHasWayBack() {
        val current = mutableStateOf<CaregiverAccess?>(grant.copy(revoked = true))
        var backs = 0
        compose.setContent { CareLinkTheme {
            CaregiverAccessScreen(current.value, "Caregiver", true, onCancel = { backs++ })
        } }
        compose.onNodeWithContentDescription("view access").assertIsNotEnabled()
        compose.onNodeWithText("Save access").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Access has been revoked.").assertExists()
        compose.runOnIdle { current.value = null }
        compose.onNodeWithText("Caregiver access could not be found.").assertExists()
        compose.onNodeWithText("Back").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, backs) }
    }
}
