package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.remember
import com.example.data.model.AttendanceEntity
import com.example.data.model.MonthlyPdfEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.WorkerEntity
import com.example.ui.theme.CharcoalDark
import com.example.ui.theme.ConstructionOrange
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusHalfDay
import com.example.ui.theme.StatusPresent
import com.example.ui.viewmodel.DashboardMetrics
import com.example.util.AdMobBanner
import com.example.util.AmavasyaDashboardSummary
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.AttendanceReminderManager
import com.example.util.Formatters

@Composable
fun DashboardScreen(
    language: AppLanguage,
    metrics: DashboardMetrics,
    amavasyaSummary: AmavasyaDashboardSummary,
    workers: List<WorkerEntity>,
    recentAttendance: List<AttendanceEntity>,
    recentPayments: List<PaymentEntity>,
    monthlyPdfs: List<MonthlyPdfEntity> = emptyList(),
    onNavigateToAttendance: () -> Unit,
    onNavigateToPendingAttendance: () -> Unit = onNavigateToAttendance,
    onNavigateToWorkers: () -> Unit,
    onNavigateToSites: () -> Unit,
    onNavigateToWages: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToAmavasya: () -> Unit,
    onNavigateToMonthlyPdfs: () -> Unit = {},
    onAddWorkerClick: () -> Unit,
    onAddSiteClick: () -> Unit,
    onRecordAdvanceClick: () -> Unit,
    onRecordPaymentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayIso = remember { Formatters.getTodayIsoDate() }
    val activeWorkersList = remember(workers) { workers.filter { it.isActive } }
    val todayRecords = remember(recentAttendance, todayIso) { recentAttendance.filter { it.date == todayIso } }
    val pendingTodayWorkers = remember(activeWorkersList, todayRecords) {
        AttendanceReminderManager.findPendingAttendanceWorkers(activeWorkersList, todayRecords)
    }
    val missingPhotosCount = remember(activeWorkersList) {
        activeWorkersList.count { it.localPhotoPath.isBlank() && it.photoUrl.isBlank() }
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Today's Hero Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("dashboard_hero_card")
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ConstructionOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Engineering,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "KGN CONSTRUCTION",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = Formatters.formatToIndianDate(Formatters.getTodayIsoDate()),
                                    color = Color(0xFFB0BEC5),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Today's Labour Cost Badge
                        Surface(
                            color = Color(0xFF2C3033),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = AppStrings.get("today_cost", language),
                                    color = Color(0xFF90A4AE),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = Formatters.formatInr(metrics.todayLabourCost),
                                    color = ConstructionOrange,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    Divider(color = Color(0xFF37474F), thickness = 0.8.dp)

                    // 4 Core Live Attendance Counters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AttendanceCountItem(
                            count = metrics.presentToday.toString(),
                            label = AppStrings.get("present", language),
                            color = StatusPresent
                        )
                        AttendanceCountItem(
                            count = metrics.halfDayToday.toString(),
                            label = AppStrings.get("half_day", language),
                            color = StatusHalfDay
                        )
                        AttendanceCountItem(
                            count = metrics.absentToday.toString(),
                            label = AppStrings.get("absent", language),
                            color = StatusAbsent
                        )
                        AttendanceCountItem(
                            count = metrics.totalActiveWorkers.toString(),
                            label = AppStrings.get("active_workers", language),
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Today's Attendance Completion & Pending Workers Card
        item {
            TodayAttendanceCompletionCard(
                language = language,
                totalActive = activeWorkersList.size,
                pendingWorkers = pendingTodayWorkers,
                onClick = onNavigateToPendingAttendance
            )
        }

        // Next Amavasya Hisab Dashboard Card
        item {
            NextAmavasyaHisabCard(
                language = language,
                summary = amavasyaSummary,
                onClick = onNavigateToAmavasya
            )
        }

        // 4 Core Structure Cards (as per user attachment mockup)
        item {
            Text(
                text = if (language == AppLanguage.HINDI) "मुख्य प्रबंधन / Core Sections" else "Core Sections",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StructureCard(
                    title = if (language == AppLanguage.HINDI) "Workers" else "Workers",
                    subtitle = if (language == AppLanguage.HINDI) "नाम, हुनर और दिहाड़ी" else "Name, skill & rate",
                    count = "${metrics.totalActiveWorkers} Active",
                    icon = Icons.Default.Group,
                    accentColor = Color(0xFFE65100),
                    onClick = onNavigateToWorkers,
                    modifier = Modifier.weight(1f)
                )

                StructureCard(
                    title = if (language == AppLanguage.HINDI) "Attendance" else "Attendance",
                    subtitle = if (language == AppLanguage.HINDI) "हाज़िरी, हाफ़-डे, गैरहाज़िर" else "Present, absent, half-day",
                    count = "${metrics.presentToday} Present",
                    icon = Icons.Default.DateRange,
                    accentColor = StatusPresent,
                    onClick = onNavigateToAttendance,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StructureCard(
                    title = if (language == AppLanguage.HINDI) "Wages & Advances" else "Wages & Advances",
                    subtitle = if (language == AppLanguage.HINDI) "ओवरटाइम, भुगतान, बकाया" else "Overtime, payment, balance",
                    count = Formatters.formatInr(metrics.totalUnpaidWages),
                    icon = Icons.Default.CurrencyRupee,
                    accentColor = Color(0xFF0284C7),
                    onClick = onNavigateToWages,
                    modifier = Modifier.weight(1f)
                )

                StructureCard(
                    title = if (language == AppLanguage.HINDI) "Construction Sites" else "Sites",
                    subtitle = if (language == AppLanguage.HINDI) "साइट-वार हाज़िरी व खर्च" else "Site-wise labour expense",
                    count = "${metrics.activeSitesCount} Active",
                    icon = Icons.Default.Business,
                    accentColor = Color(0xFF7C3AED),
                    onClick = onNavigateToSites,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = AppStrings.get("quick_actions", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.CheckCircle,
                        label = AppStrings.get("action_mark_att", language),
                        color = StatusPresent,
                        onClick = onNavigateToAttendance,
                        modifier = Modifier.weight(1f).testTag("quick_action_mark_attendance")
                    )

                    QuickActionButton(
                        icon = Icons.Default.Add,
                        label = AppStrings.get("action_add_worker", language),
                        color = ConstructionOrange,
                        onClick = onAddWorkerClick,
                        modifier = Modifier.weight(1f).testTag("quick_action_add_worker")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Payment,
                        label = AppStrings.get("action_record_payment", language),
                        color = Color(0xFF0284C7),
                        onClick = onRecordPaymentClick,
                        modifier = Modifier.weight(1f).testTag("quick_action_record_payment")
                    )

                    QuickActionButton(
                        icon = Icons.Default.HourglassBottom,
                        label = AppStrings.get("action_record_advance", language),
                        color = Color(0xFFD97706),
                        onClick = onRecordAdvanceClick,
                        modifier = Modifier.weight(1f).testTag("quick_action_give_advance")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Business,
                        label = AppStrings.get("action_add_site", language),
                        color = Color(0xFF6B7280),
                        onClick = onAddSiteClick,
                        modifier = Modifier.weight(1f).testTag("quick_action_add_site")
                    )

                    QuickActionButton(
                        icon = Icons.Default.ReceiptLong,
                        label = AppStrings.get("action_view_reports", language),
                        color = Color(0xFF4F46E5),
                        onClick = onNavigateToReports,
                        modifier = Modifier.weight(1f).testTag("quick_action_reports")
                    )
                }

                QuickActionButton(
                    icon = Icons.Default.NightsStay,
                    label = AppStrings.get("amavasya_menu", language),
                    color = ConstructionOrange,
                    onClick = onNavigateToAmavasya,
                    modifier = Modifier.fillMaxWidth().testTag("quick_action_amavasya_hisab")
                )
            }
        }

        // Monthly Total Wages & Outstanding Balance Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onNavigateToWages)
                    .testTag("dashboard_monthly_wages_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = AppStrings.get("month_earned", language),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatInr(metrics.monthEarnedWages),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = AppStrings.get("total_advances", language),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatInr(metrics.totalPendingAdvances),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = StatusHalfDay
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = AppStrings.get("unpaid_wages", language),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatInr(metrics.totalUnpaidWages),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (metrics.totalUnpaidWages > 0.01) ConstructionOrange else StatusPresent
                            )
                        }
                    }
                }
            }
        }

        // Latest Monthly PDF Reports & Worker Photo Status Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LatestMonthlyPdfsCard(
                    language = language,
                    monthlyPdfs = monthlyPdfs,
                    onClick = onNavigateToMonthlyPdfs,
                    modifier = Modifier.weight(1f)
                )

                WorkerPhotoStatusCard(
                    language = language,
                    totalActiveWorkers = activeWorkersList.size,
                    missingPhotosCount = missingPhotosCount,
                    onClick = onNavigateToWorkers,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent Payments / Activity
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (language == AppLanguage.HINDI) "हाल के भुगतान / Recent Payments" else "Recent Payments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (language == AppLanguage.HINDI) "सब देखें" else "View All",
                    color = ConstructionOrange,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToWages() }
                )
            }
        }

        if (recentPayments.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(
                        text = AppStrings.get("no_transactions", language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(recentPayments.take(3)) { payment ->
                val worker = workers.find { it.id == payment.workerId }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = worker?.name ?: "Worker ${payment.workerId}",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${Formatters.formatToIndianDate(payment.date)} • ${payment.paymentMethod}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = Formatters.formatInr(payment.amount),
                            fontWeight = FontWeight.Bold,
                            color = StatusPresent,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // Adaptive AdMob Banner at bottom of Dashboard
        item {
            Spacer(modifier = Modifier.height(10.dp))
            AdMobBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun AttendanceCountItem(
    count: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            text = label,
            color = Color(0xFFB0BEC5),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun StructureCard(
    title: String,
    subtitle: String,
    count: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = count,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = accentColor
                )
            }

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun NextAmavasyaHisabCard(
    language: AppLanguage,
    summary: AmavasyaDashboardSummary,
    onClick: () -> Unit
) {
    val daysBadgeText = when {
        summary.daysRemaining == 0L -> if (language == AppLanguage.HINDI) "आज हिसाब का दिन" else "Today"
        summary.daysRemaining > 0L -> if (language == AppLanguage.HINDI) "${summary.daysRemaining} दिन शेष" else "${summary.daysRemaining} Days Left"
        else -> if (language == AppLanguage.HINDI) "${-summary.daysRemaining} दिन बीत गए" else "${-summary.daysRemaining} Days Overdue"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (summary.isReminderActive) 1.5.dp else 1.dp,
            color = if (summary.isReminderActive) ConstructionOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("dashboard_next_amavasya_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ConstructionOrange.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (summary.isReminderActive) Icons.Default.NotificationsActive else Icons.Default.NightsStay,
                            contentDescription = null,
                            tint = ConstructionOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = AppStrings.get("next_amavasya_hisab", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (language == AppLanguage.HINDI) summary.lunarNameHi else summary.lunarNameEn,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (summary.daysRemaining <= 3) ConstructionOrange else CharcoalDark,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = daysBadgeText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (language == AppLanguage.HINDI) "अगली तारीख" else "Settlement Date",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = summary.nextSettlementDateDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "बाकी मज़दूर" else "Workers Pending",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = summary.workersRequiringSettlement.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (summary.workersRequiringSettlement > 0) ConstructionOrange else StatusPresent
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = when {
                            summary.totalPayableAmount > 0.01 -> if (language == AppLanguage.HINDI) "कुल देय राशि" else "Total Payable"
                            summary.totalAdvanceOutstanding > 0.01 -> if (language == AppLanguage.HINDI) "बकाया एडवांस" else "Adv Outstanding"
                            else -> AppStrings.get("total_outstanding", language)
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Formatters.formatInr(summary.totalOutstandingCombined),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = when {
                            summary.totalPayableAmount > 0.01 -> ConstructionOrange
                            summary.totalAdvanceOutstanding > 0.01 -> StatusAbsent
                            else -> StatusPresent
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (language == AppLanguage.HINDI) "पूरा हिसाब खोलें" else "Open Amavasya Hisab",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConstructionOrange
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = ConstructionOrange,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun TodayAttendanceCompletionCard(
    language: AppLanguage,
    totalActive: Int,
    pendingWorkers: List<WorkerEntity>,
    onClick: () -> Unit
) {
    val markedCount = (totalActive - pendingWorkers.size).coerceAtLeast(0)
    val progress = if (totalActive > 0) (markedCount.toFloat() / totalActive.toFloat()).coerceIn(0f, 1f) else 1f
    val isComplete = totalActive == 0 || pendingWorkers.isEmpty()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isComplete) StatusPresent.copy(alpha = 0.45f) else ConstructionOrange.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("dashboard_today_attendance_completion_card")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (isComplete) StatusPresent else ConstructionOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (language == AppLanguage.HINDI) "आज की हाज़िरी प्रगति" else "Today's Attendance",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Surface(
                    color = if (isComplete) StatusPresent.copy(alpha = 0.14f) else ConstructionOrange.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isComplete) {
                            if (language == AppLanguage.HINDI) "पूर्ण ($markedCount/$totalActive)" else "Complete ($markedCount/$totalActive)"
                        } else {
                            if (language == AppLanguage.HINDI) "${pendingWorkers.size} बाकी ($markedCount/$totalActive)" else "${pendingWorkers.size} Pending ($markedCount/$totalActive)"
                        },
                        color = if (isComplete) StatusPresent else ConstructionOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                color = if (isComplete) StatusPresent else ConstructionOrange,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            if (pendingWorkers.isNotEmpty()) {
                val names = pendingWorkers.take(3).joinToString(", ") { it.name } +
                    if (pendingWorkers.size > 3) " +${pendingWorkers.size - 3}" else ""
                Text(
                    text = if (language == AppLanguage.HINDI) {
                        "बाकी मज़दूर: $names • 6:30 PM रिमाइंडर सक्रिय"
                    } else {
                        "Pending: $names • Tap to mark"
                    },
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = if (language == AppLanguage.HINDI) {
                        "सभी सक्रिय मज़दूरों की आज की हाज़िरी दर्ज हो चुकी है।"
                    } else {
                        "All active workers have attendance recorded for today."
                    },
                    fontSize = 11.5.sp,
                    color = StatusPresent
                )
            }
        }
    }
}

@Composable
private fun LatestMonthlyPdfsCard(
    language: AppLanguage,
    monthlyPdfs: List<MonthlyPdfEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latestMonthLabel = monthlyPdfs.maxByOrNull { it.generatedAt }?.monthLabel ?: "--"
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("dashboard_latest_pdfs_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ConstructionOrange.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = ConstructionOrange,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Text(
                    text = "${monthlyPdfs.size} PDFs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = ConstructionOrange
                )
            }
            Text(
                text = AppStrings.get("monthly_pdfs_menu", language),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1
            )
            Text(
                text = if (monthlyPdfs.isEmpty()) {
                    if (language == AppLanguage.HINDI) "कोई मासिक PDF नहीं" else "No PDFs saved yet"
                } else {
                    if (language == AppLanguage.HINDI) "नवीनतम: $latestMonthLabel" else "Latest: $latestMonthLabel"
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WorkerPhotoStatusCard(
    language: AppLanguage,
    totalActiveWorkers: Int,
    missingPhotosCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (missingPhotosCount > 0) StatusHalfDay else StatusPresent
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("dashboard_worker_photos_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Text(
                    text = "$totalActiveWorkers Active",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = accent
                )
            }
            Text(
                text = if (language == AppLanguage.HINDI) "मज़दूर प्रोफ़ाइल फ़ोटो" else "Worker Photos",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1
            )
            Text(
                text = if (missingPhotosCount > 0) {
                    if (language == AppLanguage.HINDI) "$missingPhotosCount मज़दूरों की फ़ोटो बाकी" else "$missingPhotosCount missing photo"
                } else {
                    if (language == AppLanguage.HINDI) "सभी फ़ोटो अपडेट हैं" else "All photos uploaded"
                },
                fontSize = 11.sp,
                color = accent,
                maxLines = 1
            )
        }
    }
}


