package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.SiteEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class GeneratedPdfFileResult(
    val file: File,
    val year: Int,
    val month: Int,
    val monthLabel: String
)

object AmavasyaPdfHelper {

    fun parseYearMonthFromIso(isoDate: String): Triple<Int, Int, String> {
        val parts = isoDate.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).get(Calendar.YEAR)
        val month = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: (Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).get(Calendar.MONTH) + 1)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
            clear()
            set(year, month - 1, 1)
        }
        val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }.format(cal.time)
        return Triple(year, month, monthLabel)
    }

    fun getPersistentPdfFile(context: Context, year: Int, month: Int, fileName: String): File {
        val dir = File(
            context.filesDir,
            String.format(Locale.US, "monthly_hisab_pdfs/%04d/%02d", year, month)
        ).apply {
            if (!exists()) mkdirs()
        }
        return File(dir, fileName)
    }

    fun generatePersistentWorkerPdf(
        context: Context,
        statement: WorkerHisabStatement,
        siteName: String = "General"
    ): GeneratedPdfFileResult? {
        return try {
            val (year, month, monthLabel) = parseYearMonthFromIso(statement.periodEndDate)
            val safeWorkerId = statement.worker.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val file = getPersistentPdfFile(
                context,
                year,
                month,
                "KGN_Hisab_${safeWorkerId}_${statement.periodEndDate}.pdf"
            )
            val workerBitmap = WorkerPhotoHelper.loadBitmapForPdf(context, statement.worker)

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            drawStatementPage(page.canvas, statement, siteName, workerBitmap)
            document.finishPage(page)

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            workerBitmap?.recycle()

            GeneratedPdfFileResult(file = file, year = year, month = month, monthLabel = monthLabel)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generatePersistentCombinedMonthlyPdf(
        context: Context,
        periodEndDate: String,
        statements: List<WorkerHisabStatement>,
        sites: List<SiteEntity>
    ): GeneratedPdfFileResult? {
        return try {
            val (year, month, monthLabel) = parseYearMonthFromIso(periodEndDate)
            val file = getPersistentPdfFile(
                context,
                year,
                month,
                "KGN_Combined_Monthly_Hisab_${periodEndDate}.pdf"
            )
            val sitesMap = sites.associateBy { it.id }

            val document = PdfDocument()
            val pageWidth = 595f
            val margin = 32f
            val contentRight = pageWidth - margin

            val charcoalPaint = Paint().apply { color = Color.parseColor("#1E2022") }
            val orangePaint = Paint().apply { color = Color.parseColor("#FF6D00") }
            val lightBgPaint = Paint().apply { color = Color.parseColor("#F8F9FA") }
            val altRowPaint = Paint().apply { color = Color.parseColor("#FFF7ED") }
            val borderPaint = Paint().apply {
                color = Color.parseColor("#D1D5DB")
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 19f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CFD8DC")
                textSize = 10.5f
            }
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF6D00")
                textSize = 11.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#111827")
                textSize = 9.5f
            }
            val cellBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#111827")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cellRightPaint = Paint(cellPaint).apply { textAlign = Paint.Align.RIGHT }
            val cellRightBoldPaint = Paint(cellBoldPaint).apply { textAlign = Paint.Align.RIGHT }

            val totalEarnings = statements.sumOf { it.totalEarnings }
            val totalAdvances = statements.sumOf { it.advancesTaken }
            val totalPayments = statements.sumOf { it.paymentsMade }
            val totalPayable = statements.sumOf { it.payableAmount }
            val totalAdvDue = statements.sumOf { it.outstandingAdvanceAmount }
            val totalManDays = statements.sumOf { it.payableManDays }

            val rowsPerPageFirst = 16
            val rowsPerPageNext = 24
            val chunks = mutableListOf<List<WorkerHisabStatement>>()
            if (statements.isEmpty()) {
                chunks.add(emptyList())
            } else {
                var idx = 0
                chunks.add(statements.subList(0, minOf(rowsPerPageFirst, statements.size)))
                idx = minOf(rowsPerPageFirst, statements.size)
                while (idx < statements.size) {
                    val end = minOf(idx + rowsPerPageNext, statements.size)
                    chunks.add(statements.subList(idx, end))
                    idx = end
                }
            }

            chunks.forEachIndexed { pageIndex, pageRows ->
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageIndex + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // Banner
                canvas.drawRect(0f, 0f, pageWidth, 84f, charcoalPaint)
                canvas.drawRect(0f, 80f, pageWidth, 84f, orangePaint)
                canvas.drawText("KGN CONSTRUCTION", margin, 32f, headerTitlePaint)
                canvas.drawText(
                    "COMBINED MONTHLY HISAB REPORT — $monthLabel",
                    margin,
                    50f,
                    headerSubPaint
                )
                canvas.drawText(
                    "Amavasya Date: ${Formatters.formatToIndianDate(periodEndDate)}  |  Workers: ${statements.size}  |  Page ${pageIndex + 1}/${chunks.size}",
                    margin,
                    68f,
                    headerSubPaint
                )

                var y = 104f

                if (pageIndex == 0) {
                    val summaryRect = RectF(margin, y, contentRight, y + 76f)
                    canvas.drawRoundRect(summaryRect, 8f, 8f, lightBgPaint)
                    canvas.drawRoundRect(summaryRect, 8f, 8f, borderPaint)
                    canvas.drawText("MONTHLY GRAND SUMMARY", margin + 10f, y + 18f, sectionPaint)

                    canvas.drawText("Workers: ${statements.size}  |  Man-Days: ${String.format("%.1f", totalManDays)}", margin + 10f, y + 36f, cellBoldPaint)
                    canvas.drawText("Total Earnings: ${Formatters.formatInr(totalEarnings)}", margin + 10f, y + 52f, cellPaint)
                    canvas.drawText("Advances: ${Formatters.formatInr(totalAdvances)}  |  Paid: ${Formatters.formatInr(totalPayments)}", margin + 10f, y + 66f, cellPaint)

                    val rightSummaryPaint = Paint(cellRightBoldPaint).apply { color = Color.parseColor("#FF6D00"); textSize = 11f }
                    canvas.drawText("Net Payable: ${Formatters.formatInr(totalPayable)}", contentRight - 10f, y + 40f, rightSummaryPaint)
                    val advPaint = Paint(cellRightPaint).apply { color = Color.parseColor("#DC2626") }
                    canvas.drawText("Adv Recoverable: ${Formatters.formatInr(totalAdvDue)}", contentRight - 10f, y + 58f, advPaint)

                    y += 94f
                }

                // Table Header
                canvas.drawRect(margin, y, contentRight, y + 22f, charcoalPaint)
                val thPaint = Paint(cellBoldPaint).apply { color = Color.WHITE }
                val thRightPaint = Paint(cellRightBoldPaint).apply { color = Color.WHITE }
                canvas.drawText("Worker", margin + 6f, y + 14f, thPaint)
                canvas.drawText("Site / Skill", margin + 152f, y + 14f, thPaint)
                canvas.drawText("Days", margin + 275f, y + 14f, thRightPaint)
                canvas.drawText("Earnings", margin + 345f, y + 14f, thRightPaint)
                canvas.drawText("Adv+Paid", margin + 425f, y + 14f, thRightPaint)
                canvas.drawText("Net Bal", contentRight - 6f, y + 14f, thRightPaint)
                y += 22f

                pageRows.forEachIndexed { rowIdx, stmt ->
                    val rowHeight = 28f
                    if (rowIdx % 2 == 1) {
                        canvas.drawRect(margin, y, contentRight, y + rowHeight, altRowPaint)
                    }
                    canvas.drawLine(margin, y + rowHeight, contentRight, y + rowHeight, borderPaint)

                    val siteLabel = sitesMap[stmt.worker.siteId]?.name ?: "General"
                    val nameLine = stmt.worker.name.take(20)
                    val skillLine = "${stmt.worker.category.take(10)} • ${siteLabel.take(12)}"
                    val netText = when {
                        stmt.payableAmount > 0.01 -> Formatters.formatInr(stmt.payableAmount)
                        stmt.outstandingAdvanceAmount > 0.01 -> "-${Formatters.formatInr(stmt.outstandingAdvanceAmount)}"
                        else -> "₹0"
                    }
                    val netColor = when {
                        stmt.payableAmount > 0.01 -> Color.parseColor("#C2410C")
                        stmt.outstandingAdvanceAmount > 0.01 -> Color.parseColor("#DC2626")
                        else -> Color.parseColor("#15803D")
                    }
                    val rowNetPaint = Paint(cellRightBoldPaint).apply { color = netColor }

                    canvas.drawText(nameLine, margin + 6f, y + 12f, cellBoldPaint)
                    canvas.drawText(stmt.worker.id, margin + 6f, y + 23f, Paint(cellPaint).apply { textSize = 8f; color = Color.GRAY })
                    canvas.drawText(skillLine, margin + 152f, y + 17f, cellPaint)
                    canvas.drawText(String.format("%.1f", stmt.payableManDays), margin + 275f, y + 17f, cellRightPaint)
                    canvas.drawText(Formatters.formatInr(stmt.totalEarnings), margin + 345f, y + 17f, cellRightPaint)
                    canvas.drawText(Formatters.formatInr(stmt.advancesTaken + stmt.paymentsMade), margin + 425f, y + 17f, cellRightPaint)
                    canvas.drawText(netText, contentRight - 6f, y + 17f, rowNetPaint)
                    y += rowHeight
                }

                // Footer
                val footerY = 804f
                canvas.drawLine(margin, footerY - 14f, contentRight, footerY - 14f, borderPaint)
                val footerLabel = Paint(cellPaint).apply { color = Color.parseColor("#4B5563"); textSize = 9.5f }
                val sigLabel = Paint(footerLabel).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(
                    "Generated on ${AmavasyaCalendar.formatTimestampIst(System.currentTimeMillis())} (IST)",
                    margin,
                    footerY,
                    footerLabel
                )
                canvas.drawText("Authorized Signatory - KGN Construction", contentRight, footerY, sigLabel)

                document.finishPage(page)
            }

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()

            GeneratedPdfFileResult(file = file, year = year, month = month, monthLabel = monthLabel)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateStatementPdf(context: Context, statement: WorkerHisabStatement, siteName: String = "General"): File? {
        return try {
            val pdfDir = File(context.cacheDir, "hisab_pdfs").apply {
                if (!exists()) mkdirs()
            }
            val safeWorkerId = statement.worker.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val file = File(pdfDir, "KGN_Amavasya_Hisab_${safeWorkerId}_${statement.periodEndDate}.pdf")
            val workerBitmap = WorkerPhotoHelper.loadBitmapForPdf(context, statement.worker)

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            drawStatementPage(page.canvas, statement, siteName, workerBitmap)
            document.finishPage(page)

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            workerBitmap?.recycle()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun openPdfFile(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "PDF file not found on device", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            sharePdfFile(context, file, file.nameWithoutExtension, "KGN Construction Monthly Hisab PDF")
        }
    }

    fun sharePdfFile(context: Context, file: File, subject: String, body: String) {
        if (!file.exists()) {
            Toast.makeText(context, "PDF file not found on device", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Hisab PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No app available to share PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun saveToDownloadsQ(context: Context, sourceFile: File, displayFileName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !sourceFile.exists()) return false
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayFileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/KGN Construction")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun printPdfFile(context: Context, file: File, jobName: String) {
        if (!file.exists()) {
            Toast.makeText(context, "PDF file not found on device", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "Print service unavailable", Toast.LENGTH_SHORT).show()
                return
            }
            val adapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder(file.name)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                        .build()
                    callback.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback
                ) {
                    try {
                        FileInputStream(file).use { input ->
                            FileOutputStream(destination.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback.onWriteFailed(e.message)
                    }
                }
            }
            printManager.print(jobName, adapter, PrintAttributes.Builder().build())
        } catch (e: Exception) {
            Toast.makeText(context, "Print failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportAndSharePdf(context: Context, statement: WorkerHisabStatement, siteName: String = "General") {
        val file = generateStatementPdf(context, statement, siteName)
        if (file == null || !file.exists()) {
            Toast.makeText(context, "Could not generate PDF statement", Toast.LENGTH_SHORT).show()
            return
        }
        sharePdfFile(
            context,
            file,
            "KGN Construction Hisab - ${statement.worker.name} (${statement.periodDisplay})",
            WorkerMessageHelper.buildAmavasyaHisabMessage(statement)
        )
    }

    fun viewPdf(context: Context, statement: WorkerHisabStatement, siteName: String = "General") {
        val file = generateStatementPdf(context, statement, siteName)
        if (file == null || !file.exists()) {
            Toast.makeText(context, "Could not create PDF", Toast.LENGTH_SHORT).show()
            return
        }
        openPdfFile(context, file)
    }

    fun exportSettlementHistoryPdf(
        context: Context,
        settlement: com.example.data.model.AmavasyaSettlementEntity,
        worker: com.example.data.model.WorkerEntity?,
        siteName: String = "General"
    ) {
        val resolvedWorker = worker ?: com.example.data.model.WorkerEntity(
            id = settlement.workerId,
            name = settlement.workerName.ifBlank { settlement.workerId },
            category = "Construction Worker",
            dailyWage = 0.0
        )
        val statusEnum = when (settlement.status.uppercase()) {
            "COMPLETED" -> HisabSettlementStatus.COMPLETED
            "CARRIED_FORWARD" -> HisabSettlementStatus.CARRIED_FORWARD
            "PARTIAL" -> HisabSettlementStatus.PARTIAL
            else -> HisabSettlementStatus.PENDING
        }
        val startDisp = if (settlement.periodStartDate.isNotBlank()) {
            Formatters.formatToIndianDate(settlement.periodStartDate)
        } else {
            "Start"
        }
        val endDisp = Formatters.formatToIndianDate(settlement.periodEndDate)
        val snapshotStatement = WorkerHisabStatement(
            worker = resolvedWorker,
            settlementId = settlement.id,
            periodStartDate = settlement.periodStartDate,
            periodEndDate = settlement.periodEndDate,
            periodDisplay = "$startDisp - $endDisp",
            presentDays = settlement.presentDays,
            halfDays = settlement.halfDays,
            absentDays = settlement.absentDays,
            paidLeaveDays = settlement.paidLeaveDays,
            payableManDays = settlement.payableManDays,
            totalWages = settlement.totalWages,
            totalOvertimeHours = settlement.overtimeHours,
            totalOvertimeAmount = settlement.overtimeAmount,
            totalEarnings = settlement.totalEarnings,
            previousBalance = settlement.previousBalance,
            advancesTaken = settlement.advancesTaken,
            paymentsMade = settlement.paymentsMade,
            netBalance = settlement.netBalance,
            payableAmount = settlement.payableAmount,
            outstandingAdvanceAmount = settlement.outstandingAdvanceAmount,
            status = statusEnum,
            isLocked = settlement.isLocked,
            settledAt = settlement.settledAt,
            notes = settlement.notes,
            periodAttendance = emptyList(),
            periodAdvances = emptyList(),
            periodPayments = emptyList(),
            auditLogs = emptyList()
        )
        exportAndSharePdf(context, snapshotStatement, siteName)
    }

    private fun drawStatementPage(
        canvas: Canvas,
        statement: WorkerHisabStatement,
        siteName: String,
        workerPhoto: Bitmap?
    ) {
        val pageWidth = 595f
        val margin = 36f
        val contentRight = pageWidth - margin

        val charcoalPaint = Paint().apply { color = Color.parseColor("#1E2022") }
        val orangePaint = Paint().apply { color = Color.parseColor("#FF6D00") }
        val lightBgPaint = Paint().apply { color = Color.parseColor("#F8F9FA") }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CFD8DC")
            textSize = 11f
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF6D00")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563")
            textSize = 11.5f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val valueLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Top Charcoal Header Banner
        canvas.drawRect(0f, 0f, pageWidth, 96f, charcoalPaint)
        canvas.drawRect(0f, 92f, pageWidth, 96f, orangePaint)
        canvas.drawText("KGN CONSTRUCTION", margin, 38f, headerTitlePaint)
        canvas.drawText("AMAVASYA MONTHLY HISAB / ACCOUNT STATEMENT", margin, 58f, headerSubPaint)
        canvas.drawText(
            "Settlement Date: ${Formatters.formatToIndianDate(statement.periodEndDate)}  |  Status: ${statement.status.labelEn}",
            margin,
            76f,
            headerSubPaint
        )

        var y = 118f

        // Worker Info Card with Circular Worker Photo
        val workerCardRect = RectF(margin, y, contentRight, y + 88f)
        canvas.drawRoundRect(workerCardRect, 8f, 8f, lightBgPaint)
        canvas.drawRoundRect(workerCardRect, 8f, 8f, borderPaint)

        val avatarCenterX = margin + 40f
        val avatarCenterY = y + 44f
        val avatarRadius = 28f
        if (workerPhoto != null) {
            drawCircularBitmap(canvas, workerPhoto, avatarCenterX, avatarCenterY, avatarRadius)
        } else {
            val circleBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFEAD5") }
            val circleText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF6D00")
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, circleBg)
            val initial = statement.worker.name.trim().take(1).uppercase().ifBlank { "W" }
            canvas.drawText(initial, avatarCenterX, avatarCenterY + 7f, circleText)
        }
        val avatarBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF6D00")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, avatarBorder)

        val textLeft = margin + 80f
        canvas.drawText("WORKER DETAILS", textLeft, y + 18f, sectionPaint)
        canvas.drawText("Name: ${statement.worker.name} (${statement.worker.id})", textLeft, y + 36f, valueLeftPaint)
        canvas.drawText(
            "Skill: ${statement.worker.category}  |  Site: $siteName  |  Wage: ${Formatters.formatInr(statement.worker.dailyWage)}/day",
            textLeft,
            y + 54f,
            labelPaint
        )
        val phoneText = if (statement.worker.mobile.isNotBlank()) statement.worker.mobile else "Not Provided"
        canvas.drawText(
            "Mobile: $phoneText   |   Period: ${statement.periodDisplay}",
            textLeft,
            y + 72f,
            labelPaint
        )

        y += 108f

        // Attendance Breakdown Section
        canvas.drawText("ATTENDANCE & MAN-DAYS SUMMARY", margin, y, sectionPaint)
        y += 12f

        val attRows = listOf(
            "Present Days" to "${statement.presentDays} days",
            "Half Days (0.5)" to "${statement.halfDays} days",
            "Paid Leave Days" to "${statement.paidLeaveDays} days",
            "Absent Days" to "${statement.absentDays} days",
            "Total Payable Man-Days" to String.format("%.1f days", statement.payableManDays),
            "Overtime Worked" to String.format("%.1f hrs", statement.totalOvertimeHours)
        )
        attRows.forEach { (label, valText) ->
            y += 19f
            canvas.drawText(label, margin + 8f, y, labelPaint)
            canvas.drawText(valText, contentRight - 8f, y, valuePaint)
            canvas.drawLine(margin, y + 6f, contentRight, y + 6f, borderPaint)
        }

        y += 26f

        // Financial Statement Breakdown
        canvas.drawText("FINANCIAL HISAB CALCULATION (INR)", margin, y, sectionPaint)
        y += 12f

        val finRows = listOf(
            "Wages for Payable Man-Days" to Formatters.formatInr(statement.totalWages),
            "Overtime Earnings" to Formatters.formatInr(statement.totalOvertimeAmount),
            "Total Period Earnings" to Formatters.formatInr(statement.totalEarnings),
            "Previous Settlement Balance B/F" to formatSignedBalance(statement.previousBalance),
            "Less: Advances Taken in Period" to "- ${Formatters.formatInr(statement.advancesTaken)}",
            "Less: Payments Made in Period" to "- ${Formatters.formatInr(statement.paymentsMade)}"
        )
        finRows.forEach { (label, valText) ->
            y += 21f
            canvas.drawText(label, margin + 8f, y, labelPaint)
            canvas.drawText(valText, contentRight - 8f, y, valuePaint)
            canvas.drawLine(margin, y + 6f, contentRight, y + 6f, borderPaint)
        }

        y += 22f

        // Highlighted Net Settlement Box
        val netBox = RectF(margin, y, contentRight, y + 68f)
        val highlightFill = Paint().apply {
            color = when {
                statement.payableAmount > 0 -> Color.parseColor("#FFF7ED")
                statement.outstandingAdvanceAmount > 0 -> Color.parseColor("#FEF2F2")
                else -> Color.parseColor("#F0FDF4")
            }
        }
        val highlightBorder = Paint().apply {
            color = when {
                statement.payableAmount > 0 -> Color.parseColor("#FF6D00")
                statement.outstandingAdvanceAmount > 0 -> Color.parseColor("#DC2626")
                else -> Color.parseColor("#16A34A")
            }
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
        }
        canvas.drawRoundRect(netBox, 10f, 10f, highlightFill)
        canvas.drawRoundRect(netBox, 10f, 10f, highlightBorder)

        val netTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = highlightBorder.color
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val netAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = highlightBorder.color
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val (headlineLabel, headlineValue, subExplanation) = when {
            statement.payableAmount > 0 -> Triple(
                "FINAL PAYABLE AMOUNT (TO WORKER)",
                Formatters.formatInr(statement.payableAmount),
                "Advance Recoverable: ₹0  •  Payable by KGN Construction"
            )
            statement.outstandingAdvanceAmount > 0 -> Triple(
                "FINAL OUTSTANDING AMOUNT (ADVANCE DUE)",
                Formatters.formatInr(statement.outstandingAdvanceAmount),
                "Payable to Worker: ₹0  •  Recoverable from Worker / Carry Forward"
            )
            else -> Triple(
                "ACCOUNT BALANCED / SETTLED",
                "₹0",
                "Final Payable: ₹0  •  Final Advance Outstanding: ₹0"
            )
        }
        canvas.drawText(headlineLabel, margin + 14f, y + 25f, netTitlePaint)
        canvas.drawText(headlineValue, contentRight - 14f, y + 31f, netAmountPaint)
        canvas.drawText(subExplanation, margin + 14f, y + 52f, labelPaint)

        y += 90f

        if (statement.periodPayments.isNotEmpty() || statement.periodAdvances.isNotEmpty()) {
            canvas.drawText("TRANSACTIONS INCLUDED IN THIS HISAB", margin, y, sectionPaint)
            y += 15f
            val txLines = mutableListOf<String>()
            statement.periodAdvances.take(4).forEach { adv ->
                txLines.add("Advance • ${Formatters.formatToIndianDate(adv.date)} • ${Formatters.formatInr(adv.amount)} (${adv.paymentMethod})")
            }
            statement.periodPayments.take(4).forEach { pay ->
                txLines.add("Payment • ${Formatters.formatToIndianDate(pay.date)} • ${Formatters.formatInr(pay.amount)} (${pay.paymentMethod})")
            }
            txLines.take(5).forEach { line ->
                canvas.drawText(line, margin + 8f, y, labelPaint)
                y += 15f
            }
        }

        // Signature Box & Footer
        val footerY = 804f
        canvas.drawLine(contentRight - 180f, footerY - 34f, contentRight, footerY - 34f, borderPaint)
        val sigTitlePaint = Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 10f }
        canvas.drawText("Authorized Signature / Stamp", contentRight, footerY - 20f, sigTitlePaint)

        canvas.drawLine(margin, footerY - 12f, contentRight, footerY - 12f, borderPaint)
        canvas.drawText(
            "Report Date: ${AmavasyaCalendar.formatTimestampIst(System.currentTimeMillis())} (IST)",
            margin,
            footerY + 4f,
            labelPaint
        )
        val brandFooterPaint = Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("KGN Construction • Official Hisab Statement", contentRight, footerY + 4f, brandFooterPaint)
    }

    private fun drawCircularBitmap(canvas: Canvas, src: Bitmap, cx: Float, cy: Float, radius: Float) {
        val diameter = (radius * 2f)
        val scale = maxOf(diameter / src.width, diameter / src.height)
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(cx - (src.width * scale) / 2f, cy - (src.height * scale) / 2f)
        }
        val shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(matrix)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = shader
        }
        canvas.drawCircle(cx, cy, radius, paint)
    }

    private fun formatSignedBalance(amount: Double): String {
        return when {
            amount > 0.01 -> "${Formatters.formatInr(amount)} (Payable B/F)"
            amount < -0.01 -> "- ${Formatters.formatInr(-amount)} (Advance B/F)"
            else -> "₹0"
        }
    }
}
