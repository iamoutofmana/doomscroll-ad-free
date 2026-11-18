package io.github.iamoutofmana.doomscroll.data.container

import android.content.Context
import androidx.room.Room
import io.github.iamoutofmana.doomscroll.data.db.DoomscrollDatabase
import io.github.iamoutofmana.doomscroll.data.network.ArticleEnricher
import io.github.iamoutofmana.doomscroll.data.network.FeedDiscovery
import io.github.iamoutofmana.doomscroll.data.network.FeedFetcher
import io.github.iamoutofmana.doomscroll.data.network.GnRedirectResolver
import io.github.iamoutofmana.doomscroll.data.repo.ArticleRepository
import io.github.iamoutofmana.doomscroll.data.repo.EnrichmentService
import io.github.iamoutofmana.doomscroll.data.repo.ReaderService
import io.github.iamoutofmana.doomscroll.data.repo.RuleRepository
import io.github.iamoutofmana.doomscroll.data.repo.SourceRepository
import io.github.iamoutofmana.doomscroll.data.sync.SyncEngine
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Manual dependency injection (plan §4): one container, no framework — keeps the build
 * simple and F-Droid-friendly. UI reaches it via LocalAppContainer; SyncWorker via the
 * Application cast.
 */
class AppContainer(context: Context) {
    val database: DoomscrollDatabase =
        Room.databaseBuilder(context.applicationContext, DoomscrollDatabase::class.java, "doomscroll.db")
            .addMigrations(DoomscrollDatabase.MIGRATION_1_2)
            .build()

    private val httpClient: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

    val feedFetcher = FeedFetcher(httpClient)
    val gnRedirectResolver = GnRedirectResolver(httpClient)
    val sourceRepository = SourceRepository(database.sourceDao())
    val articleRepository = ArticleRepository(database.articleDao())
    val ruleRepository = RuleRepository(database.ruleDao(), database.articleDao())
    val articleEnricher = ArticleEnricher(httpClient, gnRedirectResolver)
    val readerService = ReaderService(httpClient, gnRedirectResolver, articleRepository)
    val feedDiscovery = FeedDiscovery(httpClient)
    val syncEngine = SyncEngine(sourceRepository, articleRepository, ruleRepository, feedFetcher, gnRedirectResolver)
    val enrichment = EnrichmentService(articleRepository, articleEnricher)

    /** In-memory stamp so the UI can auto-sync on open when stale (process lifetime only). */
    @Volatile
    var lastAutoSyncAt: Long = 0L
}
