package com.medkeen.ui.screens.bloodbank

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
import com.medkeen.ui.theme.*
import com.medkeen.viewmodel.BloodBankViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodDonationDash2Screen(
    onBack: () -> Unit,
    viewModel: BloodBankViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUpcomingDonations()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Donation History") },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(White)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Eligibility Checklist
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Donation Eligibility", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkText)
                        Spacer(modifier = Modifier.height(12.dp))
                        val eligibilityItems = listOf(
                            "Age 18-65 years" to true,
                            "Weight above 50 kg" to true,
                            "No recent illness" to true,
                            "Hemoglobin above 12.5 g/dL" to true
                        )
                        eligibilityItems.forEach { (text, _) ->
                            EligibilityItem(text)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Donation History
                Text("Recent Donations", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkText)
                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.upcomingDonations.isEmpty()) {
                    Text("No donation history available", fontSize = 14.sp, color = SecondaryText)
                } else {
                    uiState.upcomingDonations.forEach { donation ->
                        val name = donation["donorName"] as? String ?: donation["name"] as? String ?: "Unknown"
                        val date = donation["date"] as? String ?: "N/A"
                        val bloodGroup = donation["bloodGroup"] as? String ?: "N/A"
                        val units = (donation["units"] as? Number)?.toInt() ?: 1
                        DonationHistoryItem(name = name, date = date, bloodGroup = bloodGroup, units = units)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun EligibilityItem(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = DarkText)
    }
}

@Composable
private fun DonationHistoryItem(name: String, date: String, bloodGroup: String, units: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(SuccessGreen, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Bloodtype, contentDescription = null, tint = White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkText)
                Text("$date • $bloodGroup • $units unit", fontSize = 12.sp, color = SecondaryText)
            }
        }
    }
}
