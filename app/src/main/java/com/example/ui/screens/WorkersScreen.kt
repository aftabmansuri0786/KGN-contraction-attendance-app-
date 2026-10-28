package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.AdvanceEntity
import com.example.data.model.AttendanceEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.SiteEntity
import com.example.data.model.WorkCategories
import com.example.data.model.WorkerEntity
import com.example.ui.theme.ConstructionOrange
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusHalfDay
import com.example.ui.theme.StatusPresent
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.Formatters

@Composable
fun WorkersScreen(
    language: AppLanguage,
    workers: List<WorkerEntity>,
    sites: List<SiteEntity>,
    attendanceRecords: List<AttendanceEntity>,
    advances: List<AdvanceEntity>,
    payments: List<PaymentEntity>,
    onAddWorkerClick: () -> Unit,
    onEditWorkerClick: (WorkerEntity) -> Unit,
    onToggleWorkerStatus: (WorkerEntity) -> Unit,
    onRecordPaymentForWorker: (workerId: String) -> Unit,
    onRecordAdvanceForWorker: (workerId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedWorkerForProfile by remember { mutableStateOf<WorkerEntity?>(null) }

    val filteredWorkers = workers.filter { worker ->
        val matchesQuery = worker.name.contains(searchQuery, ignoreCase = true) ||
                worker.id.contains(searchQuery, ignoreCase = true) ||
                worker.mobile.contains(searchQuery)
        val matchesCategory = selectedCategory.isBlank() || worker.category == selectedCategory
        matchesQuery && matchesCategory
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        if (language == AppLanguage.HINDI) "मज़दूर का नाम या ID खोजें..." else "Search worker name or ID..."
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("search_workers_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory.isBlank(),
                        onClick = { selectedCategory = "" },
                        label = { Text(if (language == AppLanguage.HINDI) "सभी (All)" else "All") }
                    )
                }
                items(WorkCategories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.first,
                        onClick = {
                            selectedCategory = if (selectedCategory == cat.first) "" else cat.first
                        },
                        label = { Text(if (language == AppLanguage.HINDI) cat.second else cat.first) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Workers List
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
                        val siteName = sites.find { it.id == worker.siteId }?.name
                            ?: (if (language == AppLanguage.HINDI) "सामान्य (General)" else "General")

                        val workerAttendance = attendanceRecords.filter { it.workerId == worker.id }
                        val workerAdvances = advances.filter { it.workerId == worker.id }
                        val workerPayments = payments.filter { it.workerId == worker.id }

                        val totalEarned = workerAttendance.sumOf { it.calculatedEarnings }
                        val totalPaid = workerPayments.sumOf { it.amount }
                        val totalAdv = workerAdvances.sumOf { it.amount }
                        val netDue = (totalEarned - totalPaid).coerceAtLeast(0.0)

                        WorkerItemCard(
                            language = language,
                            worker = worker,
                            siteName = siteName,
                            netDue = netDue,
                            totalAdv = totalAdv,
                            onCardClick = { selectedWorkerForProfile = worker },
                            onEditClick = { onEditWorkerClick(worker) },
                            onCallClick = {
                                if (worker.mobile.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${worker.mobile}"))
                                    context.startActivity(intent)
                                }
                            },
                            onToggleStatus = { onToggleWorkerStatus(worker) }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add Worker
        FloatingActionButton(
            onClick = onAddWorkerClick,
            containerColor = ConstructionOrange,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_worker")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Worker")
        }
    }

    // Worker Profile & Ledger Modal Dialog
    if (selectedWorkerForProfile != null) {
        val worker = selectedWorkerForProfile!!
        val workerAttendance = attendanceRecords.filter { it.workerId == worker.id }
        val workerAdvances = advances.filter { it.workerId == worker.id }
        val workerPayments = payments.filter { it.workerId == worker.id }

        val totalDays = workerAttendance.sumOf { it.paidDayFraction }
        val totalOt = workerAttendance.sumOf { it.overtimeHours }
        val totalEarned = workerAttendance.sumOf { it.calculatedEarnings }
        val totalAdv = workerAdvances.sumOf { it.amount }
        val totalPaid = workerPayments.sumOf { it.amount }
        val netBalanceDue = totalEarned - totalPaid

        WorkerProfileLedgerDialog(
            language = language,
            worker = worker,
            totalDays = totalDays,
            totalOt = totalOt,
            totalEarned = totalEarned,
            totalAdv = totalAdv,
            totalPaid = totalPaid,
            netBalanceDue = netBalanceDue,
            recentAttendance = workerAttendance.take(5),
            onDismiss = { selectedWorkerForProfile = null },
            onMakePayment = {
                selectedWorkerForProfile = null
                onRecordPaymentForWorker(worker.id)
            },
            onGiveAdvance = {
                selectedWorkerForProfile = null
                onRecordAdvanceForWorker(worker.id)
            }
        )
    }
}

@Composable
fun WorkerItemCard(
    language: AppLanguage,
    worker: WorkerEntity,
    siteName: String,
    netDue: Double,
    totalAdv: Double,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onCallClick: () -> Unit,
    onToggleStatus: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("worker_card_${worker.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (worker.isActive) ConstructionOrange.copy(alpha = 0.15f) else Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = worker.name.take(1).uppercase(),
                            color = if (worker.isActive) ConstructionOrange else Color.DarkGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = worker.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (!worker.isActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${AppStrings.get("inactive", language)})",
                                    color = StatusAbsent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text(
                            text = "${worker.id} • ${worker.category} • $siteName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Wage: ${Formatters.formatInr(worker.dailyWage)}/day",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    if (totalAdv > 0) {
                        Text(
                            text = "Adv: ${Formatters.formatInr(totalAdv)}",
                            color = StatusHalfDay,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "बकाया मज़दूरी" else "Balance Due",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatInr(netDue),
                            fontWeight = FontWeight.Bold,
                            color = if (netDue > 0) StatusAbsent else StatusPresent,
                            fontSize = 15.sp
                        )
                    }

                    if (worker.mobile.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onCallClick,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(StatusPresent.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Call",
                                tint = StatusPresent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkerProfileLedgerDialog(
    language: AppLanguage,
    worker: WorkerEntity,
    totalDays: Double,
    totalOt: Double,
    totalEarned: Double,
    totalAdv: Double,
    totalPaid: Double,
    netBalanceDue: Double,
    recentAttendance: List<AttendanceEntity>,
    onDismiss: () -> Unit,
    onMakePayment: () -> Unit,
    onGiveAdvance: () -> Unit
) {
    val context = LocalContext.current

    val ledgerShareText = buildString {
        appendLine("=================================")
        appendLine("   KGN CONSTRUCTION - WORKER LEDGER")
        appendLine("=================================")
        appendLine("Worker: ${worker.name} (${worker.id})")
        appendLine("Skill: ${worker.category}")
        appendLine("Daily Wage: ${Formatters.formatInr(worker.dailyWage)}/day")
        appendLine("Total Days Worked: $totalDays days")
        appendLine("Total Overtime: $totalOt hrs")
        appendLine("Total Earned: ${Formatters.formatInr(totalEarned)}")
        appendLine("Total Advances: ${Formatters.formatInr(totalAdv)}")
        appendLine("Total Paid: ${Formatters.formatInr(totalPaid)}")
        appendLine("Net Balance Due: ${Formatters.formatInr(netBalanceDue)}")
        appendLine("=================================")
        appendLine("Authorized by: KGN Construction")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(AppStrings.get("worker_profile", language), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header with Worker Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ConstructionOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = worker.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(worker.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = "${worker.category} • ${worker.mobile.ifBlank { "No Mobile" }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Financial Summary Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(AppStrings.get("total_earned", language))
                            Text(Formatters.formatInr(totalEarned), fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(AppStrings.get("total_advances_given", language))
                            Text(Formatters.formatInr(totalAdv), color = StatusHalfDay, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(AppStrings.get("total_paid", language))
                            Text(Formatters.formatInr(totalPaid), color = StatusPresent, fontWeight = FontWeight.Bold)
                        }
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(AppStrings.get("balance_due", language), fontWeight = FontWeight.Bold)
                            Text(
                                text = Formatters.formatInr(netBalanceDue),
                                color = if (netBalanceDue > 0) StatusAbsent else StatusPresent,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                // Action Buttons: Pay Wages & Give Advance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onMakePayment,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusPresent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(AppStrings.get("make_payment", language), fontSize = 12.sp, color = Color.White)
                    }

                    Button(
                        onClick = onGiveAdvance,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(AppStrings.get("give_advance", language), fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Formatters.shareText(
                        context,
                        "KGN Construction - Ledger ${worker.name}",
                        ledgerShareText
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ConstructionOrange)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStrings.get("share_whatsapp", language), color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(if (language == AppLanguage.HINDI) "बंद करें" else "Close")
            }
        }
    )
}
