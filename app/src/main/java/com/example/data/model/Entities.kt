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
