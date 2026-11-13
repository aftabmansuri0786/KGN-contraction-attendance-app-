package com.example.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MonthlyPdfEntity
import com.example.data.model.WorkerEntity
import com.example.ui.components.WorkerAvatar
import com.example.ui.theme.CharcoalDark
import com.example.ui.theme.ConstructionOrange
import com.example.ui.theme.StatusPresent
import com.example.util.AmavasyaCalendar
import com.example.util.AmavasyaPdfHelper
import com.example.util.AppLanguage
import com.example.util.Formatters
import java.io.File
import java.io.FileInputStream
import java.text.DateFormatSymbols
import java.util.Locale

@Composable
fun MonthlyPdfsScreen(
    language: AppLanguage,
    monthlyPdfs: List<MonthlyPdfEntity>,
    workers: List<WorkerEntity>,
    selectedAmavasyaDate: String,
    isGeneratingPdfs: Boolean,
    onGenerateAllForPeriod: (forceRegenerate: Boolean) -> Unit,
    onEnsurePdfReady: (MonthlyPdfEntity, (File?) -> Unit) -> Unit,
    onRegeneratePdf: (MonthlyPdfEntity) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val workersMap = remember(workers) { workers.associateBy { it.id } }

    val availableYears = remember(monthlyPdfs) {
        monthlyPdfs.map { it.year }.distinct().sortedDescending()
    }
    var selectedYear by remember(availableYears) {
        mutableIntStateOf(availableYears.firstOrNull() ?: 0)
    }
    val availableMonths = remember(monthlyPdfs, selectedYear) {
        monthlyPdfs
            .filter { selectedYear == 0 || it.year == selectedYear }
            .map { it.month }
            .distinct()
            .sortedDescending()
    }
    var selectedMonth by remember(availableMonths, selectedYear) {
        mutableIntStateOf(availableMonths.firstOrNull() ?: 0)
    }

    var pendingSaveFile by remember { mutableStateOf<File?>(null) }
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val src = pendingSaveFile
        if (uri != null && src != null && src.exists()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(src).use { input ->
                        input.copyTo(out)
                    }
                }
                Toast.makeText(
                    context,
                    if (language == AppLanguage.HINDI) "PDF डाउनलोड हो गया" else "PDF downloaded",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        pendingSaveFile = null
    }

    val filteredPdfs = remember(monthlyPdfs, selectedYear, selectedMonth) {
        monthlyPdfs.filter { pdf ->
            val yMatch = selectedYear == 0 || pdf.year == selectedYear
            val mMatch = selectedMonth == 0 || pdf.month == selectedMonth
            yMatch && mMatch
        }.sortedWith(compareByDescending<MonthlyPdfEntity> { it.isCombinedReport }.thenBy { it.workerName })
    }

    val handleDownload: (MonthlyPdfEntity) -> Unit = { pdf ->
        onEnsurePdfReady(pdf) { file ->
            if (file == null || !file.exists()) {
                Toast.makeText(context, "Could not prepare PDF", Toast.LENGTH_SHORT).show()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val saved = AmavasyaPdfHelper.saveToDownloadsQ(context, file, file.name)
                if (saved) {
                    Toast.makeText(
                        context,
                        if (language == AppLanguage.HINDI) "Downloads/KGN Construction में सेव हुआ" else "Saved to Downloads/KGN Construction",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    pendingSaveFile = file
                    createDocLauncher.launch(file.name)
                }
            } else {
                pendingSaveFile = file
                createDocLauncher.launch(file.name)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + Back Button
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
                    modifier = Modifier.testTag("monthly_pdfs_back_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
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

                Text(
                    text = if (language == AppLanguage.HINDI) "मासिक हिसाब PDFs" else "Monthly Hisab PDFs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Generate Banner Card for Selected Settlement Period
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalDark),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "मासिक PDF रिपोर्ट केंद्र" else "Monthly PDF Archive",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Period: ${Formatters.formatToIndianDate(selectedAmavasyaDate)} • ${monthlyPdfs.size} PDF(s) saved",
                                color = Color(0xFFB0BEC5),
                                fontSize = 11.5.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = ConstructionOrange,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onGenerateAllForPeriod(false) },
                            enabled = !isGeneratingPdfs,
                            colors = ButtonDefaults.buttonColors(containerColor = ConstructionOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp).testTag("generate_monthly_pdfs_btn")
                        ) {
                            if (isGeneratingPdfs) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (language == AppLanguage.HINDI) "बन रही है..." else "Generating...", color = Color.White, fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (language == AppLanguage.HINDI) "सभी PDF बनाएं" else "Generate PDFs", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { onGenerateAllForPeriod(true) },
                            enabled = !isGeneratingPdfs,
                            border = BorderStroke(1.dp, Color(0xFF546E7A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(46.dp).testTag("regenerate_all_pdfs_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate All", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (language == AppLanguage.HINDI) "री-जनरेट" else "Refresh", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Year & Month Folder Filter Chips
        if (availableYears.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "वर्ष / महीना चुनें (Year -> Month)" else "Filter by Year & Month",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedYear == 0,
                                onClick = { selectedYear = 0; selectedMonth = 0 },
                                label = { Text(if (language == AppLanguage.HINDI) "सभी वर्ष" else "All Years", fontSize = 12.sp) }
                            )
                        }
                        items(availableYears) { yr ->
                            FilterChip(
                                selected = selectedYear == yr,
                                onClick = { selectedYear = yr },
                                label = { Text(yr.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ConstructionOrange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (availableMonths.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedMonth == 0,
                                    onClick = { selectedMonth = 0 },
                                    label = { Text(if (language == AppLanguage.HINDI) "सभी महीने" else "All Months", fontSize = 11.5.sp) }
                                )
                            }
                            items(availableMonths) { m ->
                                val monthShort = DateFormatSymbols(Locale.US).shortMonths.getOrNull(m - 1) ?: "M$m"
                                FilterChip(
                                    selected = selectedMonth == m,
                                    onClick = { selectedMonth = m },
                                    label = { Text(monthShort, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // PDF Cards List
        if (filteredPdfs.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = ConstructionOrange, modifier = Modifier.size(34.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) {
                                "अभी कोई मासिक PDF उपलब्ध नहीं है। ऊपर 'सभी PDF बनाएं' दबाएं या किसी मज़दूर का अमावस्या हिसाब पूर्ण करें।"
                            } else {
                                "No monthly Hisab PDFs generated yet. Tap 'Generate PDFs' above or complete an Amavasya settlement."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredPdfs, key = { it.id }) { pdf ->
                val worker = workersMap[pdf.workerId]
                MonthlyPdfCard(
                    language = language,
                    pdf = pdf,
                    worker = worker,
                    onOpen = {
                        onEnsurePdfReady(pdf) { file ->
                            if (file != null) AmavasyaPdfHelper.openPdfFile(context, file)
                            else Toast.makeText(context, "Could not open PDF", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDownload = { handleDownload(pdf) },
                    onShare = {
                        onEnsurePdfReady(pdf) { file ->
                            if (file != null) {
                                AmavasyaPdfHelper.sharePdfFile(
                                    context,
                                    file,
                                    "KGN Construction - ${pdf.workerName} (${pdf.monthLabel})",
                                    "KGN Construction Monthly Hisab PDF (${Formatters.formatToIndianDate(pdf.periodEndDate)})"
                                )
                            }
                        }
                    },
                    onPrint = {
                        onEnsurePdfReady(pdf) { file ->
                            if (file != null) {
                                AmavasyaPdfHelper.printPdfFile(context, file, "KGN_${pdf.workerName}_${pdf.periodEndDate}")
                            }
                        }
                    },
                    onRegenerate = { onRegeneratePdf(pdf) }
                )
            }
        }
    }
}

@Composable
private fun MonthlyPdfCard(
    language: AppLanguage,
    pdf: MonthlyPdfEntity,
    worker: WorkerEntity?,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onRegenerate: () -> Unit
) {
    val isCombined = pdf.isCombinedReport
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCombined) ConstructionOrange.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isCombined) 1.5.dp else 1.dp,
            color = if (isCombined) ConstructionOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("monthly_pdf_card_${pdf.id}")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isCombined) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ConstructionOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = "Combined Report", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    } else {
                        WorkerAvatar(
                            workerName = pdf.workerName.ifBlank { pdf.workerId },
                            localPhotoPath = worker?.localPhotoPath.orEmpty(),
                            photoUrl = worker?.photoUrl.orEmpty(),
                            isActive = worker?.isActive ?: true,
                            size = 44.dp
                        )
                    }

                    Column {
                        Text(
                            text = if (isCombined) {
                                if (language == AppLanguage.HINDI) "संयुक्त मासिक रिपोर्ट (सभी मज़दूर)" else "Combined All-Workers Report"
                            } else {
                                pdf.workerName.ifBlank { pdf.workerId }
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pdf.monthLabel} • ${Formatters.formatToIndianDate(pdf.periodEndDate)}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val sizeKb = (pdf.fileSizeBytes / 1024L).coerceAtLeast(1L)
                        Text(
                            text = "${AmavasyaCalendar.formatTimestampIst(pdf.generatedAt)} • ${sizeKb}KB",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (pdf.downloadUrl.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Synced to Cloud",
                        tint = StatusPresent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            // Open / Download / Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalDark),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == AppLanguage.HINDI) "खोलें" else "Open", fontSize = 11.sp, color = Color.White)
                }

                Button(
                    onClick = onDownload,
                    colors = ButtonDefaults.buttonColors(containerColor = ConstructionOrange),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == AppLanguage.HINDI) "डाउनलोड" else "Save", fontSize = 11.sp, color = Color.White)
                }

                OutlinedButton(
                    onClick = onShare,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == AppLanguage.HINDI) "शेयर" else "Share", fontSize = 11.sp)
                }
            }

            // Print + Regenerate row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onPrint,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == AppLanguage.HINDI) "प्रिंट (Print)" else "Print", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onRegenerate,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == AppLanguage.HINDI) "पुनः बनाएं" else "Regenerate", fontSize = 11.sp)
                }
            }
        }
    }
}
