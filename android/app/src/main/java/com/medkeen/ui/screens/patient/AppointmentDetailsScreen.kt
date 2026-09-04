package com.medkeen.ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
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
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medkeen.ui.theme.*
import com.medkeen.viewmodel.PatientViewModel
import com.medkeen.viewmodel.SharedFile
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailsScreen(
    doctorUid: String,
    onBack: () -> Unit,
    onNavigateToHealthVault: (appointmentId: String, doctorUid: String, doctorName: String?) -> Unit,
    viewModel: PatientViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val doctor = uiState.doctorSearchResults.find { it.uid == doctorUid }

    val today = LocalDate.now()
    val todayStr = today.toString()

    var selectedDate by remember { mutableStateOf(todayStr) }
    var selectedSlotId by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var booked by remember { mutableStateOf(false) }

    LaunchedEffect(doctorUid) {
        viewModel.loadDoctorSlots(doctorUid)
    }

    val dateLabel = try {
        LocalDate.parse(selectedDate)
            .format(DateTimeFormatter.ofPattern("EEEE, MMM d yyyy", Locale.ENGLISH))
    } catch (_: Exception) {
        selectedDate
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

            // Date selection (date picker, defaults to today)
            Text(
                text = "Select Date",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Appointment Date",
                            fontSize = 12.sp,
                            color = SecondaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateLabel,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                    }
                }
            }

            if (showDatePicker) {
                val initialMillis = try {
                    LocalDate.parse(selectedDate)
                        .atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val picked = LocalDate.ofInstant(
                                    java.time.Instant.ofEpochMilli(millis),
                                    java.time.ZoneId.systemDefault()
                                )
                                selectedDate = picked.toString()
                                selectedSlotId = null
                            }
                            showDatePicker = false
                        }) { Text("OK", color = PrimaryBlue) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel", color = MutedText)
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Slots for the selected date
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
                    if (!booked) {
                        selectedSlotId?.let { viewModel.bookAppointment(it, doctorUid) }
                        booked = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (booked) SuccessGreen else PrimaryBlue,
                    contentColor = White
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = (selectedSlotId != null && !uiState.isLoading) || booked
            ) {
                if (uiState.isLoading && !booked) {
                    CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp))
                } else if (booked) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Appointment Booked",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(Icons.Default.Event, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Book Appointment",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (booked && uiState.lastAppointmentId != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Appointment booked!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Upload and share documents with ${doctor?.name ?: "the doctor"} before your appointment. Files are time-boxed — the doctor can only view them during your appointment window.",
                            fontSize = 13.sp,
                            color = SecondaryText
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onNavigateToHealthVault(
                            uiState.lastAppointmentId!!,
                            doctorUid,
                            doctor?.name
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Documents", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                    border = BorderStroke(1.dp, PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Done", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun ShareFilesSection(
    doctorName: String,
    myDocuments: List<com.medkeen.data.remote.VaultDocument>,
    sharedFiles: List<SharedFile>,
    shareStatus: String?,
    uploadError: String?,
    onUpload: (Triple<String, String, ByteArray>) -> Unit,
    onShare: (List<String>, Int) -> Unit,
    onRevoke: (String) -> Unit,
    onExtend: (String, Long) -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    var selectedFiles by remember { mutableStateOf(setOf<String>()) }
    var durationMinutes by remember { mutableIntStateOf(60) }

    val durationChoices = listOf(
        "15 min" to 15,
        "30 min" to 30,
        "1 hour" to 60,
        "2 hours" to 120,
        "4 hours" to 240,
        "8 hours" to 480,
        "1 day" to 1440,
    )

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { picked ->
            val name = getFileName(context, picked)
            val type = context.contentResolver.getType(picked) ?: "application/octet-stream"
            val bytes = context.contentResolver.openInputStream(picked)?.use { it.readBytes() }
            if (bytes != null) {
                onUpload(Triple(name, type, bytes))
            }
        }
    }

    val myFiles = myDocuments.flatMap { it.fileNames }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundPurple.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Share files with $doctorName",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Upload documents from your device, then select which ones to share for this appointment. Access is limited to the time window you choose — the doctor can only view them during that period.",
                fontSize = 13.sp,
                color = SecondaryText
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { filePicker.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = White
                )
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload from device")
            }

            uploadError?.let { err ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = err, fontSize = 13.sp, color = DestructiveRed)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (myFiles.isEmpty()) {
                Text(
                    text = "No files uploaded yet.",
                    fontSize = 14.sp,
                    color = SecondaryText
                )
            } else {
                Text(
                    text = "Your files (tap to select)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(myFiles) { fileName ->
                        val selected = selectedFiles.contains(fileName)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedFiles = if (selected) {
                                    selectedFiles - fileName
                                } else {
                                    selectedFiles + fileName
                                }
                            },
                            label = { Text(fileName, fontSize = 12.sp) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Access window (how long the doctor can view these files)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DarkText
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(durationChoices) { (label, minutes) ->
                    FilterChip(
                        selected = durationMinutes == minutes,
                        onClick = { durationMinutes = minutes },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (shareStatus == "shared") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Files shared with $doctorName.",
                        fontSize = 14.sp,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Button(
                    onClick = { onShare(selectedFiles.toList(), durationMinutes) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedFiles.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = White
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share selected (${selectedFiles.size})")
                }
            }

            if (sharedFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Active shares",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(8.dp))
                sharedFiles.forEach { share ->
                    SharedFileRow(
                        share = share,
                        durationChoices = durationChoices,
                        onRevoke = { onRevoke(share.documentId) },
                        onExtend = { minutes -> onExtend(share.documentId, minutes) }
                    )
                }
            }

            if (shareStatus == "shared" || sharedFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                    border = BorderStroke(1.dp, PrimaryBlue)
                ) {
                    Text("Done")
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun SharedFileRow(
    share: SharedFile,
    durationChoices: List<Pair<String, Int>>,
    onRevoke: () -> Unit,
    onExtend: (Long) -> Unit
) {
    var extendMinutes by remember(share.documentId) { mutableIntStateOf(60) }
    val isRevoked = share.status == "revoked"
    val remainingMs = if (isRevoked) 0 else (share.expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
    val remainingText = if (isRevoked) "Revoked" else formatRemaining(remainingMs)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRevoked) DisabledGray else White
        ),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isRevoked) BorderColor else PrimaryBlue)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = share.fileNames.firstOrNull() ?: "Document",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkText
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isRevoked) DestructiveRed.copy(alpha = 0.1f)
                    else SuccessGreen.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = remainingText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRevoked) DestructiveRed else SuccessGreen
                    )
                }
            }

            if (!isRevoked) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Extend access by",
                    fontSize = 12.sp,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(durationChoices) { (label, minutes) ->
                        FilterChip(
                            selected = extendMinutes == minutes,
                            onClick = { extendMinutes = minutes },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onExtend(extendMinutes.toLong()) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = White
                        )
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Extend", fontSize = 13.sp)
                    }
                    Button(
                        onClick = onRevoke,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DestructiveRed,
                            contentColor = White
                        )
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Revoke", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val totalMin = ms / 60000
    return when {
        totalMin < 60 -> "$totalMin min left"
        totalMin < 1440 -> {
            val h = totalMin / 60
            val m = totalMin % 60
            if (m == 0L) "$h hr left" else "$h hr ${m}m left"
        }
        else -> {
            val d = totalMin / 1440
            val h = (totalMin % 1440) / 60
            if (h == 0L) "$d d left" else "$d d ${h}h left"
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String {
    var name = uri.lastPathSegment ?: "document"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && it.moveToFirst()) {
            name = it.getString(idx)
        }
    }
    return name
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
