package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.carelink.ui.theme.CareLinkTheme

data class PatientProfileDetails(
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val dateOfBirth: String,
    val phoneNumber: String,
    val addressLine1: String,
    val addressLine2: String,
    val city: String,
    val state: String,
    val zipCode: String
)

internal data class ProfileErrors(
    val firstName: String? = null,
    val lastName: String? = null,
    val dateOfBirth: String? = null,
    val phoneNumber: String? = null,
    val state: String? = null,
    val zipCode: String? = null
) {
    val hasErrors: Boolean
        get() = listOf(
            firstName,
            lastName,
            dateOfBirth,
            phoneNumber,
            state,
            zipCode
        ).any { it != null }
}

internal fun validateProfile(
    firstName: String,
    lastName: String,
    dateOfBirth: String = "",
    phoneNumber: String = "",
    state: String = "",
    zipCode: String = ""
) = ProfileErrors(
    firstName = if (firstName.trim().isEmpty()) "Enter your first name" else null,
    lastName = if (lastName.trim().isEmpty()) "Enter your last name" else null,
    dateOfBirth = if (dateOfBirth.isNotBlank() && !isValidDateOfBirth(dateOfBirth)) {
        "Use MM/DD/YYYY"
    } else {
        null
    },
    phoneNumber = if (
        phoneNumber.isNotBlank() && phoneNumber.count(Char::isDigit) != 10
    ) {
        "Enter a 10-digit phone number"
    } else {
        null
    },
    state = if (state.isNotBlank() && !Regex("^[A-Za-z]{2}$").matches(state.trim())) {
        "Use the 2-letter state code"
    } else {
        null
    },
    zipCode = if (
        zipCode.isNotBlank() && !Regex("^\\d{5}(-\\d{4})?$").matches(zipCode.trim())
    ) {
        "Enter a 5-digit ZIP code"
    } else {
        null
    }
)

private fun isValidDateOfBirth(value: String): Boolean {
    val parts = value.trim().split("/")
    if (parts.size != 3 || parts[0].length != 2 || parts[1].length != 2 || parts[2].length != 4) {
        return false
    }
    val month = parts[0].toIntOrNull() ?: return false
    val day = parts[1].toIntOrNull() ?: return false
    val year = parts[2].toIntOrNull() ?: return false
    return month in 1..12 && day in 1..31 && year in 1900..2100
}

@Composable
fun CreateProfileScreen(
    modifier: Modifier = Modifier,
    isSaving: Boolean = false,
    saveError: String? = null,
    onSaveProfile: (PatientProfileDetails) -> Unit = {}
) {
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var preferredName by rememberSaveable { mutableStateOf("") }
    var dateOfBirth by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var addressLine1 by rememberSaveable { mutableStateOf("") }
    var addressLine2 by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var state by rememberSaveable { mutableStateOf("") }
    var zipCode by rememberSaveable { mutableStateOf("") }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }

    val errors = remember(
        firstName,
        lastName,
        dateOfBirth,
        phoneNumber,
        state,
        zipCode,
        attemptedSave
    ) {
        if (attemptedSave) {
            validateProfile(firstName, lastName, dateOfBirth, phoneNumber, state, zipCode)
        } else {
            ProfileErrors()
        }
    }

    fun save() {
        attemptedSave = true
        val currentErrors = validateProfile(
            firstName,
            lastName,
            dateOfBirth,
            phoneNumber,
            state,
            zipCode
        )
        if (!currentErrors.hasErrors && !isSaving) {
            onSaveProfile(
                PatientProfileDetails(
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    preferredName = preferredName.trim(),
                    dateOfBirth = dateOfBirth.trim(),
                    phoneNumber = phoneNumber.trim(),
                    addressLine1 = addressLine1.trim(),
                    addressLine2 = addressLine2.trim(),
                    city = city.trim(),
                    state = state.trim().uppercase(),
                    zipCode = zipCode.trim()
                )
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Set up your profile",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Add the details you want CareLink to use for your profile.",
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Personal information", style = MaterialTheme.typography.titleLarge)
                ProfileTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = "First name",
                    placeholder = "Jane",
                    error = errors.firstName,
                    imeAction = ImeAction.Next
                )
                ProfileTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = "Last name",
                    placeholder = "Smith",
                    error = errors.lastName,
                    imeAction = ImeAction.Next
                )
                ProfileTextField(
                    value = preferredName,
                    onValueChange = { preferredName = it },
                    label = "Preferred name (optional)",
                    placeholder = "Jane",
                    error = null,
                    imeAction = ImeAction.Next
                )
                ProfileTextField(
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = it.take(10) },
                    label = "Date of birth (optional)",
                    placeholder = "MM/DD/YYYY",
                    error = errors.dateOfBirth,
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.None
                )
                ProfileTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = "Phone number (optional)",
                    placeholder = "555-123-4567",
                    error = errors.phoneNumber,
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Phone,
                    capitalization = KeyboardCapitalization.None
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Mailing address (optional)", style = MaterialTheme.typography.titleLarge)
                ProfileTextField(
                    value = addressLine1,
                    onValueChange = { addressLine1 = it },
                    label = "Street address",
                    placeholder = "123 Main Street",
                    error = null,
                    imeAction = ImeAction.Next
                )
                ProfileTextField(
                    value = addressLine2,
                    onValueChange = { addressLine2 = it },
                    label = "Apartment, suite, or unit",
                    placeholder = "Apartment 4B",
                    error = null,
                    imeAction = ImeAction.Next
                )
                ProfileTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = "City",
                    placeholder = "Columbus",
                    error = null,
                    imeAction = ImeAction.Next
                )
                ProfileTextField(
                    value = state,
                    onValueChange = { state = it.filter(Char::isLetter).take(2).uppercase() },
                    label = "State",
                    placeholder = "OH",
                    error = errors.state,
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Characters
                )
                ProfileTextField(
                    value = zipCode,
                    onValueChange = { zipCode = it.filter { character -> character.isDigit() || character == '-' }.take(10) },
                    label = "ZIP code",
                    placeholder = "43215",
                    error = errors.zipCode,
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number,
                    capitalization = KeyboardCapitalization.None,
                    onDone = ::save
                )
            }
        }

        if (saveError != null) {
            Text(
                text = saveError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .semantics { liveRegion = LiveRegionMode.Assertive },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = ::save,
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Save profile")
            }
        }
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    error: String?,
    imeAction: ImeAction,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    onDone: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let {
                {
                    Text(
                        text = it,
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Assertive
                        }
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = capitalization,
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = {
                    focusManager.clearFocus()
                    onDone?.invoke()
                }
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun CreateProfileScreenPreview() {
    CareLinkTheme {
        CreateProfileScreen()
    }
}
