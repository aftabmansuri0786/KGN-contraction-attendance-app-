package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdvanceEntity
import com.example.data.model.AttendanceEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.SiteEntity
import com.example.data.model.WorkerEntity
import com.example.ui.theme.ConstructionOrange
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusHalfDay
import com.example.ui.theme.StatusPresent
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.Formatters

@Composable
fun ReportsScreen(
    language: AppLanguage,
    workers: List<WorkerEntity>,
    sites: List<SiteEntity>,
    attendanceRecords: List<AttendanceEntity>,
    advances: List<AdvanceEntity>,
    payments: List<PaymentEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("Month") } // "Today", "Week", "Month", "All"

    val today = Formatters.getTodayIsoDate()
    val monthPrefix = today.substring(0, 7)

    val filteredAttendance = when (selectedFilter) {
        "Today" -> attendanceRecords.filter { it.date == today }
        "Month" -> attendanceRecords.filter { it.date.startsWith(monthPrefix) }
        else -> attendanceRecords
    }

    val totalDays = filteredAttendance.sumOf { it.paidDayFraction }
    val totalOt = filteredAttendance.sumOf { it.overtimeHours }
    val totalWagesEarned = filteredAttendance.sumOf { it.calculatedEarnings }
    val totalAdvancesGiven = advances.sumOf { it.amount }
    val totalPaid = payments.sumOf { it.amount }
    val netOutstandingWages = (totalWagesEarned - totalPaid).coerceAtLeast(0.0)

    val generateCsvContent = {
        buildString {
            appendLine("KGN CONSTRUCTION - ATTENDANCE & LABOUR REPORT")
            appendLine("Generated Date,${Formatters.formatToIndianDate(today)}")
            appendLine("Period,$selectedFilter")
            appendLine("")
            appendLine("Date,Worker ID,Worker Name,Skill,Site,Status,Day Fraction,Daily Wage (INR),OT Hours,OT Rate (INR),Total Earnings (INR)")
            filteredAttendance.forEach { att ->
                val worker = workers.find { it.id == att.workerId }
                val site = sites.find { it.id == att.siteId }
                appendLine("${Formatters.formatToIndianDate(att.date)},${att.workerId},\"${worker?.name ?: ""}\",\"${worker?.category ?: ""}\",\"${site?.name ?: ""}\",${att.status},${att.paidDayFraction},${att.dailyWageSnapshot},${att.overtimeHours},${att.overtimeRateSnapshot},${att.calculatedEarnings}")
            }
            appendLine("")
            appendLine("Summary Statistics")
            appendLine("Total Man-Days Worked,$totalDays")
            appendLine("Total Overtime Hours,$totalOt")
            appendLine("Total Wages Earned (INR),$totalWagesEarned")
            appendLine("Total Advances Given (INR),$totalAdvancesGiven")
            appendLine("Total Wages Paid (INR),$totalPaid")
            appendLine("Net Balance Due (INR),$netOutstandingWages")
        }
    }

    val generateWhatsAppSummary = {
        buildString {
            appendLine("🏗 *KGN CONSTRUCTION - SUMMARY REPORT*")
            appendLine("📅 *Period:* $selectedFilter (${Formatters.formatToIndianDate(today)})")
            appendLine("---------------------------------")
            appendLine("👷 Total Active Workers: ${workers.size}")
            appendLine("⏱ Man-Days Worked: $totalDays days")
            appendLine("⏳ Total Overtime: $totalOt hrs")
            appendLine("💰 Total Wages Earned: ${Formatters.formatInr(totalWagesEarned)}")
            appendLine("💵 Total Advances Given: ${Formatters.formatInr(totalAdvancesGiven)}")
            appendLine("✅ Total Paid: ${Formatters.formatInr(totalPaid)}")
            appendLine("⚠️ Net Outstanding Wages: ${Formatters.formatInr(netOutstandingWages)}")
            appendLine("---------------------------------")
            appendLine("Authorized by: KGN Construction")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Filter Chips (Today, This Month, All Time)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedFilter == "Today",
                        onClick = { selectedFilter = "Today" },
                        label = { Text(if (language == AppLanguage.HINDI) "आज (Today)" else "Today") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "Month",
                        onClick = { selectedFilter = "Month" },
                        label = { Text(AppStrings.get("this_month", language)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "All",
                        onClick = { selectedFilter = "All" },
                        label = { Text(AppStrings.get("all_time", language)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Export Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    Formatters.shareText(
                        context,
                        "KGN Construction Summary Report",
                        generateWhatsAppSummary()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ConstructionOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("share_report_btn")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStrings.get("share_report", language), fontSize = 12.sp, color = Color.White)
            }

            OutlinedButton(
                onClick = {
                    Formatters.shareText(
                        context,
                        "KGN_Attendance_Report.csv",
                        generateCsvContent()
                    )
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("export_csv_btn")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStrings.get("export_csv", language), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Financial Metrics Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (language == AppLanguage.HINDI) "रिपोर्ट सारांश / Financial Overview" else "Financial Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = if (language == AppLanguage.HINDI) "कुल कार्य दिवस (Man-Days):" else "Man-Days Worked:")
                    Text(text = "$totalDays days", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = if (language == AppLanguage.HINDI) "कुल ओवरटाइम घंटे:" else "Total OT Hours:")
                    Text(text = "$totalOt hrs", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = if (language == AppLanguage.HINDI) "कुल अर्जित मज़दूरी:" else "Total Wages Earned:")
                    Text(
                        text = Formatters.formatInr(totalWagesEarned),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = if (language == AppLanguage.HINDI) "कुल एडवांस दिया गया:" else "Total Advances Given:")
                    Text(
                        text = Formatters.formatInr(totalAdvancesGiven),
                        fontWeight = FontWeight.Bold,
                        color = StatusHalfDay
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = if (language == AppLanguage.HINDI) "कुल भुगतान किया गया:" else "Total Paid:")
                    Text(
                        text = Formatters.formatInr(totalPaid),
                        fontWeight = FontWeight.Bold,
                        color = StatusPresent
                    )
                }

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "शुद्ध बकाया मज़दूरी (Net Due):" else "Net Balance Due:",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = Formatters.formatInr(netOutstandingWages),
                        fontWeight = FontWeight.Black,
                        color = if (netOutstandingWages > 0) StatusAbsent else StatusPresent,
                        fontSize = 17.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (language == AppLanguage.HINDI) "हाज़िरी रजिस्टर विवरण / Attendance Log" else "Attendance Log",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Attendance Log
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredAttendance) { att ->
                val worker = workers.find { it.id == att.workerId }
                val site = sites.find { it.id == att.siteId }
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
                                text = worker?.name ?: "Worker ${att.workerId}",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${Formatters.formatToIndianDate(att.date)} • ${site?.name ?: "General"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${att.status} (${att.paidDayFraction}d)",
                                fontWeight = FontWeight.Bold,
                                color = if (att.paidDayFraction > 0) StatusPresent else StatusAbsent,
                                fontSize = 13.sp
                            )
                            Text(
                                text = Formatters.formatInr(att.calculatedEarnings),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
