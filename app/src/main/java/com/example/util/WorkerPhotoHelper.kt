package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.WorkerEntity
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object WorkerPhotoHelper {
    private const val TAG = "WorkerPhotoHelper"
    private const val MAX_PHOTO_DIM = 512

    fun getWorkerPhotoFile(context: Context, workerId: String): File {
        val dir = File(context.filesDir, "worker_photos").apply {
            if (!exists()) mkdirs()
        }
        val safeId = workerId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return File(dir, "${safeId}.jpg")
    }

    fun createTempCameraUri(context: Context): Pair<Uri, File>? {
        return try {
            val cacheDir = File(context.cacheDir, "camera_temp").apply {
                if (!exists()) mkdirs()
            }
            val file = File(cacheDir, "cam_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            uri to file
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating temp camera URI", e)
            null
        }
    }

    fun saveCompressedPhotoFromUri(
        context: Context,
        sourceUri: Uri,
        targetFileNameHint: String
    ): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val bytes = inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) return null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_PHOTO_DIM, MAX_PHOTO_DIM)

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null

            val rotated = applyExifRotationIfNeeded(bytes, decoded)
            val scaled = scaleDownBitmap(rotated, MAX_PHOTO_DIM)
            if (rotated != decoded) decoded.recycle()

            val dir = File(context.filesDir, "worker_photos").apply {
                if (!exists()) mkdirs()
            }
            val safeName = targetFileNameHint.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val destFile = File(dir, "${safeName}.jpg")
            FileOutputStream(destFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (scaled != rotated) rotated.recycle()
            scaled.recycle()
            destFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed processing photo URI", e)
            null
        }
    }

    fun copyToFinalWorkerPhoto(context: Context, sourcePath: String, workerId: String): File? {
        return try {
            val src = File(sourcePath)
            if (!src.exists()) return null
            val dest = getWorkerPhotoFile(context, workerId)
            if (src.absolutePath != dest.absolutePath) {
                src.copyTo(dest, overwrite = true)
            }
            dest
        } catch (e: Exception) {
            Log.e(TAG, "Failed copying worker photo", e)
            null
        }
    }

    fun loadBitmapForPdf(context: Context, worker: WorkerEntity): Bitmap? {
        return try {
            val candidate = when {
                worker.localPhotoPath.isNotBlank() && File(worker.localPhotoPath).exists() -> File(worker.localPhotoPath)
                getWorkerPhotoFile(context, worker.id).exists() -> getWorkerPhotoFile(context, worker.id)
                else -> null
            } ?: return null
            BitmapFactory.decodeFile(candidate.absolutePath)
        } catch (e: Exception) {
            Log.w(TAG, "Failed loading photo bitmap for PDF: ${e.message}")
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun scaleDownBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / largest.toFloat()
        val newW = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
        val newH = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun applyExifRotationIfNeeded(bytes: ByteArray, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(bytes.inputStream())
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees == 0f) return bitmap
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }
}
