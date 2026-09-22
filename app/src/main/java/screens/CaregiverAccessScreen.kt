package com.example.carelink.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.carelink.model.CarePermission
import com.example.carelink.model.CaregiverAccess

@Composable
fun CaregiverAccessScreen(access: CaregiverAccess? = null, caregiverName: String = "Caregiver", canEdit: Boolean = false, onSave: (CaregiverAccess) -> Unit = {}, onCancel: () -> Unit = {},
    availablePermissions: Set<CarePermission> = CarePermission.entries.toSet(), isSaving: Boolean = false, error: String? = null) {
    // Edit a local copy so Cancel can leave the stored permission set untouched.
    var selected by remember(access) { mutableStateOf(access?.permissions.orEmpty()) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Caregiver access")
        Text(caregiverName)
        if (access == null) StateMessage("Caregiver access could not be found.", true) else {
            availablePermissions.forEach { permission ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // A null change callback makes the checkbox read-only for non-owners.
                    Checkbox(
                        modifier = Modifier.semantics { contentDescription = "${permission.name.lowercase().replace('_', ' ')} access" },
                        checked = permission in selected,
                        onCheckedChange = if (canEdit && !isSaving) {{ checked ->
                            selected = if (checked) selected + permission else selected - permission
                            if (permission == CarePermission.EDIT && checked) selected = selected + CarePermission.VIEW
                            if (permission == CarePermission.VIEW && !checked) selected = selected - CarePermission.EDIT
                        }} else null
                    )
                    Text(permission.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase))
                }
            }
            if (!canEdit) StateMessage("Only the patient can change caregiver access.")
            if (error != null) StateMessage(error, true)
            Button(onClick = { onSave(access.copy(permissions = selected)) }, enabled = canEdit && !isSaving, modifier = Modifier.fillMaxWidth()) { Text(if (isSaving) "Saving access" else "Save access") }
            OutlinedButton(onClick = onCancel, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}
