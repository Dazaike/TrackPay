package com.trackpay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackpay.app.domain.model.ThemeIds
import com.trackpay.app.domain.usecase.ObserveActiveThemeIdUseCase
import com.trackpay.app.ui.shell.TrackPayAppShell
import com.trackpay.app.ui.theme.TrackPayTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var observeActiveThemeId: ObserveActiveThemeIdUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeId by observeActiveThemeId().collectAsStateWithLifecycle(
                initialValue = ThemeIds.DEFAULT,
            )
            TrackPayTheme(themeId = themeId) {
                TrackPayAppShell()
            }
        }
    }
}
