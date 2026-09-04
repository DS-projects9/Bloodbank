package com.medkeen.ui.screens.patient

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medkeen.ui.theme.*
import com.medkeen.viewmodel.PatientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyBloodSearchScreen(
    onBack: () -> Unit,
    viewModel: PatientViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedBloodGroup by remember { mutableStateOf<String?>(null) }
    var units by remember { mutableStateOf("2") }
    var selectedTiming by remember { mutableStateOf("Immediate (Critical)") }
    var selectedPurpose by remember { mutableStateOf("Trauma / Emergency Surgery") }
    var radius by remember { mutableStateOf(15f) }
    var timingExpanded by remember { mutableStateOf(false) }
    var purposeExpanded by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var requestSent by remember { mutableStateOf<String?>(null) }

    val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    val timingOptions = listOf("Immediate (Critical)", "Within 24 hours", "Within 48 hours", "Routine (1-2 weeks)")
    val purposeOptions = listOf(
        "Trauma / Emergency Surgery",
        "Scheduled Surgery",
        "Cancer Treatment",
        "Childbirth / Obstetric",
        "Anemia / Blood Disorder",
        "Other"
    )

    fun unitsInt(): Int = units.filter { it.isDigit() }.toIntOrNull() ?: 1
    fun urgency(): String = if (selectedTiming.contains("Immediate", true) || selectedTiming.contains("Critical", true)) "critical" else "normal"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Emergency Blood Request",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkText
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
            Spacer(modifier = Modifier.height(12.dp))

            // Red-bordered configurator card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, EmergencyRed, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Required Blood Group",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )

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
                                            .height(39.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .then(
                                                if (isSelected) {
                                                    Modifier.background(EmergencyRed)
                                                } else {
                                                    Modifier.border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                                                }
                                            )
                                            .clickable { selectedBloodGroup = group },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = group,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) White else DarkText
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Units", fontSize = 12.sp, color = MutedText)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = units,
                                onValueChange = { new ->
                                    if (new.all { it.isDigit() } && new.length <= 3) {
                                        units = new
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmergencyRed,
                                    unfocusedBorderColor = BorderColor
                                )
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Required Timing", fontSize = 12.sp, color = MutedText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box {
                                OutlinedTextField(
                                    value = selectedTiming,
                                    onValueChange = {},
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { timingExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    readOnly = true,
                                    enabled = false,
                                    trailingIcon = {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = EmergencyRed)
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmergencyRed,
                                        unfocusedBorderColor = BorderColor,
                                        disabledTextColor = EmergencyRed,
                                        disabledBorderColor = BorderColor,
                                        disabledTrailingIconColor = EmergencyRed
                                    ),
                                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, color = EmergencyRed)
                                )
                                DropdownMenu(expanded = timingExpanded, onDismissRequest = { timingExpanded = false }) {
                                    timingOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(text = option, color = if (option == selectedTiming) EmergencyRed else DarkText) },
                                            onClick = { selectedTiming = option; timingExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column {
                        Text(text = "Purpose", fontSize = 12.sp, color = MutedText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            OutlinedTextField(
                                value = selectedPurpose,
                                onValueChange = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { purposeExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                readOnly = true,
                                enabled = false,
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF525252)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BorderColor,
                                    unfocusedBorderColor = BorderColor,
                                    disabledTextColor = DarkText,
                                    disabledBorderColor = BorderColor,
                                    disabledTrailingIconColor = Color(0xFF525252)
                                )
                            )
                            DropdownMenu(expanded = purposeExpanded, onDismissRequest = { purposeExpanded = false }) {
                                purposeOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(text = option, color = if (option == selectedPurpose) EmergencyRed else DarkText) },
                                        onClick = { selectedPurpose = option; purposeExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Search Radius", fontSize = 12.sp, color = MutedText)
                        Text(text = "${radius.toInt()} km", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkText)
                    }

                    Slider(
                        value = radius,
                        onValueChange = { radius = it },
                        valueRange = 5f..50f,
                        steps = 8,
                        colors = SliderDefaults.colors(thumbColor = EmergencyRed, activeTrackColor = EmergencyRed)
                    )

                    Button(
                        onClick = {
                            viewModel.loadBloodBanks(selectedBloodGroup)
                            searched = true
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed, contentColor = White),
                        shape = RoundedCornerShape(8.dp),
                        enabled = selectedBloodGroup != null
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Find Matching Blood Banks", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (searched) {
                val banks = uiState.bloodBanks

                requestSent?.let { bankName ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SuccessGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Request sent to $bankName",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = SuccessGreen
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "Available Stock Nearby (${banks.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmergencyRed)
                    }
                } else if (banks.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = White), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No blood banks with $selectedBloodGroup in stock nearby.", fontSize = 14.sp, color = MutedText)
                        }
                    }
                } else {
                    banks.forEach { bank ->
                        val unitsMap = (bank["bloodGroupUnits"] as? Map<*, *>) ?: emptyMap<Any, Any>()
                        val groupUnits = ((unitsMap[selectedBloodGroup ?: "O+"] as? Number)?.toInt()) ?: 0
                        val name = (bank["name"] as? String) ?: "Unknown Bank"
                        val address = (bank["address"] as? String) ?: ""
                        val patientName = run {
                            val p = uiState.profile
                            val n = (p?.get("name") ?: p?.get("displayName")) as? String
                            if (!n.isNullOrBlank()) n
                            else (p?.get("email") as? String)?.substringBefore("@")?.replaceFirstChar { it.uppercase() }?.takeIf { it.isNotBlank() } ?: "Patient"
                        }

                        BloodBankResultCard(
                            bankName = name,
                            distance = address.ifBlank { "—" },
                            bloodGroup = selectedBloodGroup ?: "O+",
                            availableUnits = groupUnits,
                            isHighStock = groupUnits >= 5,
                            onRequest = {
                                viewModel.createBloodRequest(
                                    patientName = patientName,
                                    bloodGroup = selectedBloodGroup ?: "O+",
                                    units = unitsInt(),
                                    hospitalName = name,
                                    hospitalAddress = address,
                                    urgency = urgency(),
                                    note = selectedPurpose
                                )
                                requestSent = name
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BloodBankResultCard(
    bankName: String,
    distance: String,
    bloodGroup: String,
    availableUnits: Int,
    isHighStock: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = bankName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText,
                    modifier = Modifier.weight(1f)
                )
                val badgeColor = if (isHighStock) Color(0xFF2E7D32) else WarningOrange
                Surface(shape = RoundedCornerShape(4.dp), color = badgeColor.copy(alpha = 0.1f)) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(badgeColor))
                        Text(
                            text = "$bloodGroup · Available: $availableUnits unit${if (availableUnits != 1) "s" else ""}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MutedText, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = distance, fontSize = 12.sp, color = MutedText)
            }

            Button(
                onClick = onRequest,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed, contentColor = White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Request Blood", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
