package io.github.iamoutofmana.doomscroll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import io.github.iamoutofmana.doomscroll.ui.DoomscrollApp
import io.github.iamoutofmana.doomscroll.ui.LocalAppContainer
import io.github.iamoutofmana.doomscroll.ui.theme.DoomscrollTheme

/** Single-activity app; all navigation lives in [DoomscrollApp] (manual tabs for Phase 1). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as DoomscrollApplication).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                DoomscrollTheme {
                    DoomscrollApp()
                }
            }
        }
    }
}
