package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LogoutScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text("Log out", style = MaterialTheme.typography.headlineLarge)
        Text("Are you sure you want to log out?", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp)) {
            Text("Log out")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp)) {
            Text("Cancel")
        }
    }
}
