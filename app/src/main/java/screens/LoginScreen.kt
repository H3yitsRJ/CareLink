package com.example.carelink.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.carelink.R
import com.example.carelink.ui.theme.CareLinkTheme

// Keeping credentials in a short-lived value object makes the screen callback explicit.
data class SignInDetails(val email: String, val password: String)

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    isSubmitting: Boolean = false,
    submitError: String? = null,
    onSignIn: (SignInDetails) -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onCreateAccount: () -> Unit = {}
) {
    // The screen owns draft input while MainActivity owns the Firebase request.
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var attemptedSubmit by rememberSaveable { mutableStateOf(false) }
    val emailError = if (attemptedSubmit) validateEmailAddress(email) else null
    val passwordError = if (attemptedSubmit && password.isBlank()) "Enter your password" else null
    val focusManager = LocalFocusManager.current

    fun submit() {
        // Basic checks avoid a network request that Firebase would reject immediately.
        attemptedSubmit = true
        if (validateEmailAddress(email) == null && password.isNotBlank() && !isSubmitting) {
            onSignIn(SignInDetails(email.trim(), password))
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding().verticalScroll(rememberScrollState()).imePadding()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.carelink_logo),
            contentDescription = "CareLink",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().height(104.dp)
        )
        Text("Sign in", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email address") },
                    placeholder = { Text("name@example.com") },
                    singleLine = true,
                    isError = emailError != null,
                    supportingText = emailError?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    isError = passwordError != null,
                    supportingText = passwordError?.let { message -> { Text(message) } },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (passwordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                }
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); submit() })
                )
            }
        }
        TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
            Text("Forgot password?")
        }
        submitError?.let {
            Text(
                it,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive },
                color = MaterialTheme.colorScheme.error
            )
        }
        Button(
            onClick = ::submit,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSubmitting) CircularProgressIndicator(
                modifier = Modifier.height(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp
            ) else Text("Sign in")
        }
        TextButton(onClick = onCreateAccount) { Text("Create an account") }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun LoginScreenPreview() { CareLinkTheme { LoginScreen() } }
