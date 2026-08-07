package com.trackpay.app.ui.util

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object MoneyFormat {
    private val currency: Currency = Currency.getInstance(Locale.US)

    fun format(minor: Long, locale: Locale = Locale.getDefault()): String {
        val major = minor / 100.0
        val nf = NumberFormat.getCurrencyInstance(locale).apply {
            currency = MoneyFormat.currency
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
        return nf.format(major)
    }

    fun formatRate(hourlyMinor: Long, locale: Locale = Locale.getDefault()): String =
        "${format(hourlyMinor, locale)}/hr"

    fun parseMajorToMinor(input: String): Long? {
        val cleaned = input.trim().replace(",", "").removePrefix("$")
        if (cleaned.isEmpty()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value < 0) return null
        return Math.round(value * 100.0)
    }
}

object TimeFormat {
    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h:mm a")

    fun formatElapsed(activeMillis: Long): String {
        val totalSeconds = maxOf(0L, activeMillis) / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatSince(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        return timeFormatter.format(zdt)
    }

    fun startOfLocalDayMillis(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        return zdt.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun startOfLocalWeekMillis(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val localDate = zdt.toLocalDate()
        val monday = localDate.minusDays(((localDate.dayOfWeek.value + 6) % 7).toLong())
        return monday.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun millisUntilNextSecond(now: Long = System.currentTimeMillis()): Long {
        val rem = now % 1_000L
        return if (rem == 0L) 1_000L else 1_000L - rem
    }
}

object DateTimeFormat {
    private val localDateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun formatLocalDateTime(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        return localDateTimeFormatter.format(zdt)
    }

    fun parseLocalDateTime(
        input: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        return runCatching {
            val local = java.time.LocalDateTime.parse(trimmed, localDateTimeFormatter)
            local.atZone(zoneId).toInstant().toEpochMilli()
        }.getOrNull()
    }
}

object DurationMath {
    fun hoursToMillis(hours: Double): Long = (hours * 3_600_000.0).toLong()
    fun minutesToMillis(minutes: Long): Long = TimeUnit.MINUTES.toMillis(minutes)
    fun absDiff(a: Long, b: Long): Long = abs(a - b)
}
