package com.medvault.routes

import com.medvault.db.FirestoreAdapter
import com.medvault.models.*
import com.medvault.plugins.requireAuth
import com.medvault.services.StorageService
import com.medvault.utils.respondRaw
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.vaultRoutes() {
    route("/vault") {
        get("/documents") {
            val auth = call.requireAuth()

            val owned = FirestoreAdapter.query<Map<String, Any?>>(
                "vault",
                listOf("ownerUid" to auth.uid),
                limit = 100,
            )

            val shared = FirestoreAdapter.queryRaw(
                "vault",
                listOf("status" to "active"),
                limit = 100,
            ).mapNotNull { doc ->
                val sharedWith = doc.get("sharedWith") as? List<*> ?: emptyList<Any?>()
                if (sharedWith.contains(auth.uid)) {
                    doc.data
                } else null
            }

            val allDocs = owned + shared
            call.respondRaw(allDocs)
        }

        get("/download-url/{path}") {
            val auth = call.requireAuth()
            val path = call.parameters["path"]
                ?: throw IllegalArgumentException("Path required")

            val url = StorageService.signedDownloadUrl(path, ttlMinutes = 15)
            call.respond(success(UrlResponse(url = url, expiresInMinutes = 15)))
        }

        get("/download/{documentId}") {
            val auth = call.requireAuth()
            val docId = call.parameters["documentId"]
                ?: throw IllegalArgumentException("Document ID required")

            val doc = FirestoreAdapter.get<Map<String, Any?>>("vault", docId)
                ?: throw IllegalArgumentException("Document not found")

            val ownerUid = doc["ownerUid"] as? String
            val sharedWith = doc["sharedWith"] as? List<*> ?: emptyList<Any?>()
            val expiresAt = (doc["expiresAt"] as? Number)?.toLong() ?: 0L

            if (ownerUid != auth.uid && !sharedWith.contains(auth.uid)) {
                throw SecurityException("Access denied")
            }
            if (expiresAt < System.currentTimeMillis()) {
                throw SecurityException("Access expired")
            }

            val fileNames = doc["fileNames"] as? List<*> ?: emptyList<Any?>()
            val urls = fileNames.mapNotNull { fileName ->
                fileName as? String
            }.associateWith { fileName ->
                StorageService.signedDownloadUrl("$ownerUid/$fileName", ttlMinutes = 15)
            }

            call.respond(success(urls))
        }
    }
}
