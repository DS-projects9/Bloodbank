package com.medkeen.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medkeen.data.model.UserRole
import com.medkeen.ui.theme.*

@Composable
fun RoleSelectScreen(
    onRoleSelected: (UserRole) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(BackgroundPurple),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select Role",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choose Your Role",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Spacer(modifier = Modifier.height(32.dp))

            UserRole.entries.filter { it == UserRole.PATIENT }.forEach { role ->
                val roleName = when (role) {
                    UserRole.PATIENT -> "Patient"
                    UserRole.DOCTOR -> "Doctor"
                    UserRole.BLOOD_BANK -> "Blood Bank"
                    UserRole.HOSPITAL -> "Hospital"
                    UserRole.ADMIN -> "Admin"
                }

                Button(
                    onClick = { onRoleSelected(role) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = roleName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
