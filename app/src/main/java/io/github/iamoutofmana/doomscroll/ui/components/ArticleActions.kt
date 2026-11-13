package io.github.iamoutofmana.doomscroll.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import io.github.iamoutofmana.doomscroll.data.db.ArticleEntity
import io.github.iamoutofmana.doomscroll.domain.links.LinkSanitizer
import io.github.iamoutofmana.doomscroll.ui.LocalAppContainer
import io.github.iamoutofmana.doomscroll.ui.openInCustomTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Shared card-tap behavior: mark read + open the sanitized URL in Custom Tabs. */
@Composable
fun rememberOpenArticle(): (ArticleEntity) -> Unit {
    val container = LocalAppContainer.current
    val context: Context = LocalContext.current
    val scope: CoroutineScope = rememberCoroutineScope()
    return { article ->
        scope.launch { container.articleRepository.markRead(article.id) }
        val target = LinkSanitizer.sanitize(article.resolvedLink ?: article.link)
        if (target.isNotBlank()) context.openInCustomTab(target)
    }
}
