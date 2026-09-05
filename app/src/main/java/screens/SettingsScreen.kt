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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenLogout: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text("Settings", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.headlineLarge)
        Row(
            Modifier.fillMaxWidth().padding(top = 24.dp).sizeIn(minHeight = 56.dp)
                .clickable(onClickLabel = "Open log out confirmation", onClick = onOpenLogout)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Log out", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
