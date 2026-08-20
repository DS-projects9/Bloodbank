package com.medvault.ui.screens.bloodbank

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medvault.ui.theme.*
import com.medvault.viewmodel.AuthViewModel
import com.medvault.viewmodel.BloodBankViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodBankProfileScreen(
    onBack: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToRequests: () -> Unit,
    onLogout: () -> Unit,
    viewModel: BloodBankViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isEmergencyDispatch by remember { mutableStateOf(true) }
    var selectedNavIndex by remember { mutableIntStateOf(2) }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(White),
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
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Requests") },
                    label = { Text("Requests") },
                    selected = selectedNavIndex == 1,
                    onClick = {
                        selectedNavIndex = 1
                        onNavigateToRequests()
                    },
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
                    onClick = { selectedNavIndex = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmergencyRed,
                        selectedTextColor = EmergencyRed,
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
                    text = "Facility Profile & Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = EmergencyRed,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Facility Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = EmergencyRed
                        ) {
                            Icon(
                                Icons.Default.LocalHospital,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = uiState.profile?.get("name") as? String ?: "",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                            Text(
                                text = "Blood Bank ID: ${uiState.profile?.get("uid") as? String ?: ""}",
                                fontSize = 12.sp,
                                color = MutedText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Emergency Dispatch Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Emergency Dispatch Status",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkText
                            )
                            Text(
                                text = "Broadcasting stock to nearby patients",
                                fontSize = 12.sp,
                                color = MutedText
                            )
                        }
                        Switch(
                            checked = isEmergencyDispatch,
                            onCheckedChange = { isEmergencyDispatch = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = White,
                                checkedTrackColor = SuccessGreen
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Location badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryBlue.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Geofirestore Active • Guntur, AP (16.3067° N, 80.4365° E)",
                                fontSize = 12.sp,
                                color = PrimaryBlue
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Regulatory & Compliance
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Regulatory & Compliance Credentials",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "License Number", fontSize = 12.sp, color = MutedText)
                            Text(text = uiState.profile?.get("bloodBankLicense") as? String ?: "", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkText)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Accreditation", fontSize = 12.sp, color = MutedText)
                            Text(text = uiState.profile?.get("accreditation") as? String ?: "", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Facility Category", fontSize = 12.sp, color = MutedText)
                            Text(text = "Major Component Unit", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkText)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "License Expiry", fontSize = 12.sp, color = MutedText)
                            Text(text = "31 Dec 2028", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkText)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Official Contact Information
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Official Contact Information",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ContactRow(
                        icon = Icons.Default.Business,
                        text = uiState.profile?.get("bankAddress") as? String ?: ""
                    )
                    ContactRow(
                        icon = Icons.Default.Phone,
                        text = "24/7 Helpline: ${uiState.profile?.get("phone") as? String ?: ""}"
                    )
                    ContactRow(
                        icon = Icons.Default.Email,
                        text = uiState.profile?.get("email") as? String ?: "",
                        textColor = PrimaryBlue
                    )
                    ContactRow(
                        icon = Icons.Default.Person,
                        text = "Nodal Officer: ${uiState.profile?.get("nodalOfficer") as? String ?: ""}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Rows
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Shield,
                        title = "Immutable Stock Audit Log"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        icon = Icons.Default.Groups,
                        title = "Staff Access & Authorization Roles"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        icon = Icons.Default.Lock,
                        title = "Firebase Auth & Security Credentials"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sign Out button
            OutlinedButton(
                onClick = {
                    authViewModel.signOut()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(EmergencyRed)
                )
            ) {
                Text(
                    text = "Sign Out of Blood Center Portal",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    textColor: Color = DarkText
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MutedText,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = textColor
        )
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = DarkText,
            modifier = Modifier.size(22.dp)
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
