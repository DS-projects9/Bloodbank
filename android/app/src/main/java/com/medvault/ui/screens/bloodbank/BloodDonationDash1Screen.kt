package com.medvault.ui.screens.bloodbank

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medvault.ui.theme.*
import com.medvault.viewmodel.BloodBankViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodDonationDash1Screen(
    onBack: () -> Unit,
    viewModel: BloodBankViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blood Donation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPurple)
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            val totalDonors = uiState.donorBookings.size
            val activeRequests = uiState.bloodRequests.size
            val unitsCollected = uiState.donorBookings.sumOf { (it["units"] as? Number)?.toInt() ?: 0 }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(White)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Donation Stats
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Donation Statistics", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkText)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatItem("Total Donors", totalDonors.toString())
                            StatItem("Active Today", activeRequests.toString())
                            StatItem("Units Collected", unitsCollected.toString())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Donor Requests
                Text("Donor Requests", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkText)
                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.bloodRequests.isEmpty()) {
                    Text("No donor requests available", fontSize = 14.sp, color = SecondaryText)
                } else {
                    uiState.bloodRequests.forEach { request ->
                        val name = request["patientName"] as? String ?: "Unknown"
                        val bloodGroup = request["bloodGroup"] as? String ?: "N/A"
                        val distance = (request["distance"] as? Number)?.let { "${it.toDouble()} km" } ?: "N/A"
                        val urgency = request["urgency"] as? String ?: "Low"
                        DonorRequestCard(name = name, bloodGroup = bloodGroup, distance = distance, urgency = urgency)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
        Text(label, fontSize = 12.sp, color = SecondaryText)
    }
}

@Composable
private fun DonorRequestCard(name: String, bloodGroup: String, distance: String, urgency: String) {
    val urgencyColor = when (urgency) {
        "High" -> EmergencyRed
        "Medium" -> WarningOrange
        else -> SuccessGreen
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkText)
                Text("$bloodGroup • $distance", fontSize = 12.sp, color = SecondaryText)
            }
            AssistChip(
                onClick = { },
                label = { Text(urgency, fontSize = 10.sp) },
                colors = AssistChipDefaults.assistChipColors(containerColor = urgencyColor.copy(alpha = 0.1f), labelColor = urgencyColor),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}
