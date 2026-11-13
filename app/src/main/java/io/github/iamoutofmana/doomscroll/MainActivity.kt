package io.github.iamoutofmana.doomscroll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.iamoutofmana.doomscroll.ui.screens.HomeScreen
import io.github.iamoutofmana.doomscroll.ui.theme.DoomscrollTheme

/**
 * Single-activity app. All screens are Compose destinations inside [HomeScreen];
 * navigation arrives in Phase 1 and widget deep-links in Phase 2 (see docs/PLAN.md).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoomscrollTheme {
                HomeScreen()
            }
        }
    }
}
