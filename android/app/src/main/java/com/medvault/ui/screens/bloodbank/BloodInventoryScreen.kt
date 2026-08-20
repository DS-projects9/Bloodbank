package com.medvault.ui.screens.bloodbank

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodInventoryScreen(
    onBack: () -> Unit,
    onStockSelected: (String) -> Unit,
    onNavigateToRequests: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLogUpdate: () -> Unit = {},
    viewModel: BloodBankViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val allBloodGroups = listOf("O-", "O+", "AB-", "A+", "B+", "A-", "B-", "AB+")

    val inventoryData = remember(uiState.inventory) {
        val rawUnits = uiState.inventory?.get("bloodGroupUnits") as? Map<*, *> ?: emptyMap<String, Any>()
        allBloodGroups.map { group ->
            val units = (rawUnits[group] as? Number)?.toInt() ?: 0
            val status = when {
                units < 5 -> "CRITICAL LOW"
                units < 10 -> "LOW STOCK"
                else -> "STABLE"
            }
            Triple(group, units, status)
        }
    }

    val totalUnits = remember(inventoryData) { inventoryData.sumOf { it.second } }
    val totalCapacity = 200
    val progress = if (totalCapacity > 0) totalUnits.toFloat() / totalCapacity else 0f

    var selectedFilter by remember { mutableIntStateOf(0) }
    val lowStockCount = inventoryData.count { it.second < 10 }
    val nearExpiryCount = inventoryData.count { it.third != "STABLE" }
    val filters = listOf("All Groups (${allBloodGroups.size})", "Low Stock ($lowStockCount)", "Near Expiry ($nearExpiryCount)")

    val filteredData = when (selectedFilter) {
        1 -> inventoryData.filter { it.second < 10 }
        2 -> inventoryData.filter { it.third != "STABLE" }
        else -> inventoryData
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = EmergencyRed,
                modifier = Modifier.size(48.dp)
            )
        }
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = White,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = "Inventory") },
                    label = { Text("Inventory") },
                    selected = true,
                    onClick = { },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmergencyRed,
                        selectedTextColor = EmergencyRed,
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Recent Requests") },
                    label = { Text("Recent Requests") },
                    selected = false,
                    onClick = { onNavigateToRequests() },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmergencyRed,
                        selectedTextColor = EmergencyRed,
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = { onNavigateToProfile() },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmergencyRed,
                        selectedTextColor = EmergencyRed,
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText
                    )
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
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = EmergencyRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Blood Inventory",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
            }

            // Capacity Usage Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Total Capacity Usage",
                                fontSize = 14.sp,
                                color = DarkText
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$totalUnits",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmergencyRed
                                )
                                Text(
                                    text = " / $totalCapacity Units",
                                    fontSize = 14.sp,
                                    color = DarkText
                                )
                            }
                        }
                        Text(
                            text = "(${String.format("%.1f", totalUnits.toDouble() * 0.5)} Liters)",
                            fontSize = 13.sp,
                            color = MutedText
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = EmergencyRed,
                        trackColor = EmergencyRed.copy(alpha = 0.15f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Last Updated: Today \u2022 08:15 AM by Admin",
                        fontSize = 12.sp,
                        color = MutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEachIndexed { index, label ->
                    val isSelected = selectedFilter == index
                    Surface(
                        modifier = Modifier.clickable { selectedFilter = index },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) EmergencyRed else White,
                        border = BorderStroke(1.dp, if (isSelected) EmergencyRed else BorderColor)
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) White else DarkText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Blood group grid
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredData.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { (group, units, status) ->
                            val isCriticalLow = status == "CRITICAL LOW"
                            val isLowStock = status == "LOW STOCK"
                            InventoryBloodCard(
                                group = group,
                                units = units,
                                liters = units.toDouble() * 0.5,
                                status = status,
                                isCritical = isCriticalLow,
                                isLow = isLowStock,
                                onClick = { onStockSelected(group) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Update Stock button
            Button(
                onClick = { onNavigateToLogUpdate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log / Update Inventory Stock",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InventoryBloodCard(
    group: String,
    units: Int,
    liters: Double,
    status: String,
    isCritical: Boolean,
    isLow: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isCritical -> EmergencyRed
        isLow -> WarningOrange
        else -> BorderColor
    }
    val statusColor = when {
        isCritical -> EmergencyRed
        isLow -> WarningOrange
        else -> SuccessGreen
    }

    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCritical) EmergencyRed else DarkText
                )
                Icon(
                    Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = if (isCritical) EmergencyRed else MutedText,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$units Units",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Text(
                text = "(${String.format("%.1f", liters)} L)",
                fontSize = 12.sp,
                color = MutedText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = statusColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}
