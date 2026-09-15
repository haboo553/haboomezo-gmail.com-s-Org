package com.markety.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.markety.app.ui.navigation.MainAppScaffold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as MarketyApp
        com.markety.app.util.SecurityManager.init(this)

        setContent {
            // Full RTL Layout for Arabic Interface
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = androidx.compose.ui.graphics.Color(0xFF0284C7),
                        onPrimary = androidx.compose.ui.graphics.Color.White,
                        primaryContainer = androidx.compose.ui.graphics.Color(0xFFE0F2FE),
                        onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF0369A1),
                        secondary = androidx.compose.ui.graphics.Color(0xFF0D9488),
                        surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                        background = androidx.compose.ui.graphics.Color(0xFFF8FAFC)
                    )
                ) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        MainAppScaffold(app = app)
                    }
                }
            }
        }
    }
}
