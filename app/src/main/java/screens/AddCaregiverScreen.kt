// Placeholder screen with static sample text. Not connected to MainActivity navigation; the displayed
// label and any copied preview are not a completed feature.

package com.example.carelink.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.carelink.ui.theme.CareLinkTheme

@Composable
fun AddCaregiverScreen() {
    Text("Password Reset Email")
}

@Preview(showBackground = true)
@Composable
private fun PasswordResetEmailScreenPreview() {
    CareLinkTheme {
        PasswordResetEmailScreen()
    }
}