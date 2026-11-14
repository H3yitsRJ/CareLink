package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import navigation.BottomNavBar
import navigation.BottomNavDestination

@Composable
fun ProfileScreen(
    fullName: String,
    email: String,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigate: (BottomNavDestination) -> Unit,
    onLogout: () -> String? = { null }
){
    var confirmingLogout by rememberSaveable { mutableStateOf(false) }
    var logoutError by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(
        bottomBar = {
            BottomNavBar(
                selectedDestination = BottomNavDestination.Profile,
                onDestinationSelected = onNavigate
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {

                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.headlineLarge
                )

                if (fullName.isNotBlank()) {
                    Text(
                        text = fullName,
                        modifier = Modifier.padding(top = 24.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                if (email.isNotBlank()) {
                    Text(
                        text = email,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .sizeIn(minHeight = 56.dp)
                        .clickable(
                            onClickLabel = "Edit profile",
                            onClick = onEditProfile
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Profile",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .sizeIn(minHeight = 56.dp)
                        .clickable(
                            onClickLabel = "Open settings",
                            onClick = onOpenSettings
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                }
            }

            val interactions = remember { MutableInteractionSource() }
            val focused by interactions.collectIsFocusedAsState()
            val hovered by interactions.collectIsHoveredAsState()
            val pressed by interactions.collectIsPressedAsState()
            val highlighted = focused || hovered || pressed
            val colors = MaterialTheme.colorScheme
            OutlinedButton(
                onClick = { confirmingLogout = true },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).sizeIn(minHeight = 48.dp),
                interactionSource = interactions,
                border = BorderStroke(1.dp, colors.error),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (highlighted) colors.error else colors.background,
                    contentColor = if (highlighted) colors.onError else colors.error
                )
            ) {
                Text("Log out")
            }
        }
    }

    if (confirmingLogout) {
        LogoutConfirmationDialog(
            errorMessage = logoutError,
            onDismiss = {
                confirmingLogout = false
                logoutError = null
            },
            onLogout = {
                logoutError = onLogout()
                confirmingLogout = logoutError != null
            }
        )
    }
}
