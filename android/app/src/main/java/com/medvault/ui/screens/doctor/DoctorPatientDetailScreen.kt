package com.medvault.ui.screens.doctor

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medvault.data.remote.VaultDocument
import com.medvault.ui.theme.*
import com.medvault.viewmodel.DoctorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorPatientDetailScreen(
    patientId: String,
    onBack: () -> Unit,
    viewModel: DoctorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editBloodGroup by remember { mutableStateOf("") }
    var editCity by remember { mutableStateOf("") }
    var editDob by remember { mutableStateOf("") }

    LaunchedEffect(patientId) {
        viewModel.loadPatient(patientId)
    }

    val profile = uiState.selectedPatient ?: emptyMap()
    val name = ((profile["name"] as? String)?.ifEmpty { null }
        ?: (profile["displayName"] as? String)) ?: "Unknown Patient"
    val phone = (profile["phone"] as? String) ?: "N/A"
    val bloodGroup = (profile["bloodGroup"] as? String) ?: "N/A"
    val city = (profile["city"] as? String) ?: "N/A"
    val dob = (profile["dob"] as? String) ?: "N/A"
    val email = (profile["email"] as? String) ?: ""
    val documents = uiState.patientDocuments

    LaunchedEffect(profile) {
        editName = (profile["name"] as? String) ?: (profile["displayName"] as? String) ?: ""
        editPhone = (profile["phone"] as? String) ?: ""
        editBloodGroup = (profile["bloodGroup"] as? String) ?: ""
        editCity = (profile["city"] as? String) ?: ""
        editDob = (profile["dob"] as? String) ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkText)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (isEditing) {
                            isEditing = false
                        } else {
                            editName = (profile["name"] as? String) ?: (profile["displayName"] as? String) ?: ""
                            editPhone = (profile["phone"] as? String) ?: ""
                            editBloodGroup = (profile["bloodGroup"] as? String) ?: ""
                            editCity = (profile["city"] as? String) ?: ""
                            editDob = (profile["dob"] as? String) ?: ""
                            isEditing = true
                        }
                    }) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Cancel" else "Edit",
                            tint = PrimaryBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(White)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Patient header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(PrimaryBlue, shape = RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.takeLast(1).uppercase(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkText)
                    if (email.isNotEmpty()) {
                        Text(text = email, fontSize = 14.sp, color = SecondaryText)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile info (editable)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                if (isEditing) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = { Text("Phone") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editBloodGroup,
                            onValueChange = { editBloodGroup = it },
                            label = { Text("Blood Group") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editCity,
                            onValueChange = { editCity = it },
                            label = { Text("City") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editDob,
                            onValueChange = { editDob = it },
                            label = { Text("Date of Birth (yyyy-MM-dd)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = {
                                viewModel.updatePatient(
                                    patientUid = patientId,
                                    name = editName.takeIf { it.isNotBlank() },
                                    phone = editPhone.takeIf { it.isNotBlank() },
                                    bloodGroup = editBloodGroup.takeIf { it.isNotBlank() },
                                    city = editCity.takeIf { it.isNotBlank() },
                                    dob = editDob.takeIf { it.isNotBlank() }
                                )
                                isEditing = false
                            }) {
                                Text("Save")
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ProfileInfoRow(label = "Blood Group", value = bloodGroup)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileInfoRow(label = "Phone", value = phone)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileInfoRow(label = "City", value = city)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileInfoRow(label = "Date of Birth", value = dob)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Records
            Text(
                text = "Records (${documents.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (documents.isEmpty()) {
                Text(text = "No records available for this patient.", fontSize = 14.sp, color = MutedText)
            } else {
                documents.forEach { doc ->
                    RecordRow(
                        document = doc,
                        onView = {
                            scope.launch {
                                val url = viewModel.getDocumentUrl(doc.documentId)
                                if (url != null) {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    )
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = SecondaryText)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkText)
    }
}

@Composable
private fun RecordRow(
    document: VaultDocument,
    onView: () -> Unit
) {
    val fileName = document.fileNames.firstOrNull() ?: "Document"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MutedText, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = fileName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkText)
                Text(text = "Tap to view", fontSize = 12.sp, color = SecondaryText)
            }
            TextButton(onClick = onView) {
                Text(text = "View", color = PrimaryBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}
