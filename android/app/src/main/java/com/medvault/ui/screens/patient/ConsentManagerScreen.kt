package com.medvault.ui.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medvault.viewmodel.PatientViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentManagerScreen(
    onBack: () -> Unit,
    viewModel: PatientViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var dataStorage by remember { mutableStateOf(false) }
    var labResults by remember { mutableStateOf(false) }
    var bloodDonation by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.profile) {
        val c = (uiState.profile?.get("consents") as? Map<*, *>) ?: return@LaunchedEffect
        dataStorage = (c["dataStorage"] as? Boolean) ?: false
        labResults = (c["labResults"] as? Boolean) ?: false
        bloodDonation = (c["bloodDonation"] as? Boolean) ?: false
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consent Manager") },
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
            Text(
                text = "Manage how MedVault uses your data under the DPDP Act, 2023.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            ConsentRow(
                title = "Store my health records",
                description = "Allow MedVault to store your profile and medical records securely.",
                checked = dataStorage,
                onCheckedChange = { dataStorage = it }
            )
            ConsentRow(
                title = "Share lab results with my doctor",
                description = "Let your linked doctor view lab and diagnostic results.",
                checked = labResults,
                onCheckedChange = { labResults = it }
            )
            ConsentRow(
                title = "Blood donation tracking",
                description = "Track your blood donation history and eligibility.",
                checked = bloodDonation,
                onCheckedChange = { bloodDonation = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.updateConsents(
                        dataStorage = dataStorage,
                        labResults = labResults,
                        bloodDonation = bloodDonation
                    )
                    scope.launch { snackbarHostState.showSnackbar("Consents saved") }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Consents")
            }
        }
    }
}

@Composable
private fun ConsentRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
