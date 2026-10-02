package com.example.carelink.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// Screens can place the same banner above cached content instead of replacing it.
@Composable
fun OfflineBanner(isOffline: Boolean, modifier: Modifier = Modifier) {
    if (isOffline) Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer) {
        Text(
            "You're offline. Saved information is available, and changes that need a connection are paused.",
            Modifier.padding(12.dp).semantics { liveRegion = LiveRegionMode.Polite },
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
