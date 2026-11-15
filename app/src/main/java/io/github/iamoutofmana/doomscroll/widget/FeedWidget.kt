package io.github.iamoutofmana.doomscroll.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.iamoutofmana.doomscroll.DoomscrollApplication
import io.github.iamoutofmana.doomscroll.MainActivity
import io.github.iamoutofmana.doomscroll.R
import io.github.iamoutofmana.doomscroll.data.db.ArticleEntity
import io.github.iamoutofmana.doomscroll.domain.time.TimeText

/**
 * The media-page replacement (Phase 2): a scrollable feed widget for the home screen.
 * Reads the local DB at render time and is re-rendered after every sync.
 * Taps deep-link straight into reader mode.
 */
class FeedWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val container = (context.applicationContext as DoomscrollApplication).container
        val articles = runCatching {
            container.articleRepository.latestForWidget(WIDGET_LIMIT)
        }.getOrDefault(emptyList())
        val header = context.getString(R.string.widget_header)
        val emptyText = context.getString(R.string.widget_empty)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        provideContent {
            WidgetContent(
                articles = articles,
                header = header,
                emptyText = emptyText,
                openAppIntent = openAppIntent,
            )
        }
    }
}

@Composable
private fun WidgetContent(
    articles: List<ArticleEntity>,
    header: String,
    emptyText: String,
    openAppIntent: Intent,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(BACKGROUND))
            .padding(12.dp),
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(openAppIntent)),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = header,
                style = TextStyle(
                    color = ColorProvider(FOREGROUND),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        if (articles.isEmpty()) {
            Text(
                text = emptyText,
                style = TextStyle(color = ColorProvider(DIM), fontSize = 13.sp),
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(articles) { article ->
                    WidgetArticleRow(article)
                }
            }
        }
    }
}

@Composable
private fun WidgetArticleRow(article: ArticleEntity) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionRunCallbackWithArticle(article.id))
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = article.title,
            style = TextStyle(
                color = ColorProvider(FOREGROUND),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 2,
        )
        val meta = listOfNotNull(
            article.publisher,
            TimeText.relative(article.publishedAt).ifBlank { null },
        ).joinToString("  ·  ")
        if (meta.isNotBlank()) {
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = meta,
                style = TextStyle(color = ColorProvider(DIM), fontSize = 10.sp),
                maxLines = 1,
            )
        }
    }
}

class FeedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FeedWidget()
}

private const val WIDGET_LIMIT = 12
private val BACKGROUND = Color(0xFF10151A)
private val FOREGROUND = Color(0xFFE9EEF2)
private val DIM = Color(0xFF9AA5AE)
