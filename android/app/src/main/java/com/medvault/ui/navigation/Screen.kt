package com.medvault.ui.navigation

sealed class Screen(val route: String) {
    // Auth
    data object Login : Screen("login")
    data object ConsentGate : Screen("consent_gate")
    data object RoleSelect : Screen("role_select")

    // Patient
    data object PatientDashboard : Screen("patient/dashboard")
    data object PatientProfile : Screen("patient/profile")
    data object DoctorSearch : Screen("patient/doctor_search")
    data object AppointmentDetails : Screen("patient/appointment/{doctorUid}") {
        fun createRoute(doctorUid: String) = "patient/appointment/$doctorUid"
    }
    data object EmergencyBloodSearch : Screen("patient/emergency_blood")
    data object HealthVault : Screen("patient/health_vault")
    data object PatientAppointments : Screen("patient/appointments")
    data object BloodDonation : Screen("patient/blood_donation")
    data object PatientSettings : Screen("patient/settings")
    data object ConsentManager : Screen("patient/consent_manager")
    data object BiometricLock : Screen("patient/biometric_lock")

    // Doctor
    data object DoctorDashboard : Screen("doctor/dashboard")
    data object DoctorProfile : Screen("doctor/profile")
    data object AppointmentQueue : Screen("doctor/appointment_queue")
    data object PatientRecordDetails : Screen("doctor/patient_record/{appointmentId}") {
        fun createRoute(appointmentId: String) = "doctor/patient_record/$appointmentId"
    }
    data object DoctorPatientDetail : Screen("doctor/patient_detail/{patientId}") {
        fun createRoute(patientId: String) = "doctor/patient_detail/$patientId"
    }
    data object SlotManager : Screen("doctor/slot_manager")

    // Blood Bank
    data object BloodBankDashboard : Screen("bloodbank/dashboard")
    data object BloodInventory : Screen("bloodbank/inventory")
    data object InventoryStockDetails : Screen("bloodbank/stock_details/{bloodGroup}") {
        fun createRoute(bloodGroup: String) = "bloodbank/stock_details/$bloodGroup"
    }
    data object BloodDonationDash1 : Screen("bloodbank/donation_dash1")
    data object BloodDonationDash2 : Screen("bloodbank/donation_dash2")
    data object RequestBlood : Screen("bloodbank/request_blood")
    data object BloodBankProfile : Screen("bloodbank/profile")
    data object LogUpdateInventory : Screen("bloodbank/log_update_inventory")
    data object AIHealthAssistant : Screen("ai_assistant")
    data object EmergencyEscalation : Screen("emergency_escalation")
}
