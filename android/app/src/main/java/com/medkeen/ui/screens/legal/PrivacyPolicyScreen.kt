package com.medkeen.ui.screens.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "MedKeen Privacy Policy",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Effective date: August 2026",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            val sections = listOf(
                "1. Information We Collect" to
                    "MedKeen collects only the information you provide directly, including:\n" +
                    "- Account details (email, name, phone)\n" +
                    "- Medical documents you upload to your health vault\n" +
                    "- Appointment and consultation records\n" +
                    "- Blood bank inventory data (for blood bank users)\n\n" +
                    "We do not collect location data, contacts, or any data from third parties.",

                "2. How We Use Your Information" to
                    "Your information is used solely to provide the MedKeen services:\n" +
                    "- Facilitating doctor-patient consultations\n" +
                    "- Managing your medical document vault\n" +
                    "- Enabling blood bank operations\n" +
                    "- Sending appointment notifications",

                "3. Data Storage & Security" to
                    "All data is stored in encrypted databases and object storage on secure servers. " +
                    "Documents in your health vault are access-controlled and time-limited. " +
                    "Passwords are hashed using bcrypt. JWT tokens expire after 24 hours. " +
                    "Biometric authentication uses your device's secure hardware (StrongBox/TEE).",

                "4. Data Sharing" to
                    "MedKeen does not sell, rent, or share your personal data with third parties. " +
                    "Your medical documents are shared only with healthcare providers you explicitly authorize through the vault sharing mechanism.",

                "5. Data Retention" to
                    "Your data is retained as long as your account is active. " +
                    "When you delete your account, all personal data, documents, and records are permanently deleted from our systems within 30 days.",

                "6. Your Rights" to
                    "You have the right to:\n" +
                    "- Access all data we hold about you\n" +
                    "- Export your data\n" +
                    "- Delete your account and all associated data\n" +
                    "- Withdraw consent for data processing\n\n" +
                    "Exercise these rights from your Profile screen in the app.",

                "7. Children's Privacy" to
                    "MedKeen is not intended for users under 18 years of age. We do not knowingly collect data from children.",

                "8. Changes to This Policy" to
                    "We may update this policy from time to time. Continued use of MedKeen after changes constitutes acceptance of the updated policy.",

                "9. Contact" to
                    "For privacy-related inquiries, contact us at privacy@medkeen.dev"
            )

            sections.forEach { (title, body) ->
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = body, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
