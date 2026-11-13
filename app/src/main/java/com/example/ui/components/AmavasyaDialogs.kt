package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.example.ui.theme.CharcoalDark
import com.example.ui.theme.ConstructionOrange
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusHalfDay
import com.example.ui.theme.StatusPresent
import com.example.util.AmavasyaCalendar
import com.example.util.AmavasyaPdfHelper
import com.example.util.AppLanguage
import com.example.util.Formatters
import com.example.util.HisabSettlementStatus
import com.example.util.WorkerHisabStatement
import com.example.util.WorkerMessageHelper

@Composable
fun FullHisabStatementDialog(
    language: AppLanguage,
    statement: WorkerHisabStatement,
    onDismiss: () -> Unit,
    onUpdateWorkerPhone: (workerId: String) -> Unit,
    onRecordPayment: () -> Unit
) {
    val context = LocalContext.current
    val hasValidPhone = WorkerMessageHelper.isValidPhoneNumber(statement.worker.mobile)
    val maskedPhone = WorkerMessageHelper.maskPhoneNumber(statement.worker.mobile)
    val preparedMessage = remember(statement) {
        WorkerMessageHelper.buildAmavasyaHisabMessage(statement)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                WorkerAvatar(
                    workerName = statement.worker.name,
                    localPhotoPath = statement.worker.localPhotoPath,
                    photoUrl = statement.worker.photoUrl,
                    isActive = statement.worker.isActive,
                    size = 44.dp
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "अमावस्या पूरा हिसाब" else "Full Monthly Hisab",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        if (statement.isLocked) {
                            Surface(
                                color = StatusPresent.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = StatusPresent, modifier = Modifier.size(12.dp))
                                    Text("Locked", fontSize = 11.sp, color = StatusPresent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Text(
                        text = "${statement.worker.name} (${statement.worker.id}) • ${statement.periodDisplay}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Saved Mobile Confirmation Strip
                Surface(
                    color = if (hasValidPhone) MaterialTheme.colorScheme.surfaceVariant else StatusAbsent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (hasValidPhone) Icons.Default.Phone else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (hasValidPhone) StatusPresent else StatusAbsent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (hasValidPhone) "Mobile: $maskedPhone" else "Mobile number missing",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (hasValidPhone) MaterialTheme.colorScheme.onSurface else StatusAbsent
                            )
                        }
                        if (!hasValidPhone) {
                            TextButton(
                                onClick = {
                                    onDismiss()
                                    onUpdateWorkerPhone(statement.worker.id)
                                },
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Add Phone", fontSize = 12.sp, color = ConstructionOrange, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Attendance Breakdown Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "हाज़िरी विवरण (Attendance)" else "ATTENDANCE SUMMARY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConstructionOrange
                        )
                        HisabDetailRow(
                            label = if (language == AppLanguage.HINDI) "उपस्थित दिन (Present)" else "Total Present Days",
                            value = "${statement.presentDays} days"
                        )
                        HisabDetailRow(
                            label = if (language == AppLanguage.HINDI) "आधे दिन (Half Days)" else "Total Half Days",
                            value = "${statement.halfDays} days"
                        )
                        if (statement.paidLeaveDays > 0) {
                            HisabDetailRow(
                                label = if (language == AppLanguage.HINDI) "सवेतन छुट्टी (Paid Leave)" else "Paid Leave Days",
                                value = "${statement.paidLeaveDays} days"
                            )
                        }
                        HisabDetailRow(
                            label = if (language == AppLanguage.HINDI) "कुल देय दिन (Man-Days)" else "Total Payable Man-Days",
                            value = String.format("%.1f days", statement.payableManDays),
                            boldValue = true
                        )
                        HisabDetailRow(
                            label = if (language == AppLanguage.HINDI) "ओवरटाइम (Overtime)" else "Overtime Hours",
                            value = String.format("%.1f hrs", statement.totalOvertimeHours)
                        )
                    }
                }

                // Financial Ledger Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "हिसाब विवरण (Ledger Breakdown)" else "FINANCIAL HISAB LEDGER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConstructionOrange
                        )
                        HisabDetailRow(
                            label = if (language == AppLanguage.HINDI) "दैनिक मज़दूरी (Base Wages)" else "Total Wages",
                            value = Formatters.formatInr(statement.totalWages)
                        )
                        HisabDetailRow(
                            label = if (language == AppLanguage.HINDI) "ओवरटाइम राशि (OT Amount)" else "Overtime Amount",
                            value = Formatters.formatInr(statement.totalOvertimeAmount)
                        )
                        HisabDetailRow(
                            label = if (language == AppLanguage.HINDI) "कुल कमाई (Total Earnings)" else "Total Earnings",
                            value = Formatters.formatInr(statement.totalEarnings),
                            boldValue = true
                        )
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        val prevText = when {
                            statement.previousBalance > 0.01 -> "${Formatters.formatInr(statement.previousBalance)} (Payable B/F)"
                            statement.previousBalance < -0.01 -> "- ${Formatters.formatInr(-statement.previousBalance)} (Adv B/F)"
                            else -> "₹0"
                        }
                        HisabDetailRow(
                            label = if (language == AppLanguage.HINDI) "पिछला बकाया (Previous Balance)" else "Previous Balance B/F",
                            value = prevText
                        )
                        HisabDetailRow(
                            label = if (language == AppLanguage.HINDI) "लिया गया एडवांस (Advances)" else "Advances Taken",
                            value = "- ${Formatters.formatInr(statement.advancesTaken)}",
                            valueColor = StatusHalfDay
                        )
                        HisabDetailRow(
                            label = if (language == AppLanguage.HINDI) "किया गया भुगतान (Payments)" else "Payments Made",
                            value = "- ${Formatters.formatInr(statement.paymentsMade)}",
                            valueColor = StatusPresent
                        )
                    }
                }

                // Clear Final Payable vs Outstanding Balance Highlight
                val (boxBg, boxBorder, titleText, amountText, subNote) = when {
                    statement.payableAmount > 0.01 -> listOf(
                        ConstructionOrange.copy(alpha = 0.1f),
                        ConstructionOrange,
                        if (language == AppLanguage.HINDI) "मज़दूर को देय राशि (Payable)" else "Final Payable Amount (To Worker)",
                        Formatters.formatInr(statement.payableAmount),
                        "Advance Outstanding: ₹0"
                    )
                    statement.outstandingAdvanceAmount > 0.01 -> listOf(
                        StatusAbsent.copy(alpha = 0.08f),
                        StatusAbsent,
                        if (language == AppLanguage.HINDI) "मज़दूर पर बकाया एडवांस (Advance Due)" else "Outstanding Advance (Recoverable)",
                        Formatters.formatInr(statement.outstandingAdvanceAmount),
                        "Payable to Worker: ₹0"
                    )
                    else -> listOf(
                        StatusPresent.copy(alpha = 0.1f),
                        StatusPresent,
                        if (language == AppLanguage.HINDI) "हिसाब बराबर (Balanced)" else "Final Balance Settled",
                        "₹0",
                        "Payable: ₹0 • Advance Due: ₹0"
                    )
                }

                @Suppress("UNCHECKED_CAST")
                Surface(
                    color = boxBg as Color,
                    border = BorderStroke(1.2.dp, boxBorder as Color),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = titleText as String,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = boxBorder
                            )
                            Text(
                                text = amountText as String,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = boxBorder
                            )
                        }
                        Text(
                            text = subNote as String,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Transactions included in period
                if (statement.periodAdvances.isNotEmpty() || statement.periodPayments.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "इस अवधि के लेन-देन" else "Transactions in Period",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        statement.periodAdvances.forEach { adv ->
                            Text(
                                text = "• Advance: ${Formatters.formatInr(adv.amount)} on ${Formatters.formatToIndianDate(adv.date)} (${adv.paymentMethod})",
                                fontSize = 11.sp,
                                color = StatusHalfDay
                            )
                        }
                        statement.periodPayments.forEach { pay ->
                            Text(
                                text = "• Payment: ${Formatters.formatInr(pay.amount)} on ${Formatters.formatToIndianDate(pay.date)} (${pay.paymentMethod})",
                                fontSize = 11.sp,
                                color = StatusPresent
                            )
                        }
                    }
                }

                // Audit History for this statement
                if (statement.auditLogs.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp), tint = ConstructionOrange)
                            Text(
                                text = if (language == AppLanguage.HINDI) "ऑडिट इतिहास (Audit History)" else "Audit History",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        statement.auditLogs.take(4).forEach { audit ->
                            Text(
                                text = "• ${audit.action}: ${audit.reason} (${AmavasyaCalendar.formatTimestampIst(audit.timestamp)})",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action Buttons inside Full Statement: PDF, WhatsApp, SMS, Copy
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { AmavasyaPdfHelper.exportAndSharePdf(context, statement) },
                            colors = ButtonDefaults.buttonColors(containerColor = CharcoalDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("export_pdf_btn")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = ConstructionOrange)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == AppLanguage.HINDI) "PDF शेयर" else "Share PDF", fontSize = 12.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = { AmavasyaPdfHelper.viewPdf(context, statement) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("view_pdf_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == AppLanguage.HINDI) "PDF देखें" else "View PDF", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (hasValidPhone) {
                                    WorkerMessageHelper.openWhatsApp(context, statement.worker.mobile, preparedMessage)
                                } else {
                                    onDismiss()
                                    onUpdateWorkerPhone(statement.worker.id)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 12.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                if (hasValidPhone) {
                                    WorkerMessageHelper.openSms(context, statement.worker.mobile, preparedMessage)
                                } else {
                                    onDismiss()
                                    onUpdateWorkerPhone(statement.worker.id)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SMS", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { WorkerMessageHelper.copyToClipboard(context, preparedMessage) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (language == AppLanguage.HINDI) "कॉपी करें" else "Copy", fontSize = 12.sp)
                        }

                        if (!statement.isLocked && statement.payableAmount > 0.01) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onRecordPayment()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ConstructionOrange),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (language == AppLanguage.HINDI) "भुगतान करें" else "Pay Now", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == AppLanguage.HINDI) "बंद करें" else "Close")
            }
        }
    )
}

@Composable
private fun HisabDetailRow(
    label: String,
    value: String,
    boldValue: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = if (boldValue) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
fun MarkSettlementDialog(
    language: AppLanguage,
    statement: WorkerHisabStatement,
    onDismiss: () -> Unit,
    onRecordPaymentFirst: () -> Unit,
    onConfirmSettlement: (carryForwardUnpaid: Boolean, notes: String) -> Unit
) {
    var notes by remember { mutableStateOf("") }
    val hasPayableRemaining = statement.payableAmount > 0.01
    val hasAdvanceOutstanding = statement.outstandingAdvanceAmount > 0.01

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (hasPayableRemaining) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (hasPayableRemaining) StatusHalfDay else StatusPresent
                )
                Text(
                    text = if (language == AppLanguage.HINDI) "अमावस्या हिसाब पूरा करें" else "Complete Settlement",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${statement.worker.name} • ${statement.periodDisplay}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                if (hasPayableRemaining) {
                    Surface(
                        color = StatusHalfDay.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, StatusHalfDay),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (language == AppLanguage.HINDI) {
                                    "भुगतान अभी बाकी है: ${Formatters.formatInr(statement.payableAmount)}"
                                } else {
                                    "Payable Balance Remaining: ${Formatters.formatInr(statement.payableAmount)}"
                                },
                                fontWeight = FontWeight.Bold,
                                color = StatusHalfDay,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI) {
                                    "नियम: वास्तविक भुगतान दर्ज किए बिना खाता 'पूर्ण' नहीं माना जाएगा। कृपया पहले भुगतान दर्ज करें, या बकाया राशि अगले अमावस्या हिसाब में आगे बढ़ाएं (Carry Forward)।"
                                } else {
                                    "Rule: An account cannot be marked 'Settled in Full' until actual payment is recorded. Record the payment first, or close this period to carry forward the unpaid balance to the next Amavasya."
                                },
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else if (hasAdvanceOutstanding) {
                    Surface(
                        color = StatusAbsent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) {
                                "मज़दूर पर ${Formatters.formatInr(statement.outstandingAdvanceAmount)} एडवांस बकाया है। हिसाब बंद करने पर यह राशि अगले महीने में अपने आप आगे बढ़ जाएगी।"
                            } else {
                                "Worker owes ${Formatters.formatInr(statement.outstandingAdvanceAmount)} in excess advance. Closing this period will carry this advance balance forward to the next Amavasya."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    Surface(
                        color = StatusPresent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) {
                                "हिसाब ₹0 पर बराबर है। इस अवधि को लॉक करने पर यह रिकॉर्ड सुरक्षित हो जाएगा।"
                            } else {
                                "Balance is ₹0. Confirming will lock this settlement period to prevent duplicate calculations."
                            },
                            fontSize = 12.sp,
                            color = StatusPresent,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (language == AppLanguage.HINDI) "टिप्पणी / नोट्स (वैकल्पिक)" else "Settlement Note (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            if (hasPayableRemaining) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            onConfirmSettlement(true, notes.ifBlank { "Unpaid balance carried forward to next Amavasya" })
                            onDismiss()
                        },
                        modifier = Modifier.testTag("carry_forward_confirm_btn")
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "बकाया आगे बढ़ाएं" else "Carry Forward",
                            fontSize = 12.sp,
                            color = StatusHalfDay
                        )
                    }
                    Button(
                        onClick = {
                            onDismiss()
                            onRecordPaymentFirst()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ConstructionOrange),
                        modifier = Modifier.testTag("record_payment_first_btn")
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "भुगतान दर्ज करें" else "Record Payment",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        onConfirmSettlement(hasAdvanceOutstanding, notes)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusPresent),
                    modifier = Modifier.testTag("confirm_full_settlement_btn")
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "पुष्टि व लॉक करें" else "Confirm & Lock",
                        color = Color.White
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel")
            }
        }
    )
}

@Composable
fun AuthorizedCorrectionDialog(
    language: AppLanguage,
    statement: WorkerHisabStatement,
    onDismiss: () -> Unit,
    onConfirmCorrection: (reason: String, reopenPeriod: Boolean) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var reopenPeriod by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = ConstructionOrange)
                Text(
                    text = if (language == AppLanguage.HINDI) "अधिकृत सुधार (Correction)" else "Authorized Correction",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${statement.worker.name} • ${statement.periodDisplay}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = if (language == AppLanguage.HINDI) {
                        "यह हिसाब अवधि लॉक है। हर सुधार ऑडिट इतिहास में दर्ज किया जाएगा।"
                    } else {
                        "This settlement period is locked. Every authorized change is recorded in the audit history."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !reopenPeriod, onClick = { reopenPeriod = false })
                    Column {
                        Text(
                            text = if (language == AppLanguage.HINDI) "ताज़ा डेटा से पुनः गणना व लॉक" else "Recalculate & Re-lock Snapshot",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (language == AppLanguage.HINDI) "यदि हाज़िरी/भुगतान बदला गया हो" else "Syncs updated attendance/payments & logs audit",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = reopenPeriod, onClick = { reopenPeriod = true })
                    Column {
                        Text(
                            text = if (language == AppLanguage.HINDI) "अवधि को फिर से खोलें (Unlock)" else "Unlock / Re-open Period",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (language == AppLanguage.HINDI) "नया भुगतान जोड़ने के लिए अनलॉक करें" else "Unlock to add payments or advances before locking",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(if (language == AppLanguage.HINDI) "सुधार का कारण (अनिवार्य) *" else "Reason for Correction *") },
                    modifier = Modifier.fillMaxWidth().testTag("correction_reason_input"),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isNotBlank()) {
                        onConfirmCorrection(reason.trim(), reopenPeriod)
                        onDismiss()
                    }
                },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ConstructionOrange),
                modifier = Modifier.testTag("confirm_correction_btn")
            ) {
                Text(if (language == AppLanguage.HINDI) "सुधार सेव करें" else "Apply Correction", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmavasyaDatePickerModal(
    initialIsoDate: String,
    language: AppLanguage,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = remember(initialIsoDate) {
        AmavasyaCalendar.parseIsoToUtcDatePickerMillis(initialIsoDate)
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) {
                        onDateSelected(AmavasyaCalendar.formatMillisToIso(selected))
                    }
                    onDismiss()
                }
            ) {
                Text(
                    text = if (language == AppLanguage.HINDI) "चुनें (Select)" else "Select",
                    color = ConstructionOrange,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
