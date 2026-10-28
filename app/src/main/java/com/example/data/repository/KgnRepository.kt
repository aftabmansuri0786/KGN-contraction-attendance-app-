package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.AdvanceEntity
import com.example.data.model.AttendanceEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.SiteEntity
import com.example.data.model.WorkerEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class WorkerLedgerSummary(
    val worker: WorkerEntity,
    val totalDaysWorked: Double,
    val totalOvertimeHours: Double,
    val totalEarnings: Double,
    val totalAdvances: Double,
    val totalPayments: Double,
    val netBalanceDue: Double
)

class KgnRepository(private val db: AppDatabase) {
    private val workerDao = db.workerDao()
    private val siteDao = db.siteDao()
    private val attendanceDao = db.attendanceDao()
    private val advanceDao = db.advanceDao()
    private val paymentDao = db.paymentDao()

    val allWorkers: Flow<List<WorkerEntity>> = workerDao.getAllWorkers()
    val activeWorkers: Flow<List<WorkerEntity>> = workerDao.getActiveWorkers()
    val allSites: Flow<List<SiteEntity>> = siteDao.getAllSites()
    val activeSites: Flow<List<SiteEntity>> = siteDao.getActiveSites()
    val allAttendance: Flow<List<AttendanceEntity>> = attendanceDao.getAllAttendance()
    val allAdvances: Flow<List<AdvanceEntity>> = advanceDao.getAllAdvances()
    val allPayments: Flow<List<PaymentEntity>> = paymentDao.getAllPayments()

    fun getWorkerById(id: String): Flow<WorkerEntity?> = workerDao.getWorkerById(id)
    fun getAttendanceForDate(date: String): Flow<List<AttendanceEntity>> = attendanceDao.getAttendanceForDate(date)
    fun getAttendanceForWorker(workerId: String): Flow<List<AttendanceEntity>> = attendanceDao.getAttendanceForWorker(workerId)
    fun getAdvancesForWorker(workerId: String): Flow<List<AdvanceEntity>> = advanceDao.getAdvancesForWorker(workerId)
    fun getPaymentsForWorker(workerId: String): Flow<List<PaymentEntity>> = paymentDao.getPaymentsForWorker(workerId)

    suspend fun saveWorker(worker: WorkerEntity) {
        workerDao.insertWorker(worker)
    }

    suspend fun saveSite(site: SiteEntity) {
        siteDao.insertSite(site)
    }

    suspend fun saveAttendanceRecord(
        workerId: String,
        siteId: String,
        date: String,
        status: String,
        fraction: Double,
        overtimeHours: Double,
        remarks: String = ""
    ) {
        val worker = workerDao.getWorkerByIdSync(workerId)
        val wageRate = worker?.dailyWage ?: 0.0
        val otRate = worker?.overtimeHourlyRate ?: if (wageRate > 0) (wageRate / 8.0) else 0.0
        val earnings = (fraction * wageRate) + (overtimeHours * otRate)

        val id = "att_${workerId}_${date}_$siteId"
        val record = AttendanceEntity(
            id = id,
            workerId = workerId,
            siteId = siteId,
            date = date,
            status = status,
            paidDayFraction = fraction,
            dailyWageSnapshot = wageRate,
            overtimeHours = overtimeHours,
            overtimeRateSnapshot = otRate,
            calculatedEarnings = earnings,
            remarks = remarks,
            updatedAt = System.currentTimeMillis()
        )
        attendanceDao.insertAttendance(record)
    }

    suspend fun saveAttendanceBatch(records: List<AttendanceEntity>) {
        attendanceDao.insertAttendanceBatch(records)
    }

    suspend fun deleteAttendance(id: String) {
        attendanceDao.deleteAttendanceById(id)
    }

    suspend fun saveAdvance(advance: AdvanceEntity) {
        advanceDao.insertAdvance(advance)
    }

    suspend fun savePayment(payment: PaymentEntity) {
        paymentDao.insertPayment(payment)
    }

    suspend fun deleteAdvance(id: String) {
        advanceDao.deleteAdvanceById(id)
    }

    suspend fun deletePayment(id: String) {
        paymentDao.deletePaymentById(id)
    }

    suspend fun clearAllData() {
        workerDao.deleteAllWorkers()
        siteDao.deleteAllSites()
        attendanceDao.deleteAllAttendance()
        advanceDao.deleteAllAdvances()
        paymentDao.deleteAllPayments()
    }
}
