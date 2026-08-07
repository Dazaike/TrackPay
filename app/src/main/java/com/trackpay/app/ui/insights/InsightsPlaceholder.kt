package com.trackpay.app.ui.insights

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.trackpay.app.ui.components.PlaceholderPane

@Composable
fun InsightsPlaceholder(modifier: Modifier = Modifier) {
    PlaceholderPane(
        title = "Insights",
        subtitle = "Phase 0 · Insights",
        modifier = modifier,
    )
}
