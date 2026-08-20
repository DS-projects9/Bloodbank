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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class DoctorUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val profile: Map<String, Any>? = null,
    val scheduleConfig: Map<String, Any>? = null,
    val appointments: List<Map<String, Any>> = emptyList(),
    val selectedPatient: Map<String, Any>? = null,
    val selectedAppointment: Map<String, Any>? = null,
    val patientDocuments: List<VaultDocument> = emptyList(),
    val publishedSlotCount: Int = 0
)

@HiltViewModel
class DoctorViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorUiState())
    val uiState: StateFlow<DoctorUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadSchedule()
        loadAppointments()
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

    fun loadSchedule() {
        viewModelScope.launch {
            try {
                val response = apiClient.doctorApi.getMySchedule()
                _uiState.value = _uiState.value.copy(scheduleConfig = response.data)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateSchedule(slots: List<SlotUpdate>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                apiClient.doctorApi.updateMySchedule(UpdateDoctorScheduleRequest(slots = slots))
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadSchedule()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun publishNextWeek(slots: List<SlotUpdate>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val nextMonday = LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(1)
                val response = apiClient.doctorApi.publishNextWeek(
                    PublishNextWeekRequest(weekStart = nextMonday.toString(), slots = slots)
                )
                val count = (response.data?.get("slotsCreated") as? Number)?.toInt() ?: 0
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    publishedSlotCount = count,
                    error = null
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

    fun confirmAppointment(appointmentId: String, diagnosis: String? = null, followUpDate: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                apiClient.appointmentApi.confirmAppointment(
                    ConfirmAppointmentRequest(
                        appointmentId = appointmentId,
                        diagnosis = diagnosis,
                        followUpDate = followUpDate
                    )
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun loadAppointmentDetails(appointmentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiClient.appointmentApi.getAppointment(appointmentId)
                val appointment = response.data
                _uiState.value = _uiState.value.copy(
                    selectedAppointment = appointment,
                    isLoading = false
                )
                val patientId = appointment?.get("patientId") as? String
                if (patientId != null) {
                    loadPatientDocuments(patientId)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun loadPatientDocuments(patientId: String) {
        viewModelScope.launch {
            try {
                val response = apiClient.vaultApi.getDocuments()
                val docs = response.data?.filter { doc ->
                    val ownerUid = doc.ownerUid
                    val sharedWith = doc.sharedWith
                    ownerUid == patientId || sharedWith.contains(patientId)
                } ?: emptyList()
                _uiState.value = _uiState.value.copy(patientDocuments = docs)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadPatient(patientUid: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = apiClient.doctorApi.getPatient(patientUid)
                val data = response.data ?: emptyMap()
                val profile = (data["profile"] as? Map<String, Any>) ?: emptyMap()
                val documents = (data["documents"] as? List<*>)
                    ?.mapNotNull { it as? Map<*, *> }
                    ?.mapNotNull { toVaultDocument(it) }
                    ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedPatient = profile,
                    patientDocuments = documents
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun toVaultDocument(map: Map<*, *>): VaultDocument {
        fun str(key: String): String = (map[key] as? String) ?: ""
        fun strList(key: String): List<String> =
            (map[key] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        return VaultDocument(
            documentId = str("documentId"),
            ownerUid = str("ownerUid"),
            sharedWith = strList("sharedWith"),
            fileNames = strList("fileNames"),
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0,
            expiresAt = (map["expiresAt"] as? Number)?.toLong() ?: 0
        )
    }

    fun updatePatient(
        patientUid: String,
        name: String? = null,
        phone: String? = null,
        bloodGroup: String? = null,
        city: String? = null,
        dob: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                apiClient.doctorApi.updatePatient(
                    patientUid,
                    UpdateProfileRequest(
                        name = name,
                        phone = phone,
                        bloodGroup = bloodGroup,
                        city = city,
                        dob = dob
                    )
                )
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadPatient(patientUid)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    suspend fun getDocumentUrl(documentId: String): String? {
        return try {
            val response = apiClient.vaultApi.getDownloadUrls(documentId)
            response.data?.values?.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }
}
