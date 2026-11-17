package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class RecordPillar(val wireValue: String, val displayLabel: String) {
    OBSERVATIONS("OBSERVATIONS", "Observations"),
    INCIDENTS_CAPA("INCIDENTS_CAPA", "Incidents & CAPA"),
    PROACTIVE("PROACTIVE", "Proactive Measures"),
    ENVIRONMENT("ENVIRONMENT", "Environment");

    companion object {
        fun fromRaw(raw: String?): RecordPillar =
            entries.firstOrNull { it.wireValue == raw } ?: OBSERVATIONS
    }
}

enum class SixSensePillar(
    val title: String,
    val subtitle: String,
    val shortCode: String
) {
    IMS_AUDITS("IMS & Audits", "Checklists & Site Compliance", "01"),
    TRAINING_TBT("Training & TBT", "Toolbox Talks & Reviews", "02"),
    INCIDENTS_CAPA("Incidents & CAPA", "Investigations & Closures", "03"),
    OBSERVATIONS("Observations", "UA / UC & Near-Miss Logs", "04"),
    PROACTIVE("Proactive Measures", "PTW, Safety Walks & PPE", "05"),
    ENVIRONMENT("Environment", "Spills, Waste & 5S Control", "06")
}

enum class SeverityLevel(val wireValue: String, val label: String) {
    LOW("LOW", "Low"),
    MEDIUM("MEDIUM", "Medium"),
    HIGH("HIGH", "High"),
    CRITICAL("CRITICAL", "Critical");

    companion object {
        fun fromRaw(raw: String?): SeverityLevel =
            entries.firstOrNull { it.wireValue == raw } ?: MEDIUM
    }
}

enum class RecordStatus(val wireValue: String, val label: String) {
    OPEN("OPEN", "Open"),
    IN_PROGRESS("IN_PROGRESS", "In Progress"),
    CLOSED("CLOSED", "Closed");

    companion object {
        fun fromRaw(raw: String?): RecordStatus =
            entries.firstOrNull { it.wireValue == raw } ?: OPEN
    }
}

enum class SiteStatus(val wireValue: String, val label: String) {
    ACTIVE("ACTIVE", "Active"),
    COMPLETED("COMPLETED", "Completed");

    companion object {
        fun fromRaw(raw: String?): SiteStatus =
            entries.firstOrNull { it.wireValue == raw } ?: ACTIVE
    }
}

enum class InspectionStatus(val wireValue: String, val label: String) {
    OPEN("OPEN", "Open"),
    COMPLETED("COMPLETED", "Completed");

    companion object {
        fun fromRaw(raw: String?): InspectionStatus =
            entries.firstOrNull { it.wireValue == raw } ?: COMPLETED
    }
}

enum class TbtCategory(val wireValue: String, val label: String) {
    TBT("TBT", "Toolbox Talk"),
    ORIENTATION("ORIENTATION", "Orientation"),
    REVIEW_MEET("REVIEW_MEET", "EHS Review Meet");

    companion object {
        fun fromRaw(raw: String?): TbtCategory =
            entries.firstOrNull { it.wireValue == raw } ?: TBT
    }
}

enum class ChecklistAnswer(val wireValue: String, val label: String) {
    PASS("PASS", "Pass"),
    FAIL("FAIL", "Fail"),
    NA("NA", "N/A");

    companion object {
        fun fromRaw(raw: String?): ChecklistAnswer =
            entries.firstOrNull { it.wireValue == raw } ?: PASS
    }
}

data class ChecklistLineItem(
    val prompt: String,
    val answer: ChecklistAnswer = ChecklistAnswer.PASS
) {
    fun encode(): String = "${prompt.replace("|", "/")}|${answer.wireValue}"

    companion object {
        fun decode(encoded: String): ChecklistLineItem {
            val idx = encoded.lastIndexOf('|')
            if (idx <= 0 || idx >= encoded.length - 1) {
                return ChecklistLineItem(prompt = encoded, answer = ChecklistAnswer.PASS)
            }
            val text = encoded.substring(0, idx)
            val ans = ChecklistAnswer.fromRaw(encoded.substring(idx + 1))
            return ChecklistLineItem(prompt = text, answer = ans)
        }
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val roleTitle: String = "EHS Officer",
    val activeSiteName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SafetySite(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val projectCode: String = "",
    val location: String = "",
    val managerName: String = "",
    val status: String = SiteStatus.ACTIVE.wireValue,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun siteStatusEnum(): SiteStatus = SiteStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class SafetyRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val pillar: String = RecordPillar.OBSERVATIONS.wireValue,
    val recordType: String = "Unsafe Condition",
    val title: String = "",
    val description: String = "",
    val siteId: String = "",
    val siteName: String = "",
    val locationArea: String = "",
    val severity: String = SeverityLevel.MEDIUM.wireValue,
    val status: String = RecordStatus.OPEN.wireValue,
    val assignedTo: String = "",
    val capaAction: String = "",
    val rootCause: String = "",
    val dueDateLabel: String = "",
    val closureNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun pillarEnum(): RecordPillar = RecordPillar.fromRaw(pillar)
    fun severityEnum(): SeverityLevel = SeverityLevel.fromRaw(severity)
    fun statusEnum(): RecordStatus = RecordStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class SafetyInspection(
    @DocumentId val id: String = "",
    val userId: String = "",
    val siteId: String = "",
    val siteName: String = "",
    val templateName: String = "",
    val inspectorName: String = "",
    val area: String = "",
    val passedCount: Long = 0L,
    val totalCount: Long = 1L,
    val scorePercent: Long = 0L,
    val status: String = InspectionStatus.COMPLETED.wireValue,
    val checklistItems: List<String> = emptyList(),
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun inspectionStatusEnum(): InspectionStatus = InspectionStatus.fromRaw(status)
    fun decodedItems(): List<ChecklistLineItem> = checklistItems.map { ChecklistLineItem.decode(it) }
}

@Keep
@IgnoreExtraProperties
data class ToolboxTalk(
    @DocumentId val id: String = "",
    val userId: String = "",
    val siteId: String = "",
    val siteName: String = "",
    val topic: String = "",
    val conductorName: String = "",
    val category: String = TbtCategory.TBT.wireValue,
    val attendeeCount: Long = 1L,
    val durationMinutes: Long = 15L,
    val keyTakeaways: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): TbtCategory = TbtCategory.fromRaw(category)
}

data class PillarScoreSummary(
    val pillar: SixSensePillar,
    val scorePercent: Int,
    val totalEntries: Int,
    val openCount: Int,
    val closedCount: Int
)

data class EhsKpiSummary(
    val overallScorePercent: Int = 0,
    val hasAnyData: Boolean = false,
    val pillarScores: List<PillarScoreSummary> = emptyList(),
    val openCriticalCount: Int = 0,
    val totalOpenCount: Int = 0,
    val totalClosedCount: Int = 0,
    val totalInspections: Int = 0,
    val avgAuditScore: Int = 0,
    val totalTbtSessions: Int = 0,
    val totalTbtAttendees: Int = 0
)

object EhsTemplates {
    val recordTypesByPillar: Map<RecordPillar, List<String>> = mapOf(
        RecordPillar.OBSERVATIONS to listOf(
            "Unsafe Condition",
            "Unsafe Act",
            "Near Miss",
            "Positive Observation"
        ),
        RecordPillar.INCIDENTS_CAPA to listOf(
            "First Aid Case",
            "Minor Incident",
            "Near Miss Investigation",
            "Equipment / Property Damage",
            "LTI / Serious Incident"
        ),
        RecordPillar.PROACTIVE to listOf(
            "Permit to Work (PTW)",
            "Management Safety Walk",
            "PPE Compliance Drive",
            "JSA / HIRA Review",
            "LOTO Verification"
        ),
        RecordPillar.ENVIRONMENT to listOf(
            "5S Housekeeping",
            "Spill & Bund Control",
            "Hazardous Waste Disposal",
            "Dust / Air Quality Check",
            "Water & Effluent Check"
        )
    )

    val auditTemplates: Map<String, List<String>> = mapOf(
        "Scaffolding & Height Safety" to listOf(
            "Valid green scaffold tag displayed at access point",
            "Top rail, mid rail, and toe-boards secured on all platforms",
            "Base plates and sole boards resting on firm ground",
            "100% tie-off double lanyard harness worn above 1.8m",
            "Access ladder extends 1m above landing and tied off"
        ),
        "Electrical & LOTO Compliance" to listOf(
            "30mA ELCB / RCCB tested and operational on DBs",
            "Double-insulated industrial cables free of bare joints",
            "Lockout-Tagout (LOTO) padlocks & tags applied before maintenance",
            "Rubber insulating mats placed in front of panels",
            "Earthing continuity verified on welding & portable tools"
        ),
        "Excavation & Confined Space" to listOf(
            "Confined space entry permit & gas test recorded (O2/LEL/H2S)",
            "Trained standby attendant stationed at entry point",
            "Excavation edges barricaded 1.5m clear with warning signage",
            "Shoring / sloping provided for trenches deeper than 1.5m",
            "Safe ladder egress available within every 7.5m of trench"
        ),
        "Fire, Emergency & 5S" to listOf(
            "ABC / CO2 fire extinguishers inspected and unobstructed",
            "Emergency assembly point signage clearly visible",
            "First aid kit stocked and stretcher accessible",
            "Walkways clear of tripping hazards and loose material",
            "Segregated scrap, chemical drums & spill kit available"
        )
    )
}
