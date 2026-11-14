package com.example.carelink.screens
import androidx.activity.compose.BackHandler

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onCaregiverAccess: () -> Unit = {},
    clockFormat: ClockFormat = ClockFormat.TwelveHour,
    onClockFormatChange: (ClockFormat) -> Unit = {}
) {
    var choosingClockFormat by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text("Settings", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.headlineLarge)
        Row(
            Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp)
                .clickable { choosingClockFormat = true }.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Clock format", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(clockFormat.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
        Row(Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp).clickable(onClick = onCaregiverAccess).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Medication caregiver access", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }

    if (choosingClockFormat) {
        AlertDialog(
            onDismissRequest = { choosingClockFormat = false },
            title = { Text("Clock format") },
            text = {
                Column(Modifier.selectableGroup()) {
                    ClockFormat.entries.forEach { option ->
                        Row(
                            Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp).selectable(
                                selected = option == clockFormat,
                                role = Role.RadioButton,
                                onClick = {
                                    onClockFormatChange(option)
                                    choosingClockFormat = false
                                }
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = option == clockFormat, onClick = null)
                            Text("${option.label} (${option.displayTime("13:30")})", Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { choosingClockFormat = false }) { Text("Cancel") }
            }
        )
    }
}
