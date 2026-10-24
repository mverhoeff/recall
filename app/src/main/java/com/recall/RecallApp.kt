package com.recall

import android.app.Application
import com.recall.di.AppContainer

class RecallApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
