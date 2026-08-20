package com.medvault.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medvault.data.model.DpdpConsents
import com.medvault.data.model.UserRole
import com.medvault.ui.theme.*
import com.medvault.viewmodel.AuthViewModel

@Composable
fun ConsentGateScreen(
    onConsentComplete: (UserRole) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var selectedRole by remember { mutableStateOf(UserRole.PATIENT) }
    var consents by remember { mutableStateOf(DpdpConsents()) }
    val authState by authViewModel.authState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(White)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(BackgroundPurple),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Complete Setup",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Select Your Role",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Role Selection (self-onboarding is restricted to patients;
            // doctors and blood banks are onboarded by an admin)
            UserRole.entries.filter { it == UserRole.PATIENT }.forEach { role ->
                RoleOption(
                    role = role,
                    selected = selectedRole == role,
                    onClick = { selectedRole = role }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DPDP Consent
            Text(
                text = "Data Protection Consents",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "DPDP Act 2023 Compliance",
                fontSize = 14.sp,
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            ConsentCheckbox(
                title = "Store Health Records",
                description = "Allow MedVault to store your health records securely",
                checked = consents.storeRecords,
                onCheckedChange = { consents = consents.copy(storeRecords = it) }
            )

            ConsentCheckbox(
                title = "Share with Doctor",
                description = "Allow sharing records with your doctors during appointments",
                checked = consents.shareWithDoctor,
                onCheckedChange = { consents = consents.copy(shareWithDoctor = it) }
            )

            ConsentCheckbox(
                title = "AI Processing",
                description = "Allow AI to analyze your health data for insights",
                checked = consents.aiProcessing,
                onCheckedChange = { consents = consents.copy(aiProcessing = it) }
            )

            ConsentCheckbox(
                title = "Blood Network",
                description = "Join the blood donor network for emergency requests",
                checked = consents.bloodNetwork,
                onCheckedChange = { consents = consents.copy(bloodNetwork = it) }
            )

            ConsentCheckbox(
                title = "Emergency Contact",
                description = "Share contact info in emergency situations",
                checked = consents.emergencyContact,
                onCheckedChange = { consents = consents.copy(emergencyContact = it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Continue Button
            Button(
                onClick = {
                    authViewModel.acceptConsents(consents, selectedRole)
                    onConsentComplete(selectedRole)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = White
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = consents.storeRecords && consents.shareWithDoctor
            ) {
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RoleOption(
    role: UserRole,
    selected: Boolean,
    onClick: () -> Unit
) {
    val roleName = when (role) {
        UserRole.PATIENT -> "Patient"
        UserRole.DOCTOR -> "Doctor"
        UserRole.BLOOD_BANK -> "Blood Bank"
        UserRole.HOSPITAL -> "Hospital"
        UserRole.ADMIN -> "Admin"
    }

    val roleDescription = when (role) {
        UserRole.PATIENT -> "Access your health records and book appointments"
        UserRole.DOCTOR -> "Manage patients and appointments"
        UserRole.BLOOD_BANK -> "Manage blood inventory and requests"
        UserRole.HOSPITAL -> "Manage hospital operations"
        UserRole.ADMIN -> "System administration"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) PrimaryBlue.copy(alpha = 0.1f) else White
        ),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (selected) PrimaryBlue else BorderColor
            )
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = PrimaryBlue,
                    unselectedColor = BorderColor
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = roleName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Text(
                    text = roleDescription,
                    fontSize = 12.sp,
                    color = SecondaryText
                )
            }
        }
    }
}

@Composable
private fun ConsentCheckbox(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = PrimaryBlue,
                uncheckedColor = BorderColor
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DarkText
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = SecondaryText
            )
        }
    }
}
