package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChecklistLineItem
import com.example.data.EhsKpiSummary
import com.example.data.EhsRepository
import com.example.data.PillarScoreSummary
import com.example.data.RecordPillar
import com.example.data.RecordStatus
import com.example.data.SafetyInspection
import com.example.data.SafetyRecord
import com.example.data.SafetySite
import com.example.data.SeverityLevel
import com.example.data.SixSensePillar
import com.example.data.TbtCategory
import com.example.data.ToolboxTalk
import com.example.data.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class EhsTab {
    DASHBOARD,
    LOGS,
    AUDITS,
    TRAINING_SITES
}

data class EhsDashboardData(
    val profile: UserProfile?,
    val sites: List<SafetySite>,
    val records: List<SafetyRecord>,
    val inspections: List<SafetyInspection>,
    val toolboxTalks: List<ToolboxTalk>,
    val activeSiteFilter: String, // "" = All Sites
    val kpiSummary: EhsKpiSummary
)

class EhsViewModel(
    private val repository: EhsRepository,
    private val currentUserId: String,
    private val initialDisplayName: String
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(EhsTab.DASHBOARD)
    val selectedTab: StateFlow<EhsTab> = _selectedTab.asStateFlow()

    private val _selectedSiteName = MutableStateFlow("")
    val selectedSiteName: StateFlow<String> = _selectedSiteName.asStateFlow()

    private val _logPillarFilter = MutableStateFlow<RecordPillar?>(null)
    val logPillarFilter: StateFlow<RecordPillar?> = _logPillarFilter.asStateFlow()

    private val _logStatusFilter = MutableStateFlow<RecordStatus?>(null)
    val logStatusFilter: StateFlow<RecordStatus?> = _logStatusFilter.asStateFlow()

    private val _logSearchQuery = MutableStateFlow("")
    val logSearchQuery: StateFlow<String> = _logSearchQuery.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            // Ensure profile exists for the authenticated officer
            repository.observeProfile(currentUserId)
                .catch { e ->
                    if (e is CancellationException && e.cause !is FirebaseFirestoreException) throw e
                    Log.w(TAG, "Profile preload check failed", e)
                }
                .collect { profile ->
                    if (profile == null) {
                        repository.upsertProfile(
                            displayName = initialDisplayName.ifBlank { "EHS Officer" },
                            roleTitle = "Site Safety Officer",
                            activeSiteName = ""
                        )
                    } else if (_selectedSiteName.value.isEmpty() && profile.activeSiteName.isNotBlank()) {
                        _selectedSiteName.value = profile.activeSiteName
                    }
                    return@collect
                }
        }
    }

    private val coreStreams = combine(
        repository.observeProfile(currentUserId),
        repository.observeSites(currentUserId),
        repository.observeSafetyRecords(currentUserId),
        repository.observeInspections(currentUserId),
        repository.observeToolboxTalks(currentUserId)
    ) { profile, sites, records, inspections, talks ->
        RawEhsStreams(profile, sites, records, inspections, talks)
    }

    val uiState: StateFlow<UiState<EhsDashboardData>> = combine(
        coreStreams,
        _selectedSiteName
    ) { streams, siteFilter ->
        val filteredRecords = if (siteFilter.isBlank()) {
            streams.records
        } else {
            streams.records.filter { it.siteName.equals(siteFilter, ignoreCase = true) }
        }
        val filteredInspections = if (siteFilter.isBlank()) {
            streams.inspections
        } else {
            streams.inspections.filter { it.siteName.equals(siteFilter, ignoreCase = true) }
        }
        val filteredTalks = if (siteFilter.isBlank()) {
            streams.talks
        } else {
            streams.talks.filter { it.siteName.equals(siteFilter, ignoreCase = true) }
        }

        val kpi = computeSixSensesKpi(
            records = filteredRecords,
            inspections = filteredInspections,
            talks = filteredTalks
        )

        UiState.Success(
            EhsDashboardData(
                profile = streams.profile,
                sites = streams.sites.sortedByDescending { it.createdAt ?: Timestamp(0, 0) },
                records = filteredRecords.sortedByDescending { it.createdAt ?: Timestamp(0, 0) },
                inspections = filteredInspections.sortedByDescending { it.createdAt ?: Timestamp(0, 0) },
                toolboxTalks = filteredTalks.sortedByDescending { it.createdAt ?: Timestamp(0, 0) },
                activeSiteFilter = siteFilter,
                kpiSummary = kpi
            )
        ) as UiState<EhsDashboardData>
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing EHS dashboard streams", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to sync EHS safety data"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
    )

    fun selectTab(tab: EhsTab) {
        _selectedTab.value = tab
    }

    fun openPillarWorkspace(pillar: SixSensePillar) {
        when (pillar) {
            SixSensePillar.IMS_AUDITS -> _selectedTab.value = EhsTab.AUDITS
            SixSensePillar.TRAINING_TBT -> _selectedTab.value = EhsTab.TRAINING_SITES
            SixSensePillar.INCIDENTS_CAPA -> {
                _logPillarFilter.value = RecordPillar.INCIDENTS_CAPA
                _selectedTab.value = EhsTab.LOGS
            }
            SixSensePillar.OBSERVATIONS -> {
                _logPillarFilter.value = RecordPillar.OBSERVATIONS
                _selectedTab.value = EhsTab.LOGS
            }
            SixSensePillar.PROACTIVE -> {
                _logPillarFilter.value = RecordPillar.PROACTIVE
                _selectedTab.value = EhsTab.LOGS
            }
            SixSensePillar.ENVIRONMENT -> {
                _logPillarFilter.value = RecordPillar.ENVIRONMENT
                _selectedTab.value = EhsTab.LOGS
            }
        }
    }

    fun setSiteFilter(siteName: String) {
        _selectedSiteName.value = siteName
    }

    fun setLogPillarFilter(pillar: RecordPillar?) {
        _logPillarFilter.value = pillar
    }

    fun setLogStatusFilter(status: RecordStatus?) {
        _logStatusFilter.value = status
    }

    fun setLogSearchQuery(query: String) {
        _logSearchQuery.value = query
    }

    fun createSafetyRecord(
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
        dueDateLabel: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.createSafetyRecord(
                pillar = pillar,
                recordType = recordType,
                title = title,
                description = description,
                siteId = siteId,
                siteName = siteName,
                locationArea = locationArea,
                severity = severity,
                assignedTo = assignedTo,
                capaAction = capaAction,
                rootCause = rootCause,
                dueDateLabel = dueDateLabel
            )
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    _messages.tryEmit("${pillar.displayLabel} logged to cloud")
                    onSuccess()
                },
                onFailure = { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Failed to save record")
                }
            )
        }
    }

    fun moveRecordInProgress(record: SafetyRecord) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.updateSafetyRecordProgress(
                recordId = record.id,
                status = RecordStatus.IN_PROGRESS,
                assignedTo = record.assignedTo,
                capaAction = record.capaAction,
                rootCause = record.rootCause,
                dueDateLabel = record.dueDateLabel,
                severity = record.severityEnum(),
                description = record.description
            )
            _isSubmitting.value = false
            result.fold(
                onSuccess = { _messages.tryEmit("Status updated to In Progress") },
                onFailure = { _messages.tryEmit(it.localizedMessage ?: "Update failed") }
            )
        }
    }

    fun closeSafetyRecord(
        record: SafetyRecord,
        closureNotes: String,
        capaAction: String,
        rootCause: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.closeSafetyRecord(
                recordId = record.id,
                closureNotes = closureNotes,
                capaAction = capaAction,
                rootCause = rootCause
            )
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    _messages.tryEmit("CAPA verified & record closed")
                    onSuccess()
                },
                onFailure = { _messages.tryEmit(it.localizedMessage ?: "Failed to close record") }
            )
        }
    }

    fun deleteSafetyRecord(recordId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteSafetyRecord(recordId)
            result.fold(
                onSuccess = {
                    _messages.tryEmit("Safety record removed")
                    onSuccess()
                },
                onFailure = { _messages.tryEmit(it.localizedMessage ?: "Delete failed") }
            )
        }
    }

    fun createInspection(
        siteId: String,
        siteName: String,
        templateName: String,
        inspectorName: String,
        area: String,
        items: List<ChecklistLineItem>,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.createInspection(
                siteId = siteId,
                siteName = siteName,
                templateName = templateName,
                inspectorName = inspectorName,
                area = area,
                items = items,
                notes = notes
            )
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    _messages.tryEmit("IMS Audit saved")
                    onSuccess()
                },
                onFailure = { _messages.tryEmit(it.localizedMessage ?: "Failed to save audit") }
            )
        }
    }

    fun deleteInspection(inspectionId: String) {
        viewModelScope.launch {
            repository.deleteInspection(inspectionId).fold(
                onSuccess = { _messages.tryEmit("Audit deleted") },
                onFailure = { _messages.tryEmit(it.localizedMessage ?: "Delete failed") }
            )
        }
    }

    fun createToolboxTalk(
        siteId: String,
        siteName: String,
        topic: String,
        conductorName: String,
        category: TbtCategory,
        attendeeCount: Int,
        durationMinutes: Int,
        keyTakeaways: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.createToolboxTalk(
                siteId = siteId,
                siteName = siteName,
                topic = topic,
                conductorName = conductorName,
                category = category,
                attendeeCount = attendeeCount,
                durationMinutes = durationMinutes,
                keyTakeaways = keyTakeaways
            )
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    _messages.tryEmit("${category.label} logged")
                    onSuccess()
                },
                onFailure = { _messages.tryEmit(it.localizedMessage ?: "Failed to save session") }
            )
        }
    }

    fun deleteToolboxTalk(tbtId: String) {
        viewModelScope.launch {
            repository.deleteToolboxTalk(tbtId).fold(
                onSuccess = { _messages.tryEmit("Session deleted") },
                onFailure = { _messages.tryEmit(it.localizedMessage ?: "Delete failed") }
            )
        }
    }

    fun createSite(
        name: String,
        projectCode: String,
        location: String,
        managerName: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.createSite(name, projectCode, location, managerName)
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    _messages.tryEmit("Project site added")
                    onSuccess()
                },
                onFailure = { _messages.tryEmit(it.localizedMessage ?: "Failed to add site") }
            )
        }
    }

    fun completeSite(siteId: String) {
        viewModelScope.launch {
            repository.completeSite(siteId).fold(
                onSuccess = { _messages.tryEmit("Site marked completed") },
                onFailure = { _messages.tryEmit(it.localizedMessage ?: "Update failed") }
            )
        }
    }

    fun deleteSite(siteId: String) {
        viewModelScope.launch {
            repository.deleteSite(siteId).fold(
                onSuccess = { _messages.tryEmit("Site removed") },
                onFailure = { _messages.tryEmit(it.localizedMessage ?: "Delete failed") }
            )
        }
    }

    fun updateOfficerProfile(displayName: String, roleTitle: String, activeSiteName: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.upsertProfile(displayName, roleTitle, activeSiteName).fold(
                onSuccess = {
                    _isSubmitting.value = false
                    _messages.tryEmit("Officer profile updated")
                    onSuccess()
                },
                onFailure = {
                    _isSubmitting.value = false
                    _messages.tryEmit(it.localizedMessage ?: "Update failed")
                }
            )
        }
    }

    private fun computeSixSensesKpi(
        records: List<SafetyRecord>,
        inspections: List<SafetyInspection>,
        talks: List<ToolboxTalk>
    ): EhsKpiSummary {
        val hasAny = records.isNotEmpty() || inspections.isNotEmpty() || talks.isNotEmpty()

        fun scoreForRecordPillar(pillar: RecordPillar, sense: SixSensePillar): PillarScoreSummary {
            val list = records.filter { it.pillarEnum() == pillar }
            val closed = list.count { it.statusEnum() == RecordStatus.CLOSED }
            val open = list.size - closed
            val score = if (list.isEmpty()) {
                0
            } else {
                val baseClosure = (closed * 100.0) / list.size
                val criticalPenalty = list.count {
                    it.statusEnum() != RecordStatus.CLOSED && it.severityEnum() == SeverityLevel.CRITICAL
                } * 12
                // Proactive & Environment reward logging positive/preventive checks too
                val floor = if (pillar == RecordPillar.PROACTIVE || pillar == RecordPillar.ENVIRONMENT) 65 else 25
                (baseClosure.coerceAtLeast(floor.toDouble()) - criticalPenalty).toInt().coerceIn(0, 100)
            }
            return PillarScoreSummary(sense, score, list.size, open, closed)
        }

        val imsScore = if (inspections.isEmpty()) 0 else inspections.map { it.scorePercent.toInt() }.average().toInt().coerceIn(0, 100)
        val imsSummary = PillarScoreSummary(
            pillar = SixSensePillar.IMS_AUDITS,
            scorePercent = imsScore,
            totalEntries = inspections.size,
            openCount = inspections.count { it.scorePercent < 80 },
            closedCount = inspections.count { it.scorePercent >= 80 }
        )

        val tbtScore = if (talks.isEmpty()) 0 else (70 + (talks.size.coerceAtMost(6) * 5)).coerceIn(0, 100)
        val tbtSummary = PillarScoreSummary(
            pillar = SixSensePillar.TRAINING_TBT,
            scorePercent = tbtScore,
            totalEntries = talks.size,
            openCount = 0,
            closedCount = talks.size
        )

        val incSummary = scoreForRecordPillar(RecordPillar.INCIDENTS_CAPA, SixSensePillar.INCIDENTS_CAPA)
        val obsSummary = scoreForRecordPillar(RecordPillar.OBSERVATIONS, SixSensePillar.OBSERVATIONS)
        val proSummary = scoreForRecordPillar(RecordPillar.PROACTIVE, SixSensePillar.PROACTIVE)
        val envSummary = scoreForRecordPillar(RecordPillar.ENVIRONMENT, SixSensePillar.ENVIRONMENT)

        val allPillars = listOf(imsSummary, tbtSummary, incSummary, obsSummary, proSummary, envSummary)
        val activePillars = allPillars.filter { it.totalEntries > 0 }
        val overall = if (activePillars.isEmpty()) 0 else activePillars.map { it.scorePercent }.average().toInt()

        return EhsKpiSummary(
            overallScorePercent = overall,
            hasAnyData = hasAny,
            pillarScores = allPillars,
            openCriticalCount = records.count {
                it.statusEnum() != RecordStatus.CLOSED && it.severityEnum() == SeverityLevel.CRITICAL
            },
            totalOpenCount = records.count { it.statusEnum() != RecordStatus.CLOSED },
            totalClosedCount = records.count { it.statusEnum() == RecordStatus.CLOSED },
            totalInspections = inspections.size,
            avgAuditScore = imsScore,
            totalTbtSessions = talks.size,
            totalTbtAttendees = talks.sumOf { it.attendeeCount.toInt() }
        )
    }

    private data class RawEhsStreams(
        val profile: UserProfile?,
        val sites: List<SafetySite>,
        val records: List<SafetyRecord>,
        val inspections: List<SafetyInspection>,
        val talks: List<ToolboxTalk>
    )

    private companion object {
        const val TAG = "EhsViewModel"
    }
}
