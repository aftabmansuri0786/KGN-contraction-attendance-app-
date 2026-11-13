package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AdvanceEntity
import com.example.data.model.AmavasyaConfigEntity
import com.example.data.model.AmavasyaSettlementEntity
import com.example.data.model.AttendanceEntity
import com.example.data.model.AttendanceStatus
import com.example.data.model.PaymentEntity
import com.example.data.model.SettlementAuditEntity
import com.example.data.model.SiteEntity
import com.example.data.model.WorkerEntity
import android.content.Context
import com.example.R
import com.example.data.model.MonthlyPdfEntity
import com.example.data.repository.FirebaseStorageHelper
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.KgnRepository
import com.example.util.AmavasyaCalendar
import com.example.util.AmavasyaDashboardSummary
import com.example.util.AmavasyaHisabCalculator
import com.example.util.AmavasyaPdfHelper
import com.example.util.AppLanguage
import com.example.util.AttendanceReminderManager
import com.example.util.AttendanceReminderPrefs
import com.example.util.AttendanceReminderSettings
import com.example.util.Formatters
import com.example.util.GoogleAuthHelper
import com.example.util.WorkerHisabStatement
import com.example.util.WorkerPhotoHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class DashboardMetrics(
    val totalActiveWorkers: Int = 0,
    val presentToday: Int = 0,
    val absentToday: Int = 0,
    val halfDayToday: Int = 0,
    val activeSitesCount: Int = 0,
    val todayLabourCost: Double = 0.0,
    val monthEarnedWages: Double = 0.0,
    val totalPendingAdvances: Double = 0.0,
    val totalUnpaidWages: Double = 0.0
)

class KgnViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = KgnRepository(db)

    private val firestore: FirebaseFirestore? = try {
        val databaseId = application.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
    } catch (e: Exception) {
        null
    }

    private val syncRepository = if (firestore != null) FirestoreSyncRepository(firestore, db) else null

    // Google Auth & Cloud Sync
    private val _currentUser = MutableStateFlow<FirebaseUser?>(GoogleAuthHelper.getCurrentUser())
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        _currentUser.value = user
        if (user != null) {
            syncCloudData()
        }
    }

    init {
        GoogleAuthHelper.getAuth().addAuthStateListener(authListener)
        if (_currentUser.value != null) {
            syncCloudData()
        }
        viewModelScope.launch {
            val settings = AttendanceReminderPrefs.getSettingsSync(application)
            AttendanceReminderManager.scheduleReminder(application, settings)
        }
    }

    override fun onCleared() {
        super.onCleared()
        GoogleAuthHelper.getAuth().removeAuthStateListener(authListener)
    }

    // Language & Theme State
    private val _language = MutableStateFlow(AppLanguage.HINDI)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Owner Security
    private val _isOwnerUnlocked = MutableStateFlow(true)
    val isOwnerUnlocked: StateFlow<Boolean> = _isOwnerUnlocked.asStateFlow()

    private val _ownerPin = MutableStateFlow("1234")
    val ownerPin: StateFlow<String> = _ownerPin.asStateFlow()

    // Filters
    private val _selectedDate = MutableStateFlow(Formatters.getTodayIsoDate())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedSiteId = MutableStateFlow("")
    val selectedSiteId: StateFlow<String> = _selectedSiteId.asStateFlow()

    // Data streams
    val allWorkers: StateFlow<List<WorkerEntity>> = repository.allWorkers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeWorkers: StateFlow<List<WorkerEntity>> = repository.activeWorkers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSites: StateFlow<List<SiteEntity>> = repository.allSites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSites: StateFlow<List<SiteEntity>> = repository.activeSites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendance: StateFlow<List<AttendanceEntity>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAdvances: StateFlow<List<AdvanceEntity>> = repository.allAdvances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<PaymentEntity>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val amavasyaConfig: StateFlow<AmavasyaConfigEntity?> = repository.amavasyaConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSettlements: StateFlow<List<AmavasyaSettlementEntity>> = repository.allSettlements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSettlementAudits: StateFlow<List<SettlementAuditEntity>> = repository.allSettlementAudits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMonthlyPdfs: StateFlow<List<MonthlyPdfEntity>> = repository.allMonthlyPdfs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGeneratingPdfs = MutableStateFlow(false)
    val isGeneratingPdfs: StateFlow<Boolean> = _isGeneratingPdfs.asStateFlow()

    val reminderSettings: StateFlow<AttendanceReminderSettings> =
        AttendanceReminderPrefs.observeSettings(application)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AttendanceReminderSettings())

    private val defaultNextAmavasyaIso = AmavasyaCalendar.getNextAmavasyaDate().isoDate

    val selectedAmavasyaDate: StateFlow<String> = amavasyaConfig
        .combine(MutableStateFlow(defaultNextAmavasyaIso)) { config, fallback ->
            val saved = config?.selectedSettlementDate.orEmpty()
            if (saved.isNotBlank()) saved else fallback
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultNextAmavasyaIso)

    // Worker-wise Monthly Hisab Statements (reactive to live attendance/advances/payments & locked settlements)
    val hisabStatements: StateFlow<List<WorkerHisabStatement>> = combine(
        allWorkers,
        allAttendance,
        allAdvances,
        allPayments,
        allSettlements,
        allSettlementAudits,
        selectedAmavasyaDate
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val workersList = values[0] as List<WorkerEntity>
        @Suppress("UNCHECKED_CAST")
        val attList = values[1] as List<AttendanceEntity>
        @Suppress("UNCHECKED_CAST")
        val advList = values[2] as List<AdvanceEntity>
        @Suppress("UNCHECKED_CAST")
        val payList = values[3] as List<PaymentEntity>
        @Suppress("UNCHECKED_CAST")
        val settleList = values[4] as List<AmavasyaSettlementEntity>
        @Suppress("UNCHECKED_CAST")
        val auditList = values[5] as List<SettlementAuditEntity>
        val targetDate = values[6] as String

        AmavasyaHisabCalculator.calculateWorkerStatements(
            workers = workersList,
            attendanceRecords = attList,
            advances = advList,
            payments = payList,
            settlements = settleList,
            audits = auditList,
            selectedAmavasyaDate = targetDate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val amavasyaDashboardSummary: StateFlow<AmavasyaDashboardSummary> = combine(
        selectedAmavasyaDate,
        amavasyaConfig,
        hisabStatements
    ) { targetDate, config, statements ->
        AmavasyaHisabCalculator.buildDashboardSummary(
            selectedAmavasyaDate = targetDate,
            isManualDate = config?.isManualDate ?: false,
            reminderDaysBefore = config?.reminderDaysBefore ?: 3,
            statements = statements
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AmavasyaHisabCalculator.buildDashboardSummary(
            selectedAmavasyaDate = defaultNextAmavasyaIso,
            isManualDate = false,
            reminderDaysBefore = 3,
            statements = emptyList()
        )
    )

    // Dashboard Metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        activeWorkers,
        activeSites,
        allAttendance,
        allAdvances,
        allPayments
    ) { workers, sites, attendance, advances, payments ->
        val today = _selectedDate.value
        val currentMonthPrefix = today.substring(0, 7) // "YYYY-MM"

        val todayAttendance = attendance.filter { it.date == today }
        val presentCount = todayAttendance.count { it.status == AttendanceStatus.PRESENT.name }
        val halfDayCount = todayAttendance.count { it.status == AttendanceStatus.HALF_DAY.name }
        val absentCount = todayAttendance.count { it.status == AttendanceStatus.ABSENT.name }

        val todayCost = todayAttendance.sumOf { it.calculatedEarnings }
        val monthWages = attendance
            .filter { it.date.startsWith(currentMonthPrefix) }
            .sumOf { it.calculatedEarnings }

        val totalAdvancesGiven = advances.sumOf { it.amount }
        val totalAdvancesSettled = advances.sumOf { it.settledAmount }
        val pendingAdvances = (totalAdvancesGiven - totalAdvancesSettled).coerceAtLeast(0.0)

        val totalAllEarnings = attendance.sumOf { it.calculatedEarnings }
        val totalAllPayments = payments.sumOf { it.amount }
        val unpaidWages = (totalAllEarnings - totalAllPayments).coerceAtLeast(0.0)

        DashboardMetrics(
            totalActiveWorkers = workers.size,
            presentToday = presentCount,
            absentToday = absentCount,
            halfDayToday = halfDayCount,
            activeSitesCount = sites.size,
            todayLabourCost = todayCost,
            monthEarnedWages = monthWages,
            totalPendingAdvances = pendingAdvances,
            totalUnpaidWages = unpaidWages
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun toggleLanguage() {
        _language.value = if (_language.value == AppLanguage.HINDI) AppLanguage.ENGLISH else AppLanguage.HINDI
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun unlockOwner(pin: String): Boolean {
        if (pin == _ownerPin.value) {
            _isOwnerUnlocked.value = true
            return true
        }
        return false
    }

    fun lockOwner() {
        _isOwnerUnlocked.value = false
    }

    fun changeOwnerPin(newPin: String) {
        if (newPin.length == 4) {
            _ownerPin.value = newPin
        }
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun setSelectedSite(siteId: String) {
        _selectedSiteId.value = siteId
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            AttendanceReminderPrefs.setEnabled(app, enabled)
            val updated = AttendanceReminderPrefs.getSettingsSync(app)
            AttendanceReminderManager.scheduleReminder(app, updated)
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            AttendanceReminderPrefs.setTime(app, hour, minute)
            val updated = AttendanceReminderPrefs.getSettingsSync(app)
            AttendanceReminderManager.scheduleReminder(app, updated)
        }
    }

    private suspend fun cancelTodayReminderIfAllMarked() {
        val app = getApplication<Application>()
        val todayIso = Formatters.getTodayIsoDate()
        val active = db.workerDao().getActiveWorkersSync()
        val todayAtt = db.attendanceDao().getAttendanceForDateSync(todayIso)
        AttendanceReminderManager.cancelNotificationIfAllMarked(app, active, todayAtt)
    }

    fun markAllPresent(siteId: String, date: String) {
        viewModelScope.launch {
            val workers = activeWorkers.value.filter {
                siteId.isBlank() || it.siteId == siteId || it.siteId.isBlank()
            }
            val records = workers.map { worker ->
                val wageRate = worker.dailyWage
                val otRate = worker.overtimeHourlyRate
                AttendanceEntity(
                    id = "att_${worker.id}_${date}_${if (siteId.isNotBlank()) siteId else worker.siteId}",
                    workerId = worker.id,
                    siteId = if (siteId.isNotBlank()) siteId else worker.siteId,
                    date = date,
                    status = AttendanceStatus.PRESENT.name,
                    paidDayFraction = 1.0,
                    dailyWageSnapshot = wageRate,
                    overtimeHours = 0.0,
                    overtimeRateSnapshot = otRate,
                    calculatedEarnings = wageRate,
                    remarks = "All Present",
                    updatedAt = System.currentTimeMillis()
                )
            }
            repository.saveAttendanceBatch(records)
            val uid = _currentUser.value?.uid
            if (uid != null) {
                records.forEach { rec ->
                    syncRepository?.uploadAttendance(uid, rec)
                }
            }
            if (date == Formatters.getTodayIsoDate()) {
                cancelTodayReminderIfAllMarked()
            }
        }
    }

    fun syncCloudData() {
        val user = _currentUser.value ?: return
        val syncRepo = syncRepository ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            val result = syncRepo.syncAll(user.uid)
            if (result.isSuccess) {
                _lastSyncTimestamp.value = System.currentTimeMillis()
                syncMediaFilesWithStorage(user.uid)
            }
            _isSyncing.value = false
        }
    }

    private suspend fun syncMediaFilesWithStorage(userId: String) {
        val app = getApplication<Application>()
        val workers = db.workerDao().getAllWorkers().first()
        for (worker in workers) {
            val localFile = if (worker.localPhotoPath.isNotBlank()) File(worker.localPhotoPath) else WorkerPhotoHelper.getWorkerPhotoFile(app, worker.id)
            if (localFile.exists() && worker.photoUrl.isBlank()) {
                val uploaded = FirebaseStorageHelper.uploadWorkerPhoto(userId, worker.id, localFile)
                if (uploaded != null) {
                    val updated = worker.copy(
                        localPhotoPath = localFile.absolutePath,
                        photoStoragePath = uploaded.storagePath,
                        photoUrl = uploaded.downloadUrl
                    )
                    repository.saveWorker(updated)
                    syncRepository?.uploadWorker(userId, updated)
                }
            } else if (!localFile.exists() && worker.photoStoragePath.isNotBlank()) {
                val dest = WorkerPhotoHelper.getWorkerPhotoFile(app, worker.id)
                val downloaded = FirebaseStorageHelper.downloadFileToLocal(worker.photoStoragePath, dest)
                if (downloaded) {
                    repository.saveWorker(worker.copy(localPhotoPath = dest.absolutePath))
                }
            }
        }

        val pdfs = db.monthlyPdfDao().getAllMonthlyPdfsSync()
        for (pdf in pdfs) {
            val localPdf = if (pdf.localFilePath.isNotBlank()) File(pdf.localFilePath) else null
            if (localPdf != null && localPdf.exists() && pdf.downloadUrl.isBlank()) {
                val uploaded = FirebaseStorageHelper.uploadMonthlyPdf(userId, pdf.year, pdf.month, localPdf.name, localPdf)
                if (uploaded != null) {
                    val updated = pdf.copy(
                        storagePath = uploaded.storagePath,
                        downloadUrl = uploaded.downloadUrl,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.saveMonthlyPdf(updated)
                    syncRepository?.uploadMonthlyPdf(userId, updated)
                }
            }
        }
    }

    fun signInWithGoogle(context: Context, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = GoogleAuthHelper.signInWithGoogle(context)
            if (result.isSuccess) {
                val user = result.getOrNull()
                _currentUser.value = user
                syncCloudData()
                onResult(true, null)
            } else {
                _isSyncing.value = false
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Sign in failed"
                onResult(false, errorMsg)
            }
        }
    }

    fun signOutGoogle() {
        GoogleAuthHelper.signOut()
        _currentUser.value = null
    }

    fun saveAttendance(
        workerId: String,
        siteId: String,
        date: String,
        status: AttendanceStatus,
        overtimeHours: Double,
        remarks: String = ""
    ) {
        viewModelScope.launch {
            repository.saveAttendanceRecord(
                workerId = workerId,
                siteId = siteId,
                date = date,
                status = status.name,
                fraction = status.fraction,
                overtimeHours = overtimeHours,
                remarks = remarks
            )
            val uid = _currentUser.value?.uid
            if (uid != null) {
                val wageRate = allWorkers.value.find { it.id == workerId }?.dailyWage ?: 0.0
                val otRate = allWorkers.value.find { it.id == workerId }?.overtimeHourlyRate ?: if (wageRate > 0) (wageRate / 8.0) else 0.0
                val earnings = (status.fraction * wageRate) + (overtimeHours * otRate)
                val att = AttendanceEntity(
                    id = "att_${workerId}_${date}_$siteId",
                    workerId = workerId,
                    siteId = siteId,
                    date = date,
                    status = status.name,
                    paidDayFraction = status.fraction,
                    dailyWageSnapshot = wageRate,
                    overtimeHours = overtimeHours,
                    overtimeRateSnapshot = otRate,
                    calculatedEarnings = earnings,
                    remarks = remarks,
                    updatedAt = System.currentTimeMillis()
                )
                syncRepository?.uploadAttendance(uid, att)
            }
            if (date == Formatters.getTodayIsoDate()) {
                cancelTodayReminderIfAllMarked()
            }
        }
    }

    fun saveWorker(worker: WorkerEntity) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            var resolvedWorker = worker
            if (worker.localPhotoPath.isNotBlank()) {
                val finalFile = WorkerPhotoHelper.copyToFinalWorkerPhoto(app, worker.localPhotoPath, worker.id)
                if (finalFile != null && finalFile.exists()) {
                    resolvedWorker = worker.copy(localPhotoPath = finalFile.absolutePath)
                }
            }

            repository.saveWorker(resolvedWorker)
            val uid = _currentUser.value?.uid
            if (uid != null) {
                syncRepository?.uploadWorker(uid, resolvedWorker)
                if (resolvedWorker.localPhotoPath.isNotBlank()) {
                    val photoFile = File(resolvedWorker.localPhotoPath)
                    if (photoFile.exists()) {
                        val uploaded = FirebaseStorageHelper.uploadWorkerPhoto(uid, resolvedWorker.id, photoFile)
                        if (uploaded != null) {
                            val withUrl = resolvedWorker.copy(
                                photoUrl = uploaded.downloadUrl,
                                photoStoragePath = uploaded.storagePath
                            )
                            repository.saveWorker(withUrl)
                            syncRepository?.uploadWorker(uid, withUrl)
                        }
                    }
                }
            }
        }
    }

    fun toggleWorkerStatus(worker: WorkerEntity) {
        viewModelScope.launch {
            val updated = worker.copy(isActive = !worker.isActive)
            repository.saveWorker(updated)
            val uid = _currentUser.value?.uid
            if (uid != null) {
                syncRepository?.uploadWorker(uid, updated)
            }
        }
    }

    fun saveSite(site: SiteEntity) {
        viewModelScope.launch {
            repository.saveSite(site)
            val uid = _currentUser.value?.uid
            if (uid != null) {
                syncRepository?.uploadSite(uid, site)
            }
        }
    }

    fun recordAdvance(
        workerId: String,
        amount: Double,
        paymentMethod: String,
        reference: String,
        notes: String,
        dateOverride: String? = null
    ) {
        viewModelScope.launch {
            val advance = AdvanceEntity(
                id = "ADV-${System.currentTimeMillis().toString().takeLast(6)}",
                workerId = workerId,
                date = dateOverride ?: _selectedDate.value,
                amount = amount,
                paymentMethod = paymentMethod,
                reference = reference,
                notes = notes
            )
            repository.saveAdvance(advance)
            val uid = _currentUser.value?.uid
            if (uid != null) {
                syncRepository?.uploadAdvance(uid, advance)
            }
        }
    }

    fun recordPayment(
        workerId: String,
        amount: Double,
        paymentMethod: String,
        reference: String,
        notes: String,
        period: String = "",
        dateOverride: String? = null
    ) {
        viewModelScope.launch {
            val payment = PaymentEntity(
                id = "PAY-${System.currentTimeMillis().toString().takeLast(6)}",
                workerId = workerId,
                date = dateOverride ?: _selectedDate.value,
                amount = amount,
                paymentMethod = paymentMethod,
                reference = reference,
                notes = notes,
                settlementPeriod = period
            )
            repository.savePayment(payment)
            val uid = _currentUser.value?.uid
            if (uid != null) {
                syncRepository?.uploadPayment(uid, payment)
            }
        }
    }

    fun selectAmavasyaDate(isoDate: String, isManual: Boolean) {
        viewModelScope.launch {
            val current = amavasyaConfig.value ?: AmavasyaConfigEntity()
            val updated = current.copy(
                selectedSettlementDate = isoDate,
                isManualDate = isManual,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveAmavasyaConfig(updated)
            val uid = _currentUser.value?.uid
            if (uid != null) {
                syncRepository?.uploadAmavasyaConfig(uid, updated)
            }
        }
    }

    fun resetToCalculatedAmavasya() {
        val nextCalculated = AmavasyaCalendar.getNextAmavasyaDate().isoDate
        selectAmavasyaDate(nextCalculated, isManual = false)
    }

    fun updateReminderDays(days: Int) {
        viewModelScope.launch {
            val current = amavasyaConfig.value ?: AmavasyaConfigEntity()
            val updated = current.copy(
                reminderDaysBefore = days.coerceIn(1, 14),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveAmavasyaConfig(updated)
            val uid = _currentUser.value?.uid
            if (uid != null) {
                syncRepository?.uploadAmavasyaConfig(uid, updated)
            }
        }
    }

    fun markSettlementComplete(
        statement: WorkerHisabStatement,
        carryForwardUnpaid: Boolean,
        notes: String = ""
    ) {
        if (!carryForwardUnpaid && !statement.canMarkFullySettled) {
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val isFullyZeroed = statement.payableAmount <= 0.01 && statement.outstandingAdvanceAmount <= 0.01
            val finalStatus = if (isFullyZeroed && !carryForwardUnpaid) "COMPLETED" else "CARRIED_FORWARD"
            val auditAction = if (finalStatus == "COMPLETED") "SETTLED_FULL" else "CARRIED_FORWARD"

            val settlementEntity = AmavasyaSettlementEntity(
                id = statement.settlementId,
                workerId = statement.worker.id,
                workerName = statement.worker.name,
                periodStartDate = statement.periodStartDate,
                periodEndDate = statement.periodEndDate,
                presentDays = statement.presentDays,
                halfDays = statement.halfDays,
                absentDays = statement.absentDays,
                paidLeaveDays = statement.paidLeaveDays,
                payableManDays = statement.payableManDays,
                totalWages = statement.totalWages,
                overtimeHours = statement.totalOvertimeHours,
                overtimeAmount = statement.totalOvertimeAmount,
                totalEarnings = statement.totalEarnings,
                previousBalance = statement.previousBalance,
                advancesTaken = statement.advancesTaken,
                paymentsMade = statement.paymentsMade,
                netBalance = statement.netBalance,
                payableAmount = statement.payableAmount,
                outstandingAdvanceAmount = statement.outstandingAdvanceAmount,
                carryForwardBalance = if (isFullyZeroed) 0.0 else statement.netBalance,
                status = finalStatus,
                isLocked = true,
                notes = notes,
                settledAt = now,
                updatedAt = now
            )

            val ownerLabel = _currentUser.value?.email ?: "Owner"
            val auditEntity = SettlementAuditEntity(
                id = "AUD-${now}-${statement.worker.id.takeLast(4)}",
                settlementId = statement.settlementId,
                workerId = statement.worker.id,
                workerName = statement.worker.name,
                action = auditAction,
                reason = notes.ifBlank {
                    if (finalStatus == "COMPLETED") {
                        "Monthly Amavasya Hisab settled in full"
                    } else {
                        "Period closed; remaining balance ₹${statement.netBalance.toLong()} carried forward"
                    }
                },
                previousNetBalance = statement.previousBalance,
                newNetBalance = statement.netBalance,
                performedBy = ownerLabel,
                timestamp = now
            )

            repository.saveSettlement(settlementEntity)
            repository.saveSettlementAudit(auditEntity)
            val uid = _currentUser.value?.uid
            if (uid != null) {
                syncRepository?.uploadSettlement(uid, settlementEntity)
                syncRepository?.uploadSettlementAudit(uid, auditEntity)
            }

            // Auto-generate this worker's PDF (no duplicate if already exists) and update combined monthly PDF
            generateWorkerPdfInternal(statement, forceRegenerate = false)
            val updatedStatements = AmavasyaHisabCalculator.calculateWorkerStatements(
                workers = allWorkers.value,
                attendanceRecords = allAttendance.value,
                advances = allAdvances.value,
                payments = allPayments.value,
                settlements = allSettlements.value + settlementEntity,
                audits = allSettlementAudits.value + auditEntity,
                selectedAmavasyaDate = statement.periodEndDate
            )
            generateCombinedPdfInternal(statement.periodEndDate, updatedStatements, forceRegenerate = true)
        }
    }

    fun generateMonthlyPdfsForPeriod(
        targetStatements: List<WorkerHisabStatement> = hisabStatements.value,
        forceRegenerate: Boolean = false
    ) {
        if (targetStatements.isEmpty()) return
        viewModelScope.launch {
            _isGeneratingPdfs.value = true
            try {
                for (stmt in targetStatements) {
                    generateWorkerPdfInternal(stmt, forceRegenerate = forceRegenerate)
                }
                val periodEnd = targetStatements.first().periodEndDate
                generateCombinedPdfInternal(periodEnd, targetStatements, forceRegenerate = forceRegenerate)
            } finally {
                _isGeneratingPdfs.value = false
            }
        }
    }

    fun regenerateMonthlyPdf(pdf: MonthlyPdfEntity) {
        viewModelScope.launch {
            _isGeneratingPdfs.value = true
            try {
                val periodStatements = AmavasyaHisabCalculator.calculateWorkerStatements(
                    workers = allWorkers.value,
                    attendanceRecords = allAttendance.value,
                    advances = allAdvances.value,
                    payments = allPayments.value,
                    settlements = allSettlements.value,
                    audits = allSettlementAudits.value,
                    selectedAmavasyaDate = pdf.periodEndDate
                )
                if (pdf.isCombinedReport) {
                    generateCombinedPdfInternal(pdf.periodEndDate, periodStatements, forceRegenerate = true)
                } else {
                    val match = periodStatements.find { it.worker.id == pdf.workerId }
                    if (match != null) {
                        generateWorkerPdfInternal(match, forceRegenerate = true)
                    }
                }
            } finally {
                _isGeneratingPdfs.value = false
            }
        }
    }

    fun ensurePdfLocalFileReady(pdf: MonthlyPdfEntity, onReady: (File?) -> Unit) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                val app = getApplication<Application>()
                val direct = pdf.localFilePath.takeIf { it.isNotBlank() }?.let { File(it) }
                if (direct != null && direct.exists() && direct.length() > 0L) {
                    return@withContext direct
                }

                val (yr, mo, _) = AmavasyaPdfHelper.parseYearMonthFromIso(pdf.periodEndDate)
                val fileName = if (pdf.isCombinedReport) {
                    "KGN_Combined_Monthly_Hisab_${pdf.periodEndDate}.pdf"
                } else {
                    val safeId = pdf.workerId.replace(Regex("[^A-Za-z0-9_-]"), "_")
                    "KGN_Hisab_${safeId}_${pdf.periodEndDate}.pdf"
                }
                val expected = AmavasyaPdfHelper.getPersistentPdfFile(app, yr, mo, fileName)
                if (expected.exists() && expected.length() > 0L) {
                    repository.saveMonthlyPdf(pdf.copy(localFilePath = expected.absolutePath))
                    return@withContext expected
                }

                if (pdf.storagePath.isNotBlank()) {
                    val downloaded = FirebaseStorageHelper.downloadFileToLocal(pdf.storagePath, expected)
                    if (downloaded) {
                        repository.saveMonthlyPdf(
                            pdf.copy(
                                localFilePath = expected.absolutePath,
                                fileSizeBytes = expected.length()
                            )
                        )
                        return@withContext expected
                    }
                }

                // Fallback: regenerate from local data
                val periodStatements = AmavasyaHisabCalculator.calculateWorkerStatements(
                    workers = allWorkers.value,
                    attendanceRecords = allAttendance.value,
                    advances = allAdvances.value,
                    payments = allPayments.value,
                    settlements = allSettlements.value,
                    audits = allSettlementAudits.value,
                    selectedAmavasyaDate = pdf.periodEndDate
                )
                val regenerated = if (pdf.isCombinedReport) {
                    AmavasyaPdfHelper.generatePersistentCombinedMonthlyPdf(app, pdf.periodEndDate, periodStatements, allSites.value)
                } else {
                    val stmt = periodStatements.find { it.worker.id == pdf.workerId }
                    if (stmt != null) {
                        val siteName = allSites.value.find { it.id == stmt.worker.siteId }?.name ?: "General"
                        AmavasyaPdfHelper.generatePersistentWorkerPdf(app, stmt, siteName)
                    } else null
                }
                if (regenerated != null && regenerated.file.exists()) {
                    repository.saveMonthlyPdf(
                        pdf.copy(
                            localFilePath = regenerated.file.absolutePath,
                            fileSizeBytes = regenerated.file.length(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    regenerated.file
                } else {
                    null
                }
            }
            onReady(file)
        }
    }

    private suspend fun generateWorkerPdfInternal(
        statement: WorkerHisabStatement,
        forceRegenerate: Boolean
    ) {
        val app = getApplication<Application>()
        val existing = repository.getMonthlyPdfByWorkerAndPeriod(statement.worker.id, statement.periodEndDate)
        if (!forceRegenerate && existing != null && existing.localFilePath.isNotBlank() && File(existing.localFilePath).exists()) {
            return
        }
        val uid = _currentUser.value?.uid
        // Ensure worker photo exists locally if stored in cloud
        if (uid != null && statement.worker.localPhotoPath.isBlank() && statement.worker.photoStoragePath.isNotBlank()) {
            val destPhoto = WorkerPhotoHelper.getWorkerPhotoFile(app, statement.worker.id)
            if (FirebaseStorageHelper.downloadFileToLocal(statement.worker.photoStoragePath, destPhoto)) {
                repository.saveWorker(statement.worker.copy(localPhotoPath = destPhoto.absolutePath))
            }
        }

        val siteName = allSites.value.find { it.id == statement.worker.siteId }?.name ?: "General"
        val res = withContext(Dispatchers.IO) {
            AmavasyaPdfHelper.generatePersistentWorkerPdf(app, statement, siteName)
        } ?: return

        val now = System.currentTimeMillis()
        var entity = MonthlyPdfEntity(
            id = existing?.id ?: "pdf_${statement.worker.id}_${statement.periodEndDate}",
            workerId = statement.worker.id,
            workerName = statement.worker.name,
            year = res.year,
            month = res.month,
            monthLabel = res.monthLabel,
            periodStartDate = statement.periodStartDate,
            periodEndDate = statement.periodEndDate,
            isCombinedReport = false,
            localFilePath = res.file.absolutePath,
            storagePath = existing?.storagePath.orEmpty(),
            downloadUrl = existing?.downloadUrl.orEmpty(),
            fileSizeBytes = res.file.length(),
            generatedAt = existing?.generatedAt ?: now,
            updatedAt = now
        )
        repository.saveMonthlyPdf(entity)

        if (uid != null) {
            val uploaded = FirebaseStorageHelper.uploadMonthlyPdf(uid, res.year, res.month, res.file.name, res.file)
            if (uploaded != null) {
                entity = entity.copy(storagePath = uploaded.storagePath, downloadUrl = uploaded.downloadUrl)
                repository.saveMonthlyPdf(entity)
            }
            syncRepository?.uploadMonthlyPdf(uid, entity)
        }
    }

    private suspend fun generateCombinedPdfInternal(
        periodEndDate: String,
        statements: List<WorkerHisabStatement>,
        forceRegenerate: Boolean
    ) {
        if (statements.isEmpty()) return
        val app = getApplication<Application>()
        val existing = repository.getMonthlyPdfByWorkerAndPeriod("COMBINED", periodEndDate)
        if (!forceRegenerate && existing != null && existing.localFilePath.isNotBlank() && File(existing.localFilePath).exists()) {
            return
        }

        val res = withContext(Dispatchers.IO) {
            AmavasyaPdfHelper.generatePersistentCombinedMonthlyPdf(app, periodEndDate, statements, allSites.value)
        } ?: return

        val now = System.currentTimeMillis()
        val firstStart = statements.mapNotNull { it.periodStartDate.takeIf { s -> s.isNotBlank() } }.minOrNull() ?: ""
        var entity = MonthlyPdfEntity(
            id = existing?.id ?: "pdf_COMBINED_${periodEndDate}",
            workerId = "COMBINED",
            workerName = "All Workers Combined Report",
            year = res.year,
            month = res.month,
            monthLabel = res.monthLabel,
            periodStartDate = firstStart,
            periodEndDate = periodEndDate,
            isCombinedReport = true,
            localFilePath = res.file.absolutePath,
            storagePath = existing?.storagePath.orEmpty(),
            downloadUrl = existing?.downloadUrl.orEmpty(),
            fileSizeBytes = res.file.length(),
            generatedAt = existing?.generatedAt ?: now,
            updatedAt = now
        )
        repository.saveMonthlyPdf(entity)

        val uid = _currentUser.value?.uid
        if (uid != null) {
            val uploaded = FirebaseStorageHelper.uploadMonthlyPdf(uid, res.year, res.month, res.file.name, res.file)
            if (uploaded != null) {
                entity = entity.copy(storagePath = uploaded.storagePath, downloadUrl = uploaded.downloadUrl)
                repository.saveMonthlyPdf(entity)
            }
            syncRepository?.uploadMonthlyPdf(uid, entity)
        }
    }

    fun performAuthorizedCorrection(
        statement: WorkerHisabStatement,
        reason: String,
        reopenPeriod: Boolean
    ) {
        if (reason.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val ownerLabel = _currentUser.value?.email ?: "Owner"
            if (reopenPeriod) {
                repository.deleteSettlement(statement.settlementId)
                val auditEntity = SettlementAuditEntity(
                    id = "AUD-$now-${statement.worker.id.takeLast(4)}",
                    settlementId = statement.settlementId,
                    workerId = statement.worker.id,
                    workerName = statement.worker.name,
                    action = "REOPENED",
                    reason = reason.trim(),
                    previousNetBalance = statement.netBalance,
                    newNetBalance = statement.netBalance,
                    performedBy = ownerLabel,
                    timestamp = now
                )
                repository.saveSettlementAudit(auditEntity)
                val uid = _currentUser.value?.uid
                if (uid != null) {
                    syncRepository?.deleteSettlement(uid, statement.settlementId)
                    syncRepository?.uploadSettlementAudit(uid, auditEntity)
                }
            } else {
                // Recompute live statement ignoring the locked snapshot for this period, then re-lock
                val withoutCurrentLock = allSettlements.value.filterNot { it.id == statement.settlementId }
                val recomputed = AmavasyaHisabCalculator.calculateStatementForWorker(
                    worker = statement.worker,
                    attendanceRecords = allAttendance.value,
                    advances = allAdvances.value,
                    payments = allPayments.value,
                    settlements = withoutCurrentLock,
                    audits = allSettlementAudits.value,
                    selectedAmavasyaDate = statement.periodEndDate
                )
                val isZeroed = recomputed.payableAmount <= 0.01 && recomputed.outstandingAdvanceAmount <= 0.01
                val updatedStatus = if (isZeroed) "COMPLETED" else "CARRIED_FORWARD"
                val updatedEntity = AmavasyaSettlementEntity(
                    id = statement.settlementId,
                    workerId = recomputed.worker.id,
                    workerName = recomputed.worker.name,
                    periodStartDate = recomputed.periodStartDate,
                    periodEndDate = recomputed.periodEndDate,
                    presentDays = recomputed.presentDays,
                    halfDays = recomputed.halfDays,
                    absentDays = recomputed.absentDays,
                    paidLeaveDays = recomputed.paidLeaveDays,
                    payableManDays = recomputed.payableManDays,
                    totalWages = recomputed.totalWages,
                    overtimeHours = recomputed.totalOvertimeHours,
                    overtimeAmount = recomputed.totalOvertimeAmount,
                    totalEarnings = recomputed.totalEarnings,
                    previousBalance = recomputed.previousBalance,
                    advancesTaken = recomputed.advancesTaken,
                    paymentsMade = recomputed.paymentsMade,
                    netBalance = recomputed.netBalance,
                    payableAmount = recomputed.payableAmount,
                    outstandingAdvanceAmount = recomputed.outstandingAdvanceAmount,
                    carryForwardBalance = if (isZeroed) 0.0 else recomputed.netBalance,
                    status = updatedStatus,
                    isLocked = true,
                    notes = reason.trim(),
                    settledAt = statement.settledAt ?: now,
                    updatedAt = now
                )
                val auditEntity = SettlementAuditEntity(
                    id = "AUD-$now-${statement.worker.id.takeLast(4)}",
                    settlementId = statement.settlementId,
                    workerId = statement.worker.id,
                    workerName = statement.worker.name,
                    action = "AUTHORIZED_CORRECTION",
                    reason = reason.trim(),
                    previousNetBalance = statement.netBalance,
                    newNetBalance = recomputed.netBalance,
                    performedBy = ownerLabel,
                    timestamp = now
                )
                repository.saveSettlement(updatedEntity)
                repository.saveSettlementAudit(auditEntity)
                val uid = _currentUser.value?.uid
                if (uid != null) {
                    syncRepository?.uploadSettlement(uid, updatedEntity)
                    syncRepository?.uploadSettlementAudit(uid, auditEntity)
                }
            }
        }
    }

    fun deleteAttendance(id: String) {
        viewModelScope.launch {
            repository.deleteAttendance(id)
        }
    }

    fun deleteAdvance(id: String) {
        viewModelScope.launch {
            repository.deleteAdvance(id)
            val uid = _currentUser.value?.uid
            if (uid != null) {
                syncRepository?.deleteAdvance(uid, id)
            }
        }
    }

    fun deletePayment(id: String) {
        viewModelScope.launch {
            repository.deletePayment(id)
            val uid = _currentUser.value?.uid
            if (uid != null) {
                syncRepository?.deletePayment(uid, id)
            }
        }
    }
}

