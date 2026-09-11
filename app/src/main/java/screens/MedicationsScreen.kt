package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Medication
import com.example.carelink.ui.theme.CareLinkTheme
import navigation.BottomNavBar
import navigation.BottomNavDestination

/**
 * Top-level medication list screen.
 *
 * The parent supplies data and handles navigation through the callback parameters. Keeping those
 * responsibilities outside this file makes the UI easy to preview and prevents Firebase or app
 * navigation code from getting mixed into the layout.
 *
 * Editing guide:
 * - Change the title, description, and button copy in the first three LazyColumn items below.
 * - Change page spacing in contentPadding and verticalArrangement.
 * - Change a populated row in MedicationCard.
 * - Change loading, error, and empty content in the when block and state-card helpers.
 * - Change the bottom navigation in navigation/BottomNavBar.kt, not here.
 */
@Composable
fun MedicationsScreen(
    // Defaults let previews and callers render a safe empty screen before data arrives.
    medications: List<Medication> = emptyList(),
    isLoading: Boolean = false,
    error: String? = null,
    // The screen reports taps to its parent instead of deciding which screen opens next.
    onAdd: () -> Unit = {},
    onSelect: (Medication) -> Unit = {},
    onNavigate: (BottomNavDestination) -> Unit = {}
) {
    // Scaffold reserves space for the persistent bottom bar. Its innerPadding keeps list content
    // above the navigation and Android gesture area on every supported phone size.
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Passing Medications marks only this destination as selected.
        bottomBar = { BottomNavBar(BottomNavDestination.Medications, onNavigate) }
    ) { innerPadding ->
        // One LazyColumn owns all vertical scrolling. This avoids nested scroll areas and lets long
        // medication names or larger accessibility text increase row height without clipping.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            // Edit these values to change the outside inset. The design guide uses a 16 dp phone
            // inset, with extra space above the title and below the final card.
            contentPadding = PaddingValues(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 24.dp),
            // This is the space between the title block, button, section heading, and every card.
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen heading. The short description comes from the supplied screenshot and explains
            // the purpose without repeating details found in the cards.
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Medications",
                        style = MaterialTheme.typography.headlineLarge,
                        // heading() lets TalkBack users jump between this title and "Today."
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "Manage and schedule your medications",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // The main action stays near the title, as required by the CareLink design guide.
            // A minimum width matches the reference without forcing a full-width button. The fixed
            // height gives it a touch target larger than Android's 48 dp accessibility minimum.
            item {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.widthIn(min = 196.dp).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Add medication", style = MaterialTheme.typography.labelMedium)
                }
            }

            // "Today" groups current schedule information. The spacer adds the larger visual break
            // seen in the reference while the list's standard 16 dp gap handles normal separation.
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )
            }

            // Keep these branches mutually exclusive so stale medication data never appears behind
            // a loading or error message. Add future states, such as offline sync, in this block.
            when {
                isLoading -> item { MedicationLoadingCard() }
                error != null -> item {
                    // The parent owns the exact recovery message because it knows what failed.
                    MedicationStateCard("Medications unavailable", error, isError = true)
                }
                medications.isEmpty() -> item {
                    // A real empty state replaces the blank placeholder cards in the screenshot.
                    // Fake health records could mislead a patient, even in an early prototype.
                    MedicationStateCard("No medications yet", "Add a medication to create its schedule.")
                }
                else -> items(medications, key = { it.id }) { medication ->
                    // Stable IDs let Compose reuse the correct row when the list changes order.
                    MedicationCard(medication = medication, onClick = { onSelect(medication) })
                }
            }
        }
    }
}

/** One populated medication row. Edit this composable to change card content or styling. */
@Composable
private fun MedicationCard(medication: Medication, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // clip keeps the clickable ripple inside the same rounded edge as the Card.
            .clip(RoundedCornerShape(16.dp))
            // Role.Button tells assistive technology that the whole row opens medication details.
            .clickable(role = Role.Button, onClick = onClick),
        // Keep this radius in sync with MedicationStateCard and MedicationLoadingCard.
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        // The small shadow separates white cards from the pale blue page without a heavy border.
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                // weight gives text all remaining width while reserving room for the chevron.
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Medication name comes first because it is the primary identifier patients scan.
                Text(
                    text = medication.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                // Strength and dose are separate fields in the model. This line keeps both visible
                // and removes either value cleanly if older remote data is incomplete.
                Text(
                    text = listOf(medication.strength, medication.dose)
                        .filter(String::isNotBlank)
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // The first reminder is the next useful schedule detail available in this model.
                // Replace this selection when schedule logic can calculate the next upcoming dose.
                Text(
                    text = medication.reminderTimes.firstOrNull()?.let { "Next dose at $it" }
                        ?: "No reminder scheduled",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // The chevron reinforces that the entire card opens details. It is decorative, so its
            // contentDescription is null and TalkBack announces only the card's combined text.
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Shared geometry for empty and error states keeps the layout from jumping as data changes. */
@Composable
private fun MedicationStateCard(title: String, message: String, isError: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                // Error text uses the theme's semantic error color. Empty-state text stays neutral.
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Loading state uses a fixed card instead of placeholder medication records. */
@Composable
private fun MedicationLoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The visible label means loading does not rely on animation alone.
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
            Text("Loading medications", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// Android Studio renders this preview at the 412 by 892 baseline from the UI design guide.
// Preview records stay here only. They never enter the running app or a patient's data.
@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun MedicationsScreenPreview() {
    CareLinkTheme {
        MedicationsScreen(
            medications = listOf(
                Medication("1", "patient", "Lisinopril", "10 mg", "1 tablet", "Daily", listOf("08:00")),
                Medication("2", "patient", "Metformin", "500 mg", "1 tablet", "Twice daily", listOf("08:30", "18:30")),
                Medication("3", "patient", "Atorvastatin", "20 mg", "1 tablet", "Daily", listOf("21:00"))
            )
        )
    }
}
