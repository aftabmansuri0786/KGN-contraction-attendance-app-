package com.example.util

import com.example.data.model.AdvanceEntity
import com.example.data.model.AmavasyaSettlementEntity
import com.example.data.model.AttendanceEntity
import com.example.data.model.AttendanceStatus
import com.example.data.model.PaymentEntity
import com.example.data.model.SettlementAuditEntity
import com.example.data.model.WorkerEntity
import kotlin.math.abs

enum class HisabSettlementStatus(val labelEn: String, val labelHi: String) {
    PENDING("Pending", "बाकी / शेष"),
    PARTIAL("Partial Paid", "आंशिक भुगतान"),
    COMPLETED("Settled (Locked)", "हिसाब पूर्ण (लॉक)"),
    CARRIED_FORWARD("Carried Forward (Locked)", "आगे बढ़ाया गया (लॉक)")
}

data class WorkerHisabStatement(
    val worker: WorkerEntity,
    val settlementId: String,
    val periodStartDate: String, // YYYY-MM-DD (inclusive start of current period) or "" if from start
    val periodEndDate: String, // YYYY-MM-DD (selected Amavasya date)
    val periodDisplay: String, // e.g., "11/10/2026 - 09/11/2026"
    val presentDays: Int,
    val halfDays: Int,
    val absentDays: Int,
    val paidLeaveDays: Int,
    val payableManDays: Double,
    val totalWages: Double, // base wage earnings without overtime
    val totalOvertimeHours: Double,
    val totalOvertimeAmount: Double,
    val totalEarnings: Double, // totalWages + totalOvertimeAmount
    val previousBalance: Double, // +ve = unpaid wage from previous period, -ve = advance due from previous period
    val advancesTaken: Double,
    val paymentsMade: Double,
    val netBalance: Double, // previousBalance + totalEarnings - advancesTaken - paymentsMade
    val payableAmount: Double, // Amount contractor must pay worker (max(0, netBalance))
    val outstandingAdvanceAmount: Double, // Advance worker owes contractor (max(0, -netBalance))
    val status: HisabSettlementStatus,
    val isLocked: Boolean,
    val settledAt: Long?,
    val notes: String,
    val periodAttendance: List<AttendanceEntity>,
    val periodAdvances: List<AdvanceEntity>,
    val periodPayments: List<PaymentEntity>,
    val auditLogs: List<SettlementAuditEntity>
) {
    /**
     * Accounting rule: Do not mark a worker's account fully settled until
     * actual payment has been recorded (when payable > 0) or balance is zeroed out.
     */
    val canMarkFullySettled: Boolean
        get() = payableAmount <= 0.01 && (
            netBalance >= -0.01 && (
                (totalEarnings + previousBalance <= 0.01) ||
                    paymentsMade > 0.0 ||
                    advancesTaken > 0.0
                )
            )

    val requiresSettlementAction: Boolean
        get() = !isLocked && (
            periodAttendance.isNotEmpty() ||
                periodAdvances.isNotEmpty() ||
                periodPayments.isNotEmpty() ||
                abs(previousBalance) > 0.01 ||
                abs(netBalance) > 0.01
            )
}

data class AmavasyaDashboardSummary(
    val nextSettlementDateIso: String,
    val nextSettlementDateDisplay: String,
    val lunarNameEn: String,
    val lunarNameHi: String,
    val isManualDate: Boolean,
    val daysRemaining: Long,
    val isReminderActive: Boolean,
    val workersRequiringSettlement: Int,
    val totalPayableAmount: Double, // Net wages payable to workers
    val totalAdvanceOutstanding: Double, // Net advances owed back by workers
    val totalOutstandingCombined: Double // Primary headline figure on Dashboard
)

object AmavasyaHisabCalculator {

    fun calculateWorkerStatements(
        workers: List<WorkerEntity>,
        attendanceRecords: List<AttendanceEntity>,
        advances: List<AdvanceEntity>,
        payments: List<PaymentEntity>,
        settlements: List<AmavasyaSettlementEntity>,
        audits: List<SettlementAuditEntity>,
        selectedAmavasyaDate: String
    ): List<WorkerHisabStatement> {
        return workers.map { worker ->
            calculateStatementForWorker(
                worker = worker,
                attendanceRecords = attendanceRecords,
                advances = advances,
                payments = payments,
                settlements = settlements,
                audits = audits,
                selectedAmavasyaDate = selectedAmavasyaDate
            )
        }
    }

    fun calculateStatementForWorker(
        worker: WorkerEntity,
        attendanceRecords: List<AttendanceEntity>,
        advances: List<AdvanceEntity>,
        payments: List<PaymentEntity>,
        settlements: List<AmavasyaSettlementEntity>,
        audits: List<SettlementAuditEntity>,
        selectedAmavasyaDate: String
    ): WorkerHisabStatement {
        val workerSettlements = settlements
            .filter { it.workerId == worker.id }
            .sortedBy { it.periodEndDate }

        // Check if this exact Amavasya date already has a locked settlement snapshot
        val existingLockedForDate = workerSettlements.find {
            it.periodEndDate == selectedAmavasyaDate && it.isLocked
        }

        // Previous locked settlement strictly before selectedAmavasyaDate
        val previousLocked = workerSettlements
            .filter { it.periodEndDate < selectedAmavasyaDate && it.isLocked }
            .maxByOrNull { it.periodEndDate }

        val periodStartExclusive = existingLockedForDate?.let { snap ->
            // Find the settlement strictly prior to this snapshot for record display slicing
            workerSettlements
                .filter { it.periodEndDate < snap.periodEndDate && it.isLocked }
                .maxByOrNull { it.periodEndDate }
                ?.periodEndDate
        } ?: previousLocked?.periodEndDate

        val periodStartInclusive = if (periodStartExclusive != null) {
            AmavasyaCalendar.addDays(periodStartExclusive, 1)
        } else {
            ""
        }

        // Strictly slice transactions in (periodStartExclusive, selectedAmavasyaDate]
        // so NO transaction from any already-settled period is counted twice.
        val periodAtt = attendanceRecords.filter {
            it.workerId == worker.id &&
                (periodStartExclusive == null || it.date > periodStartExclusive) &&
                it.date <= selectedAmavasyaDate
        }.sortedBy { it.date }

        val periodAdv = advances.filter {
            it.workerId == worker.id &&
                (periodStartExclusive == null || it.date > periodStartExclusive) &&
                it.date <= selectedAmavasyaDate
        }.sortedBy { it.date }

        val periodPay = payments.filter {
            it.workerId == worker.id &&
                (periodStartExclusive == null || it.date > periodStartExclusive) &&
                it.date <= selectedAmavasyaDate
        }.sortedBy { it.date }

        val settlementId = existingLockedForDate?.id ?: "hisab_${worker.id}_$selectedAmavasyaDate"
        val relatedAudits = audits.filter {
            it.settlementId == settlementId || (it.workerId == worker.id && it.settlementId.endsWith(selectedAmavasyaDate))
        }.sortedByDescending { it.timestamp }

        val effectiveStartDisplay = when {
            existingLockedForDate != null && existingLockedForDate.periodStartDate.isNotBlank() ->
                Formatters.formatToIndianDate(existingLockedForDate.periodStartDate)
            periodStartInclusive.isNotBlank() ->
                Formatters.formatToIndianDate(periodStartInclusive)
            else -> {
                val earliestRecordDate = listOfNotNull(
                    periodAtt.minOfOrNull { it.date },
                    periodAdv.minOfOrNull { it.date },
                    periodPay.minOfOrNull { it.date }
                ).minOrNull()
                if (earliestRecordDate != null) {
                    Formatters.formatToIndianDate(earliestRecordDate)
                } else if (worker.joiningDate.isNotBlank()) {
                    worker.joiningDate
                } else {
                    "Start"
                }
            }
        }
        val periodEndDisplay = Formatters.formatToIndianDate(selectedAmavasyaDate)
        val periodDisplay = "$effectiveStartDisplay - $periodEndDisplay"

        // If locked snapshot exists for this exact date, preserve locked financial numbers
        // (protects historical integrity against wage-rate edits or duplicate settlements)
        if (existingLockedForDate != null) {
            val status = when (existingLockedForDate.status) {
                "CARRIED_FORWARD" -> HisabSettlementStatus.CARRIED_FORWARD
                else -> HisabSettlementStatus.COMPLETED
            }
            return WorkerHisabStatement(
                worker = worker,
                settlementId = existingLockedForDate.id,
                periodStartDate = existingLockedForDate.periodStartDate,
                periodEndDate = existingLockedForDate.periodEndDate,
                periodDisplay = periodDisplay,
                presentDays = existingLockedForDate.presentDays,
                halfDays = existingLockedForDate.halfDays,
                absentDays = existingLockedForDate.absentDays,
                paidLeaveDays = existingLockedForDate.paidLeaveDays,
                payableManDays = existingLockedForDate.payableManDays,
                totalWages = existingLockedForDate.totalWages,
                totalOvertimeHours = existingLockedForDate.overtimeHours,
                totalOvertimeAmount = existingLockedForDate.overtimeAmount,
                totalEarnings = existingLockedForDate.totalEarnings,
                previousBalance = existingLockedForDate.previousBalance,
                advancesTaken = existingLockedForDate.advancesTaken,
                paymentsMade = existingLockedForDate.paymentsMade,
                netBalance = existingLockedForDate.netBalance,
                payableAmount = existingLockedForDate.payableAmount,
                outstandingAdvanceAmount = existingLockedForDate.outstandingAdvanceAmount,
                status = status,
                isLocked = true,
                settledAt = existingLockedForDate.settledAt,
                notes = existingLockedForDate.notes,
                periodAttendance = periodAtt,
                periodAdvances = periodAdv,
                periodPayments = periodPay,
                auditLogs = relatedAudits
            )
        }

        // Live calculation from actual saved records
        var presentDays = 0
        var halfDays = 0
        var absentDays = 0
        var paidLeaveDays = 0
        var payableManDays = 0.0
        var totalWages = 0.0
        var totalOtHours = 0.0
        var totalOtAmount = 0.0

        periodAtt.forEach { record ->
            val attStatus = AttendanceStatus.fromString(record.status)
            when (attStatus) {
                AttendanceStatus.PRESENT -> presentDays++
                AttendanceStatus.HALF_DAY -> halfDays++
                AttendanceStatus.ABSENT -> absentDays++
                AttendanceStatus.PAID_LEAVE -> paidLeaveDays++
                AttendanceStatus.WEEKLY_OFF -> Unit
            }

            val dayFraction = if (record.paidDayFraction > 0 || attStatus == AttendanceStatus.ABSENT || attStatus == AttendanceStatus.WEEKLY_OFF) {
                record.paidDayFraction
            } else {
                attStatus.fraction
            }
            payableManDays += dayFraction

            val wageSnapshot = if (record.dailyWageSnapshot > 0) record.dailyWageSnapshot else worker.dailyWage
            val baseForDay = dayFraction * wageSnapshot

            val otRateSnapshot = when {
                record.overtimeRateSnapshot > 0 -> record.overtimeRateSnapshot
                worker.overtimeHourlyRate > 0 -> worker.overtimeHourlyRate
                wageSnapshot > 0 -> wageSnapshot / 8.0
                else -> 0.0
            }
            val otForDay = record.overtimeHours * otRateSnapshot

            totalWages += baseForDay
            totalOtHours += record.overtimeHours
            totalOtAmount += otForDay
        }

        val totalEarnings = totalWages + totalOtAmount
        val previousBalance = previousLocked?.carryForwardBalance ?: 0.0
        val advancesTaken = periodAdv.sumOf { it.amount }
        val paymentsMade = periodPay.sumOf { it.amount }

        val netBalance = previousBalance + totalEarnings - advancesTaken - paymentsMade
        val payableAmount = if (netBalance > 0.01) netBalance else 0.0
        val outstandingAdvanceAmount = if (netBalance < -0.01) -netBalance else 0.0

        val status = if (paymentsMade > 0.0 && payableAmount > 0.01) {
            HisabSettlementStatus.PARTIAL
        } else {
            HisabSettlementStatus.PENDING
        }

        return WorkerHisabStatement(
            worker = worker,
            settlementId = settlementId,
            periodStartDate = periodStartInclusive,
            periodEndDate = selectedAmavasyaDate,
            periodDisplay = periodDisplay,
            presentDays = presentDays,
            halfDays = halfDays,
            absentDays = absentDays,
            paidLeaveDays = paidLeaveDays,
            payableManDays = payableManDays,
            totalWages = totalWages,
            totalOvertimeHours = totalOtHours,
            totalOvertimeAmount = totalOtAmount,
            totalEarnings = totalEarnings,
            previousBalance = previousBalance,
            advancesTaken = advancesTaken,
            paymentsMade = paymentsMade,
            netBalance = netBalance,
            payableAmount = payableAmount,
            outstandingAdvanceAmount = outstandingAdvanceAmount,
            status = status,
            isLocked = false,
            settledAt = null,
            notes = "",
            periodAttendance = periodAtt,
            periodAdvances = periodAdv,
            periodPayments = periodPay,
            auditLogs = relatedAudits
        )
    }

    fun buildDashboardSummary(
        selectedAmavasyaDate: String,
        isManualDate: Boolean,
        reminderDaysBefore: Int,
        statements: List<WorkerHisabStatement>,
        todayIso: String = Formatters.getTodayIsoDate()
    ): AmavasyaDashboardSummary {
        val lunarInfo = AmavasyaCalendar.findAmavasyaInfo(selectedAmavasyaDate)
        val daysRemaining = AmavasyaCalendar.daysBetween(todayIso, selectedAmavasyaDate)
        val pendingStatements = statements.filter { it.requiresSettlementAction }

        val totalPayable = pendingStatements.sumOf { it.payableAmount }
        val totalAdvanceOutstanding = pendingStatements.sumOf { it.outstandingAdvanceAmount }
        val headlineAmount = if (totalPayable > 0.0) totalPayable else totalAdvanceOutstanding

        return AmavasyaDashboardSummary(
            nextSettlementDateIso = selectedAmavasyaDate,
            nextSettlementDateDisplay = Formatters.formatToIndianDate(selectedAmavasyaDate),
            lunarNameEn = lunarInfo?.lunarNameEn ?: if (isManualDate) "Custom Hisab Date" else "Amavasya Hisab",
            lunarNameHi = lunarInfo?.lunarNameHi ?: if (isManualDate) "चयनित हिसाब तारीख" else "अमावस्या हिसाब",
            isManualDate = isManualDate,
            daysRemaining = daysRemaining,
            isReminderActive = daysRemaining in 0..reminderDaysBefore.toLong() && pendingStatements.isNotEmpty(),
            workersRequiringSettlement = pendingStatements.size,
            totalPayableAmount = totalPayable,
            totalAdvanceOutstanding = totalAdvanceOutstanding,
            totalOutstandingCombined = headlineAmount
        )
    }
}
