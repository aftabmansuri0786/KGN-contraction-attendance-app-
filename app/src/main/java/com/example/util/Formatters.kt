package com.example.util

import android.content.Context
import android.content.Intent
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Formatters {
    private val indianLocale = Locale("en", "IN")
    private val currencyFormat = NumberFormat.getCurrencyInstance(indianLocale).apply {
        maximumFractionDigits = 0
    }

    private val kolkataTimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = kolkataTimeZone
    }

    private val indianDisplayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
        timeZone = kolkataTimeZone
    }

    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.US).apply {
        timeZone = kolkataTimeZone
    }

    fun formatInr(amount: Double): String {
        return try {
            val formatted = currencyFormat.format(amount)
            // Replace generic symbols if needed, ensuring ₹ symbol
            if (!formatted.contains("₹")) "₹$formatted" else formatted
        } catch (e: Exception) {
            "₹${amount.toLong()}"
        }
    }

    fun getTodayIsoDate(): String {
        return isoDateFormat.format(Date())
    }

    fun formatToIndianDate(isoDate: String): String {
        return try {
            val parsed = isoDateFormat.parse(isoDate) ?: return isoDate
            indianDisplayDateFormat.format(parsed)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun formatToIsoDate(date: Date): String {
        return isoDateFormat.format(date)
    }

    fun getMonthYearDisplay(date: Date = Date()): String {
        return monthYearFormat.format(date)
    }

    fun shareText(context: Context, title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, title)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
