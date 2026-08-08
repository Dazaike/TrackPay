package com.trackpay.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import com.trackpay.app.domain.model.JobDefaults

/**
 * Active ISO 4217 currency from Settings / onboarding.
 * Provided at the app root so money UI reformats when the preference changes.
 */
val LocalCurrencyCode = compositionLocalOf { JobDefaults.DEFAULT_CURRENCY_CODE }

fun formatMoney(amountMinor: Long, currencyCode: String): String =
    MoneyFormat.format(amountMinor, currencyCode = currencyCode)

fun formatMoneyRate(hourlyMinor: Long, currencyCode: String): String =
    MoneyFormat.formatRate(hourlyMinor, currencyCode = currencyCode)

@Composable
@ReadOnlyComposable
fun formatMoney(amountMinor: Long): String =
    formatMoney(amountMinor, LocalCurrencyCode.current)

@Composable
@ReadOnlyComposable
fun formatMoneyRate(hourlyMinor: Long): String =
    formatMoneyRate(hourlyMinor, LocalCurrencyCode.current)

@Composable
@ReadOnlyComposable
fun currencySymbol(): String =
    CurrencyFormat.symbol(LocalCurrencyCode.current)
