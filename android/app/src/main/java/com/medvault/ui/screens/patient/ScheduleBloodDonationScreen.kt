package com.medvault.ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.medvault.viewmodel.PatientViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medvault.ui.theme.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBloodDonationScreen(
    onBack: () -> Unit,
    viewModel: PatientViewModel = hiltViewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedBloodGroup by remember { mutableStateOf<String?>(null) }
    var selectedBankIndex by remember { mutableIntStateOf(-1) }
    var selectedTimeSlot by remember { mutableStateOf<String?>(null) }
    var bankExpanded by remember { mutableStateOf(false) }
    var isBooking by remember { mutableStateOf(false) }
    var isBooked by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val bloodBanks = uiState.bloodBanks

    LaunchedEffect(Unit) {
        viewModel.loadBloodBanks()
    }

    val scope = rememberCoroutineScope()

    fun convertTo24Hour(time: String): String {
        val cleaned = time.trim()
        val isPm = cleaned.endsWith("PM", ignoreCase = true)
        val (hStr, mStr) = cleaned.substringBefore(" ").split(":")
        var hour = hStr.toIntOrNull() ?: 0
        if (isPm && hour != 12) hour += 12
        if (!isPm && hour == 12) hour = 0
        return "%02d:%02d".format(hour, mStr.toIntOrNull() ?: 0)
    }

    val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    val timeSlots = listOf("09:00 AM", "11:30 AM", "02:00 PM", "04:30 PM")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Schedule Blood Donation",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ActiveBlue
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ActiveBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .padding(paddingValues)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Full Name
            Text(
                text = "Full Name",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Phone Number
            Text(
                text = "Phone Number",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                placeholder = { Text("+91 ", color = SecondaryText) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Select Blood Group
            Text(
                text = "Select Blood Group",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (row in bloodGroups.chunked(4)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { group ->
                            val isSelected = selectedBloodGroup == group
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(PrimaryBlue)
                                        } else {
                                            Modifier.border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                                        }
                                    )
                                    .clickable { selectedBloodGroup = group },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = group,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) White else DarkText
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Select Nearest Blood Bank
            Text(
                text = "Select Nearest Blood Bank",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box {
                val displayText = if (selectedBankIndex >= 0 && selectedBankIndex < bloodBanks.size) {
                    bloodBanks[selectedBankIndex]["name"] as? String ?: ""
                } else ""
                OutlinedTextField(
                    value = displayText,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { bankExpanded = true },
                    shape = RoundedCornerShape(8.dp),
                    readOnly = true,
                    enabled = false,
                    placeholder = { Text("Choose from ${bloodBanks.size} banks", color = SecondaryText) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = DarkText
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderColor,
                        disabledTextColor = DarkText,
                        disabledBorderColor = BorderColor,
                        disabledTrailingIconColor = DarkText
                    )
                )
                DropdownMenu(
                    expanded = bankExpanded,
                    onDismissRequest = { bankExpanded = false }
                ) {
                    bloodBanks.forEachIndexed { index, bank ->
                        val name = bank["name"] as? String ?: "Unknown"
                        val address = bank["address"] as? String ?: ""
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = name,
                                        color = if (index == selectedBankIndex) PrimaryBlue else DarkText,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (address.isNotBlank()) {
                                        Text(
                                            text = address,
                                            fontSize = 12.sp,
                                            color = SecondaryText
                                        )
                                    }
                                }
                            },
                            onClick = {
                                selectedBankIndex = index
                                bankExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Select Donation Time Slot
            Text(
                text = "Select Donation Time Slot",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (row in timeSlots.chunked(3)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { slot ->
                            val isSelected = selectedTimeSlot == slot
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.dp, PrimaryBlue, RoundedCornerShape(4.dp))
                                        } else {
                                            Modifier.border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                                        }
                                    )
                                    .clickable { selectedTimeSlot = slot },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = slot,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryBlue else DarkText
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Confirm Button
            Button(
                onClick = {
                    val time = selectedTimeSlot ?: return@Button
                    val group = selectedBloodGroup ?: return@Button
                    if (selectedBankIndex < 0 || selectedBankIndex >= bloodBanks.size) return@Button
                    val bank = bloodBanks[selectedBankIndex]
                    val bankName = bank["name"] as? String ?: return@Button
                    val bankAddress = bank["address"] as? String ?: ""
                    val today = java.time.LocalDate.now().toString()
                    val time24 = convertTo24Hour(time)
                    isBooking = true
                    viewModel.scheduleBloodDonation(
                        bloodGroup = group,
                        scheduledDate = today,
                        scheduledTime = time24,
                        hospitalName = bankName,
                        hospitalAddress = bankAddress
                    ) { ok, _ ->
                        isBooking = false
                        if (ok) {
                            isBooked = true
                            scope.launch {
                                delay(2000)
                                onBack()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = White
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = fullName.isNotBlank() && phoneNumber.isNotBlank() && selectedBloodGroup != null && selectedBankIndex >= 0 && selectedTimeSlot != null && !isBooking && !isBooked
            ) {
                if (isBooking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Confirm & Notify Blood Bank",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (isBooking) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Booking your slot…",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                    }
                }
            }
        }

        if (isBooked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Slot Booked!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Your donation slot is confirmed and the blood bank has been notified.",
                            fontSize = 14.sp,
                            color = SecondaryText
                        )
                    }
                }
            }
        }
    }
}
}
