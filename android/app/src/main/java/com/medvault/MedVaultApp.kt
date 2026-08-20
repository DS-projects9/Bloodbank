package com.medvault

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MedVaultApp : Application() {

    override fun onCreate() {
        // Connect to local Firebase emulators in debug builds. For a physical
        // device we use 127.0.0.1 together with `adb reverse` port forwarding
        // (adb reverse tcp:PORT tcp:PORT for 9000, 9090, 9099). For the emulator
        // use 10.0.2.2 instead.
        if (BuildConfig.DEBUG) {
            FirebaseAuth.getInstance().useEmulator("127.0.0.1", 9099)
            FirebaseFirestore.getInstance().useEmulator("127.0.0.1", 9090)
        }
        super.onCreate()
    }
}
