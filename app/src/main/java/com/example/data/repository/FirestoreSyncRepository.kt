package com.example.data.repository

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.AdvanceEntity
import com.example.data.model.AmavasyaConfigEntity
import com.example.data.model.AmavasyaSettlementEntity
import com.example.data.model.AttendanceEntity
import com.example.data.model.MonthlyPdfEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.SettlementAuditEntity
import com.example.data.model.SiteEntity
import com.example.data.model.WorkerEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreSyncRepository(
    private val firestore: FirebaseFirestore,
    private val appDatabase: AppDatabase
) {
    private val workerDao = appDatabase.workerDao()
    private val siteDao = appDatabase.siteDao()
    private val attendanceDao = appDatabase.attendanceDao()
    private val advanceDao = appDatabase.advanceDao()
    private val paymentDao = appDatabase.paymentDao()
    private val amavasyaConfigDao = appDatabase.amavasyaConfigDao()
    private val amavasyaSettlementDao = appDatabase.amavasyaSettlementDao()
    private val settlementAuditDao = appDatabase.settlementAuditDao()
    private val monthlyPdfDao = appDatabase.monthlyPdfDao()

    companion object {
        private const val TAG = "FirestoreSync"
    }

    suspend fun syncAll(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting two-way sync for user $userId")
            val userDoc = firestore.collection("users").document(userId)

            // 1. Sync Workers from remote (preserve existing device-local photo paths)
            val existingWorkersById = workerDao.getAllWorkers().first().associateBy { it.id }
            val remoteWorkers = userDoc.collection("workers").get().await()
            val remoteWorkerList = remoteWorkers.documents.mapNotNull { doc ->
                try {
                    val wId = doc.getString("id") ?: doc.id
                    val existing = existingWorkersById[wId]
                    val remotePhotoUrl = doc.getString("photoUrl") ?: ""
                    val remoteStoragePath = doc.getString("photoStoragePath") ?: ""
                    WorkerEntity(
                        id = wId,
                        name = doc.getString("name") ?: "",
                        mobile = doc.getString("mobile") ?: "",
                        category = doc.getString("category") ?: "Helper",
                        dailyWage = doc.getDouble("dailyWage") ?: 0.0,
                        overtimeHourlyRate = doc.getDouble("overtimeHourlyRate") ?: 0.0,
                        joiningDate = doc.getString("joiningDate") ?: "",
                        siteId = doc.getString("siteId") ?: "",
                        isActive = doc.getBoolean("isActive") ?: true,
                        notes = doc.getString("notes") ?: "",
                        photoUrl = remotePhotoUrl.ifBlank { existing?.photoUrl.orEmpty() },
                        photoStoragePath = remoteStoragePath.ifBlank { existing?.photoStoragePath.orEmpty() },
                        localPhotoPath = existing?.localPhotoPath.orEmpty()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (remoteWorkerList.isNotEmpty()) {
                workerDao.insertWorkers(remoteWorkerList)
            }

            // Upload any local workers to remote (exclude device-specific localPhotoPath)
            val localWorkers = workerDao.getAllWorkers().first()
            for (w in localWorkers) {
                userDoc.collection("workers").document(w.id).set(w.copy(localPhotoPath = "")).await()
            }

            // 2. Sync Sites from remote
            val remoteSites = userDoc.collection("sites").get().await()
            val remoteSiteList = remoteSites.documents.mapNotNull { doc ->
                try {
                    SiteEntity(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        address = doc.getString("address") ?: "",
                        startDate = doc.getString("startDate") ?: "",
                        expectedEndDate = doc.getString("expectedEndDate") ?: "",
                        status = doc.getString("status") ?: "Active",
                        notes = doc.getString("notes") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (remoteSiteList.isNotEmpty()) {
                siteDao.insertSites(remoteSiteList)
            }

            val localSites = siteDao.getAllSites().first()
            for (s in localSites) {
                userDoc.collection("sites").document(s.id).set(s).await()
            }

            // 3. Sync Attendance from remote
            val remoteAttendance = userDoc.collection("attendance").get().await()
            val remoteAttendanceList = remoteAttendance.documents.mapNotNull { doc ->
                try {
                    AttendanceEntity(
                        id = doc.getString("id") ?: doc.id,
                        workerId = doc.getString("workerId") ?: "",
                        siteId = doc.getString("siteId") ?: "",
                        date = doc.getString("date") ?: "",
                        status = doc.getString("status") ?: "PRESENT",
                        paidDayFraction = doc.getDouble("paidDayFraction") ?: 1.0,
                        dailyWageSnapshot = doc.getDouble("dailyWageSnapshot") ?: 0.0,
                        overtimeHours = doc.getDouble("overtimeHours") ?: 0.0,
                        overtimeRateSnapshot = doc.getDouble("overtimeRateSnapshot") ?: 0.0,
                        calculatedEarnings = doc.getDouble("calculatedEarnings") ?: 0.0,
                        remarks = doc.getString("remarks") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (remoteAttendanceList.isNotEmpty()) {
                attendanceDao.insertAttendanceBatch(remoteAttendanceList)
            }

            val localAttendance = attendanceDao.getAllAttendance().first()
            for (a in localAttendance) {
                userDoc.collection("attendance").document(a.id).set(a).await()
            }

            // 4. Sync Advances from remote
            val remoteAdvances = userDoc.collection("advances").get().await()
            val remoteAdvanceList = remoteAdvances.documents.mapNotNull { doc ->
                try {
                    AdvanceEntity(
                        id = doc.getString("id") ?: doc.id,
                        workerId = doc.getString("workerId") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        date = doc.getString("date") ?: "",
                        paymentMethod = doc.getString("paymentMethod") ?: "Cash",
                        reference = doc.getString("reference") ?: "",
                        notes = doc.getString("notes") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
            for (adv in remoteAdvanceList) {
                advanceDao.insertAdvance(adv)
            }

            val localAdvances = advanceDao.getAllAdvances().first()
            for (adv in localAdvances) {
                userDoc.collection("advances").document(adv.id).set(adv).await()
            }

            // 5. Sync Payments from remote
            val remotePayments = userDoc.collection("payments").get().await()
            val remotePaymentList = remotePayments.documents.mapNotNull { doc ->
                try {
                    PaymentEntity(
                        id = doc.getString("id") ?: doc.id,
                        workerId = doc.getString("workerId") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        date = doc.getString("date") ?: "",
                        paymentMethod = doc.getString("paymentMethod") ?: "Cash",
                        reference = doc.getString("reference") ?: "",
                        notes = doc.getString("notes") ?: "",
                        settlementPeriod = doc.getString("settlementPeriod") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
            for (p in remotePaymentList) {
                paymentDao.insertPayment(p)
            }

            val localPayments = paymentDao.getAllPayments().first()
            for (p in localPayments) {
                userDoc.collection("payments").document(p.id).set(p).await()
            }

            // 6. Sync Amavasya Config from remote & local
            val remoteConfigDoc = userDoc.collection("amavasya_settings").document("default_config").get().await()
            val localConfig = amavasyaConfigDao.getConfigSync()
            if (remoteConfigDoc.exists()) {
                val remoteUpdated = remoteConfigDoc.getLong("updatedAt") ?: 0L
                val localUpdated = localConfig?.updatedAt ?: 0L
                if (remoteUpdated >= localUpdated) {
                    val remoteConfig = AmavasyaConfigEntity(
                        id = "default_config",
                        selectedSettlementDate = remoteConfigDoc.getString("selectedSettlementDate") ?: "",
                        isManualDate = remoteConfigDoc.getBoolean("isManualDate") ?: false,
                        reminderDaysBefore = (remoteConfigDoc.getLong("reminderDaysBefore") ?: 3L).toInt(),
                        updatedAt = remoteUpdated
                    )
                    amavasyaConfigDao.upsertConfig(remoteConfig)
                } else if (localConfig != null) {
                    userDoc.collection("amavasya_settings").document("default_config").set(localConfig).await()
                }
            } else if (localConfig != null) {
                userDoc.collection("amavasya_settings").document("default_config").set(localConfig).await()
            }

            // 7. Sync Amavasya Settlements
            val remoteSettlements = userDoc.collection("settlements").get().await()
            val remoteSettlementList = remoteSettlements.documents.mapNotNull { doc ->
                try {
                    AmavasyaSettlementEntity(
                        id = doc.getString("id") ?: doc.id,
                        workerId = doc.getString("workerId") ?: "",
                        workerName = doc.getString("workerName") ?: "",
                        periodStartDate = doc.getString("periodStartDate") ?: "",
                        periodEndDate = doc.getString("periodEndDate") ?: "",
                        presentDays = (doc.getLong("presentDays") ?: 0L).toInt(),
                        halfDays = (doc.getLong("halfDays") ?: 0L).toInt(),
                        absentDays = (doc.getLong("absentDays") ?: 0L).toInt(),
                        paidLeaveDays = (doc.getLong("paidLeaveDays") ?: 0L).toInt(),
                        payableManDays = doc.getDouble("payableManDays") ?: 0.0,
                        totalWages = doc.getDouble("totalWages") ?: 0.0,
                        overtimeHours = doc.getDouble("overtimeHours") ?: 0.0,
                        overtimeAmount = doc.getDouble("overtimeAmount") ?: 0.0,
                        totalEarnings = doc.getDouble("totalEarnings") ?: 0.0,
                        previousBalance = doc.getDouble("previousBalance") ?: 0.0,
                        advancesTaken = doc.getDouble("advancesTaken") ?: 0.0,
                        paymentsMade = doc.getDouble("paymentsMade") ?: 0.0,
                        netBalance = doc.getDouble("netBalance") ?: 0.0,
                        payableAmount = doc.getDouble("payableAmount") ?: 0.0,
                        outstandingAdvanceAmount = doc.getDouble("outstandingAdvanceAmount") ?: 0.0,
                        carryForwardBalance = doc.getDouble("carryForwardBalance") ?: 0.0,
                        status = doc.getString("status") ?: "COMPLETED",
                        isLocked = doc.getBoolean("isLocked") ?: true,
                        notes = doc.getString("notes") ?: "",
                        settledAt = doc.getLong("settledAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (remoteSettlementList.isNotEmpty()) {
                amavasyaSettlementDao.insertSettlements(remoteSettlementList)
            }

            val localSettlements = amavasyaSettlementDao.getAllSettlements().first()
            for (st in localSettlements) {
                userDoc.collection("settlements").document(st.id).set(st).await()
            }

            // 8. Sync Settlement Audit Logs
            val remoteAudits = userDoc.collection("settlement_audits").get().await()
            val remoteAuditList = remoteAudits.documents.mapNotNull { doc ->
                try {
                    SettlementAuditEntity(
                        id = doc.getString("id") ?: doc.id,
                        settlementId = doc.getString("settlementId") ?: "",
                        workerId = doc.getString("workerId") ?: "",
                        workerName = doc.getString("workerName") ?: "",
                        action = doc.getString("action") ?: "",
                        reason = doc.getString("reason") ?: "",
                        previousNetBalance = doc.getDouble("previousNetBalance") ?: 0.0,
                        newNetBalance = doc.getDouble("newNetBalance") ?: 0.0,
                        performedBy = doc.getString("performedBy") ?: "Owner",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (remoteAuditList.isNotEmpty()) {
                settlementAuditDao.insertAudits(remoteAuditList)
            }

            val localAudits = settlementAuditDao.getAllAudits().first()
            for (au in localAudits) {
                userDoc.collection("settlement_audits").document(au.id).set(au).await()
            }

            // 9. Sync Monthly Hisab PDFs metadata (preserve device-local file paths)
            val existingPdfsById = monthlyPdfDao.getAllMonthlyPdfsSync().associateBy { it.id }
            val remotePdfs = userDoc.collection("monthly_hisab_pdfs").get().await()
            val remotePdfList = remotePdfs.documents.mapNotNull { doc ->
                try {
                    val pId = doc.getString("id") ?: doc.id
                    val existing = existingPdfsById[pId]
                    val remoteUpdated = doc.getLong("updatedAt") ?: 0L
                    val localUpdated = existing?.updatedAt ?: 0L
                    if (existing != null && localUpdated > remoteUpdated) {
                        existing
                    } else {
                        MonthlyPdfEntity(
                            id = pId,
                            workerId = doc.getString("workerId") ?: "",
                            workerName = doc.getString("workerName") ?: "",
                            year = (doc.getLong("year") ?: 0L).toInt(),
                            month = (doc.getLong("month") ?: 0L).toInt(),
                            monthLabel = doc.getString("monthLabel") ?: "",
                            periodStartDate = doc.getString("periodStartDate") ?: "",
                            periodEndDate = doc.getString("periodEndDate") ?: "",
                            isCombinedReport = doc.getBoolean("isCombinedReport") ?: false,
                            localFilePath = existing?.localFilePath.orEmpty(),
                            storagePath = (doc.getString("storagePath") ?: "").ifBlank { existing?.storagePath.orEmpty() },
                            downloadUrl = (doc.getString("downloadUrl") ?: "").ifBlank { existing?.downloadUrl.orEmpty() },
                            fileSizeBytes = doc.getLong("fileSizeBytes") ?: existing?.fileSizeBytes ?: 0L,
                            generatedAt = doc.getLong("generatedAt") ?: existing?.generatedAt ?: System.currentTimeMillis(),
                            updatedAt = maxOf(remoteUpdated, localUpdated)
                        )
                    }
                } catch (e: Exception) {
                    null
                }
            }
            if (remotePdfList.isNotEmpty()) {
                monthlyPdfDao.insertMonthlyPdfs(remotePdfList)
            }

            val localPdfs = monthlyPdfDao.getAllMonthlyPdfsSync()
            for (pdf in localPdfs) {
                userDoc.collection("monthly_hisab_pdfs").document(pdf.id).set(pdf.copy(localFilePath = "")).await()
            }

            Log.d(TAG, "Sync successfully completed for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing with Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun uploadWorker(userId: String, worker: WorkerEntity) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(userId).collection("workers").document(worker.id)
                .set(worker.copy(localPhotoPath = "")).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading worker", e)
        }
    }

    suspend fun uploadSite(userId: String, site: SiteEntity) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(userId).collection("sites").document(site.id).set(site).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading site", e)
        }
    }

    suspend fun uploadAttendance(userId: String, attendance: AttendanceEntity) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(userId).collection("attendance").document(attendance.id).set(attendance).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading attendance", e)
        }
    }

    suspend fun uploadAdvance(userId: String, advance: AdvanceEntity) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(userId).collection("advances").document(advance.id).set(advance).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading advance", e)
        }
    }

    suspend fun uploadPayment(userId: String, payment: PaymentEntity) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(userId).collection("payments").document(payment.id).set(payment).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading payment", e)
        }
    }

    suspend fun deleteAdvance(userId: String, advanceId: String) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(userId).collection("advances").document(advanceId).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting advance", e)
        }
    }

    suspend fun deletePayment(userId: String, paymentId: String) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(userId).collection("payments").document(paymentId).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting payment", e)
        }
    }

    suspend fun uploadAmavasyaConfig(userId: String, config: AmavasyaConfigEntity) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(userId).collection("amavasya_settings").document(config.id).set(config).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading amavasya config", e)
        }
    }

    suspend fun uploadSettlement(userId: String, settlement: AmavasyaSettlementEntity) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(userId).collection("settlements").document(settlement.id).set(settlement).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading settlement", e)
        }
    }

    suspend fun deleteSettlement(userId: String, settlementId: String) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(userId).collection("settlements").document(settlementId).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting settlement", e)
        }
    }

    suspend fun uploadSettlementAudit(userId: String, audit: SettlementAuditEntity) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(userId).collection("settlement_audits").document(audit.id).set(audit).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading settlement audit", e)
        }
    }

    suspend fun uploadMonthlyPdf(userId: String, pdf: MonthlyPdfEntity) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(userId).collection("monthly_hisab_pdfs").document(pdf.id)
                .set(pdf.copy(localFilePath = "")).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading monthly PDF metadata", e)
        }
    }
}

