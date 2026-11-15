package io.github.iamoutofmana.doomscroll

import android.app.Application
import io.github.iamoutofmana.doomscroll.data.container.AppContainer
import io.github.iamoutofmana.doomscroll.data.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DoomscrollApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SyncScheduler.ensurePeriodic(this)
        invalidateStaleReaderCaches()
    }

    /**
     * Reader extraction improves over time; stamp the version so cached text from an
     * older extractor is dropped once instead of showing stale junk forever.
     */
    private fun invalidateStaleReaderCaches() {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = getSharedPreferences("doomscroll", MODE_PRIVATE)
            if (prefs.getInt(KEY_READER_VERSION, 0) >= READER_EXTRACTION_VERSION) return@launch
            runCatching {
                container.articleRepository.clearReaderCache()
                prefs.edit().putInt(KEY_READER_VERSION, READER_EXTRACTION_VERSION).apply()
            }
        }
    }

    private companion object {
        const val KEY_READER_VERSION = "reader_extraction_version"
        const val READER_EXTRACTION_VERSION = 1
    }
}
