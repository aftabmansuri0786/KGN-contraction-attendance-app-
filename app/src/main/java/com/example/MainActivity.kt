package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceStatus
import com.example.data.model.SiteEntity
import com.example.data.model.WorkerEntity
import com.example.ui.components.AddSiteDialog
import com.example.ui.components.AddWorkerDialog
import com.example.ui.components.AppTopBar
import com.example.ui.components.MessageNotificationDialog
import com.example.ui.components.RecordTransactionDialog
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SitesScreen
import com.example.ui.screens.WagesScreen
import com.example.ui.screens.WorkersScreen
import com.example.ui.theme.ConstructionOrange
import com.example.ui.theme.KgnConstructionTheme
import com.example.ui.viewmodel.KgnViewModel
import com.example.util.AdMobConfig
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.Formatters
import com.example.util.NotificationMessageType
import com.example.util.WorkerMessageData
import com.example.util.WorkerMessageHelper

class MainActivity : ComponentActivity() {
    private val viewModel: KgnViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Initialize Google Mobile Ads SDK
        AdMobConfig.initialize(this)

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            val language by viewModel.language.collectAsStateWithLifecycle()
            val isUnlocked by viewModel.isOwnerUnlocked.collectAsStateWithLifecycle()

            KgnConstructionTheme(darkTheme = isDarkTheme) {
                if (!isUnlocked) {
                    AuthScreen(
                        language = language,
                        onUnlock = { pin -> viewModel.unlockOwner(pin) }
                    )
                } else {
                    MainScreenContent(
                        viewModel = viewModel,
                        language = language,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreenContent(
    viewModel: KgnViewModel,
    language: AppLanguage,
    isDarkTheme: Boolean
) {
    var currentTab by remember { mutableIntStateOf(0) } // 0=Home, 1=Attendance, 2=Workers, 3=Wages, 4=Reports, 5=Sites, 6=Settings

    // Dialog States
    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var workerToEdit by remember { mutableStateOf<WorkerEntity?>(null) }

    var showAddSiteDialog by remember { mutableStateOf(false) }
    var siteToEdit by remember { mutableStateOf<SiteEntity?>(null) }

    var showAdvanceDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var transactionPreselectedWorkerId by remember { mutableStateOf("") }
    var pendingMessageNotification by remember { mutableStateOf<WorkerMessageData?>(null) }

    // Collected states
    val workers by viewModel.allWorkers.collectAsStateWithLifecycle()
    val activeWorkers by viewModel.activeWorkers.collectAsStateWithLifecycle()
    val sites by viewModel.allSites.collectAsStateWithLifecycle()
    val attendanceRecords by viewModel.allAttendance.collectAsStateWithLifecycle()
    val advances by viewModel.allAdvances.collectAsStateWithLifecycle()
    val payments by viewModel.allPayments.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedSiteId by viewModel.selectedSiteId.collectAsStateWithLifecycle()
    val ownerPin by viewModel.ownerPin.collectAsStateWithLifecycle()

    // Back handler to return to Home dashboard
    BackHandler(enabled = currentTab != 0) {
        currentTab = 0
    }

    Scaffold(
        topBar = {
            AppTopBar(
                language = language,
                isDarkTheme = isDarkTheme,
                onToggleLanguage = { viewModel.toggleLanguage() },
                onToggleTheme = { viewModel.toggleTheme() },
                onLockApp = { viewModel.lockOwner() }
            )
        },
        bottomBar = {
            NavigationBar(
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(AppStrings.get("home", language), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = ConstructionOrange, indicatorColor = ConstructionOrange.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Attendance") },
                    label = { Text(AppStrings.get("attendance", language), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = ConstructionOrange, indicatorColor = ConstructionOrange.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("nav_attendance")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Workers") },
                    label = { Text(AppStrings.get("workers", language), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = ConstructionOrange, indicatorColor = ConstructionOrange.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("nav_workers")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.CurrencyRupee, contentDescription = "Wages") },
                    label = { Text(AppStrings.get("wages", language), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = ConstructionOrange, indicatorColor = ConstructionOrange.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("nav_wages")
                )
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { currentTab = 4 },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                    label = { Text(AppStrings.get("reports", language), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = ConstructionOrange, indicatorColor = ConstructionOrange.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("nav_reports")
                )
                NavigationBarItem(
                    selected = currentTab == 5,
                    onClick = { currentTab = 5 },
                    icon = { Icon(Icons.Default.Business, contentDescription = "Sites") },
                    label = { Text(AppStrings.get("sites", language), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = ConstructionOrange, indicatorColor = ConstructionOrange.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("nav_sites")
                )
                NavigationBarItem(
                    selected = currentTab == 6,
                    onClick = { currentTab = 6 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text(AppStrings.get("settings", language), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = ConstructionOrange, indicatorColor = ConstructionOrange.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> DashboardScreen(
                    language = language,
                    metrics = metrics,
                    workers = workers,
                    recentAttendance = attendanceRecords,
                    recentPayments = payments,
                    onNavigateToAttendance = { currentTab = 1 },
                    onNavigateToWorkers = { currentTab = 2 },
                    onNavigateToSites = { currentTab = 5 },
                    onNavigateToWages = { currentTab = 3 },
                    onNavigateToReports = { currentTab = 4 },
                    onAddWorkerClick = {
                        workerToEdit = null
                        showAddWorkerDialog = true
                    },
                    onAddSiteClick = {
                        siteToEdit = null
                        showAddSiteDialog = true
                    },
                    onRecordAdvanceClick = {
                        transactionPreselectedWorkerId = ""
                        showAdvanceDialog = true
                    },
                    onRecordPaymentClick = {
                        transactionPreselectedWorkerId = ""
                        showPaymentDialog = true
                    }
                )

                1 -> AttendanceScreen(
                    language = language,
                    selectedDate = selectedDate,
                    selectedSiteId = selectedSiteId,
                    sites = sites,
                    workers = workers,
                    attendanceRecords = attendanceRecords,
                    onDateChanged = { viewModel.setSelectedDate(it) },
                    onSiteChanged = { viewModel.setSelectedSite(it) },
                    onMarkAllPresent = { siteId, date -> viewModel.markAllPresent(siteId, date) },
                    onSaveSingleAttendance = { workerId, siteId, date, status, otHours, remarks ->
                        viewModel.saveAttendance(workerId, siteId, date, status, otHours, remarks)
                        val worker = workers.find { it.id == workerId }
                        if (worker != null) {
                            val formattedDate = Formatters.formatToIndianDate(date)
                            val statusLabel = when (status) {
                                AttendanceStatus.PRESENT -> "PRESENT"
                                AttendanceStatus.HALF_DAY -> "HALF-DAY"
                                AttendanceStatus.ABSENT -> "ABSENT"
                                AttendanceStatus.PAID_LEAVE -> "PAID LEAVE"
                                AttendanceStatus.WEEKLY_OFF -> "WEEKLY OFF"
                            }
                            val preparedMsg = WorkerMessageHelper.buildAttendanceMessage(
                                workerName = worker.name,
                                date = formattedDate,
                                status = statusLabel
                            )
                            pendingMessageNotification = WorkerMessageData(
                                workerId = worker.id,
                                workerName = worker.name,
                                workerMobile = worker.mobile,
                                type = NotificationMessageType.ATTENDANCE,
                                messageText = preparedMsg,
                                dateDisplay = formattedDate,
                                statusText = statusLabel
                            )
                        }
                    }
                )

                2 -> WorkersScreen(
                    language = language,
                    workers = workers,
                    sites = sites,
                    attendanceRecords = attendanceRecords,
                    advances = advances,
                    payments = payments,
                    onAddWorkerClick = {
                        workerToEdit = null
                        showAddWorkerDialog = true
                    },
                    onEditWorkerClick = { worker ->
                        workerToEdit = worker
                        showAddWorkerDialog = true
                    },
                    onToggleWorkerStatus = { worker -> viewModel.toggleWorkerStatus(worker) },
                    onRecordPaymentForWorker = { workerId ->
                        transactionPreselectedWorkerId = workerId
                        showPaymentDialog = true
                    },
                    onRecordAdvanceForWorker = { workerId ->
                        transactionPreselectedWorkerId = workerId
                        showAdvanceDialog = true
                    }
                )

                3 -> WagesScreen(
                    language = language,
                    workers = workers,
                    advances = advances,
                    payments = payments,
                    onRecordAdvanceClick = {
                        transactionPreselectedWorkerId = ""
                        showAdvanceDialog = true
                    },
                    onRecordPaymentClick = {
                        transactionPreselectedWorkerId = ""
                        showPaymentDialog = true
                    },
                    onDeleteAdvance = { viewModel.deleteAdvance(it) },
                    onDeletePayment = { viewModel.deletePayment(it) }
                )

                4 -> ReportsScreen(
                    language = language,
                    workers = workers,
                    sites = sites,
                    attendanceRecords = attendanceRecords,
                    advances = advances,
                    payments = payments
                )

                5 -> SitesScreen(
                    language = language,
                    sites = sites,
                    workers = workers,
                    attendanceRecords = attendanceRecords,
                    onAddSiteClick = {
                        siteToEdit = null
                        showAddSiteDialog = true
                    },
                    onEditSiteClick = { site ->
                        siteToEdit = site
                        showAddSiteDialog = true
                    }
                )

                6 -> SettingsScreen(
                    language = language,
                    isDarkTheme = isDarkTheme,
                    currentPin = ownerPin,
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onToggleTheme = { viewModel.toggleTheme() },
                    onChangePin = { viewModel.changeOwnerPin(it) },
                    onLockApp = { viewModel.lockOwner() },
                    onClearAllData = { viewModel.clearAllData() }
                )
            }
        }
    }

    // Modal Dialogs
    if (showAddWorkerDialog) {
        AddWorkerDialog(
            language = language,
            sites = sites,
            workerToEdit = workerToEdit,
            onDismiss = {
                showAddWorkerDialog = false
                workerToEdit = null
            },
            onSave = { worker ->
                viewModel.saveWorker(worker)
            }
        )
    }

    if (showAddSiteDialog) {
        AddSiteDialog(
            language = language,
            siteToEdit = siteToEdit,
            onDismiss = {
                showAddSiteDialog = false
                siteToEdit = null
            },
            onSave = { site ->
                viewModel.saveSite(site)
            }
        )
    }

    if (showAdvanceDialog) {
        RecordTransactionDialog(
            isAdvance = true,
            language = language,
            workers = activeWorkers,
            preSelectedWorkerId = transactionPreselectedWorkerId,
            onDismiss = {
                showAdvanceDialog = false
                transactionPreselectedWorkerId = ""
            },
            onUpdateWorker = { worker ->
                showAdvanceDialog = false
                workerToEdit = worker
                showAddWorkerDialog = true
            },
            onSubmit = { workerId, amount, method, ref, notes ->
                viewModel.recordAdvance(workerId, amount, method, ref, notes)
                val worker = workers.find { it.id == workerId }
                if (worker != null) {
                    val currentWorkerAdvances = advances.filter { it.workerId == workerId }.sumOf { it.amount }
                    val newTotalAdvance = currentWorkerAdvances + amount
                    val todayFormatted = Formatters.formatToIndianDate(Formatters.getTodayIsoDate())
                    val preparedMsg = WorkerMessageHelper.buildAdvanceMessage(
                        workerName = worker.name,
                        amount = amount,
                        date = todayFormatted,
                        totalAdvanceBalance = newTotalAdvance
                    )
                    pendingMessageNotification = WorkerMessageData(
                        workerId = worker.id,
                        workerName = worker.name,
                        workerMobile = worker.mobile,
                        type = NotificationMessageType.ADVANCE,
                        messageText = preparedMsg,
                        dateDisplay = todayFormatted,
                        amount = amount,
                        balance = newTotalAdvance
                    )
                }
            }
        )
    }

    if (showPaymentDialog) {
        RecordTransactionDialog(
            isAdvance = false,
            language = language,
            workers = activeWorkers,
            preSelectedWorkerId = transactionPreselectedWorkerId,
            onDismiss = {
                showPaymentDialog = false
                transactionPreselectedWorkerId = ""
            },
            onUpdateWorker = { worker ->
                showPaymentDialog = false
                workerToEdit = worker
                showAddWorkerDialog = true
            },
            onSubmit = { workerId, amount, method, ref, notes ->
                viewModel.recordPayment(workerId, amount, method, ref, notes)
                val worker = workers.find { it.id == workerId }
                if (worker != null) {
                    val workerAttendance = attendanceRecords.filter { it.workerId == workerId }
                    val earnedWages = workerAttendance.sumOf { record ->
                        if (record.calculatedEarnings > 0) {
                            record.calculatedEarnings
                        } else {
                            val frac = when (record.status.uppercase()) {
                                "PRESENT" -> 1.0
                                "HALF_DAY", "HALF-DAY" -> 0.5
                                "PAID_LEAVE" -> 1.0
                                else -> 0.0
                            }
                            val otRate = if (worker.overtimeHourlyRate > 0) worker.overtimeHourlyRate else (worker.dailyWage / 8.0)
                            (frac * worker.dailyWage) + (record.overtimeHours * otRate)
                        }
                    }
                    val previousPaid = payments.filter { it.workerId == workerId }.sumOf { it.amount }
                    val remainingBalance = (earnedWages - (previousPaid + amount)).coerceAtLeast(0.0)
                    val todayFormatted = Formatters.formatToIndianDate(Formatters.getTodayIsoDate())
                    val preparedMsg = WorkerMessageHelper.buildPaymentMessage(
                        workerName = worker.name,
                        amount = amount,
                        date = todayFormatted,
                        remainingBalance = remainingBalance
                    )
                    pendingMessageNotification = WorkerMessageData(
                        workerId = worker.id,
                        workerName = worker.name,
                        workerMobile = worker.mobile,
                        type = NotificationMessageType.PAYMENT,
                        messageText = preparedMsg,
                        dateDisplay = todayFormatted,
                        amount = amount,
                        balance = remainingBalance
                    )
                }
            }
        )
    }

    if (pendingMessageNotification != null) {
        MessageNotificationDialog(
            language = language,
            data = pendingMessageNotification!!,
            onDismiss = { pendingMessageNotification = null },
            onUpdateWorkerProfile = { workerId ->
                workerToEdit = workers.find { it.id == workerId }
                showAddWorkerDialog = true
            }
        )
    }
}
