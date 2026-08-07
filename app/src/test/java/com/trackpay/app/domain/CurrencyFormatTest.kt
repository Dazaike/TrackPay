package com.trackpay.app.domain

import com.trackpay.app.ui.util.CurrencyFormat
import com.trackpay.app.ui.util.MoneyFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CurrencyFormatTest {

    @Test
    fun resolve_falls_back_to_usd_on_blank_or_invalid() {
        assertEquals("USD", CurrencyFormat.resolve(null).currencyCode)
        assertEquals("USD", CurrencyFormat.resolve("").currencyCode)
        assertEquals("USD", CurrencyFormat.resolve("nope").currencyCode)
        assertEquals("USD", CurrencyFormat.resolve("ZZZ").currencyCode)
    }

    @Test
    fun resolve_accepts_common_iso_codes() {
        assertEquals("EUR", CurrencyFormat.resolve("eur").currencyCode)
        assertEquals("GBP", CurrencyFormat.resolve("GBP").currencyCode)
        assertEquals("JPY", CurrencyFormat.resolve("jpy").currencyCode)
        assertTrue(CurrencyFormat.COMMON_CODES.contains("USD"))
        assertTrue(CurrencyFormat.COMMON_CODES.contains("EUR"))
    }

    @Test
    fun format_usd_two_fraction_digits() {
        val text = MoneyFormat.format(1_250L, Locale.US, "USD")
        assertTrue(text.contains("12.50") || text.contains("12,50"))
        assertTrue(text.contains("$") || text.contains("USD"))
    }

    @Test
    fun format_jpy_zero_fraction_digits() {
        // JPY defaultFractionDigits = 0; minor units treated as whole yen
        val text = MoneyFormat.format(1_250L, Locale.US, "JPY")
        assertTrue(text.contains("1,250") || text.contains("1250") || text.contains("¥"))
    }

    @Test
    fun format_rate_appends_hr() {
        val text = MoneyFormat.formatRate(2_500L, Locale.US, "USD")
        assertTrue(text.endsWith("/hr"))
    }

    @Test
    fun parse_major_to_minor_usd() {
        assertEquals(2_500L, MoneyFormat.parseMajorToMinor("25.00", "USD"))
        assertEquals(2_500L, MoneyFormat.parseMajorToMinor("$25", "USD"))
        assertEquals(2_500L, MoneyFormat.parseMajorToMinor("25", "USD"))
        assertNull(MoneyFormat.parseMajorToMinor("", "USD"))
        assertNull(MoneyFormat.parseMajorToMinor("-5", "USD"))
        assertNull(MoneyFormat.parseMajorToMinor("abc", "USD"))
    }

    @Test
    fun parse_major_to_minor_jpy_whole_units() {
        assertEquals(1_250L, MoneyFormat.parseMajorToMinor("1250", "JPY"))
    }

    @Test
    fun display_name_includes_code() {
        val name = CurrencyFormat.displayName("USD", Locale.US)
        assertTrue(name.startsWith("USD"))
        assertNotNull(CurrencyFormat.symbol("EUR", Locale.US))
        assertFalse(CurrencyFormat.symbol("EUR", Locale.US).isBlank())
    }
}
