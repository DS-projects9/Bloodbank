package com.medvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medvault.data.remote.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
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
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiClient.bloodApi.getMyInventory()
                _uiState.value = _uiState.value.copy(inventory = response.data)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun adjustInventory(
        bloodGroup: String,
        units: Int,
        reason: String,
        expiryDate: String? = null,
        vaultLocation: String? = null,
        collectionDate: String? = null,
        volumePerUnit: Double? = null,
        storageTemp: String? = null,
        notes: String? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                apiClient.bloodApi.adjustInventory(
                    BloodInventoryAdjustRequest(
                        bloodGroup = bloodGroup,
                        units = units,
                        reason = reason,
                        expiryDate = expiryDate,
                        vaultLocation = vaultLocation,
                        collectionDate = collectionDate,
                        volumePerUnit = volumePerUnit,
                        storageTemp = storageTemp,
                        notes = notes,
                    )
                )
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadInventory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadBloodRequests() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiClient.bloodApi.getNearbyRequests()
                _uiState.value = _uiState.value.copy(bloodRequests = response.data ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun fulfillBloodRequest(requestId: String, units: Int) {
        viewModelScope.launch(Dispatchers.IO) {
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

    fun declineBloodRequest(requestId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiClient.bloodApi.declineBloodRequest(BloodRequestCancelRequest(requestId = requestId))
                loadBloodRequests()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadDonorBookings() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiClient.bloodApi.getUpcomingDonorBookings()
                _uiState.value = _uiState.value.copy(donorBookings = response.data ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadUpcomingDonations() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiClient.bloodApi.getUpcomingDonations()
                _uiState.value = _uiState.value.copy(upcomingDonations = response.data ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun cancelDonorBooking(bookingId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiClient.bloodApi.cancelDonorBooking(DonorBookingCancelRequest(bookingId = bookingId))
                loadDonorBookings()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun checkInDonorBooking(bookingId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiClient.bloodApi.checkInDonorBooking(DonorBookingCancelRequest(bookingId = bookingId))
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
        viewModelScope.launch(Dispatchers.IO) {
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
