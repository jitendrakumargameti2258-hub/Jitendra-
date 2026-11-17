package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class EhsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            userDoc(userId).snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertProfile(
        displayName: String,
        roleTitle: String,
        activeSiteName: String
    ): Result<Unit> {
        val cleanName = displayName.trim().take(80).ifBlank { "EHS Officer" }
        val cleanRole = roleTitle.trim().take(60).ifBlank { "EHS Officer" }
        val cleanSite = activeSiteName.trim().take(100)
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = userDoc(uid)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "roleTitle" to cleanRole,
                        "activeSiteName" to cleanSite,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "roleTitle" to cleanRole,
                        "activeSiteName" to cleanSite,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeSites(userId: String): Flow<List<SafetySite>> = flow {
        val path = "users/$userId/sites"
        emitAll(
            userDoc(userId).collection("sites")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SafetySite::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, userId, path)
                }
        )
    }

    suspend fun createSite(
        name: String,
        projectCode: String,
        location: String,
        managerName: String
    ): Result<Unit> {
        val cleanName = name.trim()
        val cleanCode = projectCode.trim()
        val cleanLoc = location.trim()
        val cleanMgr = managerName.trim()
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/sites"
        return try {
            require(cleanName.length in 2..100) { "Site name must be 2-100 characters." }
            require(cleanCode.length in 2..40) { "Project code must be 2-40 characters." }
            require(cleanLoc.length in 2..120) { "Location must be 2-120 characters." }
            require(cleanMgr.length in 1..80) { "Manager name must be 1-80 characters." }
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("sites").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "projectCode" to cleanCode,
                    "location" to cleanLoc,
                    "managerName" to cleanMgr,
                    "status" to SiteStatus.ACTIVE.wireValue,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun completeSite(siteId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/sites/$siteId"
        return try {
            require(siteId.isNotBlank()) { "Site ID is required." }
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("sites").document(siteId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to SiteStatus.COMPLETED.wireValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSite(siteId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/sites/$siteId"
        return try {
            require(siteId.isNotBlank()) { "Site ID is required." }
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("sites").document(siteId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observeSafetyRecords(userId: String): Flow<List<SafetyRecord>> = flow {
        val path = "users/$userId/safety_records"
        emitAll(
            userDoc(userId).collection("safety_records")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SafetyRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, userId, path)
                }
        )
    }

    suspend fun createSafetyRecord(
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
    ): Result<Unit> {
        val cleanType = recordType.trim()
        val cleanTitle = title.trim()
        val cleanDesc = description.trim().take(1000)
        val cleanSiteId = siteId.trim()
        val cleanSiteName = siteName.trim().take(100).ifBlank { "General Site" }
        val cleanArea = locationArea.trim().take(100)
        val cleanAssigned = assignedTo.trim().take(80)
        val cleanCapa = capaAction.trim().take(600)
        val cleanRootCause = rootCause.trim().take(600)
        val cleanDue = dueDateLabel.trim().take(40)

        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/safety_records"
        return try {
            require(cleanType.length in 2..50) { "Record type must be 2-50 characters." }
            require(cleanTitle.length in 3..120) { "Title must be 3-120 characters." }
            require(cleanArea.length in 1..100) { "Location/Zone is required (1-100 chars)." }
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("safety_records").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "pillar" to pillar.wireValue,
                    "recordType" to cleanType,
                    "title" to cleanTitle,
                    "description" to cleanDesc,
                    "siteId" to cleanSiteId,
                    "siteName" to cleanSiteName,
                    "locationArea" to cleanArea,
                    "severity" to severity.wireValue,
                    "status" to RecordStatus.OPEN.wireValue,
                    "assignedTo" to cleanAssigned,
                    "capaAction" to cleanCapa,
                    "rootCause" to cleanRootCause,
                    "dueDateLabel" to cleanDue,
                    "closureNotes" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSafetyRecordProgress(
        recordId: String,
        status: RecordStatus,
        assignedTo: String,
        capaAction: String,
        rootCause: String,
        dueDateLabel: String,
        severity: SeverityLevel,
        description: String
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/safety_records/$recordId"
        return try {
            require(recordId.isNotBlank()) { "Record ID is required." }
            require(status == RecordStatus.OPEN || status == RecordStatus.IN_PROGRESS) {
                "Use closeSafetyRecord to close a safety record."
            }
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("safety_records").document(recordId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to status.wireValue,
                    "assignedTo" to assignedTo.trim().take(80),
                    "capaAction" to capaAction.trim().take(600),
                    "rootCause" to rootCause.trim().take(600),
                    "dueDateLabel" to dueDateLabel.trim().take(40),
                    "severity" to severity.wireValue,
                    "description" to description.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun closeSafetyRecord(
        recordId: String,
        closureNotes: String,
        capaAction: String,
        rootCause: String
    ): Result<Unit> {
        val cleanClosure = closureNotes.trim().take(600)
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/safety_records/$recordId"
        return try {
            require(recordId.isNotBlank()) { "Record ID is required." }
            require(cleanClosure.length >= 3) { "Closure verification notes must be at least 3 characters." }
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("safety_records").document(recordId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to RecordStatus.CLOSED.wireValue,
                    "closureNotes" to cleanClosure,
                    "capaAction" to capaAction.trim().take(600),
                    "rootCause" to rootCause.trim().take(600),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSafetyRecord(recordId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/safety_records/$recordId"
        return try {
            require(recordId.isNotBlank()) { "Record ID is required." }
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("safety_records").document(recordId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observeInspections(userId: String): Flow<List<SafetyInspection>> = flow {
        val path = "users/$userId/inspections"
        emitAll(
            userDoc(userId).collection("inspections")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SafetyInspection::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, userId, path)
                }
        )
    }

    suspend fun createInspection(
        siteId: String,
        siteName: String,
        templateName: String,
        inspectorName: String,
        area: String,
        items: List<ChecklistLineItem>,
        notes: String
    ): Result<Unit> {
        val cleanTemplate = templateName.trim().take(100)
        val cleanInspector = inspectorName.trim().take(80).ifBlank { "EHS Auditor" }
        val cleanArea = area.trim().take(100)
        val cleanSiteName = siteName.trim().take(100).ifBlank { "General Site" }
        val cleanNotes = notes.trim().take(800)
        val encoded = items.take(30).map { it.encode() }
        val applicable = items.take(30).filter { it.answer != ChecklistAnswer.NA }
        val passedCount = applicable.count { it.answer == ChecklistAnswer.PASS }
        val totalCount = encoded.size.coerceAtLeast(1)
        val scorePercent = if (applicable.isEmpty()) 100 else ((passedCount * 100.0) / applicable.size).toInt().coerceIn(0, 100)

        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/inspections"
        return try {
            require(cleanTemplate.length in 2..100) { "Audit template name is required." }
            require(cleanArea.length in 1..100) { "Audit area/zone is required." }
            require(encoded.isNotEmpty()) { "At least one checklist item is required." }
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("inspections").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "siteId" to siteId.trim(),
                    "siteName" to cleanSiteName,
                    "templateName" to cleanTemplate,
                    "inspectorName" to cleanInspector,
                    "area" to cleanArea,
                    "passedCount" to passedCount.toLong(),
                    "totalCount" to totalCount.toLong(),
                    "scorePercent" to scorePercent.toLong(),
                    "status" to InspectionStatus.COMPLETED.wireValue,
                    "checklistItems" to encoded,
                    "notes" to cleanNotes,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteInspection(inspectionId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/inspections/$inspectionId"
        return try {
            require(inspectionId.isNotBlank()) { "Inspection ID is required." }
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("inspections").document(inspectionId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observeToolboxTalks(userId: String): Flow<List<ToolboxTalk>> = flow {
        val path = "users/$userId/toolbox_talks"
        emitAll(
            userDoc(userId).collection("toolbox_talks")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ToolboxTalk::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, userId, path)
                }
        )
    }

    suspend fun createToolboxTalk(
        siteId: String,
        siteName: String,
        topic: String,
        conductorName: String,
        category: TbtCategory,
        attendeeCount: Int,
        durationMinutes: Int,
        keyTakeaways: String
    ): Result<Unit> {
        val cleanTopic = topic.trim().take(120)
        val cleanConductor = conductorName.trim().take(80).ifBlank { "Safety Officer" }
        val cleanSiteName = siteName.trim().take(100).ifBlank { "General Site" }
        val cleanTakeaways = keyTakeaways.trim().take(800)

        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/toolbox_talks"
        return try {
            require(cleanTopic.length in 3..120) { "Topic must be 3-120 characters." }
            require(attendeeCount in 1..1000) { "Attendee count must be 1-1000." }
            require(durationMinutes in 5..480) { "Duration must be 5-480 minutes." }
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("toolbox_talks").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "siteId" to siteId.trim(),
                    "siteName" to cleanSiteName,
                    "topic" to cleanTopic,
                    "conductorName" to cleanConductor,
                    "category" to category.wireValue,
                    "attendeeCount" to attendeeCount.toLong(),
                    "durationMinutes" to durationMinutes.toLong(),
                    "keyTakeaways" to cleanTakeaways,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteToolboxTalk(tbtId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/toolbox_talks/$tbtId"
        return try {
            require(tbtId.isNotBlank()) { "Toolbox talk ID is required." }
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("toolbox_talks").document(tbtId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun handleFlowError(error: Throwable, userId: String, path: String): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
    }
}
