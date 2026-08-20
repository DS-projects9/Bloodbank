package com.medvault.ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medvault.ui.theme.*
import com.medvault.viewmodel.PatientViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailsScreen(
    doctorUid: String,
    onBack: () -> Unit,
    viewModel: PatientViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val doctor = uiState.doctorSearchResults.find { it.uid == doctorUid }

    val today = LocalDate.now()
    val monday = today.with(DayOfWeek.MONDAY)
    val weekDates = (0..6).map { monday.plusDays(it.toLong()) }
    val todayStr = today.toString()

    var selectedDate by remember { mutableStateOf(todayStr) }
    var selectedSlotId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(doctorUid) {
        viewModel.loadDoctorSlots(doctorUid)
    }

    val allSlots = uiState.availableSlots
    val daySlots = allSlots.filter { (it["date"] as? String) ?: "" == selectedDate }
        .sortedBy { it["startTime"] as? String }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Appointment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundPurple
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

            // Doctor Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(PrimaryBlue, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (doctor?.name ?: "Dr").takeLast(1).uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = doctor?.name ?: "Doctor",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                        Text(
                            text = doctor?.specialization ?: "General",
                            fontSize = 14.sp,
                            color = SecondaryText
                        )
                        doctor?.hospitalName?.let { hospital ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = SecondaryText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = hospital,
                                    fontSize = 12.sp,
                                    color = SecondaryText
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Date selection (this week)
            Text(
                text = "Select Date",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(weekDates) { date ->
                    val selected = date.toString() == selectedDate
                    val isPast = date.isBefore(today)
                    DateChip(
                        date = date,
                        selected = selected,
                        enabled = !isPast,
                        onClick = {
                            selectedDate = date.toString()
                            selectedSlotId = null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Slots for the selected date
            val dateLabel = try {
                LocalDate.parse(selectedDate)
                    .format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH))
            } catch (_: Exception) {
                selectedDate
            }
            Text(
                text = "Available Slots — $dateLabel",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = PrimaryBlue) }
            } else if (daySlots.isEmpty()) {
                Text(
                    text = "No slots for this date.",
                    fontSize = 14.sp,
                    color = SecondaryText
                )
            } else {
                val morning = daySlots.filter {
                    val t = (it["startTime"] as? String) ?: ""
                    t < "12:00"
                }
                val afternoon = daySlots.filter {
                    val t = (it["startTime"] as? String) ?: ""
                    t >= "12:00"
                }
                if (morning.isNotEmpty()) {
                    Text("Morning", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SecondaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    SlotGrid(slots = morning, selectedSlotId = selectedSlotId, onSelect = { selectedSlotId = it })
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (afternoon.isNotEmpty()) {
                    Text("Afternoon", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SecondaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    SlotGrid(slots = afternoon, selectedSlotId = selectedSlotId, onSelect = { selectedSlotId = it })
                }
            }

            uiState.error?.let { err ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = err,
                    fontSize = 13.sp,
                    color = DestructiveRed
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    selectedSlotId?.let { viewModel.bookAppointment(it, doctorUid) }
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = White
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = selectedSlotId != null
            ) {
                Icon(Icons.Default.Event, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Book Appointment",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SlotGrid(
    slots: List<Map<String, Any>>,
    selectedSlotId: String?,
    onSelect: (String) -> Unit
) {
    val rows = slots.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowSlots ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowSlots.forEach { slot ->
                    val slotId = slot["slotId"] as? String ?: ""
                    val time = (slot["startTime"] as? String) ?: ""
                    val status = (slot["status"] as? String) ?: "available"
                    val available = status == "available"
                    val selected = selectedSlotId == slotId
                    TimeSlotChip(
                        time = time,
                        available = available,
                        selected = selected,
                        onClick = { if (available) onSelect(slotId) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(4 - rowSlots.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TimeSlotChip(
    time: String,
    available: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        selected -> PrimaryBlue
        available -> White
        else -> DisabledGray
    }
    val contentColor = when {
        selected -> White
        available -> DarkText
        else -> SecondaryText
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (selected) PrimaryBlue else BorderColor)
        ),
        onClick = onClick,
        enabled = available
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (available) time else "$time x",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun DateChip(
    date: LocalDate,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    val dayNum = date.dayOfMonth.toString()
    Card(
        modifier = Modifier.width(56.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) PrimaryBlue else White
        ),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (selected) PrimaryBlue else BorderColor)
        ),
        onClick = onClick,
        enabled = enabled
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) White else SecondaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dayNum,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) White else DarkText
            )
        }
    }
}
