package com.medvault.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.medvault.ui.screens.auth.LoginScreen
import com.medvault.ui.screens.auth.RoleSelectScreen
import com.medvault.ui.screens.bloodbank.BloodBankDashboard
import com.medvault.ui.screens.bloodbank.BloodInventoryScreen
import com.medvault.ui.screens.bloodbank.BloodBankProfileScreen
import com.medvault.ui.screens.bloodbank.LogUpdateInventoryScreen
import com.medvault.ui.screens.bloodbank.RequestBloodScreen
import com.medvault.ui.screens.bloodbank.BloodDonationDash1Screen
import com.medvault.ui.screens.bloodbank.BloodDonationDash2Screen
import com.medvault.ui.screens.bloodbank.InventoryStockDetailsScreen
import com.medvault.ui.screens.bloodbank.AIHealthAssistantScreen
import com.medvault.ui.screens.bloodbank.EmergencyEscalationScreen
import com.medvault.ui.screens.doctor.AppointmentQueueScreen
import com.medvault.ui.screens.doctor.DoctorDashboard
import com.medvault.ui.screens.doctor.DoctorProfileScreen
import com.medvault.ui.screens.doctor.DoctorPatientDetailScreen
import com.medvault.ui.screens.doctor.PatientRecordDetailsScreen
import com.medvault.ui.screens.doctor.SlotManagerScreen
import com.medvault.ui.screens.patient.AppointmentDetailsScreen
import com.medvault.ui.screens.patient.BiometricLockScreen
import com.medvault.ui.screens.patient.ConsentManagerScreen
import com.medvault.ui.screens.patient.DoctorSearchScreen
import com.medvault.ui.screens.patient.EmergencyBloodSearchScreen
import com.medvault.ui.screens.patient.HealthVaultScreen
import com.medvault.ui.screens.patient.PatientAppointmentsScreen
import com.medvault.ui.screens.patient.PatientBloodRequestsScreen
import com.medvault.ui.screens.patient.PatientDashboard
import com.medvault.ui.screens.patient.PatientProfileScreen
import com.medvault.ui.screens.patient.ScheduleBloodDonationScreen
import com.medvault.viewmodel.AuthViewModel

@Composable
fun MedVaultNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()

    // Wait until any persisted session has been resolved before deciding the
    // start destination; otherwise a doctor is sent to the patient dashboard
    // because the role hasn't loaded yet.
    if (authState.isRestoringSession) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // The mobile app only onboards patients; doctors and blood banks are
    // created via the web/admin APIs. Patients always have a role, so we go
    // straight to the dashboard (defaulting to PATIENT if missing).
    val startDestination = when {
        !authViewModel.isLoggedIn -> Screen.Login.route
        else -> getDashboardRoute(authState.user?.role)
    }

    AppLockGate {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
        // Auth
        composable(Screen.Login.route) {
            LoginScreen(
                onSignInSuccess = { user ->
                    val role = user.role ?: com.medvault.data.model.UserRole.PATIENT
                    navController.navigate(getDashboardRoute(role)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.RoleSelect.route) {
            RoleSelectScreen(
                onRoleSelected = { role ->
                    navController.navigate(getDashboardRoute(role)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Patient
        composable(Screen.PatientDashboard.route) {
            PatientDashboard(
                onNavigateToProfile = { navController.navigate(Screen.PatientProfile.route) },
                onNavigateToDoctorSearch = { navController.navigate(Screen.DoctorSearch.route) },
                onNavigateToEmergency = { navController.navigate(Screen.EmergencyBloodSearch.route) },
                onNavigateToAppointments = { navController.navigate(Screen.PatientAppointments.route) },
                onNavigateToBloodDonation = { navController.navigate(Screen.BloodDonation.route) },
                onNavigateToAIAssistant = { navController.navigate(Screen.AIHealthAssistant.route) },
                onNavigateToEmergencyEscalation = { navController.navigate(Screen.EmergencyEscalation.route) },
                onNavigateToBloodRequests = { navController.navigate(Screen.PatientBloodRequests.route) }
            )
        }

        composable(Screen.PatientProfile.route) {
            PatientProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAIAssistant = { navController.navigate(Screen.AIHealthAssistant.route) },
                onNavigateToEmergencyEscalation = { navController.navigate(Screen.EmergencyEscalation.route) },
                onNavigateToConsentManager = { navController.navigate(Screen.ConsentManager.route) },
                onNavigateToBiometricLock = { navController.navigate(Screen.BiometricLock.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ConsentManager.route) {
            ConsentManagerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BiometricLock.route) {
            BiometricLockScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DoctorSearch.route) {
            DoctorSearchScreen(
                onBack = { navController.popBackStack() },
                onDoctorSelected = { doctorId ->
                    navController.navigate(Screen.AppointmentDetails.createRoute(doctorId))
                }
            )
        }

        composable(
            route = Screen.AppointmentDetails.route,
            arguments = listOf(navArgument("doctorUid") { type = NavType.StringType })
        ) { backStackEntry ->
            val doctorUid = backStackEntry.arguments?.getString("doctorUid") ?: ""
            AppointmentDetailsScreen(
                doctorUid = doctorUid,
                onBack = { navController.popBackStack() },
                onNavigateToHealthVault = { appointmentId, dUid, dName ->
                    navController.navigate(Screen.HealthVault.createRoute(appointmentId, dUid, dName)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.EmergencyBloodSearch.route) {
            EmergencyBloodSearchScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PatientBloodRequests.route) {
            PatientBloodRequestsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PatientAppointments.route) {
            PatientAppointmentsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToHealthVault = { appointmentId, doctorUid, doctorName ->
                    navController.navigate(Screen.HealthVault.createRoute(appointmentId, doctorUid, doctorName)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Screen.HealthVault.route,
            arguments = listOf(
                navArgument("appointmentId") { type = NavType.StringType },
                navArgument("doctorUid") { type = NavType.StringType },
                navArgument("doctorName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId") ?: ""
            val doctorUid = backStackEntry.arguments?.getString("doctorUid") ?: ""
            val rawName = backStackEntry.arguments?.getString("doctorName")
            val doctorName = rawName?.let { java.net.URLDecoder.decode(it, "UTF-8") }
            HealthVaultScreen(
                appointmentId = appointmentId,
                doctorUid = doctorUid,
                doctorName = doctorName,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BloodDonation.route) {
            ScheduleBloodDonationScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Doctor
        composable(Screen.DoctorDashboard.route) {
            DoctorDashboard(
                onNavigateToProfile = { navController.navigate(Screen.DoctorProfile.route) },
                onNavigateToQueue = { navController.navigate(Screen.AppointmentQueue.route) }
            )
        }

        composable(Screen.DoctorProfile.route) {
            DoctorProfileScreen(
                onBack = { navController.popBackStack() },
                onShiftSchedule = { navController.navigate(Screen.SlotManager.route) },
                onNavigateToQueue = { navController.navigate(Screen.AppointmentQueue.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SlotManager.route) {
            SlotManagerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AppointmentQueue.route) {
            AppointmentQueueScreen(
                onBack = { navController.popBackStack() },
                onPatientSelected = { appointmentId ->
                    navController.navigate(Screen.PatientRecordDetails.createRoute(appointmentId))
                },
                onNavigateToProfile = { navController.navigate(Screen.DoctorProfile.route) }
            )
        }

        composable(
            route = Screen.PatientRecordDetails.route,
            arguments = listOf(navArgument("appointmentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId") ?: ""
            PatientRecordDetailsScreen(
                appointmentId = appointmentId,
                onBack = { navController.popBackStack() },
                onViewFullProfile = { patientId ->
                    navController.navigate(Screen.DoctorPatientDetail.createRoute(patientId))
                }
            )
        }

        composable(
            route = Screen.DoctorPatientDetail.route,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
            DoctorPatientDetailScreen(
                patientId = patientId,
                onBack = { navController.popBackStack() }
            )
        }

        // Blood Bank (all screens share one BloodBankViewModel scoped to this
        // nested graph, so inventory/log updates propagate to the inventory view)
        navigation(startDestination = Screen.BloodBankDashboard.route, route = "bloodbank") {
            composable(Screen.BloodBankDashboard.route) { backStackEntry ->
                val owner = remember(backStackEntry) { navController.getBackStackEntry("bloodbank") }
                BloodBankDashboard(
                    viewModel = hiltViewModel(owner),
                    onNavigateToInventory = { navController.navigate(Screen.BloodInventory.route) },
                    onNavigateToRequests = { },
                    onNavigateToProfile = { navController.navigate(Screen.BloodBankProfile.route) }
                )
            }

            composable(Screen.BloodInventory.route) { backStackEntry ->
                val owner = remember(backStackEntry) { navController.getBackStackEntry("bloodbank") }
                BloodInventoryScreen(
                    viewModel = hiltViewModel(owner),
                    onBack = { navController.popBackStack() },
                    onStockSelected = { bloodGroup ->
                        navController.navigate(Screen.InventoryStockDetails.createRoute(bloodGroup))
                    },
                    onNavigateToRequests = { navController.navigate(Screen.BloodBankDashboard.route) },
                    onNavigateToProfile = { navController.navigate(Screen.BloodBankProfile.route) },
                    onNavigateToLogUpdate = { navController.navigate(Screen.LogUpdateInventory.route) }
                )
            }

            composable(Screen.RequestBlood.route) { backStackEntry ->
                val owner = remember(backStackEntry) { navController.getBackStackEntry("bloodbank") }
                RequestBloodScreen(
                    viewModel = hiltViewModel(owner),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.BloodBankProfile.route) { backStackEntry ->
                val owner = remember(backStackEntry) { navController.getBackStackEntry("bloodbank") }
                BloodBankProfileScreen(
                    viewModel = hiltViewModel(owner),
                    onBack = { navController.popBackStack() },
                    onNavigateToInventory = { navController.navigate(Screen.BloodInventory.route) },
                    onNavigateToRequests = { navController.navigate(Screen.BloodBankDashboard.route) },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.LogUpdateInventory.route) { backStackEntry ->
                val owner = remember(backStackEntry) { navController.getBackStackEntry("bloodbank") }
                LogUpdateInventoryScreen(
                    viewModel = hiltViewModel(owner),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.BloodDonationDash1.route) { backStackEntry ->
                val owner = remember(backStackEntry) { navController.getBackStackEntry("bloodbank") }
                BloodDonationDash1Screen(
                    viewModel = hiltViewModel(owner),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.BloodDonationDash2.route) { backStackEntry ->
                val owner = remember(backStackEntry) { navController.getBackStackEntry("bloodbank") }
                BloodDonationDash2Screen(
                    viewModel = hiltViewModel(owner),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.InventoryStockDetails.route,
                arguments = listOf(navArgument("bloodGroup") { type = NavType.StringType })
            ) { backStackEntry ->
                val owner = remember(backStackEntry) { navController.getBackStackEntry("bloodbank") }
                val bloodGroup = backStackEntry.arguments?.getString("bloodGroup") ?: ""
                InventoryStockDetailsScreen(
                    viewModel = hiltViewModel(owner),
                    bloodGroup = bloodGroup,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.AIHealthAssistant.route) {
            AIHealthAssistantScreen(
                onBack = { navController.popBackStack() },
                onNavigateToEmergencySearch = { navController.navigate(Screen.EmergencyBloodSearch.route) }
            )
        }

        composable(Screen.EmergencyEscalation.route) {
            EmergencyEscalationScreen(
                onBack = { navController.popBackStack() }
            )
        }
        }
    }
}

private fun getDashboardRoute(role: com.medvault.data.model.UserRole?): String {
    return when (role) {
        com.medvault.data.model.UserRole.PATIENT -> Screen.PatientDashboard.route
        com.medvault.data.model.UserRole.DOCTOR -> Screen.DoctorDashboard.route
        // Blood bank screens live in a nested "bloodbank" graph; the root graph
        // start destination must be that graph's route (its own start is "dashboard").
        com.medvault.data.model.UserRole.BLOOD_BANK -> "bloodbank"
        com.medvault.data.model.UserRole.HOSPITAL -> "bloodbank"
        com.medvault.data.model.UserRole.ADMIN -> "bloodbank"
        null -> Screen.PatientDashboard.route
    }
}
