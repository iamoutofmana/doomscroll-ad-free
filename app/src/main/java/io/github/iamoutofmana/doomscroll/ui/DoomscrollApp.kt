package io.github.iamoutofmana.doomscroll.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.github.iamoutofmana.doomscroll.R
import io.github.iamoutofmana.doomscroll.data.container.AppContainer
import io.github.iamoutofmana.doomscroll.ui.screens.FeedScreen
import io.github.iamoutofmana.doomscroll.ui.screens.ReaderScreen
import io.github.iamoutofmana.doomscroll.ui.screens.SavedScreen
import io.github.iamoutofmana.doomscroll.ui.screens.SourcesScreen

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided — wrap the tree in CompositionLocalProvider")
}

private enum class Tab(
    val icon: ImageVector,
    val labelRes: Int,
) {
    FEED(Icons.Default.Home, R.string.tab_feed),
    SAVED(Icons.Default.Star, R.string.tab_saved),
    SOURCES(Icons.Default.Settings, R.string.tab_sources),
}

@Composable
fun DoomscrollApp(pendingArticleId: MutableState<Long?>? = null) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var readerArticleId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Widget taps arrive through the activity; open reader mode directly.
    LaunchedEffect(pendingArticleId?.value) {
        pendingArticleId?.value?.let { id ->
            readerArticleId = id
            pendingArticleId.value = null
        }
    }

    val currentReader = readerArticleId
    if (currentReader != null) {
        ReaderScreen(articleId = currentReader, onClose = { readerArticleId = null })
        return
    }

    Scaffold(
        // Screens manage their own insets; the outer Scaffold only reserves the bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        when (selected) {
            0 -> FeedScreen(
                contentPadding = padding,
                onOpenSources = { selected = 2 },
                onRead = { readerArticleId = it },
            )
            1 -> SavedScreen(
                contentPadding = padding,
                onRead = { readerArticleId = it },
            )
            else -> SourcesScreen(contentPadding = padding)
        }
    }
}
