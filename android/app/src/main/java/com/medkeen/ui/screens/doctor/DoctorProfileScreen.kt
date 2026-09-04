package com.medkeen.ui.screens.doctor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medkeen.ui.theme.*
import com.medkeen.viewmodel.AuthViewModel
import com.medkeen.viewmodel.DoctorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onShiftSchedule: () -> Unit = {},
    onNavigateToQueue: () -> Unit = {},
    viewModel: DoctorViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedNavIndex by remember { mutableIntStateOf(2) }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = White,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        selected = selectedNavIndex == 0,
                        onClick = { onBack() },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            unselectedIconColor = MutedText,
                            unselectedTextColor = MutedText
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Appointments") },
                        label = { Text("Appointments") },
                        selected = selectedNavIndex == 1,
                        onClick = { onNavigateToQueue() },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            unselectedIconColor = MutedText,
                            unselectedTextColor = MutedText
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        selected = selectedNavIndex == 2,
                        onClick = { selectedNavIndex = 2 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            unselectedIconColor = MutedText,
                            unselectedTextColor = MutedText
                        )
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(White)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkText)
                    }
                    Text(
                        text = "Doctor Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(LightBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = uiState.profile?.get("name") as? String ?: "",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkText
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (uiState.profile?.get("verified") == true || uiState.profile?.get("isVerified") == true) {
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = "Verified",
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = uiState.profile?.get("specialization") as? String ?: "",
                                    fontSize = 12.sp,
                                    color = MutedText,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "License: ${uiState.profile?.get("licenseNumber") as? String ?: ""}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryBlue
                                )
                            }
                        }
                    }

                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val profile = uiState.profile
                        val yearsExp = (profile?.get("yearsExperience") as? Number)?.toInt()
                        val rating = (profile?.get("rating") as? Number)
                        val consults = (profile?.get("consultCount") as? Number)?.toInt()
                        val dept = profile?.get("department") as? String
                        val room = profile?.get("roomNumber") as? String

                        StatBox(
                            value = if (yearsExp != null && yearsExp > 0) "$yearsExp+ Yrs" else "—",
                            label = "Experience",
                            modifier = Modifier.weight(1f)
                        )
                        StatBox(
                            value = if (rating != null) "%.1f".format(rating.toDouble()) else "—",
                            label = "Rating",
                            valueColor = if (rating != null) WarningOrange else MutedText,
                            modifier = Modifier.weight(1f)
                        )
                        StatBox(
                            value = if (consults != null && consults > 0) "$consults+" else "—",
                            label = "Consults",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Current Practice Location
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Current Practice Location",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.profile?.get("hospitalName") as? String ?: "",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "Specialization", fontSize = 12.sp, color = MutedText)
                                    Text(text = uiState.profile?.get("specialization") as? String ?: "—", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkText)
                                }
                                Column {
                                    Text(text = "Room / Cabin", fontSize = 12.sp, color = MutedText)
                                    Text(text = uiState.profile?.get("roomNumber") as? String ?: "—", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkText)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "Phone", fontSize = 12.sp, color = MutedText)
                                    Text(
                                        text = uiState.profile?.get("phone") as? String ?: "",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = DarkText
                                    )
                                }
                                Column {
                                    Text(text = "Email", fontSize = 12.sp, color = MutedText)
                                    Text(
                                        text = uiState.profile?.get("email") as? String ?: "",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = DarkText
                                    )
                                }
                            }
                        }
                    }

                    // Settings Items
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column {
                            SettingsRow(
                                icon = Icons.Default.Schedule,
                                title = "Shift Schedule & Default Timings",
                                onClick = onShiftSchedule
                            )
                        }
                    }

                    // Logout Button
                    OutlinedButton(
                        onClick = {
                            authViewModel.signOut()
                            onLogout()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = EmergencyRed
                        ),
                        border = BorderStroke(1.dp, EmergencyRed)
                    ) {
                        Text(
                            text = "Log Out of Doctor Portal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    value: String,
    label: String,
    valueColor: Color = DarkText,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MutedText
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = DarkText,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            color = DarkText,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = BorderColor,
            modifier = Modifier.size(20.dp)
        )
    }
}
