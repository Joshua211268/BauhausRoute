package com.example.bauhausroute

import android.app.Application
import org.osmdroid.config.Configuration

class BauhausRouteApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // OSM rejects generic/default clients. Set a stable application identity
        // before osmdroid creates any downloader or cache component.
        Configuration.getInstance().apply {
            load(this@BauhausRouteApplication, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = "BauhausRoute/1.0 (Android; com.example.bauhausroute)"
        }
    }
}
