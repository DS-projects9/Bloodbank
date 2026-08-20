package com.medvault.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor(
    private val auth: FirebaseAuth
) {
    private val retrofit by lazy { NetworkClient.createRetrofit(auth) }

    val authApi: AuthApi by lazy { NetworkClient.createApi(retrofit, AuthApi::class.java) }
    val userApi: UserApi by lazy { NetworkClient.createApi(retrofit, UserApi::class.java) }
    val doctorApi: DoctorApi by lazy { NetworkClient.createApi(retrofit, DoctorApi::class.java) }
    val appointmentApi: AppointmentApi by lazy { NetworkClient.createApi(retrofit, AppointmentApi::class.java) }
    val bloodApi: BloodApi by lazy { NetworkClient.createApi(retrofit, BloodApi::class.java) }
    val vaultApi: VaultApi by lazy { NetworkClient.createApi(retrofit, VaultApi::class.java) }
    val aiApi: AiApi by lazy { NetworkClient.createApi(retrofit, AiApi::class.java) }
}
