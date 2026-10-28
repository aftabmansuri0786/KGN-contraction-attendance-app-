package com.example.data.repository

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.AdvanceEntity
import com.example.data.model.AttendanceEntity
import com.example.data.model.PaymentEntity
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

    companion object {
        private const val TAG = "FirestoreSync"
    }

    suspend fun syncAll(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting two-way sync for user $userId")
            val userDoc = firestore.collection("users").document(userId)

            // 1. Sync Workers from remote
            val remoteWorkers = userDoc.collection("workers").get().await()
            val remoteWorkerList = remoteWorkers.documents.mapNotNull { doc ->
                try {
                    WorkerEntity(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        mobile = doc.getString("mobile") ?: "",
                        category = doc.getString("category") ?: "Helper",
                        dailyWage = doc.getDouble("dailyWage") ?: 0.0,
                        overtimeHourlyRate = doc.getDouble("overtimeHourlyRate") ?: 0.0,
                        joiningDate = doc.getString("joiningDate") ?: "",
                        siteId = doc.getString("siteId") ?: "",
                        isActive = doc.getBoolean("isActive") ?: true,
                        notes = doc.getString("notes") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (remoteWorkerList.isNotEmpty()) {
                workerDao.insertWorkers(remoteWorkerList)
            }

            // Upload any local workers to remote
            val localWorkers = workerDao.getAllWorkers().first()
            for (w in localWorkers) {
                userDoc.collection("workers").document(w.id).set(w).await()
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

            Log.d(TAG, "Sync successfully completed for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing with Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun uploadWorker(userId: String, worker: WorkerEntity) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(userId).collection("workers").document(worker.id).set(worker).await()
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
}
