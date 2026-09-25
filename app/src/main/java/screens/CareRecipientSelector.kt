package com.example.carelink.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.data.*

@Composable
fun CareRecipientSelector(
    actorId: String, selectedPatientId: String?, onSelect: (String?) -> Unit,
    directory: CareRecipientDirectory, medicationData: CaregiverMedicationData, onBack: () -> Unit
) {
    key(actorId) {
        var recipients by remember { mutableStateOf<List<CareRecipient>>(emptyList()) }
        var loading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        var choosing by rememberSaveable { mutableStateOf(false) }
        var retry by remember { mutableIntStateOf(0) }
        val currentSelected by rememberUpdatedState(selectedPatientId)
        val selectCallback by rememberUpdatedState(onSelect)
        BackHandler { if (choosing) choosing = false else onBack() }
        DisposableEffect(actorId, directory, retry) {
            var active = true
            loading = true
            error = null
            recipients = emptyList()
            val stop = directory.watch(actorId) { result ->
                if (active) {
                    loading = false
                    result.onSuccess { latest ->
                        recipients = latest
                        if (currentSelected != null && latest.none { it.patientId == currentSelected }) selectCallback(null)
                    }.onFailure {
                        recipients = emptyList()
                        error = "We couldn't verify your care-recipient access. Connect to the internet and retry."
                    }
                }
            }
            onDispose { active = false; stop() }
        }
        val selected = recipients.find { it.patientId == selectedPatientId }
        if (!loading && error == null && selected != null && !choosing) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.statusBarsPadding().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current care recipient: ${selected.name}", Modifier.weight(1f))
                    TextButton(onClick = { choosing = true }) { Text("Change recipient") }
                }
                Box(Modifier.weight(1f)) {
                    key(actorId, selected.patientId) {
                        CaregiverMedicationsScreen(actorId, onBack, medicationData, selected.patientId)
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose a care recipient", style = MaterialTheme.typography.headlineMedium)
                when {
                    loading -> Text("Loading care recipients...")
                    error != null -> { Text(error!!); Button(onClick = { retry++ }) { Text("Retry") } }
                    recipients.isEmpty() -> Text("You don't have access to any care recipients yet.")
                    else -> recipients.forEach { recipient ->
                        OutlinedButton(onClick = { onSelect(recipient.patientId); choosing = false }, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(recipient.name)
                                Text("CareLink ID: ${recipient.patientId}")
                                if (recipient.patientId == selectedPatientId) Text("Current care recipient")
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { if (choosing && selected != null) choosing = false else onBack() }) { Text("Back") }
            }
        }
    }
}
