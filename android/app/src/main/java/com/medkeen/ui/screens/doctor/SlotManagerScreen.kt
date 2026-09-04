package com.medkeen.ui.screens.doctor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medkeen.ui.theme.*
import com.medkeen.viewmodel.DoctorViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotManagerScreen(
    onBack: () -> Unit,
    viewModel: DoctorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }
    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedSlotIds by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        viewModel.loadMySlots()
    }

    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val displayFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d yyyy", Locale.ENGLISH)

    val allSlots = uiState.slots
    val slotDates = remember(allSlots) {
        allSlots.mapNotNull { (it["date"] as? String) }.toSet()
    }
    val daySlots = allSlots.filter {
        (it["date"] as? String) == selectedDate.format(dateFormatter)
    }.sortedBy { (it["startTime"] as? String) ?: "" }

    val baseMonth = YearMonth.now().plusMonths(monthOffset.toLong())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shift Schedule & Published Slots", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Pick a date to view and manage the slots you published for it.",
                fontSize = 13.sp,
                color = SecondaryText
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Month calendar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { monthOffset-- }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", tint = PrimaryBlue)
                        }
                        Text(
                            text = baseMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                        IconButton(onClick = { monthOffset++ }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next month", tint = PrimaryBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        DayOfWeek.values().forEach { dow ->
                            val label = dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MutedText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val firstDayOffset = (baseMonth.atDay(1).dayOfWeek.value % 7)
                    val daysInMonth = baseMonth.lengthOfMonth()
                    val cells = mutableListOf<LocalDate?>()
                    repeat(firstDayOffset) { cells.add(null) }
                    for (d in 1..daysInMonth) cells.add(baseMonth.atDay(d))
                    while (cells.size % 7 != 0) cells.add(null)

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.height((((cells.size / 7) + 1) * 44).dp),
                        userScrollEnabled = false
                    ) {
                        items(cells.size) { idx ->
                            val date = cells[idx]
                            if (date == null) {
                                Box(modifier = Modifier.size(40.dp))
                            } else {
                                val hasSlots = date.format(dateFormatter) in slotDates
                                val isSelected = date == selectedDate
                                val isPast = date.isBefore(today)
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isSelected -> PrimaryBlue
                                                hasSlots -> LightBlue
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable(enabled = !isPast || hasSlots) {
                                            selectedDate = date
                                            selectedSlotIds = emptySet()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        fontSize = 13.sp,
                                        fontWeight = if (hasSlots || isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> White
                                            hasSlots -> PrimaryBlue
                                            isPast -> MutedText
                                            else -> DarkText
                                        }
                                    )
                                    if (hasSlots) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 4.dp)
                                                .size(4.dp)
                                                .background(SuccessGreen, RoundedCornerShape(2.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = selectedDate.format(displayFormatter),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Spacer(modifier = Modifier.height(4.dp))
            val availableCount = daySlots.count { it["status"] as? String == "available" }
            val bookedCount = daySlots.count { it["status"] as? String in listOf("booked", "locked") }
            Text(
                text = "${daySlots.size} slot(s)  •  $availableCount available  •  $bookedCount booked",
                fontSize = 12.sp,
                color = MutedText
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (daySlots.isEmpty()) {
                Text(
                    text = "No slots published for this date.",
                    fontSize = 14.sp,
                    color = SecondaryText
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    daySlots.forEach { slot ->
                        val slotId = (slot["slotId"] as? String) ?: ""
                        val time = (slot["startTime"] as? String) ?: ""
                        val status = (slot["status"] as? String) ?: "available"
                        val isBooked = status in listOf("booked", "locked")
                        val isSelected = slotId in selectedSlotIds
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isBooked) {
                                    selectedSlotIds = if (isSelected) selectedSlotIds - slotId
                                    else selectedSlotIds + slotId
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isSelected -> Color(0xFFE57373).copy(alpha = 0.15f)
                                isBooked -> Color(0xFF2196F3).copy(alpha = 0.12f)
                                else -> LightBlue
                            },
                            border = if (isSelected) BorderStroke(1.dp, Color(0xFFE57373)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isBooked) Icons.Default.Lock else Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (isBooked) Color(0xFF1976D2) else PrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = time,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isBooked) Color(0xFF1976D2) else DarkText
                                    )
                                }
                                if (isBooked) {
                                    Text("Booked", fontSize = 12.sp, color = Color(0xFF1976D2), fontWeight = FontWeight.Medium)
                                } else {
                                    Text(
                                        if (isSelected) "Selected" else "Tap to select",
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color(0xFFE57373) else MutedText
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        selectedSlotIds.forEach { viewModel.deleteSlot(it) }
                        selectedSlotIds = emptySet()
                    },
                    enabled = selectedSlotIds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.EditCalendar, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedSlotIds.isEmpty()) "Update Slots" else "Update Slots (remove ${selectedSlotIds.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Selected slots will be removed when you tap Update Slots. Booked slots cannot be changed.",
                    fontSize = 12.sp,
                    color = SecondaryText
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
