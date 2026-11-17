package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PillarScoreSummary
import com.example.data.RecordPillar
import com.example.data.RecordStatus
import com.example.data.SeverityLevel
import com.example.data.SixSensePillar
import com.example.ui.theme.ComplianceEmerald
import com.example.ui.theme.ComplianceEmeraldContainer
import com.example.ui.theme.CriticalCrimson
import com.example.ui.theme.CriticalCrimsonContainer
import com.example.ui.theme.HighOrange
import com.example.ui.theme.HighOrangeContainer
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.InfoCyanContainer
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SafetyAmberContainer
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

fun formatEhsTimestamp(timestamp: Timestamp?): String {
    val date = (timestamp ?: Timestamp.now()).toDate()
    val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return formatter.format(date)
}

fun SixSensePillar.icon(): ImageVector = when (this) {
    SixSensePillar.IMS_AUDITS -> Icons.AutoMirrored.Filled.Assignment
    SixSensePillar.TRAINING_TBT -> Icons.Filled.Groups
    SixSensePillar.INCIDENTS_CAPA -> Icons.Filled.ReportProblem
    SixSensePillar.OBSERVATIONS -> Icons.Filled.Visibility
    SixSensePillar.PROACTIVE -> Icons.Filled.Bolt
    SixSensePillar.ENVIRONMENT -> Icons.Filled.Eco
}

fun SixSensePillar.accentColor(): Color = when (this) {
    SixSensePillar.IMS_AUDITS -> InfoCyan
    SixSensePillar.TRAINING_TBT -> Color(0xFF7C3AED)
    SixSensePillar.INCIDENTS_CAPA -> CriticalCrimson
    SixSensePillar.OBSERVATIONS -> SafetyAmber
    SixSensePillar.PROACTIVE -> Color(0xFF0E6BA8)
    SixSensePillar.ENVIRONMENT -> ComplianceEmerald
}

fun RecordPillar.accentColor(): Color = when (this) {
    RecordPillar.OBSERVATIONS -> SafetyAmber
    RecordPillar.INCIDENTS_CAPA -> CriticalCrimson
    RecordPillar.PROACTIVE -> Color(0xFF0E6BA8)
    RecordPillar.ENVIRONMENT -> ComplianceEmerald
}

@Composable
fun EhsKpiRing(
    scorePercent: Int,
    hasData: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (hasData) (scorePercent.coerceIn(0, 100) / 100f) else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "kpi_ring"
    )
    val trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
    val ringColor = when {
        !hasData -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.45f)
        scorePercent >= 80 -> Color(0xFF34D399)
        scorePercent >= 55 -> Color(0xFFFBBF24)
        else -> Color(0xFFF87171)
    }

    Box(
        modifier = modifier.size(104.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(104.dp)) {
            val strokeWidth = 10.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            if (animatedProgress > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (hasData) "$scorePercent%" else "--",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "KPI SCORE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun SixSensePillarCard(
    summary: PillarScoreSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = summary.pillar.accentColor()
    val hasEntries = summary.totalEntries > 0

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("pillar_card_${summary.pillar.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.14f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = summary.pillar.icon(),
                            contentDescription = summary.pillar.title,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (hasEntries) accent.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (hasEntries) "${summary.scorePercent}%" else "No Data",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasEntries) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${summary.pillar.shortCode} · ${summary.pillar.title}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = summary.pillar.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LinearProgressIndicator(
                progress = { if (hasEntries) summary.scorePercent / 100f else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${summary.totalEntries} logged",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (summary.openCount > 0) {
                    Text(
                        text = "${summary.openCount} open",
                        style = MaterialTheme.typography.labelSmall,
                        color = CriticalCrimson
                    )
                } else if (hasEntries) {
                    Text(
                        text = "Compliant",
                        style = MaterialTheme.typography.labelSmall,
                        color = ComplianceEmerald
                    )
                }
            }
        }
    }
}

@Composable
fun SeverityBadge(severity: SeverityLevel) {
    val (bg, fg) = when (severity) {
        SeverityLevel.LOW -> InfoCyanContainer to InfoCyan
        SeverityLevel.MEDIUM -> SafetyAmberContainer to SafetyAmber
        SeverityLevel.HIGH -> HighOrangeContainer to HighOrange
        SeverityLevel.CRITICAL -> CriticalCrimsonContainer to CriticalCrimson
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Text(
            text = severity.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun RecordStatusBadge(status: RecordStatus) {
    val (bg, fg) = when (status) {
        RecordStatus.OPEN -> CriticalCrimsonContainer to CriticalCrimson
        RecordStatus.IN_PROGRESS -> SafetyAmberContainer to SafetyAmber
        RecordStatus.CLOSED -> ComplianceEmeraldContainer to ComplianceEmerald
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status == RecordStatus.CLOSED) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = status.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyEhsStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Surface(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable(onClick = onAction),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
