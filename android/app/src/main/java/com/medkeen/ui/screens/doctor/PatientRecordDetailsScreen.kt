package com.medkeen.ui.screens.doctor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.medkeen.ui.theme.*
import com.medkeen.viewmodel.DoctorViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientRecordDetailsScreen(
    appointmentId: String,
    onBack: () -> Unit,
    onViewFullProfile: (String) -> Unit,
    viewModel: DoctorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(appointmentId) {
        viewModel.loadAppointmentDetails(appointmentId)
    }

    val appointment = uiState.selectedAppointment

    val patientName = (appointment?.get("patientName") as? String) ?: "Unknown Patient"
    val patientId = appointment?.get("patientUid") as? String
    val bloodGroup = (appointment?.get("bloodGroup") as? String)?.ifEmpty { null } ?: "N/A"
    val age = (appointment?.get("age") as? Number)?.toInt() ?: 0
    val gender = (appointment?.get("gender") as? String) ?: ""
    val phone = (appointment?.get("phone") as? String)?.ifEmpty { null } ?: "N/A"
    val dob = appointment?.get("dob") as? String
    val apptDate = appointment?.get("date") as? String ?: ""
    val apptTime = appointment?.get("time") as? String ?: ""
    val apptStatus = appointment?.get("status") as? String ?: ""

    val ageDisplay = if (age > 0) {
        if (gender.isNotEmpty()) "$age Yrs / $gender" else "$age Yrs"
    } else if (!dob.isNullOrEmpty()) {
        if (gender.isNotEmpty()) "$dob / $gender" else dob
    } else {
        if (gender.isNotEmpty()) "N/A / $gender" else "N/A"
    }

    val documents = uiState.patientDocuments
    val hasDocuments = documents.isNotEmpty()

    val timerViewedAt = uiState.timerViewedAt
    val timerExpiresAt = uiState.timerExpiresAt
    val timerActive = timerViewedAt > 0L && timerExpiresAt > System.currentTimeMillis()

    var countdownSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(timerViewedAt, timerExpiresAt) {
        if (timerViewedAt > 0L && timerExpiresAt > System.currentTimeMillis()) {
            while (true) {
                val remaining = (timerExpiresAt - System.currentTimeMillis()) / 1000
                if (remaining <= 0) {
                    countdownSeconds = 0
                    break
                }
                countdownSeconds = remaining
                delay(1000)
            }
        } else {
            countdownSeconds = 0
        }
    }

    val displayMinutes = (countdownSeconds / 60).toInt()
    val displaySeconds = (countdownSeconds % 60).toInt()

    var timerTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(hasDocuments, documents) {
        if (hasDocuments && !timerTriggered) {
            timerTriggered = true
            val firstDoc = documents.firstOrNull()
            if (firstDoc != null) {
                viewModel.openDocument(firstDoc.documentId)
            }
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Patient Record Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkText)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkText)
                    }
                },
                actions = {
                    if (apptStatus == "in_session" || apptStatus == "active" || apptStatus == "in-progress") {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE8F5E9)) {
                            Text(
                                text = "In Session",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen
                            )
                        }
                    } else if (apptStatus.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(4.dp), color = MutedText.copy(alpha = 0.1f)) {
                            Text(
                                text = apptStatus.replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MutedText
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = patientName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkText)
                        if (bloodGroup != "N/A") {
                            Surface(shape = RoundedCornerShape(12.dp), color = EmergencyRed) {
                                Text(
                                    text = bloodGroup,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = "Age/Sex", fontSize = 12.sp, color = MutedText)
                            Text(text = ageDisplay, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkText)
                        }
                        Column {
                            Text(text = "Date & Time", fontSize = 12.sp, color = MutedText)
                            Text(
                                text = if (apptDate.isNotEmpty() && apptTime.isNotEmpty()) "$apptDate $apptTime" else "N/A",
                                fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkText
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = "Phone", fontSize = 12.sp, color = MutedText)
                            Text(text = phone, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkText)
                        }
                        Column {
                            Text(text = "Status", fontSize = 12.sp, color = MutedText)
                            Text(
                                text = apptStatus.replaceFirstChar { it.uppercase() },
                                fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (hasDocuments) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    border = BorderStroke(2.dp, if (timerActive) EmergencyRed else MutedText)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (timerActive) EmergencyRed.copy(alpha = 0.1f) else MutedText.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (timerActive) EmergencyRed else MutedText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (timerActive) "Temporary Security Access" else "Access Pending",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (timerActive) EmergencyRed else MutedText
                                    )
                                    Text(
                                        text = if (timerActive) "Expires in: ${String.format("%02d:%02d", displayMinutes, displaySeconds)}" else "Timer starts when files are accessed",
                                        fontSize = 12.sp,
                                        color = if (timerActive) EmergencyRed else SecondaryText
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "RESTRICTED DOCUMENTS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        documents.forEachIndexed { index, doc ->
                            val fileName = doc.fileNames.firstOrNull() ?: "Document"
                            val isScan = fileName.lowercase().let {
                                it.contains("scan") || it.contains("ecg") || it.contains("xray") || it.contains("mri")
                            }
                            DocumentRow(
                                fileName = fileName,
                                fileType = if (isScan) "SCAN" else "LAB REPORT",
                                typeColor = if (isScan) WarningOrange else PrimaryBlue,
                                onView = {
                                    scope.launch {
                                        val url = viewModel.getDocumentUrl(doc.documentId)
                                        if (url != null) {
                                            val target =
                                                com.medkeen.util.DecryptedFileOpener.openUriForShared(
                                                    context = context,
                                                    url = url,
                                                    wrappedKeys = doc.wrappedKeys,
                                                    fileName = fileName,
                                                )
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, target).apply {
                                                    if (target.scheme == "content") {
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                            if (index < documents.lastIndex) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FolderOff,
                            contentDescription = null,
                            tint = MutedText,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No documents shared", fontSize = 14.sp, color = MutedText)
                        Text(
                            text = "Patient has not shared any records for this appointment",
                            fontSize = 12.sp, color = SecondaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            patientId?.let { pid ->
                OutlinedButton(
                    onClick = { onViewFullProfile(pid) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                    border = BorderStroke(1.dp, PrimaryBlue)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "View & Edit Full Profile / Records",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.completeAppointment(appointmentId) { ok, _ ->
                        if (ok) {
                            scope.launch {
                                delay(300)
                                onBack()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (apptStatus == "completed") MutedText else SuccessGreen,
                    contentColor = White
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = apptStatus != "completed" && apptStatus != "cancelled"
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (apptStatus == "completed") "Consultation Completed" else "Complete Consultation & Close Session",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DocumentRow(
    fileName: String,
    fileType: String,
    typeColor: Color,
    onView: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            tint = MutedText,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = fileName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkText)
            Surface(shape = RoundedCornerShape(4.dp), color = typeColor.copy(alpha = 0.1f)) {
                Text(
                    text = fileType,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, color = typeColor
                )
            }
        }
        Text(text = "View", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
    }
}
