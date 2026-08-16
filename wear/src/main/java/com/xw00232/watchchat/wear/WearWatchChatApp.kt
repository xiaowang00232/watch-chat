package com.xw00232.watchchat.wear

import android.app.Application
import com.watchchat.app.AppContainer

class WearWatchChatApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
