package com.xw00232.watchchat.app

import android.app.Application
import com.watchchat.app.AppContainer

class WatchChatApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
