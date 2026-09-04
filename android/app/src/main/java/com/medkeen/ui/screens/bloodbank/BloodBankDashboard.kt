package com.medkeen.ui.screens.bloodbank

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medkeen.ui.theme.*
import com.medkeen.viewmodel.BloodBankViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodBankDashboard(
    onNavigateToInventory: () -> Unit,
    onNavigateToRequests: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: BloodBankViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedNavIndex by remember { mutableIntStateOf(1) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

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
        bottomBar = {
            NavigationBar(
                containerColor = White,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = "Inventory") },
                    label = { Text("Inventory") },
                    selected = selectedNavIndex == 0,
                    onClick = {
                        selectedNavIndex = 0
                        onNavigateToInventory()
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmergencyRed,
                        selectedTextColor = EmergencyRed,
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Recent Requests") },
                    label = { Text("Recent Requests") },
                    selected = selectedNavIndex == 1,
                    onClick = { selectedNavIndex = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmergencyRed,
                        selectedTextColor = EmergencyRed,
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = selectedNavIndex == 2,
                    onClick = {
                        selectedNavIndex = 2
                        onNavigateToProfile()
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmergencyRed,
                        selectedTextColor = EmergencyRed,
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .padding(paddingValues)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = EmergencyRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = (uiState.profile?.get("name") as? String ?: "Blood Bank"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(SuccessGreen, shape = androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Default.SignalCellularAlt,
                    contentDescription = "Online",
                    tint = DarkText,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Segmented Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selectedTab == 0) EmergencyRed else White,
                        contentColor = if (selectedTab == 0) White else EmergencyRed
                    ),
                    border = BorderStroke(1.dp, EmergencyRed)
                ) {
                    Text(
                        text = "Blood Requests",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                OutlinedButton(
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selectedTab == 1) EmergencyRed else White,
                        contentColor = if (selectedTab == 1) White else EmergencyRed
                    ),
                    border = BorderStroke(1.dp, EmergencyRed)
                ) {
                    Text(
                        text = "Blood Donations",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                if (selectedTab == 0) {
                    uiState.bloodRequests.forEach { request ->
                        BloodRequestCard(
                            bloodGroup = request["bloodGroup"] as? String ?: "",
                            severity = request["urgency"] as? String ?: "",
                            units = (request["units"] as? Number)?.toInt() ?: 1,
                            timeAgo = request["timeAgo"] as? String ?: "",
                            patientName = request["patientName"] as? String ?: "Unknown",
                            hospital = request["hospitalName"] as? String ?: "Unknown Hospital",
                            distance = request["distance"] as? String ?: "",
                            isCritical = (request["urgency"] as? String)?.uppercase() == "CRITICAL",
                            onFulfill = {
                                val requestId = request["requestId"] as? String ?: ""
                                val units = (request["units"] as? Number)?.toInt() ?: 1
                                viewModel.fulfillBloodRequest(requestId, units)
                            },
                            onDecline = {
                                val requestId = request["requestId"] as? String ?: ""
                                viewModel.declineBloodRequest(requestId)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "End of recent requests.",
                        fontSize = 13.sp,
                        color = MutedText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    uiState.donorBookings.forEach { booking ->
                        val bookingId = booking["bookingId"] as? String ?: ""
                        DonorBookingCard(
                            bookingId = bookingId,
                            donorName = booking["donorName"] as? String ?: "Unknown Donor",
                            bloodGroup = booking["bloodGroup"] as? String ?: "",
                            lastDonated = "Last Donated: ${booking["lastDonated"] as? String ?: "N/A"}",
                            slotTime = booking["slotTime"] as? String ?: booking["scheduledTime"] as? String ?: "",
                            status = booking["status"] as? String ?: "",
                            onCheckIn = { viewModel.checkInDonorBooking(bookingId) },
                            onCancel = { viewModel.cancelDonorBooking(bookingId) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun BloodRequestCard(
    bloodGroup: String,
    severity: String,
    units: Int,
    timeAgo: String,
    patientName: String,
    hospital: String,
    distance: String,
    isCritical: Boolean,
    onFulfill: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (isCritical) BorderStroke(1.5.dp, EmergencyRed.copy(alpha = 0.4f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row: blood group, severity, time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Blood group badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = EmergencyRed
                ) {
                    Text(
                        text = "$bloodGroup ${if (bloodGroup.contains("-")) "NEGATIVE" else "POSITIVE"}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Severity badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isCritical) EmergencyRed.copy(alpha = 0.1f) else WarningOrange.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "$severity • $units Unit${if (units > 1) "s" else ""}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCritical) EmergencyRed else WarningOrange
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MutedText,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = timeAgo,
                    fontSize = 12.sp,
                    color = MutedText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Patient info
            Text(
                text = "Patient: $patientName",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MutedText,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$hospital ($distance)",
                    fontSize = 13.sp,
                    color = MutedText
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onFulfill,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Fulfill Request",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BorderColor)
                    )
                ) {
                    Text(
                        text = "Decline / Out of Stock",
                        fontSize = 13.sp,
                        color = DarkText
                    )
                }
            }
        }
    }
}

@Composable
private fun DonorBookingCard(
    bookingId: String,
    donorName: String,
    bloodGroup: String,
    lastDonated: String,
    slotTime: String,
    status: String,
    onCheckIn: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row: name + blood group
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = donorName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Text(
                    text = bloodGroup,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmergencyRed
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = lastDonated,
                fontSize = 13.sp,
                color = MutedText
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Slot info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = PrimaryBlue.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = slotTime,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = PrimaryBlue
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SuccessGreen.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = SuccessGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCheckIn,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Check-In Donor",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(EmergencyRed)
                    )
                ) {
                    Text(
                        text = "Cancel Slot",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}