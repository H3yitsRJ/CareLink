package com.example.carelink.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.carelink.ui.theme.CareLinkTheme

@Composable
fun HelpCenterScreen() {
    Text("Password Reset Email")
}

@Preview(showBackground = true)
@Composable
private fun PasswordResetEmailScreenPreview() {
    CareLinkTheme {
        PasswordResetEmailScreen()
    }
}