package com.medvault.ui.screens.bloodbank

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medvault.ui.theme.*
import com.medvault.viewmodel.BloodBankViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogUpdateInventoryScreen(
    onBack: () -> Unit,
    viewModel: BloodBankViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedMode by remember { mutableIntStateOf(0) }
    var bloodGroup by remember { mutableStateOf("") }
    var units by remember { mutableStateOf("") }
    var collectionDate by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var volumePerUnit by remember { mutableStateOf("500") }
    var storageTemp by remember { mutableStateOf("4.0") }
    var vaultLocation by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dispatchReason by remember { mutableStateOf("") }
    var dispatchUnits by remember { mutableStateOf("") }
    var requestRef by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    var showCollectionDatePicker by remember { mutableStateOf(false) }
    var showExpiryDatePicker by remember { mutableStateOf(false) }

    val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val today = remember { Calendar.getInstance() }

    @Suppress("UNCHECKED_CAST")
    val bloodGroupUnits = remember(uiState.inventory) {
        (uiState.inventory?.get("bloodGroupUnits") as? Map<String, Any>) ?: emptyMap()
    }

    if (showCollectionDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = today.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { showCollectionDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        collectionDate = dateFormat.format(Date(it))
                    }
                    showCollectionDatePicker = false
                }) { Text("OK", color = PrimaryBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showCollectionDatePicker = false }) {
                    Text("Cancel", color = MutedText)
                }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showExpiryDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = today.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { showExpiryDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        expiryDate = dateFormat.format(Date(it))
                    }
                    showExpiryDatePicker = false
                }) { Text("OK", color = PrimaryBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showExpiryDatePicker = false }) {
                    Text("Cancel", color = MutedText)
                }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(White)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkText)
                }
                Text(
                    text = "Log / Update Inventory",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues).background(White),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (submitted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(White),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (selectedMode == 0) "Stock Logged Successfully" else "Stock Updated Successfully",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Inventory records have been updated",
                        fontSize = 14.sp,
                        color = MutedText
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Back to Inventory", color = White)
                    }
                }
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
                Spacer(modifier = Modifier.height(8.dp))

                // Mode selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectedMode = 0 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedMode == 0) SuccessGreen else White,
                            contentColor = if (selectedMode == 0) White else SuccessGreen
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(SuccessGreen)
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add New Stock", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    OutlinedButton(
                        onClick = { selectedMode = 1 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedMode == 1) EmergencyRed else White,
                            contentColor = if (selectedMode == 1) White else EmergencyRed
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(EmergencyRed)
                        )
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dispatch Stock", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (selectedMode == 0) {
                    // Add New Stock Form
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "New Blood Stock Entry",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Blood Group Selector
                            Text(text = "Blood Group", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkText)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                bloodGroups.take(4).forEach { group ->
                                    val isSelected = bloodGroup == group
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { bloodGroup = group },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) EmergencyRed else White,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp, if (isSelected) EmergencyRed else BorderColor
                                        )
                                    ) {
                                        Text(
                                            text = group,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected) White else DarkText,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                bloodGroups.drop(4).forEach { group ->
                                    val isSelected = bloodGroup == group
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { bloodGroup = group },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) EmergencyRed else White,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp, if (isSelected) EmergencyRed else BorderColor
                                        )
                                    ) {
                                        Text(
                                            text = group,
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

                            // Units
                            Text(text = "Number of Units", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkText)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = units,
                                onValueChange = { units = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g. 5", color = MutedText) },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = BorderColor
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Volume per unit + Storage temp
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Volume/Unit (ml)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkText)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = volumePerUnit,
                                        onValueChange = { volumePerUnit = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("500", color = MutedText) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryBlue,
                                            unfocusedBorderColor = BorderColor
                                        ),
                                        singleLine = true
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Storage Temp (°C)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkText)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = storageTemp,
                                        onValueChange = { storageTemp = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("4.0", color = MutedText) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryBlue,
                                            unfocusedBorderColor = BorderColor
                                        ),
                                        singleLine = true
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Collection Date + Expiry Date
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Collection Date", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkText)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(modifier = Modifier.fillMaxWidth().clickable { showCollectionDatePicker = true }) {
                                        OutlinedTextField(
                                            value = collectionDate,
                                            onValueChange = {},
                                            enabled = false,
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = { Text("Select Date", color = MutedText) },
                                            shape = RoundedCornerShape(8.dp),
                                            trailingIcon = {
                                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MutedText)
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryBlue,
                                                unfocusedBorderColor = BorderColor,
                                                disabledBorderColor = BorderColor,
                                                disabledTextColor = DarkText,
                                                disabledPlaceholderColor = MutedText
                                            )
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Expiry Date", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkText)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(modifier = Modifier.fillMaxWidth().clickable { showExpiryDatePicker = true }) {
                                        OutlinedTextField(
                                            value = expiryDate,
                                            onValueChange = {},
                                            enabled = false,
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = { Text("Select Date", color = MutedText) },
                                            shape = RoundedCornerShape(8.dp),
                                            trailingIcon = {
                                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MutedText)
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryBlue,
                                                unfocusedBorderColor = BorderColor,
                                                disabledBorderColor = BorderColor,
                                                disabledTextColor = DarkText,
                                                disabledPlaceholderColor = MutedText
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Vault Location
                            Text(text = "Vault Location", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkText)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = vaultLocation,
                                onValueChange = { vaultLocation = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g. Chiller B • Rack 05", color = MutedText) },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = BorderColor
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Notes
                            Text(text = "Notes (Optional)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkText)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Donor booking ref, batch details...", color = MutedText) },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = BorderColor
                                ),
                                minLines = 2,
                                maxLines = 3
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val unitCount = units.toIntOrNull() ?: 0
                            viewModel.adjustInventory(
                                bloodGroup = bloodGroup,
                                units = unitCount,
                                reason = notes.ifEmpty { "Stock addition" },
                                expiryDate = expiryDate.ifEmpty { null },
                                vaultLocation = vaultLocation.ifEmpty { null },
                                collectionDate = collectionDate.ifEmpty { null },
                                volumePerUnit = volumePerUnit.toDoubleOrNull()?.div(1000.0),
                                storageTemp = storageTemp.ifEmpty { null },
                                notes = notes.ifEmpty { null },
                            )
                            submitted = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(8.dp),
                        enabled = bloodGroup.isNotEmpty() && units.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Log New Stock",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                } else {
                    // Dispatch Stock Form
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Dispatch Blood Stock",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Blood Group Selector
                            Text(text = "Blood Group to Dispatch", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkText)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                bloodGroups.take(4).forEach { group ->
                                    val isSelected = bloodGroup == group
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { bloodGroup = group },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) EmergencyRed else White,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp, if (isSelected) EmergencyRed else BorderColor
                                        )
                                    ) {
                                        Text(
                                            text = group,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected) White else DarkText,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                bloodGroups.drop(4).forEach { group ->
                                    val isSelected = bloodGroup == group
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { bloodGroup = group },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) EmergencyRed else White,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp, if (isSelected) EmergencyRed else BorderColor
                                        )
                                    ) {
                                        Text(
                                            text = group,
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

                            if (bloodGroup.isNotEmpty()) {
                                val availableUnits = (bloodGroupUnits[bloodGroup] as? Number)?.toInt() ?: 0
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (availableUnits > 0) Color(0xFFFFF3E0) else EmergencyRed.copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = if (availableUnits > 0) WarningOrange else EmergencyRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Available: $availableUnits units of $bloodGroup",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (availableUnits > 0) WarningOrange else EmergencyRed
                                            )
                                            if (availableUnits == 0) {
                                                Text(
                                                    text = "Cannot dispatch - no stock available",
                                                    fontSize = 11.sp,
                                                    color = EmergencyRed
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Units to dispatch
                            Text(text = "Units to Dispatch", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkText)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = dispatchUnits,
                                onValueChange = { dispatchUnits = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g. 2", color = MutedText) },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmergencyRed,
                                    unfocusedBorderColor = BorderColor
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Request Reference
                            Text(text = "Request / Booking Reference", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkText)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = requestRef,
                                onValueChange = { requestRef = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g. Emergency Request #402", color = MutedText) },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmergencyRed,
                                    unfocusedBorderColor = BorderColor
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Reason
                            Text(text = "Dispatch Reason", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkText)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = dispatchReason,
                                onValueChange = { dispatchReason = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g. Emergency surgery, patient transfer...", color = MutedText) },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmergencyRed,
                                    unfocusedBorderColor = BorderColor
                                ),
                                minLines = 2,
                                maxLines = 3
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val unitCount = dispatchUnits.toIntOrNull() ?: 0
                            viewModel.adjustInventory(
                                bloodGroup = bloodGroup,
                                units = -unitCount,
                                reason = dispatchReason.ifEmpty { requestRef.ifEmpty { "Stock dispatch" } }
                            )
                            submitted = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                        shape = RoundedCornerShape(8.dp),
                        enabled = bloodGroup.isNotEmpty() && dispatchUnits.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Confirm Dispatch",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
