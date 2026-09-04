package com.medkeen.ui.screens.doctor

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medkeen.ui.theme.*
import com.medkeen.viewmodel.DoctorViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentQueueScreen(
    onBack: () -> Unit,
    onPatientSelected: (String) -> Unit,
    onNavigateToProfile: () -> Unit = {},
    viewModel: DoctorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val today = java.time.LocalDate.now().toString()
    val tomorrow = java.time.LocalDate.now().plusDays(1).toString()
    val tabs = listOf(
        "Today (${uiState.appointments.count { it["date"]?.toString() == today }})",
        "Tomorrow (${uiState.appointments.count { it["date"]?.toString() == tomorrow }})",
        "Upcoming (${uiState.appointments.count { (it["date"]?.toString() ?: "") > tomorrow }})"
    )

    val currentAppointments = when (selectedTab) {
        0 -> uiState.appointments.filter { it["date"]?.toString() == today }
        1 -> uiState.appointments.filter { it["date"]?.toString() == tomorrow }
        else -> uiState.appointments.filter { (it["date"]?.toString() ?: "") > tomorrow }
    }

    var selectedNavIndex by remember { mutableIntStateOf(1) }

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
                    onClick = { selectedNavIndex = 1 },
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
                    onClick = { onNavigateToProfile() },
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
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = DarkText
                    )
                }
                Text(
                    text = "Appointment Queue",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = White,
                contentColor = PrimaryBlue,
                indicator = { }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) PrimaryBlue else MutedText
                            )
                        }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                // Appointment List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentAppointments) { appointment ->
                        val id = appointment["appointmentId"]?.toString() ?: appointment["id"]?.toString() ?: ""
                        val patientUid = appointment["patientUid"]?.toString() ?: ""
                        val patientName = appointment["patientName"]?.toString() ?: "Unknown"
                        val time = appointment["time"]?.toString() ?: ""
                        val date = appointment["date"]?.toString() ?: ""
                        val age = (appointment["age"] as? Number)?.toInt() ?: 0
                        val gender = appointment["gender"]?.toString() ?: ""
                        val bloodGroup = appointment["bloodGroup"]?.toString() ?: ""
                        val hasSharedRecords = appointment["hasSharedRecords"] as? Boolean ?: false
                        val sharedRecordCount = (appointment["sharedRecordCount"] as? Number)?.toInt() ?: 0
                        val isCompleted = appointment["status"]?.toString() == "completed"

                        QueueAppointmentCard(
                            patientName = patientName,
                            time = time,
                            date = date,
                            age = age,
                            gender = gender,
                            bloodGroup = bloodGroup,
                            hasSharedRecords = hasSharedRecords,
                            sharedRecordCount = sharedRecordCount,
                            isCompleted = isCompleted,
                            onClick = { onPatientSelected(id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueAppointmentCard(
    patientName: String,
    time: String,
    date: String,
    age: Int,
    gender: String,
    bloodGroup: String,
    hasSharedRecords: Boolean,
    sharedRecordCount: Int,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Name + Time row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = patientName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                if (isCompleted) {
                    Text(
                        text = "Completed",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryBlue
                    )
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = LightBlue
                        ) {
                            Text(
                                text = time,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                        if (date.isNotEmpty()) {
                            val formattedDate = try {
                                val ld = java.time.LocalDate.parse(date)
                                ld.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
                            } catch (_: Exception) { date }
                            Text(
                                text = formattedDate,
                                fontSize = 11.sp,
                                color = SecondaryText,
                                modifier = Modifier.padding(top = 2.dp, end = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$age Yrs / $gender • Blood Group: $bloodGroup",
                    fontSize = 13.sp,
                    color = MutedText
                )
                if (!isCompleted) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = BorderColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status badge
            if (isCompleted) {
                // No additional badge for completed
            } else if (hasSharedRecords && sharedRecordCount > 0) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFFF3E0)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "$sharedRecordCount Time-Boxed Records Attached",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = WarningOrange
                        )
                    }
                }
            } else {
                Text(
                    text = "No Shared Records",
                    fontSize = 12.sp,
                    color = SecondaryText
                )
            }
        }
    }
}
