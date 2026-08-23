package com.medvault.ui.screens.bloodbank

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medvault.ui.theme.*
import com.medvault.viewmodel.BloodBankViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryStockDetailsScreen(
    bloodGroup: String,
    onBack: () -> Unit,
    viewModel: BloodBankViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val bloodGroupUnits = remember(uiState.inventory) {
        val units = uiState.inventory?.get("bloodGroupUnits") as? Map<*, *> ?: emptyMap<String, Any>()
        (units[bloodGroup] as? Number)?.toInt() ?: 0
    }
    val volumePerUnit = remember(uiState.inventory) {
        (uiState.inventory?.get("volumePerUnit") as? Number)?.toDouble() ?: 0.5
    }
    val totalVolumeLiters = remember(bloodGroupUnits) { bloodGroupUnits * volumePerUnit }
    val isCriticalLow = bloodGroupUnits <= 2

    val storageTemp = remember(uiState.inventory) {
        uiState.inventory?.get("storageTemp") as? String ?: "4.0"
    }

    val polarity = if (bloodGroup.contains("-")) "Negative" else "Positive"

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
        return
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(White)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkText)
                }
                Text(
                    text = "$bloodGroup $polarity Stock Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.5.dp, EmergencyRed.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmergencyRed
                        ) {
                            Text(
                                text = "$bloodGroup ${polarity.uppercase()}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isCriticalLow) EmergencyRed.copy(alpha = 0.1f) else SuccessGreen.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = if (isCriticalLow) "CRITICAL LOW" else "STOCK OK",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCriticalLow) EmergencyRed else SuccessGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Volume",
                                fontSize = 12.sp,
                                color = MutedText
                            )
                            Text(
                                text = "${"%.1f".format(totalVolumeLiters)} Liters",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Units Count",
                                fontSize = 12.sp,
                                color = MutedText
                            )
                            Text(
                                text = "$bloodGroupUnits Units",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCriticalLow) EmergencyRed else DarkText
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Storage Temp",
                                fontSize = 12.sp,
                                color = MutedText
                            )
                            Text(
                                text = "${storageTemp}° C",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Active Stock Batches
            Text(
                text = "Active Stock Batches ($bloodGroupUnits)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (bloodGroupUnits > 0) {
                BatchCard(
                    batchId = uiState.inventory?.get("batchId") as? String ?: "#INV-${bloodGroup.replace("+", "P").replace("-", "N")}",
                    expiryBadge = if (bloodGroupUnits <= 2) "Low Stock" else "In Stock",
                    expiryBadgeColor = if (bloodGroupUnits <= 2) WarningOrange else SuccessGreen,
                    collectionDate = uiState.inventory?.get("collectionDate") as? String
                        ?: dateFormat.format(Date(uiState.inventory?.get("lastUpdated") as? Long ?: System.currentTimeMillis())),
                    expiryDate = uiState.inventory?.get("expiryDate") as? String ?: "N/A",
                    volume = "$bloodGroupUnits Units (${bloodGroupUnits * (volumePerUnit * 1000).toInt()} ml)",
                    vaultLocation = uiState.inventory?.get("vaultLocation") as? String ?: "Not specified"
                )
            } else {
                Text(
                    text = "No active batches for $bloodGroup",
                    fontSize = 14.sp,
                    color = MutedText
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stock Audit History
            Text(
                text = "Stock Audit History",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Spacer(modifier = Modifier.height(12.dp))

            AuditHistoryItem(
                icon = Icons.Default.TrendingUp,
                iconColor = PrimaryBlue,
                title = "Current stock: $bloodGroupUnits Units available",
                titleColor = DarkText,
                subtitle = "Last updated ${dateFormat.format(Date(uiState.inventory?.get("lastUpdated") as? Long ?: System.currentTimeMillis()))}"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BatchCard(
    batchId: String,
    expiryBadge: String,
    expiryBadgeColor: Color,
    collectionDate: String,
    expiryDate: String,
    volume: String,
    vaultLocation: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Batch $batchId",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = expiryBadgeColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = expiryBadge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = expiryBadgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Collection Date", fontSize = 11.sp, color = MutedText)
                    Text(text = collectionDate, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkText)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Expiry Date", fontSize = 11.sp, color = MutedText)
                    Text(text = expiryDate, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkText)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Volume / Unit", fontSize = 11.sp, color = MutedText)
                    Text(text = volume, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkText)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Vault Location", fontSize = 11.sp, color = MutedText)
                    Text(text = vaultLocation, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkText)
                }
            }
        }
    }
}

@Composable
private fun AuditHistoryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    titleColor: Color,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = iconColor.copy(alpha = 0.1f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = titleColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MutedText
                )
            }
        }
    }
}
