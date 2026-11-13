package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AdvanceEntity
import com.example.data.model.AmavasyaConfigEntity
import com.example.data.model.AmavasyaSettlementEntity
import com.example.data.model.AttendanceEntity
import com.example.data.model.MonthlyPdfEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.SettlementAuditEntity
import com.example.data.model.SiteEntity
import com.example.data.model.WorkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY name ASC")
    fun getAllWorkers(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveWorkers(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getActiveWorkersSync(): List<WorkerEntity>

    @Query("SELECT * FROM workers WHERE id = :id")
    fun getWorkerById(id: String): Flow<WorkerEntity?>

    @Query("SELECT * FROM workers WHERE id = :id")
    suspend fun getWorkerByIdSync(id: String): WorkerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: WorkerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkers(workers: List<WorkerEntity>)

    @Update
    suspend fun updateWorker(worker: WorkerEntity)

    @Delete
    suspend fun deleteWorker(worker: WorkerEntity)

    @Query("DELETE FROM workers")
    suspend fun deleteAllWorkers()
}

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites ORDER BY name ASC")
    fun getAllSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE status = 'Active' ORDER BY name ASC")
    fun getActiveSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE id = :id")
    fun getSiteById(id: String): Flow<SiteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: SiteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSites(sites: List<SiteEntity>)

    @Update
    suspend fun updateSite(site: SiteEntity)

    @Delete
    suspend fun deleteSite(site: SiteEntity)

    @Query("DELETE FROM sites")
    suspend fun deleteAllSites()
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance ORDER BY date DESC, createdAt DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceForDate(date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date = :date")
    suspend fun getAttendanceForDateSync(date: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE date = :date AND siteId = :siteId")
    fun getAttendanceForDateAndSite(date: String, siteId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE workerId = :workerId ORDER BY date DESC")
    fun getAttendanceForWorker(workerId: String): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceBatch(list: List<AttendanceEntity>)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteAttendanceById(id: String)

    @Query("DELETE FROM attendance")
    suspend fun deleteAllAttendance()
}

@Dao
interface AdvanceDao {
    @Query("SELECT * FROM advances ORDER BY date DESC, createdAt DESC")
    fun getAllAdvances(): Flow<List<AdvanceEntity>>

    @Query("SELECT * FROM advances WHERE workerId = :workerId ORDER BY date DESC")
    fun getAdvancesForWorker(workerId: String): Flow<List<AdvanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdvance(advance: AdvanceEntity)

    @Update
    suspend fun updateAdvance(advance: AdvanceEntity)

    @Query("DELETE FROM advances WHERE id = :id")
    suspend fun deleteAdvanceById(id: String)

    @Query("DELETE FROM advances")
    suspend fun deleteAllAdvances()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY date DESC, createdAt DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE workerId = :workerId ORDER BY date DESC")
    fun getPaymentsForWorker(workerId: String): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: String)

    @Query("DELETE FROM payments")
    suspend fun deleteAllPayments()
}

@Dao
interface AmavasyaConfigDao {
    @Query("SELECT * FROM amavasya_config WHERE id = 'default_config' LIMIT 1")
    fun getConfig(): Flow<AmavasyaConfigEntity?>

    @Query("SELECT * FROM amavasya_config WHERE id = 'default_config' LIMIT 1")
    suspend fun getConfigSync(): AmavasyaConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfig(config: AmavasyaConfigEntity)

    @Query("DELETE FROM amavasya_config")
    suspend fun deleteAllConfigs()
}

@Dao
interface AmavasyaSettlementDao {
    @Query("SELECT * FROM amavasya_settlements ORDER BY periodEndDate DESC, settledAt DESC")
    fun getAllSettlements(): Flow<List<AmavasyaSettlementEntity>>

    @Query("SELECT * FROM amavasya_settlements WHERE workerId = :workerId ORDER BY periodEndDate DESC")
    fun getSettlementsForWorker(workerId: String): Flow<List<AmavasyaSettlementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: AmavasyaSettlementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlements(settlements: List<AmavasyaSettlementEntity>)

    @Query("DELETE FROM amavasya_settlements WHERE id = :id")
    suspend fun deleteSettlementById(id: String)

    @Query("DELETE FROM amavasya_settlements")
    suspend fun deleteAllSettlements()
}

@Dao
interface SettlementAuditDao {
    @Query("SELECT * FROM settlement_audits ORDER BY timestamp DESC")
    fun getAllAudits(): Flow<List<SettlementAuditEntity>>

    @Query("SELECT * FROM settlement_audits WHERE settlementId = :settlementId ORDER BY timestamp DESC")
    fun getAuditsForSettlement(settlementId: String): Flow<List<SettlementAuditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: SettlementAuditEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudits(audits: List<SettlementAuditEntity>)

    @Query("DELETE FROM settlement_audits")
    suspend fun deleteAllAudits()
}

@Dao
interface MonthlyPdfDao {
    @Query("SELECT * FROM monthly_hisab_pdfs ORDER BY year DESC, month DESC, isCombinedReport DESC, generatedAt DESC")
    fun getAllMonthlyPdfs(): Flow<List<MonthlyPdfEntity>>

    @Query("SELECT * FROM monthly_hisab_pdfs ORDER BY year DESC, month DESC, isCombinedReport DESC, generatedAt DESC")
    suspend fun getAllMonthlyPdfsSync(): List<MonthlyPdfEntity>

    @Query("SELECT * FROM monthly_hisab_pdfs WHERE id = :id LIMIT 1")
    suspend fun getMonthlyPdfByIdSync(id: String): MonthlyPdfEntity?

    @Query("SELECT * FROM monthly_hisab_pdfs WHERE workerId = :workerId AND periodEndDate = :periodEndDate LIMIT 1")
    suspend fun getByWorkerAndPeriodSync(workerId: String, periodEndDate: String): MonthlyPdfEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyPdf(pdf: MonthlyPdfEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyPdfs(pdfs: List<MonthlyPdfEntity>)

    @Query("DELETE FROM monthly_hisab_pdfs WHERE id = :id")
    suspend fun deleteMonthlyPdfById(id: String)

    @Query("DELETE FROM monthly_hisab_pdfs")
    suspend fun deleteAllMonthlyPdfs()
}

