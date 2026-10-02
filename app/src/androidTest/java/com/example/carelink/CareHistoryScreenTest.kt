package com.example.carelink

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.carelink.model.*
import com.example.carelink.screens.CareHistoryScreen
import com.example.carelink.ui.theme.CareLinkTheme
import org.junit.Rule
import org.junit.Test

class CareHistoryScreenTest {
    @get:Rule val compose = createComposeRule()
    private fun open() {
        val date = historyDateBoundary("2026-10-01")!!
        compose.setContent {
            CareLinkTheme { CareHistoryScreen(listOf(
                CareHistoryEntry("dose", "patient", date, CareActivityType.MEDICATION, "Aspirin taken"),
                CareHistoryEntry("access", "patient", date, CareActivityType.CAREGIVER_ACCESS, "Caregiver access revoked")
            )) }
        }
    }
    @Test fun applyAndClearUpdateVisibleHistory() {
        open()
        compose.onNodeWithText("Caregiver access", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithText("Apply filters").performScrollTo().performClick()
        compose.onNodeWithText("Aspirin taken").assertDoesNotExist()
        compose.onNodeWithText("Caregiver access revoked").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Active filters: Caregiver access").assertExists()
        compose.onNodeWithText("Clear").performScrollTo().performClick()
        compose.onNodeWithText("Aspirin taken").performScrollTo().assertIsDisplayed()
    }
    @Test fun invalidRangePreservesListAndEmptyFiltersExplainRecovery() {
        open()
        compose.onNodeWithText("Start date").performScrollTo().performTextReplacement("2026-10-02")
        compose.onNodeWithText("End date").performScrollTo().performTextReplacement("2026-10-01")
        compose.onNodeWithText("Apply filters").performScrollTo().performClick()
        compose.onNodeWithText("Start date must be on or before end date.").assertExists()
        compose.onNodeWithText("Aspirin taken").assertExists()
        compose.onNodeWithText("End date").performScrollTo().performTextReplacement("2026-10-03")
        compose.onNodeWithText("Apply filters").performScrollTo().performClick()
        compose.onNodeWithText("No history matches these filters. Clear filters or choose other dates or activity types.").performScrollTo().assertIsDisplayed()
    }
}
