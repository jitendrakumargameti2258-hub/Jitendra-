package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.EhsTemplates
import com.example.data.RecordPillar
import com.example.data.RecordStatus
import com.example.data.SafetyRecord
import com.example.data.SafetySite
import com.example.data.SeverityLevel
import com.example.ui.components.RecordStatusBadge
import com.example.ui.components.SeverityBadge
import com.example.ui.components.formatEhsTimestamp
import com.example.ui.theme.ComplianceEmerald
import com.example.ui.theme.CriticalCrimson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSafetyRecordSheet(
    initialPillar: RecordPillar,
    sites: List<SafetySite>,
    defaultSiteName: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        pillar: RecordPillar,
        recordType: String,
        title: String,
        description: String,
        siteId: String,
        siteName: String,
        locationArea: String,
        severity: SeverityLevel,
        assignedTo: String,
        capaAction: String,
        rootCause: String,
        dueDateLabel: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pillar by remember { mutableStateOf(initialPillar) }
    val availableTypes = remember(pillar) {
        EhsTemplates.recordTypesByPillar[pillar] ?: listOf("Safety Observation")
    }
    var recordType by remember(pillar) { mutableStateOf(availableTypes.first()) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSite by remember {
        mutableStateOf(
            sites.firstOrNull { it.name.equals(defaultSiteName, ignoreCase = true) }
                ?: sites.firstOrNull()
        )
    }
    var manualSiteName by remember {
        mutableStateOf(selectedSite?.name ?: defaultSiteName.ifBlank { "Main Plant" })
    }
    var locationArea by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf(SeverityLevel.MEDIUM) }
    var assignedTo by remember { mutableStateOf("") }
    var rootCause by remember { mutableStateOf("") }
    var capaAction by remember { mutableStateOf("") }
    var dueDateLabel by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Log View EHS Field Record",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Pillar selector
            Text("Module Pillar", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecordPillar.entries.forEach { p ->
                    FilterChip(
                        selected = pillar == p,
                        onClick = { pillar = p },
                        label = { Text(p.displayLabel) }
                    )
                }
            }

            // Record Type chips
            Text("Classification", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableTypes.forEach { type ->
                    FilterChip(
                        selected = recordType == type,
                        onClick = { recordType = type },
                        label = { Text(type) }
                    )
                }
            }

            // Severity selector
            Text("Risk Severity", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SeverityLevel.entries.forEach { lvl ->
                    FilterChip(
                        selected = severity == lvl,
                        onClick = { severity = lvl },
                        label = { Text(lvl.label) }
                    )
                }
            }

            if (sites.isNotEmpty()) {
                Text("Site", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sites.forEach { site ->
                        FilterChip(
                            selected = selectedSite?.id == site.id,
                            onClick = {
                                selectedSite = site
                                manualSiteName = site.name
                            },
                            label = { Text(site.name) }
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = manualSiteName,
                    onValueChange = { manualSiteName = it },
                    label = { Text("Site / Project Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it; validationError = null },
                label = { Text("Hazard / Observation Title *") },
                placeholder = { Text("e.g., Unprotected excavation edge near Tank 2") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("record_title_input")
            )

            OutlinedTextField(
                value = locationArea,
                onValueChange = { locationArea = it; validationError = null },
                label = { Text("Zone / Area *") },
                placeholder = { Text("e.g., Pump House Bay 3") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("record_area_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Field Description") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = assignedTo,
                onValueChange = { assignedTo = it },
                label = { Text("CAPA Assigned To") },
                placeholder = { Text("Engineer or Supervisor name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (pillar == RecordPillar.INCIDENTS_CAPA) {
                OutlinedTextField(
                    value = rootCause,
                    onValueChange = { rootCause = it },
                    label = { Text("Root Cause Analysis") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = capaAction,
                onValueChange = { capaAction = it },
                label = { Text("Immediate / Corrective Action (CAPA)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dueDateLabel,
                onValueChange = { dueDateLabel = it },
                label = { Text("Target Closure Date") },
                placeholder = { Text("e.g., Today 5 PM or 20 Nov") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val cleanTitle = title.trim()
                        val cleanArea = locationArea.trim()
                        val cleanSite = (selectedSite?.name ?: manualSiteName).trim()
                        when {
                            cleanTitle.length < 3 -> validationError = "Enter a title (min 3 characters)."
                            cleanArea.isEmpty() -> validationError = "Enter the site zone or area."
                            cleanSite.isEmpty() -> validationError = "Enter a site name."
                            else -> onSubmit(
                                pillar,
                                recordType,
                                cleanTitle,
                                description.trim(),
                                selectedSite?.id.orEmpty(),
                                cleanSite,
                                cleanArea,
                                severity,
                                assignedTo.trim(),
                                capaAction.trim(),
                                rootCause.trim(),
                                dueDateLabel.trim()
                            )
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("submit_record_button")
                ) {
                    Text(if (isSubmitting) "Saving..." else "Save Record")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyRecordDetailSheet(
    record: SafetyRecord,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onMarkInProgress: (SafetyRecord) -> Unit,
    onCloseRecord: (SafetyRecord, closureNotes: String, capaAction: String, rootCause: String) -> Unit,
    onDeleteRecord: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rootCause by remember(record.id) { mutableStateOf(record.rootCause) }
    var capaAction by remember(record.id) { mutableStateOf(record.capaAction) }
    var closureNotes by remember(record.id) { mutableStateOf(record.closureNotes) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val status = record.statusEnum()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${record.pillarEnum().displayLabel} · ${record.recordType}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                RecordStatusBadge(status = status)
            }

            Text(
                text = record.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SeverityBadge(severity = record.severityEnum())
                Text(
                    text = "${record.siteName} · ${record.locationArea}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Logged: ${formatEhsTimestamp(record.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (record.description.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = record.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (record.assignedTo.isNotBlank() || record.dueDateLabel.isNotBlank()) {
                Text(
                    text = "Assigned: ${record.assignedTo.ifBlank { "Unassigned" }}  •  Target: ${record.dueDateLabel.ifBlank { "Immediate" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (status != RecordStatus.CLOSED) {
                OutlinedTextField(
                    value = rootCause,
                    onValueChange = { rootCause = it },
                    label = { Text("Root Cause") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = capaAction,
                    onValueChange = { capaAction = it },
                    label = { Text("Corrective & Preventive Action (CAPA)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = closureNotes,
                    onValueChange = { closureNotes = it; errorText = null },
                    label = { Text("Closure Verification Notes *") },
                    placeholder = { Text("Describe action completed to close this hazard") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("closure_notes_input")
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = CriticalCrimson
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (status == RecordStatus.OPEN) {
                        OutlinedButton(
                            onClick = { onMarkInProgress(record) },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Mark In Progress")
                        }
                    }
                    Button(
                        onClick = {
                            if (closureNotes.trim().length < 3) {
                                errorText = "Enter verification notes (min 3 chars) before closing."
                            } else {
                                onCloseRecord(record, closureNotes.trim(), capaAction.trim(), rootCause.trim())
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = ComplianceEmerald),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("close_capa_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify & Close")
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ComplianceEmerald.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, ComplianceEmerald.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Closed & Verified",
                            style = MaterialTheme.typography.titleSmall,
                            color = ComplianceEmerald
                        )
                        if (record.rootCause.isNotBlank()) {
                            Text("Root Cause: ${record.rootCause}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (record.capaAction.isNotBlank()) {
                            Text("CAPA: ${record.capaAction}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = "Verification: ${record.closureNotes}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onDeleteRecord(record.id) },
                    colors = ButtonDefaults.textButtonColors(contentColor = CriticalCrimson)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        }
    }
}
