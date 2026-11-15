package io.github.iamoutofmana.doomscroll.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.updateAll
import io.github.iamoutofmana.doomscroll.MainActivity

/** Article id carried from a widget row tap into [OpenArticleAction]. */
val KEY_ARTICLE_ID = ActionParameters.Key<Long>("io.github.iamoutofmana.doomscroll.widget.ARTICLE_ID")

fun actionRunCallbackWithArticle(articleId: Long): Action =
    actionRunCallback<OpenArticleAction>(actionParametersOf(KEY_ARTICLE_ID to articleId))

/** Opens the app straight into reader mode for the tapped article. */
class OpenArticleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val articleId = parameters[KEY_ARTICLE_ID] ?: return
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_ARTICLE_ID, articleId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}

/** Re-renders the widget after a sync so it never shows stale headlines. */
suspend fun refreshFeedWidget(context: Context) {
    runCatching { FeedWidget().updateAll(context) }
}
