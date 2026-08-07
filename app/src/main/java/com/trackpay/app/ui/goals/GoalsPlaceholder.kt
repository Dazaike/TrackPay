package com.trackpay.app.ui.goals

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.trackpay.app.ui.components.PlaceholderPane

@Composable
fun GoalsPlaceholder(modifier: Modifier = Modifier) {
    PlaceholderPane(
        title = "Goals",
        subtitle = "Phase 0 · Goals",
        modifier = modifier,
    )
}
