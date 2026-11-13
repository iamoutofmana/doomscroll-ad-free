package io.github.iamoutofmana.doomscroll

import android.app.Application
import io.github.iamoutofmana.doomscroll.data.container.AppContainer
import io.github.iamoutofmana.doomscroll.data.sync.SyncScheduler

class DoomscrollApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SyncScheduler.ensurePeriodic(this)
    }
}
