package eu.tintera.tasks

import android.app.Application
import org.koin.android.ext.koin.androidContext

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        koinApp {
            androidContext(this@MainApplication)
        }
    }
}