package com.medkeen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medkeen.data.remote.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
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
    val publishedSlotCount: Int = 0,
    val slots: List<Map<String, Any>> = emptyList(),
    val clinicOpen: Boolean = true,
    val timerViewedAt: Long = 0,
    val timerExpiresAt: Long = 0,
    val timerDurationSeconds: Long = 0,
)

@HiltViewModel
class DoctorViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorUiState())
    val uiState: StateFlow<DoctorUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadSchedule()
        loadMySlots()
        loadStatus()
        loadAppointments()
    }

    fun loadStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiClient.doctorApi.getMyStatus()
                }
                val open = (response.data?.get("open") as? Boolean) ?: true
                _uiState.value = _uiState.value.copy(clinicOpen = open)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun setStatus(open: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiClient.doctorApi.setMyStatus(mapOf("open" to open))
                _uiState.value = _uiState.value.copy(clinicOpen = open)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
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

    fun loadSchedule() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiClient.doctorApi.getMySchedule()
                }
                _uiState.value = _uiState.value.copy(scheduleConfig = response.data)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadMySlots() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiClient.doctorApi.getMySlots()
                }
                _uiState.value = _uiState.value.copy(slots = response.data ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteSlot(slotId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                apiClient.doctorApi.deleteSlot(slotId)
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadMySlots()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun publishSlots(slots: List<SlotUpdate>, weekStart: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val startMs = System.currentTimeMillis()
            try {
                val count = withContext(Dispatchers.IO) {
                    apiClient.doctorApi.updateMySchedule(UpdateDoctorScheduleRequest(slots = slots))
                    val response = apiClient.doctorApi.publishNextWeek(
                        PublishNextWeekRequest(weekStart = weekStart, slots = slots)
                    )
                    (response.data?.get("slotsCreated") as? Number)?.toInt() ?: 0
                }
                val elapsed = System.currentTimeMillis() - startMs
                if (elapsed < 800) delay(800 - elapsed)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    publishedSlotCount = count,
                    error = null
                )
                loadSchedule()
                loadMySlots()
            } catch (e: Exception) {
                android.util.Log.e("DoctorViewModel", "Error in publishSlots", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadAppointments() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiClient.appointmentApi.getMyAppointments()
                }
                _uiState.value = _uiState.value.copy(appointments = response.data ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun confirmAppointment(appointmentId: String, diagnosis: String? = null, followUpDate: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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

    fun completeAppointment(appointmentId: String, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                apiClient.appointmentApi.completeAppointment(appointmentId)
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadAppointments()
                withContext(Dispatchers.Main) { onComplete(true, null) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                withContext(Dispatchers.Main) { onComplete(false, e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun loadAppointmentDetails(appointmentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiClient.appointmentApi.getAppointment(appointmentId)
                val appointment = response.data
                _uiState.value = _uiState.value.copy(
                    selectedAppointment = appointment,
                    isLoading = false
                )
                loadSharedFiles(appointmentId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadSharedFiles(appointmentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiClient.appointmentApi.getSharedFiles(appointmentId)
                val docs = response.data?.mapNotNull { toVaultDocument(it) } ?: emptyList()
                _uiState.value = _uiState.value.copy(patientDocuments = docs)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    suspend fun openDocument(documentId: String): Boolean {
        return try {
            val response = apiClient.vaultApi.openDocument(documentId)
            val data = response.data ?: emptyMap()
            val viewedAt = (data["viewedAt"] as? Number)?.toLong() ?: 0L
            val expiresAt = (data["expiresAt"] as? Number)?.toLong() ?: 0L
            val durationSec = (data["durationMinutes"] as? Number)?.toLong()?.times(60) ?: 0L
            _uiState.value = _uiState.value.copy(
                timerViewedAt = viewedAt,
                timerExpiresAt = expiresAt,
                timerDurationSeconds = durationSec,
            )
            true
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
            false
        }
    }

    private fun loadPatientDocuments(patientId: String) {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
        fun wrappedMap(key: String): Map<String, String> =
            (map[key] as? Map<*, *>)?.mapNotNull { (k, v) ->
                (k as? String)?.let { kk -> (v as? String)?.let { vv -> kk to vv } }
            }?.toMap() ?: emptyMap()
        return VaultDocument(
            documentId = str("documentId"),
            ownerUid = str("ownerUid"),
            sharedWith = strList("sharedWith"),
            fileNames = strList("fileNames"),
            appointmentId = str("appointmentId").ifEmpty { null },
            status = str("status").ifEmpty { "active" },
            durationMinutes = (map["durationMinutes"] as? Number)?.toLong() ?: 0,
            viewedAt = (map["viewedAt"] as? Number)?.toLong() ?: 0,
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0,
            expiresAt = (map["expiresAt"] as? Number)?.toLong() ?: 0,
            wrappedKeys = wrappedMap("wrappedKeys")
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
        viewModelScope.launch(Dispatchers.IO) {
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
