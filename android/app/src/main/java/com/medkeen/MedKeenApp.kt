package com.medkeen

import android.app.Application
import com.medkeen.data.remote.TokenManager
import com.medkeen.util.FileKeyStore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MedKeenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenManager.init(this)
        FileKeyStore.init(this)
    }
}
