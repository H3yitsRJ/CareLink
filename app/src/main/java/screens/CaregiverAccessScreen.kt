package com.example.carelink.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.carelink.model.CarePermission
import com.example.carelink.model.CaregiverAccess

@Composable
fun CaregiverAccessScreen(access: CaregiverAccess? = null, caregiverName: String = "Caregiver", canEdit: Boolean = false, onSave: (CaregiverAccess) -> Unit = {}, onCancel: () -> Unit = {},
    availablePermissions: Set<CarePermission> = setOf(CarePermission.VIEW, CarePermission.EDIT), isSaving: Boolean = false, error: String? = null) {
    // Edit a local copy so Cancel can leave the stored permission set untouched.
    var selected by remember(access) { mutableStateOf(access?.permissions.orEmpty()) }
    val editable = canEdit && !isSaving && access?.revoked == false
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Caregiver access", style = MaterialTheme.typography.headlineMedium)
        Text(caregiverName)
        if (access == null) StateMessage("Caregiver access could not be found.", true) else {
            availablePermissions.sortedBy { it.ordinal }.forEach { permission ->
                val label = permission.name.lowercase().replace('_', ' ')
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .semantics { contentDescription = "$label access" }
                    .toggleable(value = permission in selected, enabled = editable, role = Role.Checkbox) { checked ->
                        selected = if (checked) selected + permission else selected - permission
                        if (permission == CarePermission.EDIT && checked) selected = selected + CarePermission.VIEW
                        if (permission == CarePermission.VIEW && !checked) selected = selected - CarePermission.EDIT
                    }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = permission in selected,
                        enabled = editable,
                        onCheckedChange = null
                    )
                    Text(label.replaceFirstChar(Char::uppercase))
                }
            }
            if (!canEdit) StateMessage("Only the patient can change caregiver access.")
            if (access.revoked) StateMessage("Access has been revoked.")
            if (error != null) Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) { StateMessage(error, true) }
            Button(onClick = { onSave(access.copy(permissions = selected)) }, enabled = editable, modifier = Modifier.fillMaxWidth()) { Text(if (isSaving) "Saving access" else "Save access") }
            OutlinedButton(onClick = onCancel, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
        if (access == null) OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
