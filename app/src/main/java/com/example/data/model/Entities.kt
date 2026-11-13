package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttendanceStatus(val fraction: Double, val labelEn: String, val labelHi: String) {
    PRESENT(1.0, "Present", "उपस्थित"),
    HALF_DAY(0.5, "Half Day", "आधा दिन"),
    ABSENT(0.0, "Absent", "अनुपस्थित"),
    PAID_LEAVE(1.0, "Paid Leave", "सवेतन छुट्टी"),
    WEEKLY_OFF(0.0, "Weekly Off", "साप्ताहिक छुट्टी");

    companion object {
        fun fromString(value: String): AttendanceStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: PRESENT
        }
    }
}

val WorkCategories = listOf(
    "Mason" to "राजमिस्त्री",
    "Helper" to "हेल्पर / मज़दूर",
    "Carpenter" to "बढ़ई",
    "Electrician" to "इलेक्ट्रीशियन",
    "Plumber" to "प्लम्बर",
    "Painter" to "पेंटर",
    "Welder" to "वेल्डर",
    "Machine Operator" to "मशीन ऑपरेटर",
    "Other" to "अन्य"
)

val PaymentMethods = listOf("Cash", "UPI", "Bank Transfer", "Other")

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey
    val id: String, // e.g. "KGN-W101"
    val name: String,
    val mobile: String = "",
    val category: String = "Helper",
    val dailyWage: Double = 0.0,
    val overtimeHourlyRate: Double = 0.0,
    val overtimeMultiplier: Double = 1.0,
    val joiningDate: String = "", // DD/MM/YYYY
    val siteId: String = "",
    val isActive: Boolean = true,
    val notes: String = "",
    val photoUrl: String = "",
    val photoStoragePath: String = "",
    val localPhotoPath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey
    val id: String, // e.g. "KGN-S01"
    val name: String,
    val address: String = "",
    val startDate: String = "", // DD/MM/YYYY
    val expectedEndDate: String = "",
    val status: String = "Active", // "Active" or "Completed"
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance",
    indices = [
        Index(value = ["workerId", "date", "siteId"], unique = true),
        Index(value = ["date"]),
        Index(value = ["workerId"])
    ]
)
data class AttendanceEntity(
    @PrimaryKey
    val id: String, // "att_${workerId}_${date}_${siteId}"
    val workerId: String,
    val siteId: String,
    val date: String, // YYYY-MM-DD
    val status: String = "PRESENT", // from AttendanceStatus
    val paidDayFraction: Double = 1.0,
    val dailyWageSnapshot: Double = 0.0,
    val overtimeHours: Double = 0.0,
    val overtimeRateSnapshot: Double = 0.0,
    val calculatedEarnings: Double = 0.0,
    val remarks: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "advances",
    indices = [
        Index(value = ["workerId"]),
        Index(value = ["date"])
    ]
)
data class AdvanceEntity(
    @PrimaryKey
    val id: String, // "adv_${System.currentTimeMillis()}"
    val workerId: String,
    val date: String, // YYYY-MM-DD
    val amount: Double,
    val paymentMethod: String = "Cash",
    val reference: String = "",
    val notes: String = "",
    val settledAmount: Double = 0.0,
    val isSettled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["workerId"]),
        Index(value = ["date"])
    ]
)
data class PaymentEntity(
    @PrimaryKey
    val id: String, // "pay_${System.currentTimeMillis()}"
    val workerId: String,
    val date: String, // YYYY-MM-DD
    val amount: Double,
    val paymentMethod: String = "Cash",
    val reference: String = "",
    val notes: String = "",
    val settlementPeriod: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "amavasya_config")
data class AmavasyaConfigEntity(
    @PrimaryKey
    val id: String = "default_config",
    val selectedSettlementDate: String = "", // YYYY-MM-DD (empty means auto next calculated Amavasya)
    val isManualDate: Boolean = false,
    val reminderDaysBefore: Int = 3,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "amavasya_settlements",
    indices = [
        Index(value = ["workerId"]),
        Index(value = ["periodEndDate"]),
        Index(value = ["workerId", "periodEndDate"], unique = true)
    ]
)
data class AmavasyaSettlementEntity(
    @PrimaryKey
    val id: String, // "hisab_${workerId}_${periodEndDate}"
    val workerId: String,
    val workerName: String = "",
    val periodStartDate: String = "", // YYYY-MM-DD or "" if from beginning
    val periodEndDate: String = "", // YYYY-MM-DD (selected Amavasya date)
    val presentDays: Int = 0,
    val halfDays: Int = 0,
    val absentDays: Int = 0,
    val paidLeaveDays: Int = 0,
    val payableManDays: Double = 0.0,
    val totalWages: Double = 0.0,
    val overtimeHours: Double = 0.0,
    val overtimeAmount: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val previousBalance: Double = 0.0, // >0 unpaid wages brought forward, <0 advance brought forward
    val advancesTaken: Double = 0.0,
    val paymentsMade: Double = 0.0,
    val netBalance: Double = 0.0, // previousBalance + totalEarnings - advancesTaken - paymentsMade
    val payableAmount: Double = 0.0, // max(0, netBalance) -> owed to worker
    val outstandingAdvanceAmount: Double = 0.0, // max(0, -netBalance) -> owed by worker
    val carryForwardBalance: Double = 0.0, // signed balance carried into next period
    val status: String = "COMPLETED", // COMPLETED, CARRIED_FORWARD
    val isLocked: Boolean = true,
    val notes: String = "",
    val settledAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "settlement_audits",
    indices = [
        Index(value = ["settlementId"]),
        Index(value = ["workerId"])
    ]
)
data class SettlementAuditEntity(
    @PrimaryKey
    val id: String,
    val settlementId: String,
    val workerId: String,
    val workerName: String = "",
    val action: String, // SETTLED_FULL, CARRIED_FORWARD, AUTHORIZED_CORRECTION, REOPENED
    val reason: String = "",
    val previousNetBalance: Double = 0.0,
    val newNetBalance: Double = 0.0,
    val performedBy: String = "Owner",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "monthly_hisab_pdfs",
    indices = [
        Index(value = ["year", "month"]),
        Index(value = ["workerId", "periodEndDate"], unique = true)
    ]
)
data class MonthlyPdfEntity(
    @PrimaryKey
    val id: String, // "pdf_${workerId}_${periodEndDate}" or "pdf_COMBINED_${periodEndDate}"
    val workerId: String, // worker.id or "COMBINED"
    val workerName: String = "",
    val year: Int,
    val month: Int, // 1..12
    val monthLabel: String = "", // e.g., "November 2026"
    val periodStartDate: String = "",
    val periodEndDate: String = "", // YYYY-MM-DD
    val isCombinedReport: Boolean = false,
    val localFilePath: String = "",
    val storagePath: String = "",
    val downloadUrl: String = "",
    val fileSizeBytes: Long = 0L,
    val generatedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

