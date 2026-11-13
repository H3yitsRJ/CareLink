package com.example.carelink

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.example.carelink.screens.LoginScreen
import com.example.carelink.ui.DismissKeyboardOnOutsideTapColumn
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class KeyboardDismissalTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tappingOutsideHidesImeAndStillActivatesTheTappedButton() {
        var taps = 0
        var keyboardVisible: () -> Boolean = { false }
        var showKeyboard: () -> Unit = {}
        compose.setContent {
            val view = LocalView.current
            val keyboard = LocalSoftwareKeyboardController.current
            showKeyboard = { keyboard?.show() }
            keyboardVisible = {
                ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime()) == true
            }
            DismissKeyboardOnOutsideTapColumn(Modifier.fillMaxSize()) {
                Text("Outside the fields")
                Button(onClick = { taps++ }) { Text("Outside action") }
                LoginScreen()
            }
        }

        val email = compose.onAllNodes(hasSetTextAction())[0]
        val password = compose.onAllNodes(hasSetTextAction())[1]
        email.performTouchInput { click() }.assertIsFocused()
        compose.runOnIdle { showKeyboard() }
        compose.waitUntil(5_000) { compose.runOnIdle { keyboardVisible() } }
        password.performTouchInput { click() }.assertIsFocused()
        email.assertIsNotFocused()
        compose.waitUntil(5_000) { compose.runOnIdle { keyboardVisible() } }

        compose.onNodeWithText("Outside the fields").performTouchInput { click() }
        password.assertIsNotFocused()
        compose.waitUntil(5_000) { compose.runOnIdle { !keyboardVisible() } }

        email.performTouchInput { click() }.assertIsFocused()
        compose.onNodeWithText("Outside action").performTouchInput { click() }
        email.assertIsNotFocused()
        compose.runOnIdle { assertEquals(1, taps) }
    }
}
