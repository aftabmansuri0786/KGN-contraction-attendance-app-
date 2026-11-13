package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentMethods
import com.example.data.model.SiteEntity
import com.example.data.model.WorkCategories
import com.example.data.model.WorkerEntity
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.TextButton
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.CharcoalDark
import com.example.ui.theme.ConstructionOrange
import com.example.ui.theme.StatusPresent
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.Formatters
import com.example.util.WorkerMessageHelper
import com.example.util.WorkerPhotoHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkerDialog(
    language: AppLanguage,
    sites: List<SiteEntity>,
    workerToEdit: WorkerEntity? = null,
    onDismiss: () -> Unit,
    onSave: (WorkerEntity) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(workerToEdit?.name ?: "") }
    var mobileDigits by remember {
        mutableStateOf(
            if (workerToEdit != null) WorkerMessageHelper.extractLocal10Digits(workerToEdit.mobile) else ""
        )
    }
    var category by remember { mutableStateOf(workerToEdit?.category ?: "Mason") }
    var dailyWageText by remember { mutableStateOf(workerToEdit?.dailyWage?.takeIf { it > 0 }?.toInt()?.toString() ?: "") }
    var otRateText by remember { mutableStateOf(workerToEdit?.overtimeHourlyRate?.takeIf { it > 0 }?.toInt()?.toString() ?: "") }
    var selectedSiteId by remember { mutableStateOf(workerToEdit?.siteId ?: (sites.firstOrNull()?.id ?: "")) }
    var notes by remember { mutableStateOf(workerToEdit?.notes ?: "") }

    var localPhotoPath by remember { mutableStateOf(workerToEdit?.localPhotoPath ?: "") }
    var photoUrl by remember { mutableStateOf(workerToEdit?.photoUrl ?: "") }
    var photoStoragePath by remember { mutableStateOf(workerToEdit?.photoStoragePath ?: "") }
    var photoRemoved by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    val draftPhotoKey = remember { workerToEdit?.id ?: "draft_${System.currentTimeMillis()}" }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val saved = WorkerPhotoHelper.saveCompressedPhotoFromUri(context, uri, draftPhotoKey)
            if (saved != null) {
                localPhotoPath = saved.absolutePath
                photoRemoved = false
            } else {
                Toast.makeText(context, "Could not load selected photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val camUri = pendingCameraUri
        if (success && camUri != null) {
            val saved = WorkerPhotoHelper.saveCompressedPhotoFromUri(context, camUri, draftPhotoKey)
            if (saved != null) {
                localPhotoPath = saved.absolutePath
                photoRemoved = false
            }
        }
        pendingCameraFile?.delete()
        pendingCameraUri = null
        pendingCameraFile = null
    }

    var categoryExpanded by remember { mutableStateOf(false) }
    var siteExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (workerToEdit == null) {
                    AppStrings.get("action_add_worker", language)
                } else {
                    if (language == AppLanguage.HINDI) "मज़दूर विवरण बदलें" else "Edit Worker"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Worker Profile Photo Picker & Camera Row
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WorkerAvatar(
                            workerName = name.ifBlank { "W" },
                            localPhotoPath = if (photoRemoved) "" else localPhotoPath,
                            photoUrl = if (photoRemoved) "" else photoUrl,
                            size = 64.dp,
                            modifier = Modifier.testTag("worker_photo_preview")
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "मज़दूर की फ़ोटो (वैकल्पिक)" else "Profile Photo",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val created = WorkerPhotoHelper.createTempCameraUri(context)
                                        if (created != null) {
                                            pendingCameraUri = created.first
                                            pendingCameraFile = created.second
                                            cameraLauncher.launch(created.first)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp).testTag("worker_photo_camera_btn")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (language == AppLanguage.HINDI) "कैमरा" else "Camera", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        galleryLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp).testTag("worker_photo_gallery_btn")
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (language == AppLanguage.HINDI) "गैलरी" else "Gallery", fontSize = 11.sp)
                                }

                                if (!photoRemoved && (localPhotoPath.isNotBlank() || photoUrl.isNotBlank())) {
                                    IconButton(
                                        onClick = {
                                            localPhotoPath = ""
                                            photoUrl = ""
                                            photoStoragePath = ""
                                            photoRemoved = true
                                        },
                                        modifier = Modifier.size(36.dp).testTag("worker_photo_remove_btn")
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Remove Photo",
                                            tint = StatusAbsent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppStrings.get("worker_name", language)) },
                    modifier = Modifier.fillMaxWidth().testTag("worker_name_input"),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true
                )

                OutlinedTextField(
                    value = mobileDigits,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        if (digits.length <= 10) {
                            mobileDigits = digits
                        }
                    },
                    label = { Text(AppStrings.get("mobile_no", language)) },
                    modifier = Modifier.fillMaxWidth().testTag("worker_mobile_input"),
                    leadingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+91",
                                fontWeight = FontWeight.Bold,
                                color = ConstructionOrange,
                                fontSize = 14.sp
                            )
                        }
                    },
                    placeholder = { Text("98765 43210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = WorkCategories.find { it.first == category }?.let {
                            if (language == AppLanguage.HINDI) it.second else it.first
                        } ?: category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(AppStrings.get("work_category", language)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        WorkCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(if (language == AppLanguage.HINDI) "${cat.second} (${cat.first})" else cat.first)
                                },
                                onClick = {
                                    category = cat.first
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dailyWageText,
                        onValueChange = {
                            dailyWageText = it
                            val wage = it.toDoubleOrNull() ?: 0.0
                            otRateText = (wage / 8.0).toInt().toString()
                        },
                        label = { Text(AppStrings.get("daily_wage", language)) },
                        modifier = Modifier.weight(1f).testTag("worker_wage_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = otRateText,
                        onValueChange = { otRateText = it },
                        label = { Text(AppStrings.get("ot_rate", language)) },
                        modifier = Modifier.weight(1f).testTag("worker_ot_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                // Site Dropdown
                if (sites.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = siteExpanded,
                        onExpandedChange = { siteExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = sites.find { it.id == selectedSiteId }?.name ?: (if (language == AppLanguage.HINDI) "कोई साइट नहीं" else "Unassigned"),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(AppStrings.get("assigned_site", language)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = siteExpanded,
                            onDismissRequest = { siteExpanded = false }
                        ) {
                            sites.forEach { site ->
                                DropdownMenuItem(
                                    text = { Text(site.name) },
                                    onClick = {
                                        selectedSiteId = site.id
                                        siteExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(AppStrings.get("notes", language)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val wage = dailyWageText.toDoubleOrNull() ?: 0.0
                        val otRate = otRateText.toDoubleOrNull() ?: if (wage > 0) (wage / 8.0) else 0.0
                        val id = workerToEdit?.id ?: "KGN-W${System.currentTimeMillis().toString().takeLast(4)}"
                        val finalLocalPath = if (photoRemoved || localPhotoPath.isBlank()) {
                            ""
                        } else {
                            WorkerPhotoHelper.copyToFinalWorkerPhoto(context, localPhotoPath, id)?.absolutePath
                                ?: localPhotoPath
                        }
                        val formattedMobile = if (mobileDigits.isNotBlank()) WorkerMessageHelper.formatWithCountryCode(mobileDigits) else ""
                        val worker = WorkerEntity(
                            id = id,
                            name = name.trim(),
                            mobile = formattedMobile,
                            category = category,
                            dailyWage = wage,
                            overtimeHourlyRate = otRate,
                            joiningDate = workerToEdit?.joiningDate ?: Formatters.formatToIndianDate(Formatters.getTodayIsoDate()),
                            siteId = selectedSiteId,
                            isActive = workerToEdit?.isActive ?: true,
                            notes = notes,
                            photoUrl = if (photoRemoved) "" else photoUrl,
                            photoStoragePath = if (photoRemoved) "" else photoStoragePath,
                            localPhotoPath = finalLocalPath
                        )
                        onSave(worker)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ConstructionOrange),
                modifier = Modifier.testTag("save_worker_submit_btn")
            ) {
                Text(AppStrings.get("save_worker", language), color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel")
            }
        }
    )
}

@Composable
fun AddSiteDialog(
    language: AppLanguage,
    siteToEdit: SiteEntity? = null,
    onDismiss: () -> Unit,
    onSave: (SiteEntity) -> Unit
) {
    var name by remember { mutableStateOf(siteToEdit?.name ?: "") }
    var address by remember { mutableStateOf(siteToEdit?.address ?: "") }
    var notes by remember { mutableStateOf(siteToEdit?.notes ?: "") }
    var status by remember { mutableStateOf(siteToEdit?.status ?: "Active") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (siteToEdit == null) AppStrings.get("action_add_site", language) else "Edit Site",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppStrings.get("site_name", language)) },
                    modifier = Modifier.fillMaxWidth().testTag("site_name_input"),
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(AppStrings.get("site_address", language)) },
                    modifier = Modifier.fillMaxWidth().testTag("site_address_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(AppStrings.get("notes", language)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val id = siteToEdit?.id ?: "KGN-S${System.currentTimeMillis().toString().takeLast(3)}"
                        val site = SiteEntity(
                            id = id,
                            name = name.trim(),
                            address = address.trim(),
                            startDate = siteToEdit?.startDate ?: Formatters.formatToIndianDate(Formatters.getTodayIsoDate()),
                            status = status,
                            notes = notes
                        )
                        onSave(site)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ConstructionOrange),
                modifier = Modifier.testTag("save_site_submit_btn")
            ) {
                Text(AppStrings.get("save_site", language), color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTransactionDialog(
    isAdvance: Boolean,
    language: AppLanguage,
    workers: List<WorkerEntity>,
    preSelectedWorkerId: String = "",
    suggestedAmount: Double? = null,
    transactionDateDisplay: String? = null,
    onDismiss: () -> Unit,
    onUpdateWorker: (WorkerEntity) -> Unit = {},
    onSubmit: (workerId: String, amount: Double, method: String, reference: String, notes: String) -> Unit
) {
    var selectedWorkerId by remember {
        mutableStateOf(if (preSelectedWorkerId.isNotBlank()) preSelectedWorkerId else (workers.firstOrNull()?.id ?: ""))
    }
    var amountText by remember(suggestedAmount) {
        mutableStateOf(
            if (suggestedAmount != null && suggestedAmount > 0.0) {
                if (suggestedAmount % 1.0 == 0.0) suggestedAmount.toLong().toString() else String.format("%.2f", suggestedAmount)
            } else {
                ""
            }
        )
    }
    var selectedMethod by remember { mutableStateOf("Cash") }
    var reference by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var workerExpanded by remember { mutableStateOf(false) }
    var methodExpanded by remember { mutableStateOf(false) }

    val selectedWorker = workers.find { it.id == selectedWorkerId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (isAdvance) AppStrings.get("action_record_advance", language) else AppStrings.get("action_record_payment", language),
                    fontWeight = FontWeight.Bold
                )
                if (!transactionDateDisplay.isNullOrBlank()) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "हिसाब तारीख: $transactionDateDisplay" else "Hisab Date: $transactionDateDisplay",
                        style = MaterialTheme.typography.labelSmall,
                        color = ConstructionOrange
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Worker Picker
                ExposedDropdownMenuBox(
                    expanded = workerExpanded,
                    onExpandedChange = { workerExpanded = it }
                ) {
                    val workerName = selectedWorker?.name ?: "Select Worker"
                    OutlinedTextField(
                        value = workerName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(AppStrings.get("worker_name", language)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = workerExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = workerExpanded,
                        onDismissRequest = { workerExpanded = false }
                    ) {
                        workers.forEach { worker ->
                            DropdownMenuItem(
                                text = { Text("${worker.name} (${worker.category})") },
                                onClick = {
                                    selectedWorkerId = worker.id
                                    workerExpanded = false
                                }
                            )
                        }
                    }
                }

                // Masked Phone Number Confirmation Card
                if (selectedWorker != null) {
                    val hasPhone = WorkerMessageHelper.isValidPhoneNumber(selectedWorker.mobile)
                    val masked = WorkerMessageHelper.maskPhoneNumber(selectedWorker.mobile)
                    Surface(
                        color = if (hasPhone) MaterialTheme.colorScheme.surfaceVariant else StatusAbsent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (hasPhone) Icons.Default.Phone else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (hasPhone) StatusPresent else StatusAbsent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hasPhone) "Saved: $masked" else "No phone saved",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (hasPhone) MaterialTheme.colorScheme.onSurface else StatusAbsent
                                )
                            }
                            if (!hasPhone) {
                                TextButton(
                                    onClick = {
                                        onDismiss()
                                        onUpdateWorker(selectedWorker)
                                    },
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = "Add Phone",
                                        fontSize = 11.sp,
                                        color = ConstructionOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(AppStrings.get("amount_inr", language)) },
                    modifier = Modifier.fillMaxWidth().testTag("transaction_amount_input"),
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                // Payment Method
                ExposedDropdownMenuBox(
                    expanded = methodExpanded,
                    onExpandedChange = { methodExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(AppStrings.get("payment_mode", language)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = methodExpanded,
                        onDismissRequest = { methodExpanded = false }
                    ) {
                        PaymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    selectedMethod = method
                                    methodExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text(AppStrings.get("reference_no", language)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(AppStrings.get("notes", language)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (selectedWorkerId.isNotBlank() && amount > 0) {
                        onSubmit(selectedWorkerId, amount, selectedMethod, reference, notes)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ConstructionOrange),
                modifier = Modifier.testTag("submit_transaction_btn")
            ) {
                Text(AppStrings.get("save_transaction", language), color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel")
            }
        }
    )
}

@Composable
fun PaymentReceiptVoucherDialog(
    language: AppLanguage,
    worker: WorkerEntity?,
    amount: Double,
    date: String,
    method: String,
    reference: String,
    voucherId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val receiptText = buildString {
        appendLine("=================================")
        appendLine("     KGN CONSTRUCTION")
        appendLine("   PAYMENT VOUCHER / रसीद")
        appendLine("=================================")
        appendLine("Voucher No: $voucherId")
        appendLine("Date: ${Formatters.formatToIndianDate(date)}")
        appendLine("Worker: ${worker?.name ?: "Worker"} (${worker?.id})")
        appendLine("Skill: ${worker?.category}")
        appendLine("Amount Paid: ${Formatters.formatInr(amount)}")
        appendLine("Payment Mode: $method")
        if (reference.isNotBlank()) appendLine("Ref/UTR No: $reference")
        appendLine("=================================")
        appendLine("Authorized Owner: KGN Construction")
        appendLine("Generated via KGN Construction App")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = ConstructionOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Text(AppStrings.get("payment_voucher", language), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CharcoalDark)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "KGN CONSTRUCTION",
                            color = ConstructionOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Voucher: $voucherId", style = MaterialTheme.typography.labelMedium)
                        Text(Formatters.formatToIndianDate(date), style = MaterialTheme.typography.labelMedium)
                    }

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Worker:", fontWeight = FontWeight.SemiBold)
                        Text("${worker?.name} (${worker?.category})")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Amount:", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = Formatters.formatInr(amount),
                            color = StatusPresent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mode:", fontWeight = FontWeight.SemiBold)
                        Text(method)
                    }

                    if (reference.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ref / UTR:", fontWeight = FontWeight.SemiBold)
                            Text(reference)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Formatters.shareText(
                        context,
                        "KGN Payment Receipt - ${worker?.name}",
                        receiptText
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusPresent),
                modifier = Modifier.testTag("share_voucher_btn")
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
