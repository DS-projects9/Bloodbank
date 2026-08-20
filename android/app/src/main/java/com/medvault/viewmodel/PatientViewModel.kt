package com.medvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.medvault.data.remote.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val profile: Map<String, Any>? = null,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val appointments: List<Map<String, Any>> = emptyList(),
    val doctorSearchResults: List<DoctorSearchResult> = emptyList(),
    val availableSlots: List<Map<String, Any>> = emptyList(),
    val bloodDonationBookings: List<Map<String, Any>> = emptyList(),
    val bloodRequests: List<Map<String, Any>> = emptyList(),
    val nearbyBloodStock: List<Map<String, Any>> = emptyList(),
    val bloodBanks: List<Map<String, Any>> = emptyList()
)

@HiltViewModel
class PatientViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientUiState())
    val uiState: StateFlow<PatientUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadAppointments()
        loadBloodDonations()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiClient.userApi.getMe()
                val profile = response.data ?: emptyMap()
                val contactsRaw = profile["emergencyContacts"] as? List<*> ?: emptyList<Any>()
                val contacts = contactsRaw.mapNotNull { item ->
                    val map = item as? Map<*, *> ?: return@mapNotNull null
                    EmergencyContact(
                        name = map["name"] as? String ?: "",
                        phone = map["phone"] as? String ?: "",
                        relationship = map["relationship"] as? String ?: ""
                    )
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = profile,
                    emergencyContacts = contacts
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadAppointments() {
        viewModelScope.launch {
            try {
                val response = apiClient.appointmentApi.getMyAppointments()
                _uiState.value = _uiState.value.copy(appointments = response.data ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun searchDoctors(city: String? = null, specialization: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiClient.doctorApi.searchDoctors(city, specialization)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    doctorSearchResults = response.data ?: emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadDoctorSlots(doctorUid: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiClient.doctorApi.getDoctorSlots(doctorUid)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    availableSlots = response.data ?: emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun bookAppointment(slotId: String, doctorUid: String, patientNote: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                apiClient.appointmentApi.lockSlot(
                    LockSlotRequest(slotId = slotId, doctorUid = doctorUid, patientNote = patientNote)
                )
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadAppointments()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun cancelAppointment(appointmentId: String, reason: String) {
        viewModelScope.launch {
            try {
                apiClient.appointmentApi.cancelAppointment(
                    CancelAppointmentRequest(appointmentId = appointmentId, reason = reason)
                )
                loadAppointments()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateEmergencyContacts(contacts: List<EmergencyContact>) {
        viewModelScope.launch {
            try {
                apiClient.userApi.updateContacts(UpdateContactsRequest(emergencyContacts = contacts))
                _uiState.value = _uiState.value.copy(emergencyContacts = contacts)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadBloodDonations() {
        viewModelScope.launch {
            try {
                val response = apiClient.bloodApi.getMyDonations()
                _uiState.value = _uiState.value.copy(bloodDonationBookings = response.data ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun createBloodRequest(
        patientName: String, bloodGroup: String, units: Int,
        hospitalName: String, hospitalAddress: String, urgency: String, note: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                apiClient.bloodApi.createBloodRequest(
                    BloodRequestCreate(
                        patientName = patientName, bloodGroup = bloodGroup, units = units,
                        hospitalName = hospitalName, hospitalAddress = hospitalAddress,
                        urgency = urgency, note = note
                    )
                )
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun scheduleBloodDonation(
        bloodGroup: String, scheduledDate: String, scheduledTime: String,
        hospitalName: String, hospitalAddress: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                apiClient.bloodApi.createDonorBooking(
                    DonorBookingCreate(
                        bloodGroup = bloodGroup, scheduledDate = scheduledDate,
                        scheduledTime = scheduledTime, hospitalName = hospitalName,
                        hospitalAddress = hospitalAddress
                    )
                )
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadBloodDonations()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadNearbyBloodStock(bloodGroup: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiClient.bloodApi.getNearbyRequests()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    nearbyBloodStock = response.data ?: emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadBloodBanks(bloodGroup: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiClient.bloodApi.searchBloodBanks(bloodGroup)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    bloodBanks = response.data ?: emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun updateProfile(
        name: String? = null,
        phone: String? = null,
        bloodGroup: String? = null,
        city: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                apiClient.userApi.updateMe(
                    UpdateProfileRequest(name = name, phone = phone, bloodGroup = bloodGroup, city = city)
                )
                loadProfile()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun updateConsents(
        dataStorage: Boolean,
        labResults: Boolean,
        bloodDonation: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                apiClient.userApi.updateConsents(
                    UpdateConsentsRequest(
                        dataStorage = dataStorage,
                        labResults = labResults,
                        bloodDonation = bloodDonation
                    )
                )
                loadProfile()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
