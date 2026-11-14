package com.example.carelink

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.pressBack
import com.example.carelink.screens.ProfileScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LogoutErrorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cleanupFailureIsVisibleAndLogoutCanBeRetried() {
        var attempts = 0
        val message = "We couldn't remove your saved data. You're still signed in. Please try again."
        compose.setContent {
            ProfileScreen(
                fullName = "Test patient",
                email = "",
                onEditProfile = {},
                onOpenSettings = {},
                onNavigate = {},
                onLogout = { attempts++; if (attempts == 1) message else null }
            )
        }
        val logout = compose.onNodeWithText("Log out")
        logout.assertIsDisplayed().performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(0, attempts) }
        logout.performClick()
        val confirm = compose.onNode(hasText("Log out") and hasAnyAncestor(isDialog()))
        pressBack()
        compose.onNodeWithText("Log out?").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, attempts) }
        logout.performClick()
        confirm.performClick()
        compose.onNodeWithText(message).assertIsDisplayed()
        confirm.assertIsEnabled().performClick()
        compose.onNodeWithText(message).assertDoesNotExist()
        compose.onNodeWithText("Log out?").assertDoesNotExist()
        compose.runOnIdle { assertEquals(2, attempts) }
    }
}
