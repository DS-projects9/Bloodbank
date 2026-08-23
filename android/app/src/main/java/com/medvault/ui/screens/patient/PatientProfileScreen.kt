package com.medvault.ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medvault.ui.theme.*
import com.medvault.viewmodel.AuthViewModel
import com.medvault.viewmodel.PatientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientProfileScreen(
    onBack: () -> Unit,
    onNavigateToAIAssistant: () -> Unit = {},
    onNavigateToEmergencyEscalation: () -> Unit = {},
    onNavigateToConsentManager: () -> Unit = {},
    onNavigateToBiometricLock: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: PatientViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile

    val email = profile?.get("email") as? String ?: ""
    val name = run {
        val n = (profile?.get("name") ?: profile?.get("displayName")) as? String
        if (!n.isNullOrBlank()) n
        else email.substringBefore("@").replaceFirstChar { it.uppercase() }.takeIf { it.isNotBlank() } ?: "User"
    }
    val bloodGroup = profile?.get("bloodGroup") as? String ?: "-"
    val phone = profile?.get("phone") as? String ?: "-"
    val city = profile?.get("city") as? String ?: "-"
    val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")

    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(name) }
    var editPhone by remember { mutableStateOf(phone) }
    var editBloodGroup by remember { mutableStateOf(bloodGroup) }
    var editCity by remember { mutableStateOf(city) }

    var showDeleteOptions by remember { mutableStateOf(false) }
    var deleteChoice by remember { mutableStateOf<DeleteChoice?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (isEditing) {
                            isEditing = false
                        } else {
                            editName = name
                            editPhone = phone
                            editBloodGroup = bloodGroup
                            editCity = city
                            isEditing = true
                        }
                    }) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Cancel" else "Edit"
                        )
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
            Spacer(modifier = Modifier.height(24.dp))

            // Profile Header
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Text(
                        text = email,
                        fontSize = 14.sp,
                        color = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Profile Info
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(onClick = {
                                viewModel.updateProfile(
                                    name = editName.takeIf { it.isNotBlank() },
                                    phone = editPhone.takeIf { it.isNotBlank() },
                                    bloodGroup = editBloodGroup.takeIf { it.isNotBlank() },
                                    city = editCity.takeIf { it.isNotBlank() }
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI & Emergency
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI & Emergency",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsItem(
                        icon = Icons.Default.SmartToy,
                        title = "AI Health Assistant",
                        onClick = onNavigateToAIAssistant
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingsItem(
                        icon = Icons.Default.Warning,
                        title = "Emergency Escalation",
                        onClick = onNavigateToEmergencyEscalation
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsItem(
                        icon = Icons.Default.Lock,
                        title = "Biometric Lock",
                        onClick = onNavigateToBiometricLock
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "Consent Manager",
                        onClick = onNavigateToConsentManager
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data & Privacy
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data & Privacy",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = "Delete Account",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { showDeleteOptions = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    authViewModel.signOut()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Step 1: choose what to delete
    if (showDeleteOptions) {
        AlertDialog(
            onDismissRequest = { showDeleteOptions = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete account data") },
            text = {
                Text(
                    "We're sorry to see you go. Choose what you'd like to remove. " +
                        "You can delete only the medical reports we've stored, or erase your entire account."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    deleteChoice = DeleteChoice.Reports
                    showDeleteOptions = false
                }) {
                    Text("Delete my reports")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    deleteChoice = DeleteChoice.Account
                    showDeleteOptions = false
                }) {
                    Text("Delete my account", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // Step 2: confirm the chosen action
    deleteChoice?.let { choice ->
        val isAccount = choice == DeleteChoice.Account
        AlertDialog(
            onDismissRequest = { if (!isDeleting) deleteChoice = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(if (isAccount) "Delete entire account?" else "Delete all reports?") },
            text = {
                Text(
                    if (isAccount)
                        "This permanently deletes your account, profile, all stored reports and every related record. This cannot be undone."
                    else
                        "This permanently deletes all medical reports we've stored for you. Your account stays active. This cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        deleteError = null
                        if (isAccount) {
                            viewModel.deleteAccount { ok, err ->
                                isDeleting = false
                                if (ok) {
                                    deleteChoice = null
                                    authViewModel.signOut()
                                    onLogout()
                                } else {
                                    deleteError = err ?: "Failed to delete account"
                                }
                            }
                        } else {
                            viewModel.deleteReports { ok, err ->
                                isDeleting = false
                                if (ok) {
                                    deleteChoice = null
                                } else {
                                    deleteError = err ?: "Failed to delete reports"
                                }
                            }
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (isAccount) "Delete account" else "Delete reports")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteChoice = null }, enabled = !isDeleting) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error feedback
    deleteError?.let { message ->
        AlertDialog(
            onDismissRequest = { deleteError = null },
            title = { Text("Something went wrong") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { deleteError = null }) {
                    Text("OK")
                }
            }
        )
    }
}

private enum class DeleteChoice { Reports, Account }

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = SecondaryText
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = DarkText
        )
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    titleColor: androidx.compose.ui.graphics.Color = DarkText,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SecondaryText,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = titleColor
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = BorderColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
