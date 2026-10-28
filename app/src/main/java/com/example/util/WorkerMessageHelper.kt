package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

enum class NotificationMessageType {
    ATTENDANCE,
    PAYMENT,
    ADVANCE
}

data class WorkerMessageData(
    val workerId: String,
    val workerName: String,
    val workerMobile: String,
    val type: NotificationMessageType,
    val messageText: String,
    val dateDisplay: String,
    val amount: Double = 0.0,
    val balance: Double = 0.0,
    val statusText: String = ""
)

object WorkerMessageHelper {

    fun maskPhoneNumber(mobile: String): String {
        val digits = mobile.filter { it.isDigit() }
        return when {
            digits.length >= 10 -> {
                val last10 = digits.takeLast(10)
                "${last10.substring(0, 5)}*****"
            }
            digits.isNotBlank() -> "${digits.take(3)}***"
            else -> "Not Provided"
        }
    }

    fun isValidPhoneNumber(mobile: String): Boolean {
        val digits = mobile.filter { it.isDigit() }
        return digits.length >= 10
    }

    fun cleanForWhatsApp(mobile: String): String {
        val digits = mobile.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "91$digits"
            digits.length == 11 && digits.startsWith("0") -> "91${digits.drop(1)}"
            digits.length == 12 && digits.startsWith("91") -> digits
            else -> digits
        }
    }

    fun cleanForSms(mobile: String): String {
        val digits = mobile.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    fun buildAttendanceMessage(workerName: String, date: String, status: String): String {
        return "Dear $workerName, your attendance for $date has been recorded successfully. Status: $status. Regards, KGN Construction."
    }

    fun buildPaymentMessage(workerName: String, amount: Double, date: String, remainingBalance: Double): String {
        val amountStr = if (amount % 1.0 == 0.0) amount.toLong().toString() else String.format("%.2f", amount)
        val balanceStr = if (remainingBalance % 1.0 == 0.0) remainingBalance.toLong().toString() else String.format("%.2f", remainingBalance)
        return "Dear $workerName, your wage payment of ₹$amountStr has been recorded on $date. Remaining wage balance: ₹$balanceStr. Regards, KGN Construction."
    }

    fun buildAdvanceMessage(workerName: String, amount: Double, date: String, totalAdvanceBalance: Double): String {
        val amountStr = if (amount % 1.0 == 0.0) amount.toLong().toString() else String.format("%.2f", amount)
        val totalAdvStr = if (totalAdvanceBalance % 1.0 == 0.0) totalAdvanceBalance.toLong().toString() else String.format("%.2f", totalAdvanceBalance)
        return "Dear $workerName, an advance of ₹$amountStr has been recorded on $date. Total outstanding advance: ₹$totalAdvStr. Regards, KGN Construction."
    }

    fun openWhatsApp(context: Context, mobile: String, message: String) {
        val cleanPhone = cleanForWhatsApp(mobile)
        if (cleanPhone.length < 10) {
            Toast.makeText(context, "Worker does not have a valid mobile number", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback to wa.me link
                val encodedMessage = URLEncoder.encode(message, "UTF-8")
                val fallbackUrl = "https://wa.me/$cleanPhone?text=$encodedMessage"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Could not open WhatsApp. Please check if app or browser is installed.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun openSms(context: Context, mobile: String, message: String) {
        val cleanPhone = cleanForSms(mobile)
        if (cleanPhone.length < 10) {
            Toast.makeText(context, "Worker does not have a valid mobile number", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanPhone")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback generic SEND intent
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:$cleanPhone")
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Could not open Messages app.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("KGN Message", text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Message copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}
