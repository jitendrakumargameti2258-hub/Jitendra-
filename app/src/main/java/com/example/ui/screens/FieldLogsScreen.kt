package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.RecordPillar
import com.example.data.RecordStatus
import com.example.data.SafetyRecord
import com.example.ui.components.EmptyEhsStateCard

@Composable
fun FieldLogsScreen(
    records: List<SafetyRecord>,
    selectedPillar: RecordPillar?,
    selectedStatus: RecordStatus?,
    searchQuery: String,
    onPillarChange: (RecordPillar?) -> Unit,
    onStatusChange: (RecordStatus?) -> Unit,
    onSearchChange: (String) -> Unit,
    onOpenRecordDetail: (SafetyRecord) -> Unit,
    onCreateNewRecord: (RecordPillar) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredRecords = remember(records, selectedPillar, selectedStatus, searchQuery) {
        records.filter { rec ->
            val pillarMatch = selectedPillar == null || rec.pillarEnum() == selectedPillar
            val statusMatch = selectedStatus == null || rec.statusEnum() == selectedStatus
            val queryMatch = searchQuery.isBlank() ||
                rec.title.contains(searchQuery, ignoreCase = true) ||
                rec.recordType.contains(searchQuery, ignoreCase = true) ||
                rec.locationArea.contains(searchQuery, ignoreCase = true) ||
                rec.siteName.contains(searchQuery, ignoreCase = true)
            pillarMatch && statusMatch && queryMatch
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search hazards, CAPA, site or zone...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("logs_search_input")
            )

            // Pillar Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPillar == null,
                    onClick = { onPillarChange(null) },
                    label = { Text("All Modules") }
                )
                RecordPillar.entries.forEach { pillar ->
                    FilterChip(
                        selected = selectedPillar == pillar,
                        onClick = { onPillarChange(if (selectedPillar == pillar) null else pillar) },
                        label = { Text(pillar.displayLabel) },
                        modifier = Modifier.testTag("filter_pillar_${pillar.wireValue.lowercase()}")
                    )
                }
            }

            // Status Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { onStatusChange(null) },
                    label = { Text("All Status") }
                )
                RecordStatus.entries.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { onStatusChange(if (selectedStatus == status) null else status) },
                        label = { Text(status.label) }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredRecords.isEmpty()) {
                item {
                    EmptyEhsStateCard(
                        icon = Icons.Filled.Visibility,
                        title = "No matching safety records",
                        subtitle = if (records.isEmpty()) {
                            "Log safety observations, incidents & CAPA actions, proactive PTW walks, or environmental checks."
                        } else {
                            "Try clearing one of your pillar or status filters."
                        },
                        actionLabel = "New Safety Entry",
                        onAction = { onCreateNewRecord(selectedPillar ?: RecordPillar.OBSERVATIONS) }
                    )
                }
            } else {
                items(filteredRecords, key = { it.id }) { record ->
                    SafetyRecordSummaryCard(
                        record = record,
                        onClick = { onOpenRecordDetail(record) }
                    )
                }
            }
        }
    }
}
