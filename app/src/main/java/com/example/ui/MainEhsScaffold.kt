package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.signOutUser
import com.example.data.EhsTemplates
import com.example.data.RecordPillar
import com.example.data.SafetyRecord
import com.example.ui.components.EmptyEhsStateCard
import com.example.ui.screens.AuditRunnerSheet
import com.example.ui.screens.AuditsScreen
import com.example.ui.screens.CreateSafetyRecordSheet
import com.example.ui.screens.CreateSiteSheet
import com.example.ui.screens.CreateToolboxTalkSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditOfficerProfileSheet
import com.example.ui.screens.FieldLogsScreen
import com.example.ui.screens.SafetyRecordDetailSheet
import com.example.ui.screens.TrainingAndSitesScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainEhsScaffold(
    viewModel: EhsViewModel,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val logPillarFilter by viewModel.logPillarFilter.collectAsStateWithLifecycle()
    val logStatusFilter by viewModel.logStatusFilter.collectAsStateWithLifecycle()
    val logSearchQuery by viewModel.logSearchQuery.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Modals state
    var showCreateRecordPillar by remember { mutableStateOf<RecordPillar?>(null) }
    var selectedRecordDetail by remember { mutableStateOf<SafetyRecord?>(null) }
    var showAuditTemplate by remember { mutableStateOf<String?>(null) }
    var showCreateTbt by remember { mutableStateOf(false) }
    var showCreateSite by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Return to Dashboard from secondary tabs on system back press
    BackHandler(enabled = selectedTab != EhsTab.DASHBOARD) {
        viewModel.selectTab(EhsTab.DASHBOARD)
    }

    val navItems = listOf(
        Triple(EhsTab.DASHBOARD, stringResource(R.string.nav_dashboard), Icons.Filled.Dashboard),
        Triple(EhsTab.LOGS, stringResource(R.string.nav_logs), Icons.AutoMirrored.Filled.ListAlt),
        Triple(EhsTab.AUDITS, stringResource(R.string.nav_audits), Icons.AutoMirrored.Filled.Assignment),
        Triple(EhsTab.TRAINING_SITES, stringResource(R.string.nav_training), Icons.Filled.Groups)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 2.dp)
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = stringResource(R.string.tagline_six_senses),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        navItems.forEach { (tab, label, icon) ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                icon = { Icon(imageVector = icon, contentDescription = label) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            EhsTab.AUDITS -> showAuditTemplate = EhsTemplates.auditTemplates.keys.first()
                            EhsTab.TRAINING_SITES -> showCreateTbt = true
                            else -> showCreateRecordPillar = logPillarFilter ?: RecordPillar.OBSERVATIONS
                        }
                    },
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                    text = {
                        Text(
                            when (selectedTab) {
                                EhsTab.AUDITS -> "New Audit"
                                EhsTab.TRAINING_SITES -> "Log TBT"
                                else -> stringResource(R.string.fab_report_safety)
                            }
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_primary_action")
                )
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        navItems.forEach { (tab, label, icon) ->
                            NavigationRailItem(
                                selected = selectedTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                icon = { Icon(imageVector = icon, contentDescription = label) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 860.dp).fillMaxSize()) {
                        when (val state = uiState) {
                            UiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            is UiState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyEhsStateCard(
                                        icon = Icons.Filled.Shield,
                                        title = "Unable to sync EHS workspace",
                                        subtitle = state.message
                                    )
                                }
                            }

                            is UiState.Success -> {
                                val data = state.data
                                when (selectedTab) {
                                    EhsTab.DASHBOARD -> DashboardScreen(
                                        data = data,
                                        onSelectSiteFilter = viewModel::setSiteFilter,
                                        onOpenPillar = viewModel::openPillarWorkspace,
                                        onQuickLogRecord = { showCreateRecordPillar = it },
                                        onOpenAuditModal = {
                                            showAuditTemplate = EhsTemplates.auditTemplates.keys.first()
                                        },
                                        onOpenTbtModal = { showCreateTbt = true },
                                        onOpenRecordDetail = { selectedRecordDetail = it },
                                        onOpenSiteModal = { showCreateSite = true }
                                    )

                                    EhsTab.LOGS -> FieldLogsScreen(
                                        records = data.records,
                                        selectedPillar = logPillarFilter,
                                        selectedStatus = logStatusFilter,
                                        searchQuery = logSearchQuery,
                                        onPillarChange = viewModel::setLogPillarFilter,
                                        onStatusChange = viewModel::setLogStatusFilter,
                                        onSearchChange = viewModel::setLogSearchQuery,
                                        onOpenRecordDetail = { selectedRecordDetail = it },
                                        onCreateNewRecord = { showCreateRecordPillar = it }
                                    )

                                    EhsTab.AUDITS -> AuditsScreen(
                                        inspections = data.inspections,
                                        onStartAuditTemplate = { showAuditTemplate = it },
                                        onDeleteInspection = viewModel::deleteInspection
                                    )

                                    EhsTab.TRAINING_SITES -> TrainingAndSitesScreen(
                                        profile = data.profile,
                                        userEmail = userEmail,
                                        toolboxTalks = data.toolboxTalks,
                                        sites = data.sites,
                                        onAddToolboxTalk = { showCreateTbt = true },
                                        onDeleteToolboxTalk = viewModel::deleteToolboxTalk,
                                        onAddSite = { showCreateSite = true },
                                        onCompleteSite = viewModel::completeSite,
                                        onDeleteSite = viewModel::deleteSite,
                                        onEditProfile = { showEditProfile = true },
                                        onSignOut = {
                                            signOutUser(
                                                credentialManager = credentialManager,
                                                scope = scope
                                            )
                                        }
                                    )
                                }

                                // Modals inside Success scope so live sites & profile are always available
                                showCreateRecordPillar?.let { initialPillar ->
                                    CreateSafetyRecordSheet(
                                        initialPillar = initialPillar,
                                        sites = data.sites,
                                        defaultSiteName = data.activeSiteFilter,
                                        isSubmitting = isSubmitting,
                                        onDismiss = { showCreateRecordPillar = null },
                                        onSubmit = { pillar, type, title, desc, sId, sName, area, sev, assigned, capa, rc, due ->
                                            viewModel.createSafetyRecord(
                                                pillar = pillar,
                                                recordType = type,
                                                title = title,
                                                description = desc,
                                                siteId = sId,
                                                siteName = sName,
                                                locationArea = area,
                                                severity = sev,
                                                assignedTo = assigned,
                                                capaAction = capa,
                                                rootCause = rc,
                                                dueDateLabel = due,
                                                onSuccess = { showCreateRecordPillar = null }
                                            )
                                        }
                                    )
                                }

                                selectedRecordDetail?.let { record ->
                                    // Resolve latest snapshot version of the selected record
                                    val latest = data.records.firstOrNull { it.id == record.id } ?: record
                                    SafetyRecordDetailSheet(
                                        record = latest,
                                        isSubmitting = isSubmitting,
                                        onDismiss = { selectedRecordDetail = null },
                                        onMarkInProgress = { viewModel.moveRecordInProgress(it) },
                                        onCloseRecord = { rec, notes, capa, rc ->
                                            viewModel.closeSafetyRecord(
                                                record = rec,
                                                closureNotes = notes,
                                                capaAction = capa,
                                                rootCause = rc,
                                                onSuccess = { selectedRecordDetail = null }
                                            )
                                        },
                                        onDeleteRecord = { id ->
                                            viewModel.deleteSafetyRecord(id) {
                                                selectedRecordDetail = null
                                            }
                                        }
                                    )
                                }

                                showAuditTemplate?.let { tmpl ->
                                    AuditRunnerSheet(
                                        initialTemplate = tmpl,
                                        sites = data.sites,
                                        defaultSiteName = data.activeSiteFilter,
                                        defaultInspectorName = data.profile?.displayName.orEmpty(),
                                        isSubmitting = isSubmitting,
                                        onDismiss = { showAuditTemplate = null },
                                        onSubmit = { sId, sName, tmplName, inspector, area, items, notes ->
                                            viewModel.createInspection(
                                                siteId = sId,
                                                siteName = sName,
                                                templateName = tmplName,
                                                inspectorName = inspector,
                                                area = area,
                                                items = items,
                                                notes = notes,
                                                onSuccess = { showAuditTemplate = null }
                                            )
                                        }
                                    )
                                }

                                if (showCreateTbt) {
                                    CreateToolboxTalkSheet(
                                        sites = data.sites,
                                        defaultSiteName = data.activeSiteFilter,
                                        defaultConductor = data.profile?.displayName.orEmpty(),
                                        isSubmitting = isSubmitting,
                                        onDismiss = { showCreateTbt = false },
                                        onSubmit = { sId, sName, topic, cond, cat, att, dur, takeaways ->
                                            viewModel.createToolboxTalk(
                                                siteId = sId,
                                                siteName = sName,
                                                topic = topic,
                                                conductorName = cond,
                                                category = cat,
                                                attendeeCount = att,
                                                durationMinutes = dur,
                                                keyTakeaways = takeaways,
                                                onSuccess = { showCreateTbt = false }
                                            )
                                        }
                                    )
                                }

                                if (showCreateSite) {
                                    CreateSiteSheet(
                                        defaultManager = data.profile?.displayName.orEmpty(),
                                        isSubmitting = isSubmitting,
                                        onDismiss = { showCreateSite = false },
                                        onSubmit = { name, code, loc, mgr ->
                                            viewModel.createSite(
                                                name = name,
                                                projectCode = code,
                                                location = loc,
                                                managerName = mgr,
                                                onSuccess = { showCreateSite = false }
                                            )
                                        }
                                    )
                                }

                                if (showEditProfile) {
                                    EditOfficerProfileSheet(
                                        initialName = data.profile?.displayName ?: "EHS Officer",
                                        initialRole = data.profile?.roleTitle ?: "Site Safety Officer",
                                        initialSite = data.profile?.activeSiteName.orEmpty(),
                                        isSubmitting = isSubmitting,
                                        onDismiss = { showEditProfile = false },
                                        onSubmit = { name, role, site ->
                                            viewModel.updateOfficerProfile(
                                                displayName = name,
                                                roleTitle = role,
                                                activeSiteName = site,
                                                onSuccess = { showEditProfile = false }
                                            )
                                        }
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
