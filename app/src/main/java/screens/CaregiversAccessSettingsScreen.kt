package com.example.carelink.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.model.CaregiverAccess

@Composable
fun CaregiversAccessSettingsScreen(
    access: CaregiverAccess? = null,
    caregiverName: String = "Caregiver",
    isOwner: Boolean = false,
    onEdit: (CaregiverAccess) -> Unit = {},
    onRevoke: (CaregiverAccess) -> Unit = {}
) {
    // Revocation is destructive enough to require a separate confirmation state.
    var confirmingRevoke by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Caregiver access")
        when {
            access == null -> StateMessage("No caregiver access found.")
            access.revoked -> StateMessage("Access has been revoked.")
            else -> {
                CareCard(caregiverName) { Text("${access.permissions.size} permissions") }
                Button(onClick = { onEdit(access) }, enabled = isOwner, modifier = Modifier.fillMaxWidth()) { Text("Edit access") }
                TextButton(onClick = { confirmingRevoke = true }, enabled = isOwner, modifier = Modifier.fillMaxWidth()) { Text("Revoke access") }
                if (!isOwner) StateMessage("Only the patient can update or revoke access.")
            }
        }
    }
    if (confirmingRevoke && access != null) AlertDialog(
        onDismissRequest = { confirmingRevoke = false },
        title = { Text("Revoke caregiver access?") },
        text = { Text("$caregiverName will no longer be able to view or manage this patient's care.") },
        confirmButton = { TextButton(onClick = { confirmingRevoke = false; onRevoke(access.copy(revoked = true)) }) { Text("Revoke access") } },
        dismissButton = { TextButton(onClick = { confirmingRevoke = false }) { Text("Keep access") } }
    )
}
