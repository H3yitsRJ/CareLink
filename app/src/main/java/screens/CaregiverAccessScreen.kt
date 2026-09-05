package com.example.carelink.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.model.CarePermission
import com.example.carelink.model.CaregiverAccess

@Composable
fun CaregiverAccessScreen(access: CaregiverAccess? = null, caregiverName: String = "Caregiver", canEdit: Boolean = false, onSave: (CaregiverAccess) -> Unit = {}, onCancel: () -> Unit = {}) {
    // Edit a local copy so Cancel can leave the stored permission set untouched.
    var selected by remember(access) { mutableStateOf(access?.permissions.orEmpty()) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Caregiver access")
        Text(caregiverName)
        if (access == null) StateMessage("Caregiver access could not be found.", true) else {
            CarePermission.entries.forEach { permission ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // A null change callback makes the checkbox read-only for non-owners.
                    Checkbox(
                        checked = permission in selected,
                        onCheckedChange = if (canEdit) {{ checked -> selected = if (checked) selected + permission else selected - permission }} else null
                    )
                    Text(permission.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase))
                }
            }
            if (!canEdit) StateMessage("Only the patient can change caregiver access.")
            Button(onClick = { onSave(access.copy(permissions = selected)) }, enabled = canEdit, modifier = Modifier.fillMaxWidth()) { Text("Save access") }
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}
