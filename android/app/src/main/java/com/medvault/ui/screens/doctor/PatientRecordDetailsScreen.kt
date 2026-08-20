package com.medvault.ui.screens.doctor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medvault.ui.theme.*
import com.medvault.viewmodel.DoctorViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientRecordDetailsScreen(
    appointmentId: String,
    onBack: () -> Unit,
    onViewFullProfile: (String) -> Unit,
    viewModel: DoctorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(appointmentId) {
        viewModel.loadAppointmentDetails(appointmentId)
    }

    var countdown by remember { mutableIntStateOf(765) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            kotlinx.coroutines.delay(1000)
            countdown--
        }
    }

    val minutes = countdown / 60
    val seconds = countdown % 60

    val appointment = uiState.selectedAppointment
    val patientName = (appointment?.get("patientName") as? String) ?: "Unknown Patient"
    val patientId = appointment?.get("patientId") as? String

    @Suppress("UNCHECKED_CAST")
    val slot = appointment?.get("slot") as? Map<String, Any>
    val slotStart = (slot?.get("start") as? Number)?.toLong() ?: 0L
    val slotEnd = (slot?.get("end") as? Number)?.toLong() ?: 0L

    val slotStartStr = if (slotStart > 0) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(slotStart))
    } else ""
    val slotEndStr = if (slotEnd > 0) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(slotEnd))
    } else ""
    val slotDisplay = if (slotStartStr.isNotEmpty() && slotEndStr.isNotEmpty()) {
        "$slotStartStr - $slotEndStr"
    } else "N/A"

    @Suppress("UNCHECKED_CAST")
    val patientProfile = appointment?.get("patientProfile") as? Map<String, Any>
    val bloodGroup = (patientProfile?.get("bloodGroup") as? String)?.ifEmpty { null }
        ?: (appointment?.get("bloodGroup") as? String)?.ifEmpty { null }
        ?: "N/A"

    val dob = (patientProfile?.get("dob") as? String)
        ?: (appointment?.get("dob") as? String)
    val ageDisplay = if (!dob.isNullOrEmpty()) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birthDate = sdf.parse(dob)
            val cal = Calendar.getInstance()
            val today = cal.time
            cal.time = birthDate ?: today
            val years = Calendar.getInstance().get(Calendar.YEAR) - cal.get(Calendar.YEAR)
            val gender = (patientProfile?.get("gender") as? String)
                ?: (appointment?.get("gender") as? String)
                ?: ""
            if (gender.isNotEmpty()) "$years Yrs / $gender" else "$years Yrs"
        } catch (_: Exception) {
            val gender = (patientProfile?.get("gender") as? String)
                ?: (appointment?.get("gender") as? String)
                ?: ""
            if (gender.isNotEmpty()) "$dob / $gender" else dob ?: "N/A"
        }
    } else {
        val gender = (patientProfile?.get("gender") as? String)
            ?: (appointment?.get("gender") as? String)
            ?: ""
        if (gender.isNotEmpty()) "N/A / $gender" else "N/A"
    }

    val phone = (patientProfile?.get("phone") as? String)?.ifEmpty { null }
        ?: (appointment?.get("phone") as? String)?.ifEmpty { null }
        ?: "N/A"

    @Suppress("UNCHECKED_CAST")
    val emergencyContacts = (patientProfile?.get("emergencyContacts") as? List<Map<String, Any>>)
        ?: (appointment?.get("emergencyContacts") as? List<Map<String, Any>>)
        ?: emptyList()
    val primaryEmergency = emergencyContacts.firstOrNull()
    val emergencyName = primaryEmergency?.get("name") as? String ?: "N/A"
    val emergencyPhone = primaryEmergency?.get("phone") as? String ?: ""

    val documents = uiState.patientDocuments

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(White),
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
                    Text(
                        "Patient Record Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkText)
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = "In Session",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
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

            // Patient Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Name + Blood Group
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = patientName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                        if (bloodGroup != "N/A") {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmergencyRed
                            ) {
                                Text(
                                    text = bloodGroup,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Details grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Age/Sex", fontSize = 12.sp, color = MutedText)
                            Text(text = ageDisplay, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkText)
                        }
                        Column {
                            Text(text = "Slot", fontSize = 12.sp, color = MutedText)
                            Text(text = slotDisplay, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkText)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Phone", fontSize = 12.sp, color = MutedText)
                            Text(text = phone, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkText)
                        }
                        Column {
                            Text(text = "Emergency", fontSize = 12.sp, color = MutedText)
                            Text(text = emergencyName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkText)
                            if (emergencyPhone.isNotEmpty()) {
                                Text(text = "($emergencyPhone)", fontSize = 12.sp, color = SecondaryText)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Temporary Security Access Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                border = BorderStroke(2.dp, EmergencyRed)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmergencyRed.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = EmergencyRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Temporary Security Access",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmergencyRed
                                )
                                Text(
                                    text = "Expires in: ${String.format("%02d:%02d", minutes, seconds)} Mins",
                                    fontSize = 12.sp,
                                    color = EmergencyRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Restricted Documents
                    Text(
                        text = "RESTRICTED DOCUMENTS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (documents.isEmpty()) {
                        Text(
                            text = "No documents shared for this appointment",
                            fontSize = 13.sp,
                            color = MutedText
                        )
                    } else {
                        documents.forEachIndexed { index, doc ->
                            val fileName = doc.fileNames.firstOrNull() ?: "Document"
                            val isScan = fileName.lowercase().let {
                                it.contains("scan") || it.contains("ecg") || it.contains("xray") || it.contains("mri")
                            }
                            DocumentRow(
                                fileName = fileName,
                                fileType = if (isScan) "SCAN" else "LAB REPORT",
                                typeColor = if (isScan) WarningOrange else PrimaryBlue
                            )
                            if (index < documents.lastIndex) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // View & edit full patient profile + records
            patientId?.let { pid ->
                OutlinedButton(
                    onClick = { onViewFullProfile(pid) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                    border = BorderStroke(1.dp, PrimaryBlue)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "View & Edit Full Profile / Records",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Complete Consultation Button
            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SuccessGreen,
                    contentColor = White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Complete Consultation & Close Session",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
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
    typeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
            Text(
                text = fileName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DarkText
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = typeColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = fileType,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
            }
        }
        Text(
            text = "View",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue
        )
    }
}
