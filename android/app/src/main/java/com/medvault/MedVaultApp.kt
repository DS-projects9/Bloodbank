package com.medvault

import android.app.Application
import com.medvault.data.remote.TokenManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MedVaultApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenManager.init(this)
    }
}
