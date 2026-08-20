package com.medvault.ui.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.medvault.biometric.BiometricAuthManager
import com.medvault.data.SettingsDataStore
import com.medvault.viewmodel.PatientViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricLockScreen(
    onBack: () -> Unit,
    viewModel: PatientViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val enabled by SettingsDataStore.biometricLockEnabled(context).collectAsState(initial = false)

    var checked by remember { mutableStateOf(enabled) }
    var isBusy by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(enabled) { checked = enabled }

    fun toggle(newValue: Boolean) {
        if (isBusy) return
        val act = activity ?: return
        if (!BiometricAuthManager.canAuthenticate(act)) {
            scope.launch {
                snackbarHostState.showSnackbar("No biometric credentials enrolled on this device")
            }
            return
        }
        isBusy = true
        BiometricAuthManager.authenticate(
            activity = act,
            title = if (newValue) "Enable Biometric Lock" else "Confirm Identity",
            subtitle = if (newValue) "Authenticate to lock MedVault" else "Authenticate to disable lock",
            onSuccess = {
                isBusy = false
                scope.launch {
                    SettingsDataStore.setBiometricLockEnabled(context, newValue)
                }
                checked = newValue
            },
            onError = {
                isBusy = false
                checked = enabled
                scope.launch { snackbarHostState.showSnackbar(it) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biometric Lock") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Require biometrics to open",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lock the app with your fingerprint or face when it starts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = checked,
                        enabled = !isBusy,
                        onCheckedChange = { toggle(it) }
                    )
                }
            }
        }
    }
}
