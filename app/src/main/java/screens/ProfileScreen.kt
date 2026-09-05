package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import navigation.BottomNavBar
import navigation.BottomNavDestination

@Composable
fun ProfileScreen(
    fullName: String,
    email: String,
    onOpenSettings: () -> Unit,
    onNavigate: (BottomNavDestination) -> Unit
) {
    Scaffold(bottomBar = {
        BottomNavBar(BottomNavDestination.Profile, onNavigate)
    }) { innerPadding ->
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(innerPadding).padding(16.dp)
        ) {
            Text("Profile", style = MaterialTheme.typography.headlineLarge)
            if (fullName.isNotBlank()) {
                Text(fullName, Modifier.padding(top = 24.dp), style = MaterialTheme.typography.titleLarge)
            }
            if (email.isNotBlank()) Text(email, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                Modifier.fillMaxWidth().padding(top = 24.dp).sizeIn(minHeight = 56.dp)
                    .clickable(onClickLabel = "Open settings", onClick = onOpenSettings)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Settings", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}
