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

data class BloodBankUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val profile: Map<String, Any>? = null,
    val inventory: Map<String, Any>? = null,
    val bloodRequests: List<Map<String, Any>> = emptyList(),
    val donorBookings: List<Map<String, Any>> = emptyList(),
    val upcomingDonations: List<Map<String, Any>> = emptyList(),
    val myBloodRequests: List<Map<String, Any>> = emptyList()
)

@HiltViewModel
class BloodBankViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(BloodBankUiState())
    val uiState: StateFlow<BloodBankUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadInventory()
        loadBloodRequests()
        loadDonorBookings()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiClient.userApi.getMe()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = response.data
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadInventory() {
        viewModelScope.launch {
            try {
                val response = apiClient.bloodApi.getMyInventory()
                _uiState.value = _uiState.value.copy(inventory = response.data)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun adjustInventory(bloodGroup: String, units: Int, reason: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                apiClient.bloodApi.adjustInventory(
                    BloodInventoryAdjustRequest(bloodGroup = bloodGroup, units = units, reason = reason)
                )
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadInventory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadBloodRequests() {
        viewModelScope.launch {
            try {
                val response = apiClient.bloodApi.getNearbyRequests()
                _uiState.value = _uiState.value.copy(bloodRequests = response.data ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun fulfillBloodRequest(requestId: String, units: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                apiClient.bloodApi.fulfillBloodRequest(
                    BloodFulfillRequest(requestId = requestId, units = units)
                )
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadBloodRequests()
                loadInventory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadDonorBookings() {
        viewModelScope.launch {
            try {
                val response = apiClient.bloodApi.getUpcomingDonorBookings()
                _uiState.value = _uiState.value.copy(donorBookings = response.data ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadUpcomingDonations() {
        viewModelScope.launch {
            try {
                val response = apiClient.bloodApi.getUpcomingDonations()
                _uiState.value = _uiState.value.copy(upcomingDonations = response.data ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun cancelDonorBooking(bookingId: String) {
        viewModelScope.launch {
            try {
                apiClient.bloodApi.cancelDonorBooking(mapOf("bookingId" to bookingId))
                loadDonorBookings()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun createBloodRequest(
        patientName: String, bloodGroup: String, units: Int,
        hospitalName: String, hospitalAddress: String, urgency: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                apiClient.bloodApi.createBloodRequest(
                    BloodRequestCreate(
                        patientName = patientName, bloodGroup = bloodGroup, units = units,
                        hospitalName = hospitalName, hospitalAddress = hospitalAddress,
                        urgency = urgency
                    )
                )
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadBloodRequests()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
