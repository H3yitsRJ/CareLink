package com.example.carelink.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.carelink.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.example.carelink.ui.keepKeyboardOnTap
import navigation.BottomNavBar
import navigation.BottomNavDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareHistoryScreen(
    entries: List<CareHistoryEntry> = emptyList(),
    isLoading: Boolean = false,
    loadError: String? = null,
    onRetry: () -> Unit = {},
    onBack: () -> Unit = {},
    onOpenMedication: ((CareHistoryEntry) -> Unit)? = null,
    onOpenCaregiverAccess: ((CareHistoryEntry) -> Unit)? = null,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    loadMoreError: String? = null,
    onLoadMore: () -> Unit = {},
    onNavigate: ((BottomNavDestination) -> Unit)? = null
) {
    var start by rememberSaveable { mutableStateOf("") }
    var end by rememberSaveable { mutableStateOf("") }
    var selectedTypes by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var appliedStart by rememberSaveable { mutableStateOf<Long?>(null) }
    var appliedEnd by rememberSaveable { mutableStateOf<Long?>(null) }
    var appliedTypes by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var dateField by remember { mutableStateOf<String?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf("All") }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    val tabs = listOf(
        "All" to null,
        "Meds" to CareActivityType.MEDICATION,
        "Visits" to CareActivityType.APPOINTMENT,
        "Tasks" to CareActivityType.CARE_TASK
    )

    val tabType = tabs.first { (label, _) -> label == selectedTab }.second

    BackHandler {
        if (dateField != null) dateField = null else onBack()
    }

    val applied = remember(appliedStart, appliedEnd, appliedTypes) {
        CareHistoryFilter(
            appliedStart,
            appliedEnd,
            CareActivityType.entries.filter { it.name in appliedTypes }.toSet()
        )
    }

    val visible = remember(entries, applied, tabType) {
        val matchingEntries = applied.apply(entries).filter { entry ->
            tabType == null || entry.type == tabType
        }
        CareHistoryEntry.newestFirst(matchingEntries)
    }

    if (dateField != null) {
        val picker = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { dateField = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        picker.selectedDateMillis?.let { millis ->
                            // Material date picker dates are UTC calendar dates, not local instants.
                            val date = SimpleDateFormat(DATE_FIELD_PATTERN, Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }.format(Date(millis))

                            if (dateField == "start") start = date else end = date
                        }
                        dateField = null
                    },
                    enabled = picker.selectedDateMillis != null
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { dateField = null }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = picker, showModeToggle = false)
        }
    }

    Scaffold(
        bottomBar = {
            if (onNavigate != null) {
                BottomNavBar(
                    selectedDestination = BottomNavDestination.Medications,
                    onDestinationSelected = onNavigate
                )
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = onBack) {
                        Text("‹ Back to medications")
                    }

                    Text(
                        text = "Care History",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tabs.forEach { (label, _) ->
                            FilterChip(
                                selected = selectedTab == label,
                                onClick = { selectedTab = label },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White,
                                    labelColor = Color(0xFF2F5D60),
                                    selectedContainerColor = Color(0xFF2F5D60),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    TextButton(onClick = { showFilters = !showFilters }) {
                        Text(
                            when {
                                showFilters -> "Hide filters"
                                applied.isActive -> "Filters applied"
                                else -> "Filters"
                            }
                        )
                    }
                }
            }

            if (showFilters) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = start,
                            onValueChange = { start = it },
                            label = { Text("Start date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(onClick = { dateField = "start" }) {
                            Text("Choose start date")
                        }

                        OutlinedTextField(
                            value = end,
                            onValueChange = { end = it },
                            label = { Text("End date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(onClick = { dateField = "end" }) {
                            Text("Choose end date")
                        }
                    }
                }

                item {
                    Column {
                        CareActivityType.entries.forEach { type ->
                            FilterChip(
                                selected = type.name in selectedTypes,
                                onClick = {
                                    selectedTypes = if (type.name in selectedTypes) {
                                        selectedTypes - type.name
                                    } else {
                                        selectedTypes + type.name
                                    }
                                },
                                label = { Text(type.label) }
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val first = historyDateBoundary(start.trim())
                            val last = historyDateBoundary(end.trim(), endOfDay = true)

                            error = if (
                                (start.isNotBlank() && first == null) ||
                                (end.isNotBlank() && last == null)
                            ) {
                                "Enter dates as YYYY-MM-DD or leave them blank."
                            } else {
                                CareHistoryFilter(first, last).validate()
                            }

                            if (error == null) {
                                appliedStart = first
                                appliedEnd = last
                                appliedTypes = selectedTypes.toList()
                                showFilters = false
                            }
                        }) {
                            Text("Apply filters")
                        }

                        OutlinedButton(onClick = {
                            start = ""
                            end = ""
                            selectedTypes = emptyList()
                            appliedStart = null
                            appliedEnd = null
                            appliedTypes = emptyList()
                            error = null
                        }) {
                            Text("Clear")
                        }
                    }
                }

                error?.let { message ->
                    item {
                        Text(message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (applied.isActive) {
                item {
                    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    val filterSummary = listOfNotNull(
                        appliedStart?.let { "From ${dateFormat.format(Date(it))}" },
                        appliedEnd?.let { "Through ${dateFormat.format(Date(it))}" },
                        applied.types.takeIf { it.isNotEmpty() }?.joinToString { it.label }
                    ).joinToString(" • ")

                    Text(
                        text = "Active filters: $filterSummary",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            when {
                isLoading -> item {
                    CircularProgressIndicator()
                    Text("Loading care history...")
                }

                loadError != null -> item {
                    CareErrorState(
                        message = loadError,
                        onRetry = onRetry
                    )
                }

                visible.isEmpty() -> item {
                    Text(
                        when {
                            hasMore || isLoadingMore ->
                                "No matching events in the history loaded so far. Load more to check older events."

                            applied.isActive ->
                                "No history matches these filters. Clear filters or choose other dates or activity types."

                            tabType != null ->
                                "No ${tabType.label.lowercase()} events yet."

                            else ->
                                "No care history yet."
                        }
                    )
                }

                else -> {
                    val dayFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    val today = Date()
                    val yesterday = java.util.Calendar.getInstance().apply {
                        time = today
                        add(java.util.Calendar.DAY_OF_MONTH, -1)
                    }.time
                    val todayLabel = dayFormat.format(today)
                    val yesterdayLabel = dayFormat.format(yesterday)
                    val dayLabels = visible.map { entry ->
                        dayFormat.format(Date(entry.occurredAtMillis))
                    }

                    itemsIndexed(visible, key = { _, entry -> entry.id }) { index, entry ->
                        val day = dayLabels[index]

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (index == 0 || day != dayLabels[index - 1]) {
                                Text(
                                    text = when (day) {
                                        todayLabel -> "Today"
                                        yesterdayLabel -> "Yesterday"
                                        else -> day
                                    },
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            val connectsAbove = index > 0 && dayLabels[index - 1] == day
                            val connectsBelow = index < dayLabels.lastIndex && dayLabels[index + 1] == day
                            val dotColor = MaterialTheme.colorScheme.primary
                            val lineColor = MaterialTheme.colorScheme.outline

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .drawBehind {
                                        val center = Offset(x = 6.dp.toPx(), y = size.height / 2)
                                        val strokeWidth = 1.dp.toPx()

                                        if (connectsAbove) {
                                            drawLine(
                                                color = lineColor,
                                                start = Offset(center.x, 0f),
                                                end = center,
                                                strokeWidth = strokeWidth
                                            )
                                        }

                                        if (connectsBelow) {
                                            drawLine(
                                                color = lineColor,
                                                start = center,
                                                end = Offset(center.x, size.height + 12.dp.toPx()),
                                                strokeWidth = strokeWidth
                                            )
                                        }

                                        drawCircle(color = dotColor, radius = 3.dp.toPx(), center = center)
                                    }
                                    .padding(start = 16.dp)
                            ) {
                                CareHistoryCard(
                                    entry = entry,
                                    onOpenMedication = onOpenMedication,
                                    onOpenCaregiverAccess = onOpenCaregiverAccess
                                )
                            }
                        }
                    }
                }
            }

            if (!isLoading && loadError == null) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        loadMoreError?.let { message ->
                            CareErrorState(message = message)
                        }

                        when {
                            isLoadingMore -> {
                                CircularProgressIndicator()
                                Text("Loading older history...")
                            }

                            hasMore -> {
                                OutlinedButton(
                                    onClick = onLoadMore,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (loadMoreError != null) {
                                            "Retry loading older history"
                                        } else {
                                            "Load more"
                                        }
                                    )
                                }
                            }

                            entries.isNotEmpty() -> {
                                Text(
                                    "All history loaded.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun CareHistoryCard(
    entry: CareHistoryEntry,
    onOpenMedication: ((CareHistoryEntry) -> Unit)? = null,
    onOpenCaregiverAccess: ((CareHistoryEntry) -> Unit)? = null
) {
    val clockFormat = LocalClockFormat.current
    val time = remember(entry.occurredAtMillis, clockFormat) {
        SimpleDateFormat(clockFormat.pattern, Locale.getDefault())
            .format(Date(entry.occurredAtMillis))
    }
    val openRecord = if (entry.relatedRecordId.isNullOrBlank()) {
        null
    } else {
        when (entry.type) {
            CareActivityType.MEDICATION -> onOpenMedication
            CareActivityType.CAREGIVER_ACCESS -> onOpenCaregiverAccess
            else -> null
        }
    }
    val actionLabel = when (entry.type) {
        CareActivityType.MEDICATION -> "Open medication"
        CareActivityType.CAREGIVER_ACCESS -> "Open caregiver access"
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (openRecord != null) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClickLabel = actionLabel,
                        onClick = { openRecord(entry) }
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = entry.type.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        text = time,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Text(
                    text = entry.summary,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )

                if (openRecord != null) {
                    Text(
                        text = "›",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Text(
                text = entry.actorId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { "User: $it" }
                    ?: "User: Unavailable for this older record",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

        }
    }
}
