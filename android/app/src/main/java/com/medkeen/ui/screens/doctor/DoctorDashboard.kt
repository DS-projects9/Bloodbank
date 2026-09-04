package com.medkeen.ui.screens.doctor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.medkeen.data.remote.SlotUpdate
import com.medkeen.ui.theme.*
import com.medkeen.viewmodel.DoctorViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDashboard(
    onNavigateToProfile: () -> Unit,
    onNavigateToQueue: () -> Unit,
    viewModel: DoctorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("09:00 AM") }
    var endTime by remember { mutableStateOf("05:00 PM") }
    var selectedInterval by remember { mutableStateOf("20m") }
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var breakSlots by remember { mutableStateOf(setOf<String>()) }

    val allDays = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
    var selectedDays by remember { mutableStateOf(setOf("monday", "tuesday", "wednesday", "thursday", "friday")) }

    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.scheduleConfig) {
        uiState.scheduleConfig?.let { config ->
            (config["startTime"] as? String)?.let { startTime = it }
            (config["endTime"] as? String)?.let { endTime = it }
            (config["slotMinutes"] as? Number)?.let { selectedInterval = "${it.toInt()}m" }
            val savedDays = (config["slots"] as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.get("day")?.toString()?.lowercase() }?.toSet()
            if (!savedDays.isNullOrEmpty()) {
                selectedDays = savedDays
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val isoFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeParseFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val today = remember { Calendar.getInstance() }

    val intervalMinutes = remember(selectedInterval) {
        selectedInterval.replace("m", "").toIntOrNull() ?: 20
    }

    val generatedSlots = remember(startTime, endTime, intervalMinutes) {
        try {
            val start = timeParseFormat.parse(startTime)
            val end = timeParseFormat.parse(endTime)
            if (start == null || end == null || !start.before(end)) return@remember emptyList()
            val slots = mutableListOf<String>()
            val cal = Calendar.getInstance().apply { time = start }
            val endCal = Calendar.getInstance().apply { time = end }
            while (cal.before(endCal)) {
                slots.add(timeFormat.format(cal.time))
                cal.add(Calendar.MINUTE, intervalMinutes)
            }
            slots
        } catch (_: Exception) {
            emptyList()
        }
    }

    if (showFromDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = today.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { showFromDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        fromDate = dateFormat.format(Date(it))
                    }
                    showFromDatePicker = false
                }) { Text("OK", color = PrimaryBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showFromDatePicker = false }) {
                    Text("Cancel", color = MutedText)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showToDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = today.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { showToDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        toDate = dateFormat.format(Date(it))
                    }
                    showToDatePicker = false
                }) { Text("OK", color = PrimaryBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showToDatePicker = false }) {
                    Text("Cancel", color = MutedText)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = 9, initialMinute = 0, is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    startTime = timeFormat.format(cal.time)
                    showStartTimePicker = false
                }) { Text("OK", color = PrimaryBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text("Cancel", color = MutedText)
                }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = 17, initialMinute = 0, is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    endTime = timeFormat.format(cal.time)
                    showEndTimePicker = false
                }) { Text("OK", color = PrimaryBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text("Cancel", color = MutedText)
                }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    val intervals = listOf("15m", "20m", "30m", "45m")

    Box(modifier = Modifier.fillMaxSize()) {
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
                        onClick = { selectedNavIndex = 0 },
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
                        onClick = {
                            selectedNavIndex = 1
                            onNavigateToQueue()
                        },
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
                        onClick = {
                            selectedNavIndex = 2
                            onNavigateToProfile()
                        },
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
                    Text(
                        text = "Doctor Dashboard",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = White,
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = DarkText,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = uiState.profile?.get("hospitalName") as? String ?: "",
                                fontSize = 12.sp,
                                color = DarkText
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Online Toggle Card
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Online for Today's Clinic",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText
                                )
                                Text(
                                    text = "Visible to patients for booking",
                                    fontSize = 12.sp,
                                    color = MutedText
                                )
                            }
                            Switch(
                                checked = uiState.clinicOpen,
                                onCheckedChange = { viewModel.setStatus(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = White,
                                    checkedTrackColor = PrimaryBlue
                                )
                            )
                        }
                    }

                    // Schedule Date Range Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Schedule Date Range",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "From Date",
                                        fontSize = 12.sp,
                                        color = MutedText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.fillMaxWidth().clickable { showFromDatePicker = true }) {
                                        OutlinedTextField(
                                            value = fromDate,
                                            onValueChange = {},
                                            enabled = false,
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = { Text("Select Date") },
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
                                    Text(
                                        text = "To Date",
                                        fontSize = 12.sp,
                                        color = MutedText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.fillMaxWidth().clickable { showToDatePicker = true }) {
                                        OutlinedTextField(
                                            value = toDate,
                                            onValueChange = {},
                                            enabled = false,
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = { Text("Select Date") },
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
                        }
                    }

                    // Configure Shift Hours Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Configure Shift Hours",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Start/End Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Start Time",
                                        fontSize = 12.sp,
                                        color = MutedText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.fillMaxWidth().clickable { showStartTimePicker = true }) {
                                        OutlinedTextField(
                                            value = startTime,
                                            onValueChange = {},
                                            enabled = false,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            trailingIcon = {
                                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = MutedText)
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryBlue,
                                                unfocusedBorderColor = BorderColor,
                                                disabledBorderColor = BorderColor,
                                                disabledTextColor = DarkText
                                            )
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "End Time",
                                        fontSize = 12.sp,
                                        color = MutedText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.fillMaxWidth().clickable { showEndTimePicker = true }) {
                                        OutlinedTextField(
                                            value = endTime,
                                            onValueChange = {},
                                            enabled = false,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            trailingIcon = {
                                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = MutedText)
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryBlue,
                                                unfocusedBorderColor = BorderColor,
                                                disabledBorderColor = BorderColor,
                                                disabledTextColor = DarkText
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Slot Interval Duration
                            Text(
                                text = "Slot Interval Duration",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                intervals.forEach { interval ->
                                    val isSelected = selectedInterval == interval
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .then(
                                                if (isSelected) {
                                                    Modifier.background(PrimaryBlue)
                                                } else {
                                                    Modifier.border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                                }
                                            )
                                            .clickable { selectedInterval = interval },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = interval,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) White else DarkText
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Working Days Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Working Days",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Select the days you are available (includes weekends)",
                                fontSize = 12.sp,
                                color = MutedText
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                allDays.chunked(4).forEach { rowDays ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowDays.forEach { day ->
                                            val isSelected = day in selectedDays
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .then(
                                                        if (isSelected) {
                                                            Modifier.background(PrimaryBlue)
                                                        } else {
                                                            Modifier.border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                                        }
                                                    )
                                                    .clickable {
                                                        selectedDays = if (isSelected) {
                                                            selectedDays - day
                                                        } else {
                                                            selectedDays + day
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = day.replaceFirstChar { it.uppercase() },
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) White else DarkText
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (selectedDays.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Select at least one working day",
                                    fontSize = 12.sp,
                                    color = WarningOrange
                                )
                            }
                        }
                    }

                    // Generated Slot Preview
                    Text(
                        text = "Generated Slot Preview (${generatedSlots.size} Slots)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Text(
                        text = "Tap a slot to toggle as Break",
                        fontSize = 12.sp,
                        color = MutedText
                    )

                    // Slot chips in rows of 3
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (row in generatedSlots.chunked(3)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { slot ->
                                    val isBreak = slot in breakSlots
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                breakSlots = if (isBreak) {
                                                    breakSlots - slot
                                                } else {
                                                    breakSlots + slot
                                                }
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isBreak) WarningOrange.copy(alpha = 0.12f) else LightBlue,
                                        border = if (isBreak) BorderStroke(1.dp, WarningOrange) else null
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (isBreak) {
                                                Icon(
                                                    Icons.Default.Pause,
                                                    contentDescription = "Break",
                                                    tint = WarningOrange,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Text(
                                                text = if (isBreak) "Break" else slot,
                                                fontSize = 12.sp,
                                                fontWeight = if (isBreak) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isBreak) WarningOrange else PrimaryBlue,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (breakSlots.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${breakSlots.size} break slot(s) — will not be available for booking",
                            fontSize = 12.sp,
                            color = WarningOrange
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Publish Button
                    var publishError by remember { mutableStateOf<String?>(null) }

                    val isPublishing = uiState.isLoading

                    Button(
                        onClick = {
                            if (isPublishing) return@Button
                            publishError = null
                            if (fromDate.isBlank() || toDate.isBlank()) {
                                publishError = "Please select both From and To dates"
                                return@Button
                            }

                            val template = try {
                                val from = dateFormat.parse(fromDate)
                                val to = dateFormat.parse(toDate)
                                if (from == null || to == null) {
                                    publishError = "Invalid date format"
                                    return@Button
                                }
                                val weekStart = isoFormat.format(from)

                                if (selectedDays.isEmpty()) {
                                    publishError = "Select at least one working day"
                                    return@Button
                                }

                                val days = allDays.filter { it in selectedDays }
                                val slots = days.map { day ->
                                    SlotUpdate(
                                        day = day,
                                        startTime = startTime,
                                        endTime = endTime,
                                        slotMinutes = intervalMinutes
                                    )
                                }
                                Pair(slots, weekStart)
                            } catch (e: Exception) {
                                publishError = "Error: ${e.message}"
                                return@Button
                            }

                            viewModel.publishSlots(template.first, template.second)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = true,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPublishing) PrimaryBlue.copy(alpha = 0.85f) else PrimaryBlue,
                            contentColor = White,
                            disabledContainerColor = PrimaryBlue,
                            disabledContentColor = White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isPublishing) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 3.dp,
                                    color = White
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Publishing slots...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                            }
                        } else {
                            Text(
                                text = "Publish Slots for Patient Booking",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    uiState.error?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = err,
                            fontSize = 13.sp,
                            color = Color.Red
                        )
                    }

                    publishError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = err,
                            fontSize = 13.sp,
                            color = Color.Red
                        )
                    }

                    if (uiState.publishedSlotCount > 0 && !uiState.isLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${uiState.publishedSlotCount} slots published successfully!",
                                    fontSize = 13.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
