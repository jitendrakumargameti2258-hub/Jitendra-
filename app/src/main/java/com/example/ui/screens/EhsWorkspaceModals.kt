package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ChecklistAnswer
import com.example.data.ChecklistLineItem
import com.example.data.EhsTemplates
import com.example.data.SafetySite
import com.example.data.TbtCategory
import com.example.ui.theme.ComplianceEmerald
import com.example.ui.theme.CriticalCrimson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditRunnerSheet(
    initialTemplate: String,
    sites: List<SafetySite>,
    defaultSiteName: String,
    defaultInspectorName: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        siteId: String,
        siteName: String,
        templateName: String,
        inspectorName: String,
        area: String,
        items: List<ChecklistLineItem>,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var templateName by remember { mutableStateOf(initialTemplate) }
    var selectedSite by remember {
        mutableStateOf(
            sites.firstOrNull { it.name.equals(defaultSiteName, ignoreCase = true) }
                ?: sites.firstOrNull()
        )
    }
    var manualSiteName by remember {
        mutableStateOf(selectedSite?.name ?: defaultSiteName.ifBlank { "Main Plant" })
    }
    var inspectorName by remember { mutableStateOf(defaultInspectorName.ifBlank { "EHS Auditor" }) }
    var area by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val checklist = remember(templateName) {
        val prompts = EhsTemplates.auditTemplates[templateName]
            ?: EhsTemplates.auditTemplates.values.first()
        mutableStateListOf<ChecklistLineItem>().apply {
            addAll(prompts.map { ChecklistLineItem(it, ChecklistAnswer.PASS) })
        }
    }

    val applicable = checklist.filter { it.answer != ChecklistAnswer.NA }
    val passed = applicable.count { it.answer == ChecklistAnswer.PASS }
    val liveScore = if (applicable.isEmpty()) 100 else ((passed * 100.0) / applicable.size).toInt()

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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "IMS Safety Audit",
                    style = MaterialTheme.typography.headlineSmall
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (liveScore >= 80) ComplianceEmerald.copy(alpha = 0.14f)
                    else CriticalCrimson.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "Score: $liveScore%",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (liveScore >= 80) ComplianceEmerald else CriticalCrimson,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EhsTemplates.auditTemplates.keys.forEach { name ->
                    FilterChip(
                        selected = templateName == name,
                        onClick = { templateName = name },
                        label = { Text(name) }
                    )
                }
            }

            if (sites.isNotEmpty()) {
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
                    label = { Text("Site Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = area,
                onValueChange = { area = it; errorText = null },
                label = { Text("Inspected Zone / Area *") },
                placeholder = { Text("e.g., Clarifier Unit 2") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audit_area_input")
            )

            OutlinedTextField(
                value = inspectorName,
                onValueChange = { inspectorName = it },
                label = { Text("Auditor Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Checklist Verification", style = MaterialTheme.typography.titleSmall)

            checklist.forEachIndexed { idx, item ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${idx + 1}. ${item.prompt}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChecklistAnswer.entries.forEach { ans ->
                                FilterChip(
                                    selected = item.answer == ans,
                                    onClick = { checklist[idx] = item.copy(answer = ans) },
                                    label = { Text(ans.label) }
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Audit Observations / Remarks") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = CriticalCrimson
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
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
                        val cleanArea = area.trim()
                        val cleanSite = (selectedSite?.name ?: manualSiteName).trim()
                        val cleanInspector = inspectorName.trim()
                        when {
                            cleanArea.isEmpty() -> errorText = "Enter the inspected zone or area."
                            cleanSite.isEmpty() -> errorText = "Enter a site name."
                            cleanInspector.isEmpty() -> errorText = "Enter auditor name."
                            else -> onSubmit(
                                selectedSite?.id.orEmpty(),
                                cleanSite,
                                templateName,
                                cleanInspector,
                                cleanArea,
                                checklist.toList(),
                                notes.trim()
                            )
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("submit_audit_button")
                ) {
                    Text(if (isSubmitting) "Saving..." else "Submit Audit")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateToolboxTalkSheet(
    sites: List<SafetySite>,
    defaultSiteName: String,
    defaultConductor: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        siteId: String,
        siteName: String,
        topic: String,
        conductorName: String,
        category: TbtCategory,
        attendeeCount: Int,
        durationMinutes: Int,
        keyTakeaways: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var category by remember { mutableStateOf(TbtCategory.TBT) }
    var topic by remember { mutableStateOf("") }
    var conductor by remember { mutableStateOf(defaultConductor.ifBlank { "Safety Officer" }) }
    var attendeesStr by remember { mutableStateOf("15") }
    var durationStr by remember { mutableStateOf("15") }
    var takeaways by remember { mutableStateOf("") }
    var selectedSite by remember {
        mutableStateOf(
            sites.firstOrNull { it.name.equals(defaultSiteName, ignoreCase = true) }
                ?: sites.firstOrNull()
        )
    }
    var manualSiteName by remember {
        mutableStateOf(selectedSite?.name ?: defaultSiteName.ifBlank { "Main Plant" })
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
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
                text = "Log Training / Toolbox Talk",
                style = MaterialTheme.typography.headlineSmall
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TbtCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.label) }
                    )
                }
            }

            if (sites.isEmpty()) {
                OutlinedTextField(
                    value = manualSiteName,
                    onValueChange = { manualSiteName = it },
                    label = { Text("Site Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
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
            }

            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it; errorText = null },
                label = { Text("Safety Topic *") },
                placeholder = { Text("e.g., Confined Space Gas Monitoring") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tbt_topic_input")
            )

            OutlinedTextField(
                value = conductor,
                onValueChange = { conductor = it },
                label = { Text("Conducted By *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = attendeesStr,
                    onValueChange = { attendeesStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Attendees *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = durationStr,
                    onValueChange = { durationStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Mins (5-480) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = takeaways,
                onValueChange = { takeaways = it },
                label = { Text("Key Safety Instructions Covered") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorText != null) {
                Text(text = errorText!!, style = MaterialTheme.typography.bodySmall, color = CriticalCrimson)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
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
                        val attendees = attendeesStr.toIntOrNull() ?: 0
                        val duration = durationStr.toIntOrNull() ?: 0
                        val cleanSite = (selectedSite?.name ?: manualSiteName).trim()
                        when {
                            topic.trim().length < 3 -> errorText = "Topic must be at least 3 characters."
                            conductor.trim().isEmpty() -> errorText = "Conductor name is required."
                            attendees !in 1..1000 -> errorText = "Attendees must be 1 to 1000."
                            duration !in 5..480 -> errorText = "Duration must be 5 to 480 minutes."
                            cleanSite.isEmpty() -> errorText = "Site name is required."
                            else -> onSubmit(
                                selectedSite?.id.orEmpty(),
                                cleanSite,
                                topic.trim(),
                                conductor.trim(),
                                category,
                                attendees,
                                duration,
                                takeaways.trim()
                            )
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("submit_tbt_button")
                ) {
                    Text(if (isSubmitting) "Saving..." else "Save Session")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSiteSheet(
    defaultManager: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String, projectCode: String, location: String, managerName: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var manager by remember { mutableStateOf(defaultManager.ifBlank { "EHS Lead" }) }
    var errorText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Add Project / Plant Site", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorText = null },
                label = { Text("Site Name *") },
                placeholder = { Text("e.g., WTP Sector 4") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("site_name_input")
            )
            OutlinedTextField(
                value = code,
                onValueChange = { code = it; errorText = null },
                label = { Text("Project Code *") },
                placeholder = { Text("e.g., WTP-402") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it; errorText = null },
                label = { Text("Location / Zone *") },
                placeholder = { Text("e.g., Pune Industrial Corridor") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = manager,
                onValueChange = { manager = it; errorText = null },
                label = { Text("Site EHS Manager *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorText != null) {
                Text(text = errorText!!, style = MaterialTheme.typography.bodySmall, color = CriticalCrimson)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
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
                        when {
                            name.trim().length < 2 -> errorText = "Site name must be at least 2 characters."
                            code.trim().length < 2 -> errorText = "Project code must be at least 2 characters."
                            location.trim().length < 2 -> errorText = "Location must be at least 2 characters."
                            manager.trim().isEmpty() -> errorText = "Manager name is required."
                            else -> onSubmit(name.trim(), code.trim(), location.trim(), manager.trim())
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("submit_site_button")
                ) {
                    Text(if (isSubmitting) "Adding..." else "Save Site")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOfficerProfileSheet(
    initialName: String,
    initialRole: String,
    initialSite: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (displayName: String, roleTitle: String, activeSiteName: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var displayName by remember { mutableStateOf(initialName) }
    var roleTitle by remember { mutableStateOf(initialRole) }
    var activeSite by remember { mutableStateOf(initialSite) }
    var errorText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Officer Profile", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it; errorText = null },
                label = { Text("Officer Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = roleTitle,
                onValueChange = { roleTitle = it; errorText = null },
                label = { Text("Designation / Role *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = activeSite,
                onValueChange = { activeSite = it },
                label = { Text("Default Site Filter (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorText != null) {
                Text(text = errorText!!, style = MaterialTheme.typography.bodySmall, color = CriticalCrimson)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
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
                        when {
                            displayName.trim().isEmpty() -> errorText = "Officer name is required."
                            roleTitle.trim().isEmpty() -> errorText = "Role title is required."
                            else -> onSubmit(displayName.trim(), roleTitle.trim(), activeSite.trim())
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(if (isSubmitting) "Saving..." else "Save Profile")
                }
            }
        }
    }
}
