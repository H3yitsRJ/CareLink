package com.example.carelink.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// Preview and isolated screen tests retain their existing behavior without an app provider.
val LocalConnectionAvailable = compositionLocalOf { true }
const val OFFLINE_ACTION_MESSAGE = "Connect to the internet before saving changes. Your entered information is still here."

// Screens can place the same banner above cached content instead of replacing it.
@Composable
fun OfflineBanner(isOffline: Boolean, modifier: Modifier = Modifier) {
    if (isOffline) Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer) {
        Text(
            "CareLink can't connect. Saved information is available. Changes that need a connection are paused.",
            Modifier.padding(12.dp).semantics { liveRegion = LiveRegionMode.Polite },
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
