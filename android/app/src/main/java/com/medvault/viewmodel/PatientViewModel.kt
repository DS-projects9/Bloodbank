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
import kotlinx.coroutines.withContext
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
    val bloodBanks: List<Map<String, Any>> = emptyList(),
    val doctorProfiles: Map<String, DoctorSearchResult> = emptyMap(),
    val lastAppointmentId: String? = null,
    val myDocuments: List<VaultDocument> = emptyList(),
    val sharedFiles: List<SharedFile> = emptyList(),
    val shareStatus: String? = null,
    val uploadError: String? = null
)

data class SharedFile(
    val documentId: String,
    val fileNames: List<String> = emptyList(),
    val expiresAt: Long = 0,
    val status: String = "active",
    val durationMinutes: Long = 0,
)

@HiltViewModel
class PatientViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientUiState())
    val uiState: StateFlow<PatientUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadAppointments()
        loadBloodDonations()
    }

    fun loadProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiClient.userApi.getMe()
                val profile = response.data ?: emptyMap()
                // Self-heal the cached session: older snapshots were saved
                // without uid, which breaks "my documents" filtering.
                (profile["uid"] as? String)?.takeIf { it.isNotBlank() }?.let { realUid ->
                    com.medvault.data.remote.TokenManager.loadUser()?.takeIf { it.uid != realUid }?.let {
                        com.medvault.data.remote.TokenManager.saveUser(it.copy(uid = realUid))
                    }
                }
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiClient.appointmentApi.getMyAppointments()
                val appts = response.data ?: emptyList()
                _uiState.value = _uiState.value.copy(appointments = appts)
                val uids = appts.mapNotNull {
                    (it["doctorUid"] as? String)?.takeIf { u -> u.isNotBlank() }
                }.toSet()
                val profiles = mutableMapOf<String, DoctorSearchResult>()
                uids.forEach { uid ->
                    try {
                        apiClient.doctorApi.getDoctor(uid).data?.let { profiles[uid] = it }
                    } catch (_: Exception) {
                    }
                }
                _uiState.value = _uiState.value.copy(doctorProfiles = profiles)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun searchDoctors(city: String? = null, specialization: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, shareStatus = null)
            try {
                val response = apiClient.appointmentApi.lockSlot(
                    LockSlotRequest(slotId = slotId, doctorUid = doctorUid, patientNote = patientNote)
                )
                val appointmentId = (response.data?.get("appointmentId") as? String)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastAppointmentId = appointmentId
                )
                loadAppointments()
                loadMyDocuments()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadMyDocuments() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiClient.vaultApi.getDocuments()
                val myUid = com.medvault.data.remote.TokenManager.loadUser()?.uid
                val docs = (response.data ?: emptyList()).filter {
                    (it.ownerUid) == myUid && it.appointmentId.isNullOrBlank()
                }
                _uiState.value = _uiState.value.copy(myDocuments = docs)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(uploadError = e.message)
            }
        }
    }

    fun loadDoctorProfile(doctorUid: String) {
        if (doctorUid.isBlank() || _uiState.value.doctorProfiles.containsKey(doctorUid)) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiClient.doctorApi.getDoctor(doctorUid).data?.let { profile ->
                    _uiState.value = _uiState.value.copy(
                        doctorProfiles = _uiState.value.doctorProfiles + (doctorUid to profile)
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun uploadDocument(fileName: String, contentType: String, bytes: ByteArray) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(uploadError = null)
            try {
                val init = apiClient.vaultApi.initUpload(
                    UploadInitRequest(fileName = fileName, contentType = contentType)
                )
                val uploadUrl = (init.data?.get("uploadUrl") as? String)
                    ?: throw IllegalStateException("No upload URL")
                uploadToUrl(uploadUrl, bytes, contentType)
                loadMyDocuments()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(uploadError = e.message)
            }
        }
    }

    private fun uploadToUrl(url: String, bytes: ByteArray, contentType: String) {
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        try {
            connection.requestMethod = "PUT"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", contentType)
            // Force Content-Length instead of chunked Transfer-Encoding, which
            // MinIO rejects (unsigned header -> 403). Required for non-trivial files.
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.outputStream.use { it.write(bytes) }
            val code = connection.responseCode
            if (code !in 200..299) {
                val err = runCatching { connection.errorStream?.bufferedReader()?.readText() }.getOrNull()
                throw IllegalStateException("Upload failed with code $code: $err")
            }
        } finally {
            connection.disconnect()
        }
    }

    fun shareFilesWithDoctor(appointmentId: String, fileNames: List<String>, durationMinutes: Int = 60) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(uploadError = null)
            try {
                apiClient.appointmentApi.shareFiles(
                    appointmentId,
                    ShareFilesRequest(fileNames = fileNames, durationMinutes = durationMinutes)
                )
                _uiState.value = _uiState.value.copy(shareStatus = "shared")
                loadShares(appointmentId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(uploadError = e.message)
            }
        }
    }

    fun loadShares(appointmentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiClient.appointmentApi.getShares(appointmentId)
                val shares = response.data?.mapNotNull { toSharedFile(it) } ?: emptyList()
                _uiState.value = _uiState.value.copy(sharedFiles = shares)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(uploadError = e.message)
            }
        }
    }

    fun revokeShare(appointmentId: String, documentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(uploadError = null)
            try {
                apiClient.appointmentApi.revokeShare(appointmentId, documentId)
                loadShares(appointmentId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(uploadError = e.message)
            }
        }
    }

    fun extendShare(appointmentId: String, documentId: String, durationMinutes: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(uploadError = null)
            try {
                apiClient.appointmentApi.extendShare(
                    appointmentId,
                    documentId,
                    ExtendAccessRequest(durationMinutes = durationMinutes)
                )
                loadShares(appointmentId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(uploadError = e.message)
            }
        }
    }

    private fun toSharedFile(map: Map<*, *>): SharedFile {
        fun str(key: String): String = (map[key] as? String) ?: ""
        fun strList(key: String): List<String> =
            (map[key] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        fun num(key: String): Long = (map[key] as? Number)?.toLong() ?: 0
        return SharedFile(
            documentId = str("documentId"),
            fileNames = strList("fileNames"),
            expiresAt = num("expiresAt"),
            status = str("status").ifEmpty { "active" },
            durationMinutes = num("durationMinutes"),
        )
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

    fun updateEmergencyContacts(contacts: List<EmergencyContact>, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiClient.userApi.updateContacts(UpdateContactsRequest(emergencyContacts = contacts))
                _uiState.value = _uiState.value.copy(emergencyContacts = contacts)
                onComplete(true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
                onComplete(false)
            }
        }
    }

    fun loadBloodDonations() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiClient.bloodApi.getMyDonations()
                _uiState.value = _uiState.value.copy(bloodDonationBookings = response.data ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadMyBloodRequests() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiClient.bloodApi.getMyBloodRequests()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    bloodRequests = response.data ?: emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun cancelBloodRequest(requestId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiClient.bloodApi.cancelBloodRequest(BloodRequestCancelRequest(requestId = requestId))
                loadMyBloodRequests()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun createBloodRequest(
        patientName: String, bloodGroup: String, units: Int,
        hospitalName: String, hospitalAddress: String, urgency: String, note: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
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
                loadMyBloodRequests()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun scheduleBloodDonation(
        bloodGroup: String, scheduledDate: String, scheduledTime: String,
        hospitalName: String, hospitalAddress: String,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val (ok, err) = try {
                apiClient.bloodApi.createDonorBooking(
                    DonorBookingCreate(
                        bloodGroup = bloodGroup, scheduledDate = scheduledDate,
                        scheduledTime = scheduledTime, hospitalName = hospitalName,
                        hospitalAddress = hospitalAddress
                    )
                )
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadBloodDonations()
                true to null as String?
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                false to e.message
            }
            withContext(Dispatchers.Main) { onComplete(ok, err) }
        }
    }

    fun loadNearbyBloodStock(bloodGroup: String) {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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

    fun deleteReports(onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            val (ok, error) = try {
                val response = apiClient.userApi.deleteReports()
                if (response.ok) {
                    loadMyDocuments()
                    true to null as String?
                } else {
                    false to response.error
                }
            } catch (e: Exception) {
                false to e.message
            }
            withContext(Dispatchers.Main) { onComplete(ok, error) }
        }
    }

    fun deleteAccount(onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            val (ok, error) = try {
                val response = apiClient.userApi.deleteAccount()
                if (response.ok) true to null as String? else false to response.error
            } catch (e: Exception) {
                false to e.message
            }
            withContext(Dispatchers.Main) { onComplete(ok, error) }
        }
    }
}
