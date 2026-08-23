package com.medvault.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

object ApiConfig {
    private const val PRODUCTION_URL = "https://medvault-backend-xxxxx-uc.a.run.app/"
    private const val LOCAL_URL = "http://10.0.2.2:8080/"
    private const val DEVICE_LOCAL_URL = "http://127.0.0.1:8080/"

    const val BASE_URL = DEVICE_LOCAL_URL
}

object TokenManager {
    private const val PREFS = "medvault_auth"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER = "cached_user"

    @Volatile
    private var _token: String? = null
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: android.content.Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            _token = prefs?.getString(KEY_TOKEN, null)
        }
    }

    fun setToken(value: String?) {
        _token = value
        prefs?.edit()?.apply {
            if (value != null) putString(KEY_TOKEN, value) else remove(KEY_TOKEN)
            apply()
        }
    }

    fun getToken(): String? = _token
    val isLoggedIn: Boolean get() = _token != null

    fun saveUser(user: com.medvault.data.model.User) {
        val json = org.json.JSONObject().apply {
            put("uid", user.uid)
            put("email", user.email)
            put("displayName", user.displayName)
            put("role", user.role?.name ?: "")
            put("isOnboarded", user.isOnboarded)
            put("storeRecords", user.dpdpConsents.storeRecords)
            put("shareWithDoctor", user.dpdpConsents.shareWithDoctor)
            put("bloodNetwork", user.dpdpConsents.bloodNetwork)
        }.toString()
        prefs?.edit()?.putString(KEY_USER, json)?.apply()
    }

    fun loadUser(): com.medvault.data.model.User? {
        val json = prefs?.getString(KEY_USER, null) ?: return null
        return try {
            val obj = org.json.JSONObject(json)
            com.medvault.data.model.User(
                uid = obj.optString("uid"),
                email = obj.optString("email"),
                displayName = obj.optString("displayName"),
                role = obj.optString("role").takeIf { it.isNotBlank() }?.let {
                    try { com.medvault.data.model.UserRole.valueOf(it) } catch (_: Exception) { null }
                },
                isOnboarded = obj.optBoolean("isOnboarded"),
                dpdpConsents = com.medvault.data.model.DpdpConsents(
                    storeRecords = obj.optBoolean("storeRecords"),
                    shareWithDoctor = obj.optBoolean("shareWithDoctor"),
                    bloodNetwork = obj.optBoolean("bloodNetwork"),
                ),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        _token = null
        prefs?.edit()?.clear()?.apply()
    }
}

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val token = TokenManager.getToken()
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

    fun createRetrofit(): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
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
