package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.AttendanceEntity
import com.example.data.model.AttendanceStatus
import com.example.data.model.MonthlyPdfEntity
import com.example.data.model.WorkerEntity
import com.example.util.AmavasyaPdfHelper
import com.example.util.AttendanceReminderManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PhotosPdfReminderTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `findPendingAttendanceWorkers excludes marked including ABSENT and inactive workers`() {
        val workers = listOf(
            WorkerEntity(id = "W1", name = "Ramesh", isActive = true),
            WorkerEntity(id = "W2", name = "Suresh", isActive = true),
            WorkerEntity(id = "W3", name = "Mahesh", isActive = true),
            WorkerEntity(id = "W4", name = "Old Worker", isActive = false)
        )
        val todayAtt = listOf(
            AttendanceEntity(
                id = "att_W1", workerId = "W1", siteId = "S1",
                date = "2026-11-12", status = AttendanceStatus.PRESENT.name
            ),
            AttendanceEntity(
                id = "att_W2", workerId = "W2", siteId = "S1",
                date = "2026-11-12", status = AttendanceStatus.ABSENT.name
            )
        )

        val pending = AttendanceReminderManager.findPendingAttendanceWorkers(workers, todayAtt)
        assertEquals(1, pending.size)
        assertEquals("W3", pending.first().id)

        val message = AttendanceReminderManager.buildReminderMessage(pending)
        assertEquals(
            "Aaj ki attendance complete karein. Pending workers: Mahesh. Total pending: 1.",
            message
        )
    }

    @Test
    fun `computeNextTriggerMillisIst schedules today before 18_30 and tomorrow after 18_30`() {
        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        val beforeCal = Calendar.getInstance(ist).apply {
            set(2026, Calendar.NOVEMBER, 12, 16, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerSameDay = AttendanceReminderManager.computeNextTriggerMillisIst(18, 30, beforeCal.timeInMillis)
        val outCal1 = Calendar.getInstance(ist).apply { timeInMillis = triggerSameDay }
        assertEquals(12, outCal1.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, outCal1.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, outCal1.get(Calendar.MINUTE))

        val afterCal = Calendar.getInstance(ist).apply {
            set(2026, Calendar.NOVEMBER, 12, 18, 30, 1)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerNextDay = AttendanceReminderManager.computeNextTriggerMillisIst(18, 30, afterCal.timeInMillis)
        val outCal2 = Calendar.getInstance(ist).apply { timeInMillis = triggerNextDay }
        assertEquals(13, outCal2.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, outCal2.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, outCal2.get(Calendar.MINUTE))
    }

    @Test
    fun `parseYearMonthFromIso getPersistentPdfFile and Room enforce unique period per worker`() = runBlocking {
        val (year, month, label) = AmavasyaPdfHelper.parseYearMonthFromIso("2026-11-09")
        assertEquals(2026, year)
        assertEquals(11, month)
        assertEquals("November 2026", label)

        val worker = WorkerEntity(
            id = "KGN-W101",
            name = "Arjun Mistri",
            category = "Mason",
            dailyWage = 900.0,
            mobile = "+91 9876543210",
            siteId = "S1"
        )
        val persistentWorkerFile = AmavasyaPdfHelper.getPersistentPdfFile(
            context,
            year,
            month,
            "KGN_Hisab_${worker.id}_2026-11-09.pdf"
        )
        assertTrue(persistentWorkerFile.absolutePath.contains("monthly_hisab_pdfs/2026/11"))

        val persistentCombinedFile = AmavasyaPdfHelper.getPersistentPdfFile(
            context,
            year,
            month,
            "KGN_Combined_Monthly_Hisab_2026-11-09.pdf"
        )
        assertTrue(persistentCombinedFile.absolutePath.contains("monthly_hisab_pdfs/2026/11"))

        val pdfDao = db.monthlyPdfDao()
        pdfDao.insertMonthlyPdf(
            MonthlyPdfEntity(
                id = "pdf_${worker.id}_2026-11-09",
                workerId = worker.id,
                workerName = worker.name,
                year = 2026,
                month = 11,
                monthLabel = label,
                periodEndDate = "2026-11-09",
                localFilePath = persistentWorkerFile.absolutePath,
                fileSizeBytes = 1024L
            )
        )
        // Re-inserting with same (workerId, periodEndDate) replaces cleanly without duplicates
        pdfDao.insertMonthlyPdf(
            MonthlyPdfEntity(
                id = "pdf_${worker.id}_2026-11-09",
                workerId = worker.id,
                workerName = worker.name,
                year = 2026,
                month = 11,
                monthLabel = label,
                periodEndDate = "2026-11-09",
                localFilePath = persistentWorkerFile.absolutePath,
                fileSizeBytes = 2048L
            )
        )
        val savedList = pdfDao.getAllMonthlyPdfsSync()
        assertEquals(1, savedList.size)
        assertEquals(2048L, savedList.first().fileSizeBytes)
        assertNotNull(pdfDao.getByWorkerAndPeriodSync(worker.id, "2026-11-09"))
    }
}
