package com.example.carelink.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.carelink.ui.theme.CareLinkTheme

@Composable
fun PasswordResetEmailScreen() {
    Text("Create Password")
}

@Preview(showBackground = true)
@Composable
private fun CreatePasswordScreenPreview() {
    CareLinkTheme {
        CreatePasswordScreen()
    }
}