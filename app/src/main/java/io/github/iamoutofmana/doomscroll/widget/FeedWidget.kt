package io.github.iamoutofmana.doomscroll.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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

/*
 * Two widget variants (Phase 2):
 *
 * 1. FeedWidget — "front page": exactly the rows that fit the host, no clipping.
 *    Optimized for Samsung One UI, whose launcher does not deliver scroll gestures
 *    to third-party widgets (it shows a fixed window of the list).
 *
 * 2. ScrollingFeedWidget — full list in a root-level LazyColumn. Scrolls on launchers
 *    that support scrollable collection widgets (Nova, Kvaesitso, Lawnchair, Pixel
 *    Launcher) and clips to its viewport on One UI.
 *
 * Both share the same row rendering; taps deep-link into reader mode.
 */

class FeedWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val data = loadWidgetData(context)
        provideContent {
            WidgetList(data = data, fitToHost = true)
        }
    }
}

class ScrollingFeedWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val data = loadWidgetData(context)
        provideContent {
            WidgetList(data = data, fitToHost = false)
        }
    }
}

private data class WidgetData(
    val articles: List<ArticleEntity>,
    val header: String,
    val emptyText: String,
    val moreText: String,
    val openAppIntent: Intent,
)

private suspend fun loadWidgetData(context: Context): WidgetData {
    val container = (context.applicationContext as DoomscrollApplication).container
    val articles = runCatching {
        container.articleRepository.latestForWidget(WIDGET_LIMIT)
    }.getOrDefault(emptyList())
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return WidgetData(
        articles = articles,
        header = context.getString(R.string.widget_header),
        emptyText = context.getString(R.string.widget_empty),
        moreText = context.getString(R.string.widget_more),
        openAppIntent = openAppIntent,
    )
}

@Composable
private fun WidgetList(
    data: WidgetData,
    fitToHost: Boolean,
) {
    val height = LocalSize.current.height
    val visible = if (fitToHost) {
        data.articles.take(rowsThatFit(height))
    } else {
        data.articles
    }

    LazyColumn(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(BACKGROUND))
            .padding(12.dp),
    ) {
        item {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity(data.openAppIntent)),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = data.header,
                    style = TextStyle(
                        color = ColorProvider(FOREGROUND),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Spacer(GlanceModifier.height(8.dp))
        }
        if (data.articles.isEmpty()) {
            item {
                Text(
                    text = data.emptyText,
                    style = TextStyle(color = ColorProvider(DIM), fontSize = 13.sp),
                )
            }
        } else {
            items(visible) { article ->
                WidgetArticleRow(article)
            }
            item {
                Text(
                    text = data.moreText,
                    style = TextStyle(color = ColorProvider(DIM), fontSize = 11.sp),
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(actionStartActivity(data.openAppIntent))
                        .padding(top = 6.dp, bottom = 2.dp),
                )
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

/**
 * Rows that fit the host height for the static variant: header+padding ≈ 44dp, footer ≈ 24dp,
 * a two-line headline row ≈ 62dp. Conservative on purpose so no row ever renders half-cut.
 */
private fun rowsThatFit(height: Dp): Int {
    val available = height.value - HEADER_BLOCK_DP - FOOTER_BLOCK_DP
    if (available <= 0f) return 1
    return ((available / ROW_HEIGHT_DP).toInt()).coerceIn(1, WIDGET_LIMIT)
}

class FeedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FeedWidget()
}

class ScrollingFeedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScrollingFeedWidget()
}

private const val WIDGET_LIMIT = 24
private const val HEADER_BLOCK_DP = 44f
private const val FOOTER_BLOCK_DP = 24f
private const val ROW_HEIGHT_DP = 62f
private val BACKGROUND = Color(0xFF10151A)
private val FOREGROUND = Color(0xFFE9EEF2)
private val DIM = Color(0xFF9AA5AE)
