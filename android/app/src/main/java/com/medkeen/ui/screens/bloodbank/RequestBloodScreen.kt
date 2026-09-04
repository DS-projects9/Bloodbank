package com.medkeen.ui.screens.bloodbank

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medkeen.ui.theme.*
import com.medkeen.viewmodel.BloodBankViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestBloodScreen(
    onBack: () -> Unit,
    viewModel: BloodBankViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedBloodGroup by remember { mutableStateOf<String?>(null) }
    var units by remember { mutableIntStateOf(1) }
    var selectedUrgency by remember { mutableStateOf("Normal") }

    val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    val urgencyOptions = listOf("Normal", "Urgent", "Critical")

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = EmergencyRed)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blood Requests") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundPurple
                )
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
            Spacer(modifier = Modifier.height(16.dp))

            // New Request Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "New Blood Request",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Blood Group Selection
                    Text(
                        text = "Blood Group",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bloodGroups.take(4).forEach { group ->
                            FilterChip(
                                selected = selectedBloodGroup == group,
                                onClick = { selectedBloodGroup = group },
                                label = { Text(group) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmergencyRed,
                                    selectedLabelColor = White
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bloodGroups.takeLast(4).forEach { group ->
                            FilterChip(
                                selected = selectedBloodGroup == group,
                                onClick = { selectedBloodGroup = group },
                                label = { Text(group) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmergencyRed,
                                    selectedLabelColor = White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Units
                    Text(
                        text = "Units Required",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (units > 1) units-- },
                            modifier = Modifier
                                .size(40.dp)
                                .background(BackgroundPurple, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = units.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(
                            onClick = { if (units < 10) units++ },
                            modifier = Modifier
                                .size(40.dp)
                                .background(PrimaryBlue, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Urgency
                    Text(
                        text = "Urgency Level",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        urgencyOptions.forEach { level ->
                            val isSelected = selectedUrgency == level
                            val chipColor = when (level) {
                                "Critical" -> EmergencyRed
                                "Urgent" -> WarningOrange
                                else -> SuccessGreen
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedUrgency = level },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) chipColor else White,
                                border = BorderStroke(1.dp, if (isSelected) chipColor else BorderColor)
                            ) {
                                Text(
                                    text = level,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) White else DarkText,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            viewModel.createBloodRequest(
                                patientName = (uiState.profile?.get("name") as? String) ?: (uiState.profile?.get("full_name") as? String) ?: "Unknown",
                                bloodGroup = selectedBloodGroup ?: "",
                                units = units,
                                hospitalName = (uiState.profile?.get("hospitalName") as? String) ?: (uiState.profile?.get("hospital_name") as? String) ?: "",
                                hospitalAddress = (uiState.profile?.get("bankAddress") as? String) ?: (uiState.profile?.get("hospital_address") as? String) ?: "",
                                urgency = selectedUrgency
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmergencyRed,
                            contentColor = White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = selectedBloodGroup != null && !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Request",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Requests
            Text(
                text = "Recent Requests",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Spacer(modifier = Modifier.height(12.dp))

            uiState.myBloodRequests.forEach { request ->
                RequestItem(
                    patientName = request["patient_name"] as? String ?: "Unknown",
                    bloodGroup = request["blood_group"] as? String ?: "N/A",
                    units = (request["units"] as? Number)?.toInt() ?: 0,
                    status = request["status"] as? String ?: "Unknown",
                    time = request["created_at"] as? String ?: ""
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RequestItem(
    patientName: String,
    bloodGroup: String,
    units: Int,
    status: String,
    time: String
) {
    val statusColor = when (status) {
        "Pending" -> WarningOrange
        "Fulfilled" -> SuccessGreen
        "Urgent" -> EmergencyRed
        else -> SecondaryText
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patientName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Text(
                    text = "$bloodGroup • $units units • $time",
                    fontSize = 12.sp,
                    color = SecondaryText
                )
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
    }
}
