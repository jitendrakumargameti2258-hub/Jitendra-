package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.RecordPillar
import com.example.data.SafetyRecord
import com.example.data.SixSensePillar
import com.example.ui.EhsDashboardData
import com.example.ui.components.EhsKpiRing
import com.example.ui.components.EmptyEhsStateCard
import com.example.ui.components.RecordStatusBadge
import com.example.ui.components.SeverityBadge
import com.example.ui.components.SixSensePillarCard
import com.example.ui.components.formatEhsTimestamp
import com.example.ui.theme.CriticalCrimson
import com.example.ui.theme.SafetyAmberBright
import com.example.ui.theme.SafetyTeal
import com.example.ui.theme.SafetyTealLight

@Composable
fun DashboardScreen(
    data: EhsDashboardData,
    onSelectSiteFilter: (String) -> Unit,
    onOpenPillar: (SixSensePillar) -> Unit,
    onQuickLogRecord: (RecordPillar) -> Unit,
    onOpenAuditModal: () -> Unit,
    onOpenTbtModal: () -> Unit,
    onOpenRecordDetail: (SafetyRecord) -> Unit,
    onOpenSiteModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kpi = data.kpiSummary
    val recentRecords = data.records.take(5)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Site Filter Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = "Filter by site",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                FilterChip(
                    selected = data.activeSiteFilter.isBlank(),
                    onClick = { onSelectSiteFilter("") },
                    label = { Text("All Sites") },
                    modifier = Modifier.testTag("site_filter_all")
                )
                data.sites.forEach { site ->
                    FilterChip(
                        selected = data.activeSiteFilter.equals(site.name, ignoreCase = true),
                        onClick = { onSelectSiteFilter(site.name) },
                        label = { Text(site.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = onOpenSiteModal,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.LocationCity,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("+ Site") },
                    modifier = Modifier.testTag("quick_add_site_chip")
                )
            }
        }

        // Hero 6-Senses KPI Telemetry Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SafetyTeal, SafetyTealLight)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = SafetyAmberBright.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (data.activeSiteFilter.isBlank()) "ALL SITES · 6 SENSES KPI" else "${data.activeSiteFilter.uppercase()} · KPI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFDE68A),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Site Safety Index",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Text(
                                text = if (kpi.hasAnyData) {
                                    "${kpi.totalClosedCount} closed · ${kpi.totalOpenCount} open · ${kpi.totalTbtAttendees} trained"
                                } else {
                                    "Log an observation, audit, or TBT to compute live site score"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MiniHeroStat(label = "Audits", value = "${kpi.totalInspections}")
                                MiniHeroStat(label = "Avg Audit", value = if (kpi.totalInspections > 0) "${kpi.avgAuditScore}%" else "--")
                                MiniHeroStat(label = "TBTs", value = "${kpi.totalTbtSessions}")
                            }
                        }

                        EhsKpiRing(
                            scorePercent = kpi.overallScorePercent,
                            hasData = kpi.hasAnyData
                        )
                    }
                }
            }
        }

        // Critical Open Hazard Alert Banner (if any)
        if (kpi.openCriticalCount > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CriticalCrimson.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, CriticalCrimson.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPillar(SixSensePillar.INCIDENTS_CAPA) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Critical alert",
                            tint = CriticalCrimson,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${kpi.openCriticalCount} Critical Hazard(s) Require CAPA Closure",
                                style = MaterialTheme.typography.titleSmall,
                                color = CriticalCrimson
                            )
                            Text(
                                text = "Tap to inspect root cause & close pending actions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Quick Field Actions Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Quick Field Entry",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Filled.Visibility,
                        label = "Observation",
                        accent = Color(0xFFD97706),
                        testTag = "quick_action_observation",
                        onClick = { onQuickLogRecord(RecordPillar.OBSERVATIONS) }
                    )
                    QuickActionTile(
                        icon = Icons.Filled.ReportProblem,
                        label = "Incident / CAPA",
                        accent = CriticalCrimson,
                        testTag = "quick_action_incident",
                        onClick = { onQuickLogRecord(RecordPillar.INCIDENTS_CAPA) }
                    )
                    QuickActionTile(
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        label = "IMS Audit",
                        accent = Color(0xFF0284C7),
                        testTag = "quick_action_audit",
                        onClick = onOpenAuditModal
                    )
                    QuickActionTile(
                        icon = Icons.Filled.Groups,
                        label = "Toolbox Talk",
                        accent = Color(0xFF7C3AED),
                        testTag = "quick_action_tbt",
                        onClick = onOpenTbtModal
                    )
                    QuickActionTile(
                        icon = Icons.Filled.Bolt,
                        label = "Proactive / PTW",
                        accent = Color(0xFF0E6BA8),
                        testTag = "quick_action_proactive",
                        onClick = { onQuickLogRecord(RecordPillar.PROACTIVE) }
                    )
                    QuickActionTile(
                        icon = Icons.Filled.Eco,
                        label = "Environment",
                        accent = Color(0xFF059669),
                        testTag = "quick_action_environment",
                        onClick = { onQuickLogRecord(RecordPillar.ENVIRONMENT) }
                    )
                }
            }
        }

        // 6 Senses of EHS Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "6 Senses of EHS Modules",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tap module",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val columns = if (maxWidth >= 600.dp) 3 else 2
                    val rows = kpi.pillarScores.chunked(columns)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { summary ->
                                    SixSensePillarCard(
                                        summary = summary,
                                        onClick = { onOpenPillar(summary.pillar) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(columns - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Activity Section
        item {
            Text(
                text = "Recent Safety Logs",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (recentRecords.isEmpty()) {
            item {
                EmptyEhsStateCard(
                    icon = Icons.Filled.Shield,
                    title = "No safety logs recorded yet",
                    subtitle = "Start capturing Unsafe Conditions, Near-Misses, Incidents, Proactive PTW checks, or Environmental logs from the field.",
                    actionLabel = "Log First Observation",
                    onAction = { onQuickLogRecord(RecordPillar.OBSERVATIONS) }
                )
            }
        } else {
            items(recentRecords, key = { it.id }) { record ->
                SafetyRecordSummaryCard(
                    record = record,
                    onClick = { onOpenRecordDetail(record) }
                )
            }
        }
    }
}

@Composable
private fun MiniHeroStat(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.78f)
        )
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(108.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.14f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SafetyRecordSummaryCard(
    record: SafetyRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("safety_record_card_${record.id}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${record.pillarEnum().displayLabel} · ${record.recordType}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                RecordStatusBadge(status = record.statusEnum())
            }

            Text(
                text = record.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (record.description.isNotBlank()) {
                Text(
                    text = record.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SeverityBadge(severity = record.severityEnum())
                    Text(
                        text = "${record.siteName} · ${record.locationArea}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatEhsTimestamp(record.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
