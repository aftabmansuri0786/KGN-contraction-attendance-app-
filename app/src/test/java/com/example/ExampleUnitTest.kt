package com.example

import com.example.data.model.AttendanceStatus
import com.example.util.WorkerMessageHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PayrollCalculationUnitTest {

    @Test
    fun testAttendanceFractionAndEarnings() {
        val dailyWage = 800.0
        val otRate = 100.0

        // Present with 2 hours overtime
        val presentEarnings = (AttendanceStatus.PRESENT.fraction * dailyWage) + (2.0 * otRate)
        assertEquals(1000.0, presentEarnings, 0.001)

        // Half day with 0 overtime
        val halfDayEarnings = (AttendanceStatus.HALF_DAY.fraction * dailyWage) + (0.0 * otRate)
        assertEquals(400.0, halfDayEarnings, 0.001)

        // Absent
        val absentEarnings = (AttendanceStatus.ABSENT.fraction * dailyWage)
        assertEquals(0.0, absentEarnings, 0.001)
    }

    @Test
    fun testWorkerBalanceAccounting() {
        val totalEarned = 15000.0
        val totalAdvances = 3000.0
        val totalPayments = 8000.0

        val netUnpaidWages = totalEarned - totalPayments
        assertEquals(7000.0, netUnpaidWages, 0.001)

        // If advances are deducted from settlement
        val finalSettlementDue = netUnpaidWages - totalAdvances
        assertEquals(4000.0, finalSettlementDue, 0.001)
    }

    @Test
    fun testDefaultCountryCodeAndPhoneFormatting() {
        // 10 digits entered without country code
        val formatted = WorkerMessageHelper.formatWithCountryCode("9876543210")
        assertEquals("+91 9876543210", formatted)

        // Digits extracted
        val local10 = WorkerMessageHelper.extractLocal10Digits("+91 9876543210")
        assertEquals("9876543210", local10)

        // Phone masked with +91
        val masked = WorkerMessageHelper.maskPhoneNumber("+91 9876543210")
        assertEquals("+91 98765*****", masked)

        // WhatsApp clean
        val waClean = WorkerMessageHelper.cleanForWhatsApp("+91 9876543210")
        assertEquals("919876543210", waClean)

        // SMS clean
        val smsClean = WorkerMessageHelper.cleanForSms("+91 9876543210")
        assertEquals("9876543210", smsClean)

        assertTrue(WorkerMessageHelper.isValidPhoneNumber(formatted))
    }
}
