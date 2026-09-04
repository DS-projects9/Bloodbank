package com.medkeen.auth

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Verifies a Google OAuth ID token by calling Google's tokeninfo endpoint.
 * The endpoint validates the signature, issuer and expiry server-side, so we
 * only need to sanity-check the claims we depend on.
 */
object GoogleTokenVerifier {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun verify(idToken: String): Map<String, String>? {
        return runCatching {
            val resp = client.get("https://oauth2.googleapis.com/tokeninfo") {
                parameter("id_token", idToken)
            }
            if (resp.status != HttpStatusCode.OK) return null
            val tree = json.parseToJsonElement(resp.bodyAsText()).jsonObject
            val get = { key: String -> tree[key]?.jsonPrimitive?.content }

            val iss = get("iss") ?: return null
            if (iss != "accounts.google.com" && iss != "https://accounts.google.com") return null

            val exp = get("exp")?.toLongOrNull() ?: return null
            if (exp * 1000 < System.currentTimeMillis()) return null

            if (get("email_verified") == "false") return null

            mapOf(
                "sub" to (get("sub") ?: return null),
                "email" to (get("email") ?: return null),
                "name" to (get("name") ?: ""),
                "picture" to (get("picture") ?: ""),
                "aud" to (get("aud") ?: ""),
            )
        }.getOrNull()
    }
}
