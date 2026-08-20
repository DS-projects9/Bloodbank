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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medvault.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBloodDonationScreen(
    onBack: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedBloodGroup by remember { mutableStateOf<String?>(null) }
    var selectedBank by remember { mutableStateOf("City Central Blood Bank (2.4 km away)") }
    var selectedTimeSlot by remember { mutableStateOf<String?>(null) }
    var bankExpanded by remember { mutableStateOf(false) }

    val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    val timeSlots = listOf("09:00 AM", "11:30 AM", "02:00 PM", "04:30 PM")
    val bloodBanks = listOf(
        "City Central Blood Bank (2.4 km away)",
        "Red Cross Blood Bank (5.1 km away)",
        "Metro Hospital Blood Bank (7.8 km away)"
    )

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .padding(paddingValues)
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
                OutlinedTextField(
                    value = selectedBank,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { bankExpanded = true },
                    shape = RoundedCornerShape(8.dp),
                    readOnly = true,
                    enabled = false,
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
                    bloodBanks.forEach { bank ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = bank,
                                    color = if (bank == selectedBank) PrimaryBlue else DarkText
                                )
                            },
                            onClick = {
                                selectedBank = bank
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
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = White
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = fullName.isNotBlank() && phoneNumber.isNotBlank() && selectedBloodGroup != null && selectedTimeSlot != null
            ) {
                Text(
                    text = "Confirm & Notify Blood Bank",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
