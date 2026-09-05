package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import navigation.BottomNavBar
import navigation.BottomNavDestination

@Composable
fun AppointmentsScreen(onNavigate: (BottomNavDestination) -> Unit = {}) {
    Scaffold(bottomBar = {
        BottomNavBar(BottomNavDestination.Appointments, onNavigate)
    }) { innerPadding ->
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(innerPadding).padding(16.dp)
        ) {
            Text("Appointments", style = MaterialTheme.typography.headlineLarge)
            Text("No appointments yet.", modifier = Modifier.padding(top = 24.dp))
        }
    }
}
