package io.github.iamoutofmana.doomscroll

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import io.github.iamoutofmana.doomscroll.ui.DoomscrollApp
import io.github.iamoutofmana.doomscroll.ui.LocalAppContainer
import io.github.iamoutofmana.doomscroll.ui.theme.DoomscrollTheme

/** Single-activity app; widget taps arrive as [EXTRA_ARTICLE_ID] deep links into reader mode. */
class MainActivity : ComponentActivity() {
    private val pendingArticleId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        val container = (application as DoomscrollApplication).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                DoomscrollTheme {
                    DoomscrollApp(pendingArticleId)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val articleId = intent?.getLongExtra(EXTRA_ARTICLE_ID, -1L) ?: -1L
        if (articleId > 0) {
            intent?.removeExtra(EXTRA_ARTICLE_ID)
            pendingArticleId.value = articleId
        }
    }

    companion object {
        const val EXTRA_ARTICLE_ID = "io.github.iamoutofmana.doomscroll.extra.ARTICLE_ID"
    }
}
