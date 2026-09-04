package com.medkeen.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.medkeen.biometric.BiometricAuthManager
import com.medkeen.data.SettingsDataStore
import kotlinx.coroutines.flow.collect

@Composable
fun AppLockGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // Load the real preference first (null = not yet loaded).
    var loadedEnabled by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        SettingsDataStore.biometricLockEnabled(context).collect { loadedEnabled = it }
    }

    val enabled = loadedEnabled ?: false
    var unlocked by remember { mutableStateOf(false) }

    // Only act once we know the real value: lock when enabled, unlock when disabled.
    LaunchedEffect(loadedEnabled) {
        when (loadedEnabled) {
            true -> unlocked = false
            false -> unlocked = true
            null -> { /* still loading */ }
        }
    }

    when {
        loadedEnabled == null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        unlocked || !enabled -> {
            content()
        }
        else -> {
            val canAuth = activity?.let { BiometricAuthManager.canAuthenticate(it) } ?: false
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unlock to open MedKeen",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (!canAuth) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No biometric credentials enrolled.\nPlease enroll a fingerprint or face, or use a device lock (PIN/pattern).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            if (canAuth) {
                LaunchedEffect(Unit) {
                    activity?.let { act ->
                        BiometricAuthManager.authenticate(
                            activity = act,
                            title = "Unlock MedKeen",
                            subtitle = "Use your biometrics or device credentials to continue",
                            onSuccess = { unlocked = true },
                            onError = { }
                        )
                    }
                }
            }
        }
    }
}
