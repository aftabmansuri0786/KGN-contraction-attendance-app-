package com.example.data.repository

import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

data class StorageUploadResult(
    val storagePath: String,
    val downloadUrl: String
)

object FirebaseStorageHelper {
    private const val TAG = "FirebaseStorageHelper"

    private fun getStorage(): FirebaseStorage? {
        return try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseStorage unavailable: ${e.message}")
            null
        }
    }

    suspend fun uploadWorkerPhoto(
        userId: String,
        workerId: String,
        localFile: File
    ): StorageUploadResult? = withContext(Dispatchers.IO) {
        if (!localFile.exists() || userId.isBlank()) return@withContext null
        val storage = getStorage() ?: return@withContext null
        return@withContext try {
            val safeId = workerId.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val storagePath = "users/$userId/worker_photos/${safeId}.jpg"
            val ref = storage.reference.child(storagePath)
            ref.putBytes(localFile.readBytes()).await()
            val url = ref.downloadUrl.await().toString()
            StorageUploadResult(storagePath = storagePath, downloadUrl = url)
        } catch (e: Exception) {
            Log.e(TAG, "Failed uploading worker photo for $workerId", e)
            null
        }
    }

    suspend fun uploadMonthlyPdf(
        userId: String,
        year: Int,
        month: Int,
        fileName: String,
        localFile: File
    ): StorageUploadResult? = withContext(Dispatchers.IO) {
        if (!localFile.exists() || userId.isBlank()) return@withContext null
        val storage = getStorage() ?: return@withContext null
        return@withContext try {
            val storagePath = "users/$userId/monthly_hisab_pdfs/$year/$month/$fileName"
            val ref = storage.reference.child(storagePath)
            ref.putBytes(localFile.readBytes()).await()
            val url = ref.downloadUrl.await().toString()
            StorageUploadResult(storagePath = storagePath, downloadUrl = url)
        } catch (e: Exception) {
            Log.e(TAG, "Failed uploading monthly PDF $fileName", e)
            null
        }
    }

    suspend fun downloadFileToLocal(
        storagePath: String,
        destinationFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (storagePath.isBlank()) return@withContext false
        val storage = getStorage() ?: return@withContext false
        return@withContext try {
            destinationFile.parentFile?.mkdirs()
            storage.reference.child(storagePath).getFile(destinationFile).await()
            destinationFile.exists() && destinationFile.length() > 0L
        } catch (e: Exception) {
            Log.w(TAG, "Could not download $storagePath: ${e.message}")
            false
        }
    }
}
