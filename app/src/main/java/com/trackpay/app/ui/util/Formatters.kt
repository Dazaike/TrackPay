package com.trackpay.app.ui.util

import com.trackpay.app.domain.model.JobDefaults
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * ISO 4217 helpers for settings lists and [MoneyFormat].
 */
object CurrencyFormat {
    val COMMON_CODES: List<String> = listOf(
        "USD", "EUR", "GBP", "CAD", "AUD", "NZD", "JPY", "CHF",
        "SEK", "NOK", "DKK", "PLN", "CZK", "HUF", "RON", "BGN",
        "TRY", "BRL", "MXN", "ARS", "CLP", "COP", "PEN",
        "INR", "PKR", "BDT", "LKR", "NPR",
        "CNY", "HKD", "TWD", "KRW", "SGD", "MYR", "THB", "IDR", "PHP", "VND",
        "AED", "SAR", "ILS", "ZAR", "NGN", "KES", "GHS", "EGP",
    )

    fun resolve(code: String?): Currency {
        val normalized = code?.trim()?.uppercase().orEmpty()
        if (normalized.isEmpty()) {
            return Currency.getInstance(JobDefaults.DEFAULT_CURRENCY_CODE)
        }
        return runCatching { Currency.getInstance(normalized) }
            .getOrElse { Currency.getInstance(JobDefaults.DEFAULT_CURRENCY_CODE) }
    }

    fun symbol(code: String?, locale: Locale = Locale.getDefault()): String =
        resolve(code).getSymbol(locale)

    fun displayName(code: String?, locale: Locale = Locale.getDefault()): String {
        val currency = resolve(code)
        return "${currency.currencyCode} (${currency.getSymbol(locale)})"
    }
}

object MoneyFormat {
    fun format(
        minor: Long,
        locale: Locale = Locale.getDefault(),
        currencyCode: String = JobDefaults.DEFAULT_CURRENCY_CODE,
    ): String {
        val currency = CurrencyFormat.resolve(currencyCode)
        val fractionDigits = currency.defaultFractionDigits.coerceAtLeast(0)
        val divisor = Math.pow(10.0, fractionDigits.toDouble())
        val major = if (fractionDigits == 0) minor.toDouble() else minor / divisor
        val nf = NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
            maximumFractionDigits = fractionDigits
            minimumFractionDigits = fractionDigits
        }
        return nf.format(major)
    }

    fun formatRate(
        hourlyMinor: Long,
        locale: Locale = Locale.getDefault(),
        currencyCode: String = JobDefaults.DEFAULT_CURRENCY_CODE,
    ): String = "${format(hourlyMinor, locale, currencyCode)}/hr"

    fun parseMajorToMinor(
        input: String,
        currencyCode: String = JobDefaults.DEFAULT_CURRENCY_CODE,
    ): Long? {
        val currency = CurrencyFormat.resolve(currencyCode)
        val fractionDigits = currency.defaultFractionDigits.coerceAtLeast(0)
        val symbol = currency.getSymbol(Locale.getDefault())
        val cleaned = input.trim()
            .replace(",", "")
            .removePrefix(symbol)
            .removePrefix(currency.currencyCode)
            .removePrefix("$")
            .trim()
        if (cleaned.isEmpty()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value < 0) return null
        val multiplier = Math.pow(10.0, fractionDigits.toDouble())
        return Math.round(value * multiplier)
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
