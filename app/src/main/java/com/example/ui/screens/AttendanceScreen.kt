package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import com.example.util.WorkerMessageHelper
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceEntity
import com.example.data.model.AttendanceStatus
import com.example.data.model.SiteEntity
import com.example.data.model.WorkerEntity
import com.example.ui.theme.ConstructionOrange
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusHalfDay
import com.example.ui.theme.StatusLeave
import com.example.ui.theme.StatusPresent
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.Formatters
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    language: AppLanguage,
    selectedDate: String,
    selectedSiteId: String,
    sites: List<SiteEntity>,
    workers: List<WorkerEntity>,
    attendanceRecords: List<AttendanceEntity>,
    onDateChanged: (String) -> Unit,
    onSiteChanged: (String) -> Unit,
    onMarkAllPresent: (siteId: String, date: String) -> Unit,
    onSaveSingleAttendance: (workerId: String, siteId: String, date: String, status: AttendanceStatus, otHours: Double, remarks: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var siteMenuExpanded by remember { mutableStateOf(false) }

    val filteredWorkers = workers.filter { worker ->
        worker.isActive && (selectedSiteId.isBlank() || worker.siteId == selectedSiteId || worker.siteId.isBlank())
    }

    // Date navigation helper
    fun shiftDate(days: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            val date = sdf.parse(selectedDate) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.DAY_OF_YEAR, days)
            }
            onDateChanged(sdf.format(cal.time))
        } catch (e: Exception) {
            // fallback
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Date Bar with Prev, Today, Next controls
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("attendance_date_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { shiftDate(-1) },
                    modifier = Modifier.testTag("prev_date_btn")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous Day")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onDateChanged(Formatters.getTodayIsoDate()) }
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = ConstructionOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = Formatters.formatToIndianDate(selectedDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedDate == Formatters.getTodayIsoDate()) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "आज (Today)" else "Today",
                                style = MaterialTheme.typography.labelSmall,
                                color = ConstructionOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { shiftDate(1) },
                    modifier = Modifier.testTag("next_date_btn")
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next Day")
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Site Filter & Mark All Present Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Site Selector Dropdown
            ExposedDropdownMenuBox(
                expanded = siteMenuExpanded,
                onExpandedChange = { siteMenuExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                val currentSiteName = sites.find { it.id == selectedSiteId }?.name
                    ?: AppStrings.get("all_sites", language)

                OutlinedTextField(
                    value = currentSiteName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(AppStrings.get("select_site", language)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded = siteMenuExpanded,
                    onDismissRequest = { siteMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(AppStrings.get("all_sites", language)) },
                        onClick = {
                            onSiteChanged("")
                            siteMenuExpanded = false
                        }
                    )
                    sites.forEach { site ->
                        DropdownMenuItem(
                            text = { Text(site.name) },
                            onClick = {
                                onSiteChanged(site.id)
                                siteMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Quick "Mark All Present" Button
            Button(
                onClick = {
                    onMarkAllPresent(selectedSiteId, selectedDate)
                    Toast.makeText(
                        context,
                        if (language == AppLanguage.HINDI) "सभी मज़दूर उपस्थित दर्ज किए गए" else "All workers marked present",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusPresent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(56.dp).testTag("mark_all_present_btn")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (language == AppLanguage.HINDI) "सब उपस्थित" else "Mark All",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Worker Attendance List
        if (filteredWorkers.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = AppStrings.get("no_workers_found", language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredWorkers, key = { it.id }) { worker ->
                    val existingRecord = attendanceRecords.find {
                        it.workerId == worker.id && it.date == selectedDate
                    }

                    WorkerAttendanceCard(
                        language = language,
                        worker = worker,
                        existingRecord = existingRecord,
                        onStatusSelected = { status, otHours ->
                            val siteToUse = if (selectedSiteId.isNotBlank()) selectedSiteId else worker.siteId
                            onSaveSingleAttendance(
                                worker.id,
                                siteToUse,
                                selectedDate,
                                status,
                                otHours,
                                ""
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkerAttendanceCard(
    language: AppLanguage,
    worker: WorkerEntity,
    existingRecord: AttendanceEntity?,
    onStatusSelected: (status: AttendanceStatus, otHours: Double) -> Unit
) {
    var otHours by remember(existingRecord?.overtimeHours) {
        mutableDoubleStateOf(existingRecord?.overtimeHours ?: 0.0)
    }

    val currentStatus = existingRecord?.status?.let { AttendanceStatus.fromString(it) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("worker_attendance_card_${worker.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Worker Header: Name, Skill badge, Daily wage rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = worker.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = worker.category,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        val hasPhone = WorkerMessageHelper.isValidPhoneNumber(worker.mobile)
                        val maskedPhone = WorkerMessageHelper.maskPhoneNumber(worker.mobile)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = if (hasPhone) StatusPresent.copy(alpha = 0.12f) else StatusAbsent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = if (hasPhone) Icons.Default.Phone else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (hasPhone) StatusPresent else StatusAbsent,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = maskedPhone,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (hasPhone) StatusPresent else StatusAbsent
                                )
                            }
                        }
                    }

                    Text(
                        text = "Wage: ${Formatters.formatInr(worker.dailyWage)}/day  •  OT: ${Formatters.formatInr(worker.overtimeHourlyRate)}/hr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // If marked, show calculated earnings snapshot
                if (existingRecord != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = Formatters.formatInr(existingRecord.calculatedEarnings),
                            color = StatusPresent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (language == AppLanguage.HINDI) "कमाई" else "Earned",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Attendance Status Chips (P, HD, A, Leave)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusChip(
                    label = if (language == AppLanguage.HINDI) "उपस्थित (1.0)" else "Present",
                    isSelected = currentStatus == AttendanceStatus.PRESENT,
                    color = StatusPresent,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusSelected(AttendanceStatus.PRESENT, otHours) }
                )

                StatusChip(
                    label = if (language == AppLanguage.HINDI) "आधा दिन (0.5)" else "Half Day",
                    isSelected = currentStatus == AttendanceStatus.HALF_DAY,
                    color = StatusHalfDay,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusSelected(AttendanceStatus.HALF_DAY, otHours) }
                )

                StatusChip(
                    label = if (language == AppLanguage.HINDI) "अनुपस्थित (0)" else "Absent",
                    isSelected = currentStatus == AttendanceStatus.ABSENT,
                    color = StatusAbsent,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusSelected(AttendanceStatus.ABSENT, 0.0) }
                )

                StatusChip(
                    label = if (language == AppLanguage.HINDI) "छुट्टी" else "Leave",
                    isSelected = currentStatus == AttendanceStatus.PAID_LEAVE,
                    color = StatusLeave,
                    modifier = Modifier.weight(0.9f),
                    onClick = { onStatusSelected(AttendanceStatus.PAID_LEAVE, 0.0) }
                )
            }

            // Overtime Stepper (if present or half-day)
            if (currentStatus == AttendanceStatus.PRESENT || currentStatus == AttendanceStatus.HALF_DAY) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "ओवरटाइम घंटे (OT Hours):" else "Overtime Hours:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (otHours > 0) {
                                    otHours = (otHours - 0.5).coerceAtLeast(0.0)
                                    onStatusSelected(currentStatus, otHours)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease OT", modifier = Modifier.size(16.dp))
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${otHours}h",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 13.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                otHours += 0.5
                                onStatusSelected(currentStatus, otHours)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase OT", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color else color.copy(alpha = 0.10f),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (isSelected) Color.White else color,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}
