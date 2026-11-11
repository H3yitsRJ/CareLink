package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.carelink.model.ConcernSeverity
import com.example.carelink.model.ConcernStatus
import com.example.carelink.model.HealthConcern

@Composable
fun HealthConcernsScreen(
    concerns: List<HealthConcern> = emptyList(),
    isLoading: Boolean = false,
    error: String? = null,
    onAdd: (() -> Unit)? = null,
    onSelect: (HealthConcern) -> Unit = {},
    onBack: (() -> Unit)? = null,
    onOpenCareRecipientConcerns: (() -> Unit)? = null
) {
    var status by rememberSaveable { mutableStateOf<ConcernStatus?>(null) }
    var severity by rememberSaveable { mutableStateOf<ConcernSeverity?>(null) }

    // Filters are combined so patients can narrow by status and severity at the same time.
    val visible = concerns.filter {
        (status == null || it.status == status) &&
                (severity == null || it.severity == severity)
    }

    Column(Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to home"
                    )
                }
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (onOpenCareRecipientConcerns != null) {
                    IconButton(onClick = onOpenCareRecipientConcerns) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Open care recipient health concerns",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Health Concerns",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Manage & view healthcare concerns",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (onAdd != null)
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+ Add concern")
            }

        val statusChipColors = FilterChipDefaults.filterChipColors(
            containerColor = Color(0xFF305D60),
            labelColor = Color.White,
            selectedContainerColor = Color(0xFF204548),
            selectedLabelColor = Color.White
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = status == null,
                onClick = { status = null },
                label = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("All")
                    }
                },
                modifier = Modifier.weight(1f),
                colors = statusChipColors,
                border = null
            )
            FilterChip(
                selected = status == ConcernStatus.ACTIVE,
                onClick = { status = ConcernStatus.ACTIVE },
                label = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Active")
                    }
                },
                modifier = Modifier.weight(1f),
                colors = statusChipColors,
                border = null
            )
            FilterChip(
                selected = status == ConcernStatus.DISCUSSED,
                onClick = { status = ConcernStatus.DISCUSSED },
                label = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Discussed")
                    }
                },
                modifier = Modifier.weight(1f),
                colors = statusChipColors,
                border = null
            )
        }

        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConcernSeverity.entries.forEach { item ->
                FilterChip(
                    selected = severity == item,
                    onClick = { severity = if (severity == item) null else item },
                    label = {
                        Text(
                            item.name
                                .lowercase()
                                .replaceFirstChar(Char::uppercase)
                        )
                    }
                )
            }
        }

        when {
            isLoading -> StateMessage("Loading health concerns")
            error != null -> CareErrorState(message = error)
            concerns.isEmpty() -> StateMessage("No health concerns recorded.")
            visible.isEmpty() -> StateMessage("No health concerns match these filters.")

            // Higher-severity concerns come first without hiding the text severity label.
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val sections = listOf(
                    "Active Concerns" to ConcernStatus.ACTIVE,
                    "Recently discussed" to ConcernStatus.DISCUSSED
                )

                sections.forEach { (heading, sectionStatus) ->
                    val sectionConcerns = visible
                        .filter { it.status == sectionStatus }
                        .sortedByDescending { it.severity.ordinal }

                    if (sectionConcerns.isNotEmpty()) {
                        item(key = "section-${sectionStatus.name}") {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = heading,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }

                        items(sectionConcerns, key = { it.id }) { concern ->
                            val severityLabel = concern.severity.name
                                .lowercase()
                                .replaceFirstChar(Char::uppercase)
                            val statusLabel = concern.status.name
                                .lowercase()
                                .replaceFirstChar(Char::uppercase)

                            val discussed = concern.status == ConcernStatus.DISCUSSED
                            val accent = if (discussed) {
                                Color(0xFF3F6631)
                            } else {
                                Color(0xFF305D60)
                            }
                            val badgeBackground = if (discussed) {
                                Color(0xFFE3EEDC)
                            } else {
                                Color(0xFFDCEEEF)
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                contentColor = Color(0xFF172426),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(role = Role.Button) {
                                            onSelect(concern)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = badgeBackground
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (discussed) {
                                                    Icons.Filled.Check
                                                } else {
                                                    Icons.Filled.Info
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = accent
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = concern.title,
                                            style = MaterialTheme.typography.titleSmall
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = badgeBackground,
                                            contentColor = accent
                                        ) {
                                            Text(
                                                text = statusLabel,
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 2.dp
                                                ),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }

                                        Text(
                                            text = "$severityLabel severity · ${concern.recordedDate}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Text(
                                        text = "›",
                                        modifier = Modifier.clearAndSetSemantics {},
                                        color = accent,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
