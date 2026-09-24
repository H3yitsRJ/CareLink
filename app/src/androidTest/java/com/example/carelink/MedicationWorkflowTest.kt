package com.example.carelink

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Medication
import com.example.carelink.screens.AddEditMedicationScreen
import com.example.carelink.ui.theme.CareLinkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MedicationWorkflowTest {
    @get:Rule val compose = createComposeRule()
    private val original = Medication("med-test", "patient-test", "Test medication", "5 mg", "1 tablet",
        "Daily", listOf("08:00", "20:00"))

    @Test fun editingPreservesEveryReminderAndIdentity() {
        var saved: Medication? = null
        compose.setContent { CareLinkTheme { AddEditMedicationScreen(original, onSave = { saved = it }) } }
        compose.onNodeWithText("Medication name").performTextReplacement("Updated medication")
        compose.onNodeWithText("Save medication").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(original.reminderTimes, saved?.reminderTimes)
            assertEquals(original.id, saved?.id)
            assertEquals(original.patientId, saved?.patientId)
            assertEquals("Updated medication", saved?.name)
        }
    }

    @Test fun cancelDoesNotSaveAndRemovalRequiresConfirmation() {
        var saves = 0
        var cancels = 0
        var removes = 0
        compose.setContent { CareLinkTheme { AddEditMedicationScreen(original,
            onSave = { saves++ }, onCancel = { cancels++ }, onDelete = { removes++ }) } }
        compose.onNodeWithText("Medication name").performTextReplacement("Unsaved change")
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, saves); assertEquals(1, cancels) }
        compose.onNodeWithText("Remove medication").performScrollTo().performClick()
        compose.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        compose.runOnIdle { assertEquals(0, removes) }
        compose.onNodeWithText("Remove medication").performScrollTo().performClick()
        compose.onNode(hasText("Remove") and hasAnyAncestor(isDialog())).performClick()
        compose.runOnIdle { assertEquals(1, removes) }
    }

    @Test fun invalidValuesDoNotSaveAndErrorsPreserveInput() {
        var saved: Medication? = null
        compose.setContent { CareLinkTheme { AddEditMedicationScreen(original, saveError = "Could not save. Try again.", onSave = { saved = it }) } }
        compose.onNodeWithText("Medication name").performTextReplacement("")
        compose.onNodeWithText("Save medication").performScrollTo().performClick()
        compose.onNodeWithText("Enter a medication name").assertExists()
        compose.onNodeWithText("Could not save. Try again.").assertExists()
        compose.onNodeWithText("08:00, 20:00").assertExists()
        compose.runOnIdle { assertNull(saved) }
    }

    @Test fun smallDisplayAndLargeTextKeepSaveReachable() {
        var saved: Medication? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                CareLinkTheme { Box(Modifier.width(320.dp)) {
                    AddEditMedicationScreen(original, onSave = { saved = it })
                } }
            }
        }
        compose.onNodeWithText("Save medication").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(original.reminderTimes, saved?.reminderTimes) }
    }
}
