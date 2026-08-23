package com.medvault.routes

import com.medvault.db.FirestoreAdapter
import com.medvault.models.*
import com.medvault.models.requests.*
import com.medvault.plugins.requireAuth
import com.medvault.services.StorageService
import com.medvault.utils.respondRaw
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.vaultRoutes() {
    route("/vault") {
        post("/upload-init") {
            val auth = call.requireAuth()
            val req = call.receive<UploadInitRequest>()
            val documentId = UUID.randomUUID().toString()
            val storedName = "$documentId-${req.fileName.replace("/", "_")}"
            val path = "${auth.uid}/$storedName"
            val uploadUrl = StorageService.signedUploadUrl(path, req.contentType)

            val docId = FirestoreAdapter.newId()
            val doc = mapOf(
                "documentId" to docId,
                "ownerUid" to auth.uid,
                "sharedWith" to emptyList<String>(),
                "fileNames" to listOf(storedName),
                "storedPath" to path,
                "status" to "active",
                "createdAt" to System.currentTimeMillis(),
                "expiresAt" to System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000),
            )
            FirestoreAdapter.setRaw("vault", docId, doc)
            call.respond(success(mapOf("documentId" to docId, "vaultId" to docId, "storedName" to storedName, "path" to path, "uploadUrl" to uploadUrl)))
        }

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
                val sharedWith = doc.data["sharedWith"] as? List<*> ?: emptyList<Any?>()
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

        post("/{documentId}/open") {
            val auth = call.requireAuth()
            val docId = call.parameters["documentId"]
                ?: throw IllegalArgumentException("Document ID required")

            val doc = FirestoreAdapter.get<Map<String, Any?>>("vault", docId)
                ?: throw IllegalArgumentException("Document not found")

            val ownerUid = doc["ownerUid"] as? String
            val sharedWith = doc["sharedWith"] as? List<*> ?: emptyList<Any?>()
            val status = doc["status"] as? String ?: "active"
            val durationMinutes = (doc["durationMinutes"] as? Number)?.toLong() ?: 60L
            val viewedAt = (doc["viewedAt"] as? Number)?.toLong() ?: 0L

            if (status != "active") throw SecurityException("Access revoked")
            if (ownerUid != auth.uid && !sharedWith.contains(auth.uid)) {
                throw SecurityException("Access denied")
            }

            val now = System.currentTimeMillis()
            val isFirstOpen = viewedAt == 0L

            if (isFirstOpen) {
                val expiresAt = now + durationMinutes * 60L * 1000
                FirestoreAdapter.setRaw("vault", docId, mapOf(
                    "viewedAt" to now,
                    "expiresAt" to expiresAt,
                ))
                call.respond(success(mapOf(
                    "viewedAt" to now,
                    "expiresAt" to expiresAt,
                    "durationMinutes" to durationMinutes,
                    "remainingSeconds" to durationMinutes * 60,
                    "isExpired" to false,
                )))
            } else {
                val expiresAt = viewedAt + durationMinutes * 60L * 1000
                val remainingMs = expiresAt - now
                val remainingSeconds = if (remainingMs > 0) remainingMs / 1000 else 0L
                val isExpired = remainingMs <= 0
                call.respond(success(mapOf(
                    "viewedAt" to viewedAt,
                    "expiresAt" to expiresAt,
                    "durationMinutes" to durationMinutes,
                    "remainingSeconds" to remainingSeconds,
                    "isExpired" to isExpired,
                )))
            }
        }

        get("/download/{documentId}") {
            val auth = call.requireAuth()
            val docId = call.parameters["documentId"]
                ?: throw IllegalArgumentException("Document ID required")

            val doc = FirestoreAdapter.get<Map<String, Any?>>("vault", docId)
                ?: throw IllegalArgumentException("Document not found")

            val ownerUid = doc["ownerUid"] as? String
            val sharedWith = doc["sharedWith"] as? List<*> ?: emptyList<Any?>()
            val status = doc["status"] as? String ?: "active"
            val viewedAt = (doc["viewedAt"] as? Number)?.toLong() ?: 0L
            val durationMinutes = (doc["durationMinutes"] as? Number)?.toLong() ?: 60L

            if (status != "active") {
                throw SecurityException("Access revoked")
            }
            if (ownerUid != auth.uid && !sharedWith.contains(auth.uid)) {
                throw SecurityException("Access denied")
            }

            val now = System.currentTimeMillis()
            if (ownerUid != auth.uid && viewedAt > 0L) {
                val expiresAt = viewedAt + durationMinutes * 60L * 1000
                if (now > expiresAt) {
                    throw SecurityException("Access expired")
                }
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
