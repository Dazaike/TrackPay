package com.trackpay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackpay.app.domain.model.JobDefaults
import com.trackpay.app.domain.model.ThemeIds
import com.trackpay.app.domain.usecase.ObserveActiveThemeIdUseCase
import com.trackpay.app.domain.usecase.ObserveCurrencyCodeUseCase
import com.trackpay.app.ui.shell.TrackPayAppShell
import com.trackpay.app.ui.theme.TrackPayTheme
import com.trackpay.app.ui.util.LocalCurrencyCode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var observeActiveThemeId: ObserveActiveThemeIdUseCase

    @Inject
    lateinit var observeCurrencyCode: ObserveCurrencyCodeUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeId by observeActiveThemeId().collectAsStateWithLifecycle(
                initialValue = ThemeIds.DEFAULT,
            )
            val currencyCode by observeCurrencyCode().collectAsStateWithLifecycle(
                initialValue = JobDefaults.DEFAULT_CURRENCY_CODE,
            )
            TrackPayTheme(themeId = themeId) {
                CompositionLocalProvider(LocalCurrencyCode provides currencyCode) {
                    TrackPayAppShell()
                }
            }
        }
    }
}
