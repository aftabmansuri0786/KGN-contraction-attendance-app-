package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin

data class AmavasyaCalendarDate(
    val isoDate: String, // YYYY-MM-DD in Asia/Kolkata
    val displayDate: String, // DD/MM/YYYY
    val lunarNameEn: String, // e.g., "Kartik Amavasya"
    val lunarNameHi: String, // e.g., "कार्तिक अमावस्या"
    val weekdayNameEn: String,
    val weekdayNameHi: String,
    val newMoonInstantUtcMs: Long
)

object AmavasyaCalendar {
    private val kolkataTz = TimeZone.getTimeZone("Asia/Kolkata")
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = kolkataTz
        isLenient = false
    }
    private val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
        timeZone = kolkataTz
    }
    private val weekdayEnFormat = SimpleDateFormat("EEE", Locale.US).apply {
        timeZone = kolkataTz
    }

    // Approximate Indian Purnimanta lunar month mapping by Gregorian month of Amavasya
    // (Amavasya occurs near the middle/end of the solar month and ends the Purnimanta lunar month)
    private val lunarMonthPairs = listOf(
        "Pausha Amavasya" to "पौष अमावस्या",          // Jan
        "Magha Amavasya" to "माघ अमावस्या",           // Feb
        "Phalguna Amavasya" to "फाल्गुन अमावस्या",    // Mar
        "Chaitra Amavasya" to "चैत्र अमावस्या",        // Apr
        "Vaishakha Amavasya" to "वैशाख अमावस्या",      // May
        "Jyeshtha Amavasya" to "ज्येष्ठ अमावस्या",     // Jun
        "Ashadha Amavasya" to "आषाढ़ अमावस्या",       // Jul
        "Shravana Amavasya" to "श्रावण अमावस्या",      // Aug
        "Bhadrapada Amavasya" to "भाद्रपद अमावस्या",  // Sep
        "Ashwin Amavasya" to "आश्विन अमावस्या",        // Oct
        "Kartik Amavasya" to "कार्तिक अमावस्या",       // Nov
        "Margashirsha Amavasya" to "मार्गशीर्ष अमावस्या" // Dec
    )

    private val weekdayHindiMap = mapOf(
        Calendar.SUNDAY to "रविवार",
        Calendar.MONDAY to "सोमवार",
        Calendar.TUESDAY to "मंगलवार",
        Calendar.WEDNESDAY to "बुधवार",
        Calendar.THURSDAY to "गुरुवार",
        Calendar.FRIDAY to "शुक्रवार",
        Calendar.SATURDAY to "शनिवार"
    )

    /**
     * Computes exact UTC millis of New Moon (Phase k = integer) using Jean Meeus' Astronomical Algorithms (Ch. 49).
     * k = 0 corresponds to New Moon of Jan 6, 2000.
     */
    private fun calculateNewMoonUtcMillis(k: Double): Long {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t

        // Mean JDE of phase
        var jde = 2451550.09766 +
            29.530588861 * k +
            0.00015437 * t2 -
            0.000000150 * t3 +
            0.00000000073 * t4

        // Sun's mean anomaly (degrees -> radians)
        val degToRad = Math.PI / 180.0
        val m = (2.5534 + 29.10535670 * k - 0.0000014 * t2 - 0.00000011 * t3) * degToRad
        // Moon's mean anomaly
        val mp = (201.5643 + 385.81693528 * k + 0.0107582 * t2 + 0.00001238 * t3 - 0.000000058 * t4) * degToRad
        // Moon's argument of latitude
        val f = (160.7108 + 390.67050284 * k - 0.0016118 * t2 - 0.00000227 * t3 + 0.000000011 * t4) * degToRad
        // Longitude of ascending node
        val omega = (124.7746 - 1.56375588 * k + 0.0020672 * t2 + 0.00000215 * t3) * degToRad
        // Eccentricity correction
        val e = 1.0 - 0.002516 * t - 0.0000074 * t2

        // Periodic corrections for New Moon
        val correction = -0.40720 * sin(mp) +
            0.17241 * e * sin(m) +
            0.01608 * sin(2 * mp) +
            0.01039 * sin(2 * f) +
            0.00739 * e * sin(mp - m) -
            0.00514 * e * sin(mp + m) +
            0.00208 * e * e * sin(2 * m) -
            0.00111 * sin(mp - 2 * f) -
            0.00057 * sin(mp + 2 * f) +
            0.00056 * e * sin(2 * mp + m) -
            0.00042 * sin(3 * mp) +
            0.00042 * e * sin(m + 2 * f) +
            0.00038 * e * sin(m - 2 * f) -
            0.00024 * e * sin(2 * mp - m) -
            0.00017 * sin(omega) -
            0.00007 * sin(mp + 2 * m) +
            0.00004 * sin(2 * mp - 2 * f) +
            0.00004 * sin(3 * m) +
            0.00003 * sin(mp + m - 2 * f) +
            0.00003 * sin(2 * mp + 2 * f) -
            0.00003 * sin(mp + m + 2 * f) +
            0.00003 * sin(mp - m + 2 * f) -
            0.00002 * sin(mp - m - 2 * f) -
            0.00002 * sin(3 * mp + m) +
            0.00002 * sin(4 * mp)

        jde += correction
        // Convert Julian Date (JDE, approximate UT) to Unix epoch milliseconds:
        // JD 2440587.5 = 1970-01-01T00:00:00Z
        return ((jde - 2440587.5) * 86400000.0).roundToLong()
    }

    private fun buildCalendarDateFromInstant(utcMillis: Long): AmavasyaCalendarDate {
        val cal = Calendar.getInstance(kolkataTz).apply {
            timeInMillis = utcMillis
        }
        // In Hindu Panchang, if New Moon ends before afternoon/Pradosh or starts after sunrise,
        // the Amavasya date observed is typically the IST date on which the Amavasya tithi spans sunset/noon.
        // Using the IST calendar date of the New Moon instant aligns closely with Indian civil Amavasya dates.
        val dateObj = cal.time
        val iso = synchronized(isoFormat) { isoFormat.format(dateObj) }
        val display = synchronized(displayFormat) { displayFormat.format(dateObj) }
        val monthIdx = cal.get(Calendar.MONTH).coerceIn(0, 11)
        val (nameEn, nameHi) = lunarMonthPairs[monthIdx]
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val weekdayEn = synchronized(weekdayEnFormat) { weekdayEnFormat.format(dateObj) }
        val weekdayHi = weekdayHindiMap[dayOfWeek] ?: weekdayEn

        return AmavasyaCalendarDate(
            isoDate = iso,
            displayDate = display,
            lunarNameEn = nameEn,
            lunarNameHi = nameHi,
            weekdayNameEn = weekdayEn,
            weekdayNameHi = weekdayHi,
            newMoonInstantUtcMs = utcMillis
        )
    }

    fun getUpcomingAmavasyaDates(
        fromIsoDate: String = Formatters.getTodayIsoDate(),
        count: Int = 12
    ): List<AmavasyaCalendarDate> {
        val startCal = parseIsoToCalendar(fromIsoDate) ?: Calendar.getInstance(kolkataTz)
        val yearDecimal = startCal.get(Calendar.YEAR) +
            (startCal.get(Calendar.DAY_OF_YEAR) - 1) / 365.25
        var k = floor((yearDecimal - 2000.0) * 12.3685) - 2.0

        val results = mutableListOf<AmavasyaCalendarDate>()
        var safety = 0
        while (results.size < count && safety < 36) {
            val instant = calculateNewMoonUtcMillis(k)
            val entry = buildCalendarDateFromInstant(instant)
            if (entry.isoDate >= fromIsoDate) {
                if (results.none { it.isoDate == entry.isoDate }) {
                    results.add(entry)
                }
            }
            k += 1.0
            safety++
        }
        return results
    }

    fun getNextAmavasyaDate(fromIsoDate: String = Formatters.getTodayIsoDate()): AmavasyaCalendarDate {
        return getUpcomingAmavasyaDates(fromIsoDate, 1).first()
    }

    fun getAmavasyaDatesForMonth(year: Int, month1to12: Int): List<AmavasyaCalendarDate> {
        val monthStart = String.format(Locale.US, "%04d-%02d-01", year, month1to12)
        val cal = parseIsoToCalendar(monthStart) ?: return emptyList()
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthEnd = String.format(Locale.US, "%04d-%02d-%02d", year, month1to12, maxDay)
        return getUpcomingAmavasyaDates(addDays(monthStart, -5), 4)
            .filter { it.isoDate in monthStart..monthEnd }
    }

    fun findAmavasyaInfo(isoDate: String): AmavasyaCalendarDate? {
        val start = addDays(isoDate, -2)
        return getUpcomingAmavasyaDates(start, 2).find { it.isoDate == isoDate }
    }

    fun parseIsoToCalendar(isoDate: String): Calendar? {
        return try {
            val date = synchronized(isoFormat) { isoFormat.parse(isoDate) } ?: return null
            Calendar.getInstance(kolkataTz).apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun daysBetween(fromIsoDate: String, toIsoDate: String): Long {
        val fromCal = parseIsoToCalendar(fromIsoDate) ?: return 0L
        val toCal = parseIsoToCalendar(toIsoDate) ?: return 0L
        val diffMs = toCal.timeInMillis - fromCal.timeInMillis
        return (diffMs / 86400000.0).roundToLong()
    }

    fun addDays(isoDate: String, days: Int): String {
        val cal = parseIsoToCalendar(isoDate) ?: return isoDate
        cal.add(Calendar.DAY_OF_MONTH, days)
        return synchronized(isoFormat) { isoFormat.format(cal.time) }
    }

    fun formatMillisToIso(millisUtc: Long): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millisUtc
        }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    fun parseIsoToUtcDatePickerMillis(isoDate: String): Long? {
        val parts = isoDate.split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(y, m - 1, d, 0, 0, 0)
        }.timeInMillis
    }

    fun getMonthTitle(year: Int, month1to12: Int): String {
        val cal = Calendar.getInstance(kolkataTz).apply {
            clear()
            set(year, month1to12 - 1, 1)
        }
        return SimpleDateFormat("MMMM yyyy", Locale.US).apply { timeZone = kolkataTz }.format(cal.time)
    }

    fun formatTimestampIst(timestampMs: Long): String {
        return SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US).apply {
            timeZone = kolkataTz
        }.format(Date(timestampMs))
    }
}
