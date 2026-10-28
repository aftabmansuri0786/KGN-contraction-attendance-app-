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
import com.example.data.model.AttendanceEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.WorkerEntity
import com.example.ui.theme.CharcoalDark
import com.example.ui.theme.ConstructionOrange
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusHalfDay
import com.example.ui.theme.StatusPresent
import com.example.ui.viewmodel.DashboardMetrics
import com.example.util.AdMobBanner
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.Formatters

@Composable
fun DashboardScreen(
    language: AppLanguage,
    metrics: DashboardMetrics,
    workers: List<WorkerEntity>,
    recentAttendance: List<AttendanceEntity>,
    recentPayments: List<PaymentEntity>,
    onNavigateToAttendance: () -> Unit,
    onNavigateToWorkers: () -> Unit,
    onNavigateToSites: () -> Unit,
    onNavigateToWages: () -> Unit,
    onNavigateToReports: () -> Unit,
    onAddWorkerClick: () -> Unit,
    onAddSiteClick: () -> Unit,
    onRecordAdvanceClick: () -> Unit,
    onRecordPaymentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            }
        }

        // Financial Highlights Strip
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
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

                    Column(horizontalAlignment = Alignment.End) {
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
                }
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
