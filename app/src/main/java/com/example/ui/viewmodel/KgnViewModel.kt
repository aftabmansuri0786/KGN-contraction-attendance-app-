package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AdvanceEntity
import com.example.data.model.AttendanceEntity
import com.example.data.model.AttendanceStatus
import com.example.data.model.PaymentEntity
import com.example.data.model.SiteEntity
import com.example.data.model.WorkerEntity
import android.content.Context
import com.example.R
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.KgnRepository
import com.example.util.AppLanguage
import com.example.util.Formatters
import com.example.util.GoogleAuthHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

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
            }
            _isSyncing.value = false
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
        }
    }

    fun saveWorker(worker: WorkerEntity) {
        viewModelScope.launch {
            repository.saveWorker(worker)
            val uid = _currentUser.value?.uid
            if (uid != null) {
                syncRepository?.uploadWorker(uid, worker)
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
        notes: String
    ) {
        viewModelScope.launch {
            val advance = AdvanceEntity(
                id = "ADV-${System.currentTimeMillis().toString().takeLast(6)}",
                workerId = workerId,
                date = _selectedDate.value,
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
        period: String = ""
    ) {
        viewModelScope.launch {
            val payment = PaymentEntity(
                id = "PAY-${System.currentTimeMillis().toString().takeLast(6)}",
                workerId = workerId,
                date = _selectedDate.value,
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
