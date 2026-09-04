package com.medkeen.ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medkeen.ui.theme.*
import com.medkeen.viewmodel.PatientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientAppointmentsScreen(
    onBack: () -> Unit,
    onNavigateToHealthVault: (appointmentId: String, doctorUid: String, doctorName: String?) -> Unit,
    viewModel: PatientViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Upcoming", "Past", "Cancelled")

    val currentList = when (selectedTab) {
        0 -> uiState.appointments.filter {
            val status = (it["status"] as? String) ?: ""
            status in listOf("locked", "confirmed")
        }
        1 -> uiState.appointments.filter {
            (it["status"] as? String) == "completed"
        }
        else -> uiState.appointments.filter {
            (it["status"] as? String) == "cancelled"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appointments") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = White,
                contentColor = PrimaryBlue
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = PrimaryBlue,
                        unselectedContentColor = SecondaryText
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = BorderColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No ${tabs[selectedTab].lowercase()} appointments",
                            fontSize = 14.sp,
                            color = SecondaryText
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentList) { appointment ->
                        val appointmentId = (appointment["appointmentId"] as? String) ?: ""
                        val doctorUid = (appointment["doctorUid"] as? String) ?: ""
                        val doctor = uiState.doctorProfiles[doctorUid]
                        val date = (appointment["date"] as? String) ?: ""
                        val time = (appointment["time"] as? String) ?: ""
                        val status = (appointment["status"] as? String) ?: ""
                        val patientNote = (appointment["patientNote"] as? String) ?: ""

                        val displayStatus = when (status) {
                            "locked" -> "Pending"
                            "confirmed" -> "Confirmed"
                            "completed" -> "Completed"
                            "cancelled" -> "Cancelled"
                            else -> status.replaceFirstChar { it.uppercase() }
                        }

                        val canAddDocument = status in listOf("locked", "confirmed")

                        AppointmentCard(
                            doctorName = doctor?.name ?: (if (doctorUid.isNotBlank()) "Doctor" else "Unknown Doctor"),
                            specialization = doctor?.specialization,
                            hospitalName = doctor?.hospitalName,
                            patientNote = patientNote,
                            date = date,
                            time = time,
                            status = displayStatus,
                            onAddDocument = if (canAddDocument && appointmentId.isNotBlank()) {
                                { onNavigateToHealthVault(appointmentId, doctorUid, doctor?.name) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(
    doctorName: String,
    specialization: String?,
    hospitalName: String?,
    patientNote: String?,
    date: String,
    time: String,
    status: String,
    onAddDocument: (() -> Unit)? = null
) {
    val statusColor = when (status) {
        "Confirmed" -> SuccessGreen
        "Pending" -> WarningOrange
        "Completed" -> PrimaryBlue
        "Cancelled" -> EmergencyRed
        else -> SecondaryText
    }

    val initials = doctorName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifEmpty { "Dr" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(PrimaryBlue, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doctorName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    specialization?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            color = SecondaryText
                        )
                    }
                    hospitalName?.takeIf { it.isNotBlank() }?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SecondaryText,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(it, fontSize = 12.sp, color = SecondaryText)
                        }
                    }
                }
                AssistChip(
                    onClick = { },
                    label = { Text(status, fontSize = 10.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusColor.copy(alpha = 0.1f),
                        labelColor = statusColor
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = SecondaryText, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(date, fontSize = 12.sp, color = SecondaryText)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = SecondaryText, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(time, fontSize = 12.sp, color = SecondaryText)
                }
            }
            patientNote?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Note: $note",
                    fontSize = 12.sp,
                    color = MutedText
                )
            }
            onAddDocument?.let {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = it,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                    border = BorderStroke(1.dp, PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add document", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
