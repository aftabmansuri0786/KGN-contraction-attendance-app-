package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AmavasyaSettlementEntity
import com.example.data.model.SettlementAuditEntity
import com.example.data.model.WorkerEntity
import com.example.ui.components.AmavasyaDatePickerModal
import com.example.ui.components.AuthorizedCorrectionDialog
import com.example.ui.components.FullHisabStatementDialog
import com.example.ui.components.MarkSettlementDialog
import com.example.ui.components.WorkerAvatar
import com.example.ui.theme.CharcoalDark
import com.example.ui.theme.ConstructionOrange
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusHalfDay
import com.example.ui.theme.StatusPresent
import com.example.util.AmavasyaCalendar
import com.example.util.AmavasyaDashboardSummary
import com.example.util.AmavasyaPdfHelper
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.Formatters
import com.example.util.HisabSettlementStatus
import com.example.util.WorkerHisabStatement
import com.example.util.WorkerMessageHelper
import java.util.Calendar
import java.util.Locale

@Composable
fun AmavasyaHisabScreen(
    language: AppLanguage,
    selectedAmavasyaDate: String,
    isManualDate: Boolean,
    reminderDaysBefore: Int = 3,
    dashboardSummary: AmavasyaDashboardSummary,
    statements: List<WorkerHisabStatement>,
    settlementHistory: List<AmavasyaSettlementEntity>,
    auditLogs: List<SettlementAuditEntity>,
    onSelectDate: (isoDate: String, isManual: Boolean) -> Unit,
    onResetToCalculated: () -> Unit,
    onUpdateReminderDays: (Int) -> Unit = {},
    onBackToHome: () -> Unit = {},
    onRecordPaymentForWorker: (workerId: String, suggestedAmount: Double, period: String, targetDate: String) -> Unit,
    onRecordAdvanceForWorker: (workerId: String, targetDate: String) -> Unit,
    onMarkSettlementComplete: (statement: WorkerHisabStatement, carryForwardUnpaid: Boolean, notes: String) -> Unit,
    onAuthorizedCorrection: (statement: WorkerHisabStatement, reason: String, reopenPeriod: Boolean) -> Unit,
    onUpdateWorkerPhone: (workerId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayIso = remember { Formatters.getTodayIsoDate() }
    val upcomingAmavasyas = remember(todayIso) {
        AmavasyaCalendar.getUpcomingAmavasyaDates(AmavasyaCalendar.addDays(todayIso, -35), 10)
    }

    var filterTab by remember { mutableIntStateOf(0) } // 0=All, 1=Pending, 2=Settled, 3=History & Audit
    var searchQuery by remember { mutableStateOf("") }
    var historyWorkerFilterId by remember { mutableStateOf("") }
    var showCalendarGrid by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    var statementForDetail by remember { mutableStateOf<WorkerHisabStatement?>(null) }
    var statementToSettle by remember { mutableStateOf<WorkerHisabStatement?>(null) }
    var statementToCorrect by remember { mutableStateOf<WorkerHisabStatement?>(null) }

    val filteredStatements = remember(statements, filterTab, searchQuery) {
        statements.filter { stmt ->
            val matchesSearch = searchQuery.isBlank() ||
                stmt.worker.name.contains(searchQuery, ignoreCase = true) ||
                stmt.worker.id.contains(searchQuery, ignoreCase = true)
            val matchesTab = when (filterTab) {
                1 -> !stmt.isLocked && (stmt.requiresSettlementAction || stmt.worker.isActive)
                2 -> stmt.isLocked
                else -> true
            }
            matchesSearch && matchesTab
        }
    }

    val historyWorkers = remember(settlementHistory) {
        settlementHistory.distinctBy { it.workerId }.map { it.workerId to it.workerName.ifBlank { it.workerId } }
    }
    val visibleHistory = remember(settlementHistory, historyWorkerFilterId) {
        if (historyWorkerFilterId.isBlank()) settlementHistory
        else settlementHistory.filter { it.workerId == historyWorkerFilterId }
    }
    val visibleAudits = remember(auditLogs, historyWorkerFilterId) {
        if (historyWorkerFilterId.isBlank()) auditLogs
        else auditLogs.filter { it.workerId == historyWorkerFilterId }
    }

    val totalEarningsAll = statements.sumOf { it.totalEarnings }
    val totalPayableAll = statements.sumOf { it.payableAmount }
    val totalAdvanceOutstandingAll = statements.sumOf { it.outstandingAdvanceAmount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 0. Back to Dashboard Pill + Configurable Reminder Chip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBackToHome,
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("amavasya_back_home_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back to Home",
                            tint = ConstructionOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (language == AppLanguage.HINDI) "डैशबोर्ड" else "Dashboard",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                val reminderOptions = listOf(1, 3, 5, 7)
                Surface(
                    onClick = {
                        val idx = reminderOptions.indexOf(reminderDaysBefore)
                        val next = reminderOptions[(idx + 1) % reminderOptions.size]
                        onUpdateReminderDays(next)
                    },
                    color = ConstructionOrange.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ConstructionOrange.copy(alpha = 0.45f)),
                    modifier = Modifier.testTag("amavasya_reminder_days_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Reminder timing",
                            tint = ConstructionOrange,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (language == AppLanguage.HINDI) {
                                "रिमाइंडर: ${reminderDaysBefore} दिन पहले"
                            } else {
                                "Remind: ${reminderDaysBefore}d before"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConstructionOrange
                        )
                    }
                }
            }
        }

        // 1. Reminder Banner if Amavasya settlement is due soon
        if (dashboardSummary.isReminderActive) {
            item {
                Surface(
                    color = ConstructionOrange.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, ConstructionOrange),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("amavasya_reminder_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = ConstructionOrange,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    dashboardSummary.daysRemaining == 0L ->
                                        if (language == AppLanguage.HINDI) "आज अमावस्या / हिसाब का दिन है!" else "Today is Amavasya Hisab Day!"
                                    else ->
                                        if (language == AppLanguage.HINDI) {
                                            "हिसाब रिमाइंडर: ${dashboardSummary.daysRemaining} दिन शेष (${dashboardSummary.nextSettlementDateDisplay})"
                                        } else {
                                            "Hisab Reminder: ${dashboardSummary.daysRemaining} day(s) left (${dashboardSummary.nextSettlementDateDisplay})"
                                        }
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = ConstructionOrange
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI) {
                                    "${dashboardSummary.workersRequiringSettlement} मज़दूरों का हिसाब बाकी है"
                                } else {
                                    "${dashboardSummary.workersRequiringSettlement} workers pending settlement"
                                },
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 2. Hero Amavasya Date & Calendar Selector Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().testTag("amavasya_hero_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(ConstructionOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NightsStay,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = AppStrings.get("amavasya_menu", language),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (language == AppLanguage.HINDI) dashboardSummary.lunarNameHi else dashboardSummary.lunarNameEn,
                                    color = Color(0xFFB0BEC5),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Surface(
                            color = if (isManualDate) Color(0xFF37474F) else ConstructionOrange.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isManualDate) {
                                    if (language == AppLanguage.HINDI) "मैन्युअल तारीख" else "Custom Date"
                                } else {
                                    if (language == AppLanguage.HINDI) "कैलेंडर अमावस्या" else "Amavasya"
                                },
                                color = if (isManualDate) Color.White else ConstructionOrange,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (language == AppLanguage.HINDI) "हिसाब तारीख (IST)" else "Settlement Date (IST)",
                                color = Color(0xFF90A4AE),
                                fontSize = 11.sp
                            )
                            Text(
                                text = Formatters.formatToIndianDate(selectedAmavasyaDate),
                                color = ConstructionOrange,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp
                            )
                        }

                        val daysText = when {
                            dashboardSummary.daysRemaining == 0L -> if (language == AppLanguage.HINDI) "आज" else "Today"
                            dashboardSummary.daysRemaining > 0 -> "${dashboardSummary.daysRemaining}d left"
                            else -> "${-dashboardSummary.daysRemaining}d ago"
                        }
                        Surface(
                            color = Color(0xFF2C3033),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = daysText,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Date control buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showDatePickerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ConstructionOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("pick_custom_amavasya_date_btn")
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI) "तारीख बदलें" else "Change Date",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { showCalendarGrid = !showCalendarGrid },
                            border = BorderStroke(1.dp, Color(0xFF546E7A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("toggle_amavasya_calendar_btn")
                        ) {
                            Text(
                                text = if (showCalendarGrid) {
                                    if (language == AppLanguage.HINDI) "कैलेंडर छुपाएं" else "Hide Calendar"
                                } else {
                                    if (language == AppLanguage.HINDI) "कैलेंडर देखें" else "Calendar View"
                                },
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (isManualDate) {
                        TextButton(
                            onClick = onResetToCalculated,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = ConstructionOrange, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI) "अगली अमावस्या पर रीसेट करें" else "Use Next Calculated Amavasya",
                                color = ConstructionOrange,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    // Upcoming Amavasyas quick pill strip
                    Text(
                        text = if (language == AppLanguage.HINDI) "आगामी अमावस्या तिथियाँ:" else "Upcoming Amavasya Dates:",
                        color = Color(0xFFB0BEC5),
                        fontSize = 11.sp
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(upcomingAmavasyas) { item ->
                            val isSelected = item.isoDate == selectedAmavasyaDate
                            Surface(
                                onClick = { onSelectDate(item.isoDate, false) },
                                color = if (isSelected) ConstructionOrange else Color(0xFF2C3033),
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF455A64))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = item.displayDate,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = if (language == AppLanguage.HINDI) item.lunarNameHi else item.lunarNameEn.replace(" Amavasya", ""),
                                        color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color(0xFF90A4AE),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Expandable Interactive Amavasya Calendar Grid
        item {
            AnimatedVisibility(visible = showCalendarGrid) {
                AmavasyaMonthCalendarCard(
                    language = language,
                    selectedIsoDate = selectedAmavasyaDate,
                    todayIso = todayIso,
                    onDateClick = { iso, isAmavasya -> onSelectDate(iso, !isAmavasya) }
                )
            }
        }

        // 3. Period Summary Strip
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (language == AppLanguage.HINDI) "कुल कमाई (Period)" else "Period Earnings",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatInr(totalEarningsAll),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "देय मज़दूरी" else "Total Payable",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatInr(totalPayableAll),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = ConstructionOrange
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "एडवांस बकाया" else "Adv Recoverable",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatInr(totalAdvanceOutstandingAll),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = StatusAbsent
                        )
                    }
                }
            }
        }

        // 4. Filter Pills + Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tabs = listOf(
                        if (language == AppLanguage.HINDI) "सभी (${statements.size})" else "All (${statements.size})",
                        if (language == AppLanguage.HINDI) "बाकी हिसाब" else "Pending",
                        if (language == AppLanguage.HINDI) "पूर्ण / लॉक" else "Settled 🔒",
                        if (language == AppLanguage.HINDI) "इतिहास व ऑडिट" else "History & Audit"
                    )
                    items(tabs.size) { index ->
                        FilterChip(
                            selected = filterTab == index,
                            onClick = { filterTab = index },
                            label = { Text(tabs[index], fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ConstructionOrange,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("hisab_tab_$index")
                        )
                    }
                }

                if (filterTab != 3) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(if (language == AppLanguage.HINDI) "मज़दूर खोजें (नाम या ID)..." else "Search worker by name or ID...")
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 5. Tab Content: Worker Statements OR History & Audit
        if (filterTab == 3) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "मासिक व मज़दूर-वार हिसाब इतिहास" else "Worker & Monthly Settlement History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (historyWorkers.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = historyWorkerFilterId.isBlank(),
                                    onClick = { historyWorkerFilterId = "" },
                                    label = {
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "सभी मज़दूर" else "All Workers",
                                            fontSize = 11.5.sp
                                        )
                                    }
                                )
                            }
                            items(historyWorkers.size) { i ->
                                val (wId, wName) = historyWorkers[i]
                                FilterChip(
                                    selected = historyWorkerFilterId == wId,
                                    onClick = { historyWorkerFilterId = wId },
                                    label = { Text(wName, fontSize = 11.5.sp) }
                                )
                            }
                        }
                    }
                }
            }
            if (visibleHistory.isEmpty()) {
                item {
                    EmptyNoticeCard(
                        text = if (language == AppLanguage.HINDI) {
                            "अभी कोई लॉक या पूर्ण हिसाब इतिहास दर्ज नहीं है।"
                        } else {
                            "No completed Amavasya settlements recorded yet."
                        }
                    )
                }
            } else {
                items(visibleHistory, key = { it.id }) { item ->
                    val matchedWorker = statements.find { it.worker.id == item.workerId }?.worker
                    SettlementHistoryCard(
                        language = language,
                        settlement = item,
                        onJumpToDate = {
                            onSelectDate(item.periodEndDate, true)
                            filterTab = 0
                        },
                        onExportPdf = {
                            AmavasyaPdfHelper.exportSettlementHistoryPdf(context, item, matchedWorker)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = ConstructionOrange, modifier = Modifier.size(18.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "ऑडिट व सुधार लॉग (Audit Trail)" else "Settlement Audit History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (visibleAudits.isEmpty()) {
                item {
                    EmptyNoticeCard(
                        text = if (language == AppLanguage.HINDI) "अभी कोई ऑडिट लॉग नहीं है।" else "No audit events recorded yet."
                    )
                }
            } else {
                items(visibleAudits, key = { it.id }) { audit ->
                    AuditTrailCard(audit = audit)
                }
            }
        } else if (filteredStatements.isEmpty()) {
            item {
                EmptyNoticeCard(
                    text = if (language == AppLanguage.HINDI) {
                        "इस फ़िल्टर में कोई मज़दूर नहीं मिला।"
                    } else {
                        "No worker settlements match this filter."
                    }
                )
            }
        } else {
            items(filteredStatements, key = { it.worker.id }) { stmt ->
                WorkerHisabCard(
                    language = language,
                    statement = stmt,
                    onViewFull = { statementForDetail = stmt },
                    onRecordPayment = {
                        onRecordPaymentForWorker(stmt.worker.id, stmt.payableAmount, stmt.periodDisplay, stmt.periodEndDate)
                    },
                    onRecordAdvance = {
                        onRecordAdvanceForWorker(stmt.worker.id, stmt.periodEndDate)
                    },
                    onMarkComplete = { statementToSettle = stmt },
                    onAuthorizedCorrection = { statementToCorrect = stmt },
                    onSendWhatsApp = {
                        if (WorkerMessageHelper.isValidPhoneNumber(stmt.worker.mobile)) {
                            WorkerMessageHelper.openWhatsApp(
                                context,
                                stmt.worker.mobile,
                                WorkerMessageHelper.buildAmavasyaHisabMessage(stmt)
                            )
                        } else {
                            onUpdateWorkerPhone(stmt.worker.id)
                        }
                    },
                    onSendSms = {
                        if (WorkerMessageHelper.isValidPhoneNumber(stmt.worker.mobile)) {
                            WorkerMessageHelper.openSms(
                                context,
                                stmt.worker.mobile,
                                WorkerMessageHelper.buildAmavasyaHisabMessage(stmt)
                            )
                        } else {
                            onUpdateWorkerPhone(stmt.worker.id)
                        }
                    },
                    onExportPdf = {
                        AmavasyaPdfHelper.exportAndSharePdf(context, stmt)
                    },
                    onUpdatePhone = { onUpdateWorkerPhone(stmt.worker.id) }
                )
            }
        }
    }

    // Dialogs
    if (showDatePickerDialog) {
        AmavasyaDatePickerModal(
            initialIsoDate = selectedAmavasyaDate,
            language = language,
            onDateSelected = { pickedIso ->
                val isCalc = AmavasyaCalendar.findAmavasyaInfo(pickedIso) != null
                onSelectDate(pickedIso, !isCalc)
            },
            onDismiss = { showDatePickerDialog = false }
        )
    }

    statementForDetail?.let { stmt ->
        FullHisabStatementDialog(
            language = language,
            statement = stmt,
            onDismiss = { statementForDetail = null },
            onUpdateWorkerPhone = onUpdateWorkerPhone,
            onRecordPayment = {
                onRecordPaymentForWorker(stmt.worker.id, stmt.payableAmount, stmt.periodDisplay, stmt.periodEndDate)
            }
        )
    }

    statementToSettle?.let { stmt ->
        MarkSettlementDialog(
            language = language,
            statement = stmt,
            onDismiss = { statementToSettle = null },
            onRecordPaymentFirst = {
                onRecordPaymentForWorker(stmt.worker.id, stmt.payableAmount, stmt.periodDisplay, stmt.periodEndDate)
            },
            onConfirmSettlement = { carryForward, notes ->
                onMarkSettlementComplete(stmt, carryForward, notes)
            }
        )
    }

    statementToCorrect?.let { stmt ->
        AuthorizedCorrectionDialog(
            language = language,
            statement = stmt,
            onDismiss = { statementToCorrect = null },
            onConfirmCorrection = { reason, reopen ->
                onAuthorizedCorrection(stmt, reason, reopen)
            }
        )
    }
}

@Composable
private fun WorkerHisabCard(
    language: AppLanguage,
    statement: WorkerHisabStatement,
    onViewFull: () -> Unit,
    onRecordPayment: () -> Unit,
    onRecordAdvance: () -> Unit,
    onMarkComplete: () -> Unit,
    onAuthorizedCorrection: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onSendSms: () -> Unit,
    onExportPdf: () -> Unit,
    onUpdatePhone: () -> Unit
) {
    val hasPhone = WorkerMessageHelper.isValidPhoneNumber(statement.worker.mobile)
    val maskedPhone = WorkerMessageHelper.maskPhoneNumber(statement.worker.mobile)

    val (badgeBg, badgeFg) = when (statement.status) {
        HisabSettlementStatus.COMPLETED -> StatusPresent.copy(alpha = 0.14f) to StatusPresent
        HisabSettlementStatus.CARRIED_FORWARD -> Color(0xFF0284C7).copy(alpha = 0.14f) to Color(0xFF0284C7)
        HisabSettlementStatus.PARTIAL -> StatusHalfDay.copy(alpha = 0.15f) to StatusHalfDay
        HisabSettlementStatus.PENDING -> ConstructionOrange.copy(alpha = 0.14f) to ConstructionOrange
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().testTag("hisab_worker_card_${statement.worker.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Worker Header + Photo + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    WorkerAvatar(
                        workerName = statement.worker.name,
                        localPhotoPath = statement.worker.localPhotoPath,
                        photoUrl = statement.worker.photoUrl,
                        isActive = statement.worker.isActive,
                        size = 44.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = statement.worker.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${statement.worker.id} • ${statement.periodDisplay}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable(enabled = !hasPhone, onClick = onUpdatePhone)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = if (hasPhone) StatusPresent else StatusAbsent,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (hasPhone) maskedPhone else if (language == AppLanguage.HINDI) "फ़ोन जोड़ें" else "Add Phone",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (hasPhone) StatusPresent else StatusAbsent
                            )
                        }
                    }
                }

                Surface(color = badgeBg, shape = RoundedCornerShape(10.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (statement.isLocked) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = badgeFg, modifier = Modifier.size(12.dp))
                        }
                        Text(
                            text = if (language == AppLanguage.HINDI) statement.status.labelHi else statement.status.labelEn,
                            color = badgeFg,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Attendance & Financial Mini Metrics
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "P: ${statement.presentDays} • H: ${statement.halfDays} • Days: ${String.format("%.1f", statement.payableManDays)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "OT: ${String.format("%.1f", statement.totalOvertimeHours)}h (${Formatters.formatInr(statement.totalOvertimeAmount)})",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Earn: ${Formatters.formatInr(statement.totalEarnings)}", fontSize = 11.5.sp)
                        Text("Prev: ${Formatters.formatInr(statement.previousBalance)}", fontSize = 11.5.sp)
                        Text("Adv: ${Formatters.formatInr(statement.advancesTaken)}", fontSize = 11.5.sp, color = StatusHalfDay)
                        Text("Paid: ${Formatters.formatInr(statement.paymentsMade)}", fontSize = 11.5.sp, color = StatusPresent)
                    }
                }
            }

            // Clear Payable vs Outstanding Advance Strip
            val (balLabel, balValue, balColor) = when {
                statement.payableAmount > 0.01 -> Triple(
                    if (language == AppLanguage.HINDI) "देय राशि (Payable to Worker)" else "Final Payable (To Worker)",
                    Formatters.formatInr(statement.payableAmount),
                    ConstructionOrange
                )
                statement.outstandingAdvanceAmount > 0.01 -> Triple(
                    if (language == AppLanguage.HINDI) "बकाया एडवांस (Advance Due)" else "Advance Outstanding (Due)",
                    Formatters.formatInr(statement.outstandingAdvanceAmount),
                    StatusAbsent
                )
                else -> Triple(
                    if (language == AppLanguage.HINDI) "अंतिम शेष (Settled)" else "Final Balance",
                    "₹0",
                    StatusPresent
                )
            }

            Surface(
                color = balColor.copy(alpha = 0.09f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = balLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = balColor)
                    Text(text = balValue, fontSize = 17.sp, fontWeight = FontWeight.Black, color = balColor)
                }
            }

            // Action Grid: 6 Required Actions + PDF
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onViewFull,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "पूरा हिसाब" else "Full Hisab",
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onExportPdf,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = StatusAbsent, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("PDF", fontSize = 11.sp)
                }

                if (!statement.isLocked) {
                    Button(
                        onClick = onMarkComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusPresent),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("complete_hisab_${statement.worker.id}")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "पूर्ण करें" else "Settle",
                            fontSize = 11.sp,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onAuthorizedCorrection,
                        border = BorderStroke(1.dp, ConstructionOrange),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = ConstructionOrange)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "सुधार" else "Correct",
                            fontSize = 11.sp,
                            color = ConstructionOrange,
                            maxLines = 1
                        )
                    }
                }
            }

            if (!statement.isLocked) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onRecordPayment,
                        colors = ButtonDefaults.buttonColors(containerColor = ConstructionOrange),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "भुगतान दें" else "Pay Wages",
                            fontSize = 11.sp,
                            color = Color.White,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onRecordAdvance,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "एडवांस दें" else "Advance",
                            fontSize = 11.sp,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }

            // Direct WhatsApp + Normal SMS Send Buttons (automatically uses saved worker mobile number)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onSendWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("hisab_whatsapp_${statement.worker.id}")
                ) {
                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "WhatsApp हिसाब" else "WhatsApp Hisab",
                        fontSize = 11.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = onSendSms,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("hisab_sms_${statement.worker.id}")
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "SMS हिसाब" else "Normal SMS",
                        fontSize = 11.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AmavasyaMonthCalendarCard(
    language: AppLanguage,
    selectedIsoDate: String,
    todayIso: String,
    onDateClick: (isoDate: String, isAmavasya: Boolean) -> Unit
) {
    val initialCal = remember(selectedIsoDate) {
        AmavasyaCalendar.parseIsoToCalendar(selectedIsoDate) ?: Calendar.getInstance()
    }
    var displayYear by remember(selectedIsoDate) { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var displayMonth by remember(selectedIsoDate) { mutableIntStateOf(initialCal.get(Calendar.MONTH) + 1) }

    val amavasyasInMonth = remember(displayYear, displayMonth) {
        AmavasyaCalendar.getAmavasyaDatesForMonth(displayYear, displayMonth).associateBy { it.isoDate }
    }

    val monthStartCal = remember(displayYear, displayMonth) {
        Calendar.getInstance().apply {
            clear()
            set(displayYear, displayMonth - 1, 1)
        }
    }
    val firstDayOffset = monthStartCal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val daysInMonth = monthStartCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (displayMonth == 1) {
                            displayMonth = 12
                            displayYear--
                        } else {
                            displayMonth--
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                }

                Text(
                    text = AmavasyaCalendar.getMonthTitle(displayYear, displayMonth),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                IconButton(
                    onClick = {
                        if (displayMonth == 12) {
                            displayMonth = 1
                            displayYear++
                        } else {
                            displayMonth++
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                }
            }

            // Weekday Header
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val totalCells = firstDayOffset + daysInMonth
            val rows = (totalCells + 6) / 7
            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (c in 0 until 7) {
                        val cellIndex = r * 7 + c
                        val dayNum = cellIndex - firstDayOffset + 1
                        if (dayNum in 1..daysInMonth) {
                            val cellIso = String.format(Locale.US, "%04d-%02d-%02d", displayYear, displayMonth, dayNum)
                            val isSelected = cellIso == selectedIsoDate
                            val isToday = cellIso == todayIso
                            val amavasyaEntry = amavasyasInMonth[cellIso]
                            val isAmavasya = amavasyaEntry != null

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> ConstructionOrange
                                            isAmavasya -> ConstructionOrange.copy(alpha = 0.16f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .then(
                                        if (isToday && !isSelected) {
                                            Modifier.border(1.5.dp, CharcoalDark, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { onDateClick(cellIso, isAmavasya) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dayNum.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected || isAmavasya) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isAmavasya) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color.White else ConstructionOrange)
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (amavasyasInMonth.isNotEmpty()) {
                val info = amavasyasInMonth.values.first()
                Text(
                    text = "🌑 ${info.displayDate} — ${if (language == AppLanguage.HINDI) info.lunarNameHi else info.lunarNameEn}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ConstructionOrange
                )
            }
        }
    }
}

@Composable
private fun SettlementHistoryCard(
    language: AppLanguage,
    settlement: AmavasyaSettlementEntity,
    onJumpToDate: () -> Unit,
    onExportPdf: () -> Unit
) {
    val startText = if (settlement.periodStartDate.isNotBlank()) {
        Formatters.formatToIndianDate(settlement.periodStartDate)
    } else {
        "Start"
    }
    val endText = Formatters.formatToIndianDate(settlement.periodEndDate)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onJumpToDate)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = settlement.workerName.ifBlank { settlement.workerId }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "$startText - $endText", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onExportPdf,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export History PDF",
                            tint = StatusAbsent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Surface(
                        color = if (settlement.status == "COMPLETED") StatusPresent.copy(alpha = 0.14f) else ConstructionOrange.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = settlement.status,
                            color = if (settlement.status == "COMPLETED") StatusPresent else ConstructionOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Earn: ${Formatters.formatInr(settlement.totalEarnings)}", fontSize = 11.5.sp)
                Text("Paid: ${Formatters.formatInr(settlement.paymentsMade)}", fontSize = 11.5.sp, color = StatusPresent)
                Text("C/F: ${Formatters.formatInr(settlement.carryForwardBalance)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AuditTrailCard(audit: SettlementAuditEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${audit.workerName} • ${audit.action}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ConstructionOrange)
                Text(AmavasyaCalendar.formatTimestampIst(audit.timestamp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = audit.reason, fontSize = 11.5.sp)
            Text(
                text = "By: ${audit.performedBy} • Balance: ${Formatters.formatInr(audit.previousNetBalance)} -> ${Formatters.formatInr(audit.newNetBalance)}",
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyNoticeCard(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp),
            textAlign = TextAlign.Center
        )
    }
}
