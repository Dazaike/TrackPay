package com.trackpay.app.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.trackpay.app.ui.components.PlaceholderPane

@Composable
fun HistoryPlaceholder(modifier: Modifier = Modifier) {
    PlaceholderPane(
        title = "History",
        subtitle = "Phase 0 · History",
        modifier = modifier,
    )
}
