package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.carelink.ui.theme.CareLinkTheme

internal fun validateEmailAddress(email: String): String? {
    val trimmedEmail = email.trim()
    return when {
        trimmedEmail.isEmpty() -> "Enter your email address"
        !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(trimmedEmail) -> "Enter a valid email address"
        else -> null
    }
}

@Composable
fun PasswordResetEmailScreen(
    modifier: Modifier = Modifier,
    isSubmitting: Boolean = false,
    submitError: String? = null,
    emailSent: Boolean = false,
    onSendResetEmail: (String) -> Unit = {},
    onBackToSignIn: () -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf("") }
    var attemptedSubmit by rememberSaveable { mutableStateOf(false) }
    val emailError = if (attemptedSubmit) validateEmailAddress(email) else null
    val focusManager = LocalFocusManager.current

    fun submit() {
        attemptedSubmit = true
        if (validateEmailAddress(email) == null && !isSubmitting) {
            focusManager.clearFocus()
            onSendResetEmail(email.trim())
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding().verticalScroll(rememberScrollState()).imePadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBackToSignIn, modifier = Modifier.align(Alignment.Start)) {
            Text("Back to sign in")
        }
        Text("Reset your password", style = MaterialTheme.typography.headlineLarge)
        if (emailSent) {
            Card(
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Check your email", style = MaterialTheme.typography.headlineSmall)
                    Text("If an account exists for $email, you will receive a link to reset your password.")
                    Button(
                        onClick = onBackToSignIn,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) { Text("Return to sign in") }
                }
            }
        } else {
            Text("Enter the email address for your CareLink account.")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    label = { Text("Email address") },
                    placeholder = { Text("name@example.com") },
                    singleLine = true,
                    isError = emailError != null,
                    supportingText = emailError?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )
            }
            submitError?.let {
                Text(
                    it,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = ::submit,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                if (isSubmitting) CircularProgressIndicator(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                ) else Text("Send reset link")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun PasswordResetEmailScreenPreview() {
    CareLinkTheme { PasswordResetEmailScreen() }
}
