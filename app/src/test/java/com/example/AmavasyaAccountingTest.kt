package com.example

import com.example.data.model.AdvanceEntity
import com.example.data.model.AmavasyaSettlementEntity
import com.example.data.model.AttendanceEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.WorkerEntity
import com.example.util.AmavasyaCalendar
import com.example.util.AmavasyaHisabCalculator
import com.example.util.HisabSettlementStatus
import com.example.util.WorkerMessageHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmavasyaAccountingTest {

    @Test
    fun `upcoming amavasya dates are spaced roughly 29 to 30 days apart`() {
        val dates = AmavasyaCalendar.getUpcomingAmavasyaDates("2026-11-01", 6)
        assertEquals(6, dates.size)
        for (i in 0 until dates.size - 1) {
            val diff = AmavasyaCalendar.daysBetween(dates[i].isoDate, dates[i + 1].isoDate)
            assertTrue("Expected lunar cycle ~29-30 days, got $diff", diff in 28L..31L)
        }
        assertTrue(dates.first().displayDate.matches(Regex("\\d{2}/\\d{2}/\\d{4}")))
    }

    @Test
    fun `monthly hisab calculates earnings advances payments and prevents double counting across periods`() {
        val worker = WorkerEntity(
            id = "KGN-W001",
            name = "Ramesh Kumar",
            mobile = "+91 9876543210",
            dailyWage = 800.0,
            overtimeHourlyRate = 100.0
        )

        val attendance = listOf(
            // Period 1 (<= 2026-10-10)
            AttendanceEntity(
                id = "att1", workerId = worker.id, siteId = "S1", date = "2026-10-05",
                status = "PRESENT", paidDayFraction = 1.0, dailyWageSnapshot = 800.0,
                overtimeHours = 2.0, overtimeRateSnapshot = 100.0, calculatedEarnings = 1000.0
            ),
            AttendanceEntity(
                id = "att2", workerId = worker.id, siteId = "S1", date = "2026-10-06",
                status = "HALF_DAY", paidDayFraction = 0.5, dailyWageSnapshot = 800.0,
                overtimeHours = 0.0, overtimeRateSnapshot = 100.0, calculatedEarnings = 400.0
            ),
            // Period 2 (> 2026-10-10 and <= 2026-11-09)
            AttendanceEntity(
                id = "att3", workerId = worker.id, siteId = "S1", date = "2026-10-20",
                status = "PRESENT", paidDayFraction = 1.0, dailyWageSnapshot = 800.0,
                overtimeHours = 3.0, overtimeRateSnapshot = 100.0, calculatedEarnings = 1100.0
            )
        )

        val advances = listOf(
            AdvanceEntity(id = "adv1", workerId = worker.id, date = "2026-10-05", amount = 300.0),
            AdvanceEntity(id = "adv2", workerId = worker.id, date = "2026-10-25", amount = 200.0)
        )

        val payments = listOf(
            PaymentEntity(id = "pay1", workerId = worker.id, date = "2026-10-10", amount = 600.0)
        )

        // Period 1 live statement on 2026-10-10:
        // Base = 1200, OT = 200, Earnings = 1400, Adv = 300, Paid = 600 -> Net Payable = 500
        val stmt1 = AmavasyaHisabCalculator.calculateStatementForWorker(
            worker = worker,
            attendanceRecords = attendance,
            advances = advances,
            payments = payments,
            settlements = emptyList(),
            audits = emptyList(),
            selectedAmavasyaDate = "2026-10-10"
        )
        assertEquals(1, stmt1.presentDays)
        assertEquals(1, stmt1.halfDays)
        assertEquals(1.5, stmt1.payableManDays, 0.001)
        assertEquals(1200.0, stmt1.totalWages, 0.01)
        assertEquals(200.0, stmt1.totalOvertimeAmount, 0.01)
        assertEquals(1400.0, stmt1.totalEarnings, 0.01)
        assertEquals(500.0, stmt1.payableAmount, 0.01)
        assertEquals(0.0, stmt1.outstandingAdvanceAmount, 0.01)
        assertEquals(HisabSettlementStatus.PARTIAL, stmt1.status)
        assertFalse("Cannot mark fully settled when ₹500 payable remains", stmt1.canMarkFullySettled)

        // Lock Period 1 as CARRIED_FORWARD with ₹500 carried forward
        val lockedPeriod1 = AmavasyaSettlementEntity(
            id = stmt1.settlementId,
            workerId = worker.id,
            workerName = worker.name,
            periodStartDate = stmt1.periodStartDate,
            periodEndDate = "2026-10-10",
            presentDays = stmt1.presentDays,
            halfDays = stmt1.halfDays,
            payableManDays = stmt1.payableManDays,
            totalWages = stmt1.totalWages,
            overtimeHours = stmt1.totalOvertimeHours,
            overtimeAmount = stmt1.totalOvertimeAmount,
            totalEarnings = stmt1.totalEarnings,
            previousBalance = 0.0,
            advancesTaken = 300.0,
            paymentsMade = 600.0,
            netBalance = 500.0,
            payableAmount = 500.0,
            outstandingAdvanceAmount = 0.0,
            carryForwardBalance = 500.0,
            status = "CARRIED_FORWARD",
            isLocked = true
        )

        // Period 2 live statement on 2026-11-09:
        // Must ONLY include att3 (800 + 300 = 1100) and adv2 (200), plus previousBalance (500)
        // Net Payable = 500 + 1100 - 200 - 0 = 1400 (NO double counting of Period 1 records!)
        val stmt2 = AmavasyaHisabCalculator.calculateStatementForWorker(
            worker = worker,
            attendanceRecords = attendance,
            advances = advances,
            payments = payments,
            settlements = listOf(lockedPeriod1),
            audits = emptyList(),
            selectedAmavasyaDate = "2026-11-09"
        )
        assertEquals(1, stmt2.presentDays)
        assertEquals(1100.0, stmt2.totalEarnings, 0.01)
        assertEquals(500.0, stmt2.previousBalance, 0.01)
        assertEquals(200.0, stmt2.advancesTaken, 0.01)
        assertEquals(0.0, stmt2.paymentsMade, 0.01)
        assertEquals(1400.0, stmt2.payableAmount, 0.01)

        // After full payment ₹1400, account can be marked fully settled
        val paidStmt2 = AmavasyaHisabCalculator.calculateStatementForWorker(
            worker = worker,
            attendanceRecords = attendance,
            advances = advances,
            payments = payments + PaymentEntity(id = "pay2", workerId = worker.id, date = "2026-11-09", amount = 1400.0),
            settlements = listOf(lockedPeriod1),
            audits = emptyList(),
            selectedAmavasyaDate = "2026-11-09"
        )
        assertEquals(0.0, paidStmt2.payableAmount, 0.01)
        assertTrue(paidStmt2.canMarkFullySettled)

        val message = WorkerMessageHelper.buildAmavasyaHisabMessage(stmt2)
        assertTrue(message.contains("Dear Ramesh Kumar,"))
        assertTrue(message.contains("Final Payable Amount (To Worker): ₹1400"))
        assertTrue(message.contains("Regards,\nKGN Construction"))
    }
}
