package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import com.example.ui.theme.ConstructionOrange
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusHalfDay
import com.example.ui.theme.StatusPresent
import com.example.util.AppLanguage
import com.example.util.NotificationMessageType
import com.example.util.WorkerMessageData
import com.example.util.WorkerMessageHelper

@Composable
fun MessageNotificationDialog(
    language: AppLanguage,
    data: WorkerMessageData,
    onDismiss: () -> Unit,
    onUpdateWorkerProfile: (workerId: String) -> Unit
) {
    val context = LocalContext.current
    var currentMessage by remember(data.messageText) { mutableStateOf(data.messageText) }
    var isEditingMessage by remember { mutableStateOf(false) }

    val hasValidPhone = WorkerMessageHelper.isValidPhoneNumber(data.workerMobile)
    val maskedPhone = WorkerMessageHelper.maskPhoneNumber(data.workerMobile)

    val typeTitle = when (data.type) {
        NotificationMessageType.ATTENDANCE -> if (language == AppLanguage.HINDI) "हाज़िरी दर्ज हुई (Attendance Saved)" else "Attendance Saved"
        NotificationMessageType.PAYMENT -> if (language == AppLanguage.HINDI) "मज़दूरी भुगतान दर्ज हुआ (Payment Saved)" else "Payment Recorded"
        NotificationMessageType.ADVANCE -> if (language == AppLanguage.HINDI) "एडवांस दर्ज हुआ (Advance Saved)" else "Advance Recorded"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(StatusPresent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusPresent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = typeTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (language == AppLanguage.HINDI) "मज़दूर को संदेश भेजें / Notify Worker" else "Send Notification to Worker",
                        style = MaterialTheme.typography.labelSmall,
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
                // Worker & Phone Confirmation Card
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = data.workerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Surface(
                                color = if (hasValidPhone) StatusPresent.copy(alpha = 0.15f) else StatusAbsent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (hasValidPhone) Icons.Default.Phone else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (hasValidPhone) StatusPresent else StatusAbsent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = maskedPhone,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (hasValidPhone) StatusPresent else StatusAbsent
                                    )
                                }
                            }
                        }

                        if (!hasValidPhone) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "मोबाइल नंबर दर्ज नहीं है। संदेश भेजने के लिए नंबर जोड़ें।" else "Mobile number is missing. Add phone number to send message.",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusAbsent
                            )
                            Button(
                                onClick = {
                                    onDismiss()
                                    onUpdateWorkerProfile(data.workerId)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ConstructionOrange),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "मज़दूर का मोबाइल नंबर जोड़ें" else "Add Mobile Number",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Message Text Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "तैयार संदेश (Ready Message):" else "Prepared Message:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        TextButton(
                            onClick = { isEditingMessage = !isEditingMessage },
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = ConstructionOrange
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isEditingMessage) {
                                    if (language == AppLanguage.HINDI) "पूर्ण (Done)" else "Done"
                                } else {
                                    if (language == AppLanguage.HINDI) "बदलें (Edit)" else "Edit Message"
                                },
                                fontSize = 11.sp,
                                color = ConstructionOrange
                            )
                        }
                    }

                    if (isEditingMessage) {
                        OutlinedTextField(
                            value = currentMessage,
                            onValueChange = { currentMessage = it },
                            modifier = Modifier.fillMaxWidth().testTag("edit_message_input"),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(8.dp)
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = currentMessage,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Primary Send Buttons (WhatsApp & Normal SMS)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // WhatsApp Button (One-click)
                    Button(
                        onClick = {
                            WorkerMessageHelper.openWhatsApp(context, data.workerMobile, currentMessage)
                            onDismiss()
                        },
                        enabled = hasValidPhone,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), // WhatsApp Green
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("send_whatsapp_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "WhatsApp पर भेजें" else "Send via WhatsApp",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    // Normal SMS Button (One-click)
                    Button(
                        onClick = {
                            WorkerMessageHelper.openSms(context, data.workerMobile, currentMessage)
                            onDismiss()
                        },
                        enabled = hasValidPhone,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)), // SMS Sky Blue
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("send_sms_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "Normal SMS भेजें" else "Send via Normal SMS",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    // Copy Message Button
                    OutlinedButton(
                        onClick = {
                            WorkerMessageHelper.copyToClipboard(context, currentMessage)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("copy_message_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "संदेश कॉपी करें (Copy Message)" else "Copy Message",
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("skip_message_btn")
            ) {
                Text(
                    text = if (language == AppLanguage.HINDI) "छोड़ें (Skip)" else "Skip",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}
