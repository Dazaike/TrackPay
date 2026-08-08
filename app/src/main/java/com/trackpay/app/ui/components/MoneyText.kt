package com.trackpay.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.trackpay.app.R
import com.trackpay.app.ui.util.LocalCurrencyCode
import com.trackpay.app.ui.util.MoneyFormat

@Composable
fun MoneyText(
    amountMinor: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayLarge.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        fontFeatureSettings = "tnum",
    ),
    color: Color = MaterialTheme.colorScheme.primary,
    textAlign: TextAlign? = null,
    currencyCode: String = LocalCurrencyCode.current,
) {
    val formatted = MoneyFormat.format(amountMinor, currencyCode = currencyCode)
    val a11y = stringResource(R.string.a11y_money_amount, formatted)
    Text(
        text = formatted,
        modifier = modifier.semantics { contentDescription = a11y },
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = 1,
    )
}
