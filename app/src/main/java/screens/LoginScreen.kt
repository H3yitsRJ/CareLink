package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carelink.ui.theme.CareLinkTheme

data class SignInDetails(val email: String, val password: String)

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    isSubmitting: Boolean = false,
    submitError: String? = null,
    onSignIn: (SignInDetails) -> Unit = {},
    onCreateAccountClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var attemptedSubmit by rememberSaveable { mutableStateOf(false) }
    val emailError = if (attemptedSubmit) validateEmailAddress(email) else null
    val passwordError = if (attemptedSubmit && password.isBlank()) "Enter your password" else null
    val focusManager = LocalFocusManager.current

    fun submit() {
        attemptedSubmit = true
        if (validateEmailAddress(email) == null && password.isNotBlank() && !isSubmitting) {
            focusManager.clearFocus()
            onSignIn(SignInDetails(email.trim(), password))
        }
    }

    val backgroundColor = Color(0xFFDDEFF1)
    val darkButtonColor = Color(0xFF292929)
    val linkColor = Color(0xFF264E52)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .safeDrawingPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(120.dp))
            Text(
                text = "CareLink",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4DB7C5)
            )
            Spacer(Modifier.height(48.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Email", fontSize = 14.sp, color = Color.DarkGray)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Email") },
                        singleLine = true,
                        isError = emailError != null,
                        supportingText = emailError?.let { message -> { Text(message) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Password", fontSize = 14.sp, color = Color.DarkGray)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Password") },
                        singleLine = true,
                        isError = passwordError != null,
                        supportingText = passwordError?.let { message -> { Text(message) } },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp)
                    )
                    TextButton(
                        onClick = onForgotPasswordClick,
                        enabled = !isSubmitting,
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("Forgot password?", color = Color(0xFF333333))
                    }
                    submitError?.let {
                        Text(
                            text = it,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Button(
                        onClick = ::submit,
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = darkButtonColor)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Sign in", color = Color.White)
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            OutlinedButton(
                onClick = onCreateAccountClick,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Create account", color = linkColor)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun LoginScreenPreview() {
    CareLinkTheme { LoginScreen() }
}
