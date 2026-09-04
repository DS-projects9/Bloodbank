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
fun DisclaimerScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medical Disclaimer") },
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
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Important Disclaimer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "MedKeen is NOT a medical device and does NOT provide medical advice.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val sections = listOf(
                "Not Medical Advice" to
                    "The information provided through MedKeen, including AI-generated health assistance, " +
                    "is for informational and administrative purposes only. It is NOT intended to be a " +
                    "substitute for professional medical advice, diagnosis, or treatment.",

                "AI Assistant" to
                    "The AI Health Assistant feature uses artificial intelligence to provide general " +
                    "health information. It is NOT a doctor and cannot diagnose conditions, prescribe " +
                    "medications, or replace a consultation with a qualified healthcare professional. " +
                    "Always consult a licensed physician for medical decisions.",

                "No Doctor-Patient Relationship" to
                    "Use of MedKeen, including consultations facilitated through the platform, does not " +
                    "create a doctor-patient relationship outside of the consultation session. " +
                    "The platform facilitates communication but is not party to the medical relationship.",

                "Emergency Situations" to
                    "MedKeen is NOT designed for emergency medical situations. If you are experiencing " +
                    "a medical emergency, call your local emergency number (108 in India, 911 in the US) " +
                    "or go to the nearest emergency room immediately.",

                "Blood Bank Services" to
                    "Blood availability information displayed through MedKeen is provided by partner " +
                    "blood banks and may change in real time. Always confirm availability directly " +
                    "with the blood bank before visiting.",

                "User Responsibility" to
                    "Users are solely responsible for verifying the accuracy of information and for " +
                    "seeking professional medical advice. MedKeen and its developers assume no " +
                    "liability for decisions made based on information provided through the app."
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
