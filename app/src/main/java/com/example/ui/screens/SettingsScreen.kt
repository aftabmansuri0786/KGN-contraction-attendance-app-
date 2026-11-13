package com.example.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Translate
import com.example.util.AdMobConfig
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ConstructionOrange
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusPresent
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.AttendanceReminderManager
import com.example.util.AttendanceReminderSettings
import com.example.util.Formatters
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    language: AppLanguage,
    isDarkTheme: Boolean,
    currentPin: String,
    currentUser: FirebaseUser? = null,
    isSyncing: Boolean = false,
    lastSyncTimestamp: Long? = null,
    reminderSettings: AttendanceReminderSettings = AttendanceReminderSettings(),
    onUpdateReminderEnabled: (Boolean) -> Unit = {},
    onUpdateReminderTime: (Int, Int) -> Unit = { _, _ -> },
    onSignInWithGoogle: () -> Unit = {},
    onSignOutGoogle: () -> Unit = {},
    onSyncNow: () -> Unit = {},
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onChangePin: (String) -> Unit,
    onLockApp: () -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasNotifPermission by remember { mutableStateOf(AttendanceReminderManager.hasNotificationPermission(context)) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (!granted) {
            Toast.makeText(
                context,
                if (language == AppLanguage.HINDI) "रिमाइंडर के लिए नोटिफिकेशन अनुमति ज़रूरी है" else "Notification permission denied. Enable in system settings to receive reminders.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            onUpdateReminderEnabled(true)
        }
    }
    var newPinText by remember { mutableStateOf("") }
    var confirmPinText by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = AppStrings.get("settings", language),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Company Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "KGN Construction",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = ConstructionOrange
                )
                Text(
                    text = "Owner Attendance & Payroll System",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Currency: Indian Rupee (₹) • Timezone: Asia/Kolkata",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Language & Appearance Settings
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = ConstructionOrange)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.HINDI) "भाषा (Language)" else "App Language",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI) "वर्तमान: हिंदी (Hindi)" else "Current: English",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onToggleLanguage,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (language == AppLanguage.HINDI) "Switch to English" else "हिंदी में बदलें")
                    }
                }

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = ConstructionOrange)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.HINDI) "डार्क मोड (Dark Theme)" else "Dark Theme",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isDarkTheme) "Enabled" else "Disabled",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleTheme() }
                    )
                }
            }
        }

        // Daily Attendance Reminder Card (6:30 PM IST Default)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("settings_attendance_reminder_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = ConstructionOrange)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.HINDI) "दैनिक हाज़िरी रिमाइंडर" else "Daily Attendance Reminder",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI) {
                                    "बाकी मज़दूरों के लिए शाम ${AttendanceReminderManager.formatTime12Hour(reminderSettings.hour, reminderSettings.minute)}"
                                } else {
                                    "Alerts for pending workers at ${AttendanceReminderManager.formatTime12Hour(reminderSettings.hour, reminderSettings.minute)}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = reminderSettings.enabled,
                        onCheckedChange = { checked ->
                            if (checked && !hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onUpdateReminderEnabled(checked)
                            }
                        },
                        modifier = Modifier.testTag("reminder_enable_switch")
                    )
                }

                if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Surface(
                        color = StatusAbsent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "नोटिफिकेशन अनुमति बंद है" else "Notification permission required",
                                fontSize = 11.5.sp,
                                color = StatusAbsent,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (language == AppLanguage.HINDI) "अनुमति दें" else "Grant", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, pickedHour, pickedMinute ->
                                    onUpdateReminderTime(pickedHour, pickedMinute)
                                    Toast.makeText(
                                        context,
                                        "Reminder set to ${AttendanceReminderManager.formatTime12Hour(pickedHour, pickedMinute)}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                reminderSettings.hour,
                                reminderSettings.minute,
                                false
                            ).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("reminder_change_time_btn")
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = ConstructionOrange)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = AttendanceReminderManager.formatTime12Hour(reminderSettings.hour, reminderSettings.minute),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                scope.launch {
                                    val sent = AttendanceReminderManager.evaluateAndShowReminderNow(
                                        context,
                                        forceDemoIfNonePending = true
                                    )
                                    if (!sent) {
                                        Toast.makeText(context, "Could not show notification", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("reminder_test_btn")
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "टेस्ट नोटिफिकेशन" else "Test Reminder",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Owner Security PIN Settings
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = ConstructionOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "मालिक पिन सुरक्षा (Owner PIN Security)" else "Owner PIN Security",
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Current PIN: **** (Default: 1234)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newPinText,
                        onValueChange = { if (it.length <= 4) newPinText = it },
                        label = { Text("New 4-digit PIN") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (newPinText.length == 4) {
                                onChangePin(newPinText)
                                newPinText = ""
                                Toast.makeText(
                                    context,
                                    if (language == AppLanguage.HINDI) "पिन सफलतापूर्वक बदल दिया गया" else "PIN changed successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(context, "Enter 4 digits", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ConstructionOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(if (language == AppLanguage.HINDI) "बदलें" else "Save")
                    }
                }

                Button(
                    onClick = onLockApp,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = AppStrings.get("lock_app", language),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Google Account & Firebase Cloud Sync Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = if (currentUser != null) StatusPresent else ConstructionOrange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "गूगल अकाउंट व क्लाउड बैकअप (Cloud Sync)" else "Google Account & Cloud Backup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                if (currentUser != null) {
                    Surface(
                        color = StatusPresent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = StatusPresent, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser.displayName ?: "Authorized Owner",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = currentUser.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (lastSyncTimestamp != null)
                                        "Last synced: Just now"
                                    else
                                        "Connected to Cloud Firestore",
                                    fontSize = 11.sp,
                                    color = StatusPresent
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSyncNow,
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = ConstructionOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (language == AppLanguage.HINDI) "सिंक हो रहा..." else "Syncing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (language == AppLanguage.HINDI) "अभी सिंक करें" else "Sync Now", fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = onSignOutGoogle,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (language == AppLanguage.HINDI) "लॉगआउट" else "Sign Out", fontSize = 12.sp)
                        }
                    }
                } else {
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "ऐप अपडेट करने पर या फोन बदलने पर अपना पूरा पुराना डेटा (मजदूर, हाजिरी, एडवांस, वेतन) सुरक्षित रखने के लिए अपने Google Account से लॉगिन करें।"
                        else
                            "Sign in with your Google Account to automatically sync and restore all workers, attendance, advances, and payments across updates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onSignInWithGoogle,
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = ConstructionOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (language == AppLanguage.HINDI) "डेटा लोड हो रहा है..." else "Signing in & Syncing...")
                        } else {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI) "Google Account से लॉगिन करें" else "Sign in with Google",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Google AdMob Monetization Status
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = ConstructionOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "Google AdMob विज्ञापन (Monetization)" else "Google AdMob Monetization",
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Status: Active (Development / Test Mode)\nBanner Ad Unit: ${AdMobConfig.bannerAdUnitId}\napp-ads.txt: Configured",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Clear All Data
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
                    text = if (language == AppLanguage.HINDI) "डेटा प्रबंधन (Data Management)" else "Data Management",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (language == AppLanguage.HINDI) "यदि आप सभी मज़दूर, साइट्स, और हाज़िरी रिकॉर्ड्स पूरी तरह साफ़ करना चाहते हैं:" else "If you want to completely erase all workers, sites, and attendance records:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { showClearConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "सभी मज़दूर व डेटा हटाएं (Clear All Data)" else "Clear All Data",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showClearConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = if (language == AppLanguage.HINDI) "क्या आप निश्चित हैं?" else "Are you sure?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == AppLanguage.HINDI) "सभी मज़दूर, साइट्स और हाज़िरी डेटा हमेशा के लिए हटा दिया जाएगा।" else "All workers, sites, and attendance data will be permanently cleared."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearConfirmDialog = false
                        Toast.makeText(
                            context,
                            if (language == AppLanguage.HINDI) "सभी डेटा साफ़ कर दिया गया है" else "All data cleared",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(if (language == AppLanguage.HINDI) "हां, हटाएं" else "Yes, Clear All", color = Color.White)
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { showClearConfirmDialog = false }) {
                    Text(if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}
