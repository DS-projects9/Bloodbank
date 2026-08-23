package com.medvault.ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medvault.ui.theme.*
import com.medvault.viewmodel.PatientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthVaultScreen(
    appointmentId: String,
    doctorUid: String,
    doctorName: String?,
    onBack: () -> Unit,
    viewModel: PatientViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(appointmentId) {
        viewModel.loadMyDocuments()
        viewModel.loadShares(appointmentId)
        if (doctorUid.isNotBlank()) {
            viewModel.loadDoctorProfile(doctorUid)
        }
    }

    val displayDoctorName = uiState.doctorProfiles[doctorUid]?.name
        ?: doctorName
        ?: "the doctor"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Vault") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPurple)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            ShareFilesSection(
                doctorName = displayDoctorName,
                myDocuments = uiState.myDocuments,
                sharedFiles = uiState.sharedFiles,
                shareStatus = uiState.shareStatus,
                uploadError = uiState.uploadError,
                onUpload = { viewModel.uploadDocument(it.first, it.second, it.third) },
                onShare = { files, duration ->
                    viewModel.shareFilesWithDoctor(appointmentId, files, duration)
                },
                onRevoke = { viewModel.revokeShare(appointmentId, it) },
                onExtend = { docId, minutes ->
                    viewModel.extendShare(appointmentId, docId, minutes)
                },
                onDone = onBack
            )
        }
    }
}
