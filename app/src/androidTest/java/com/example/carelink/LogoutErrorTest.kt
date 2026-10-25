package com.example.carelink

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.carelink.screens.LogoutScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LogoutErrorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cleanupFailureIsVisibleAndLogoutCanBeRetried() {
        var attempts = 0
        val message = "We couldn't remove your saved data. You're still signed in. Please try again."
        compose.setContent {
            var error by remember { mutableStateOf<String?>(null) }
            LogoutScreen(
                onBack = {},
                onLogout = { attempts++; error = if (attempts == 1) message else null },
                errorMessage = error
            )
        }
        compose.onNode(hasText("Log out") and hasClickAction()).performClick()
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNode(hasText("Log out") and hasClickAction()).assertIsEnabled().performClick()
        compose.onNodeWithText(message).assertDoesNotExist()
        compose.runOnIdle { assertEquals(2, attempts) }
    }
}
