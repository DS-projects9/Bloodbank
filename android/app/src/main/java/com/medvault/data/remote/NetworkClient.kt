package com.medvault.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object ApiConfig {
    private const val PRODUCTION_URL = "https://medvault-backend-xxxxx-uc.a.run.app/"
    private const val LOCAL_URL = "http://10.0.2.2:9000/"
    private const val DEVICE_LOCAL_URL = "http://127.0.0.1:9000/"

    const val BASE_URL = DEVICE_LOCAL_URL
}

private suspend fun FirebaseAuth.getIdToken(): String? {
    val user = this.currentUser ?: return null
    return suspendCoroutine { cont ->
        user.getIdToken(false)
            .addOnSuccessListener { result: GetTokenResult -> cont.resume(result.token) }
            .addOnFailureListener { cont.resume(null) }
    }
}

class AuthInterceptor(private val auth: FirebaseAuth) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val token = runBlocking {
            auth.getIdToken()
        }
        val request = chain.request().newBuilder().apply {
            if (token != null) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        return chain.proceed(request)
    }
}

@Singleton
object NetworkClient {

    fun createRetrofit(auth: FirebaseAuth): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(auth))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> createApi(retrofit: Retrofit, clazz: Class<T>): T = retrofit.create(clazz)
}
