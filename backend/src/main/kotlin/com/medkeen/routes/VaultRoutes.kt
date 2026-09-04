package com.medkeen.routes

import com.medkeen.db.FirestoreAdapter
import com.medkeen.models.*
import com.medkeen.models.requests.*
import com.medkeen.plugins.requireAuth
import com.medkeen.services.AuditService
import com.medkeen.services.StorageService
import com.medkeen.utils.respondRaw
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

private val ALLOWED_CONTENT_TYPES = setOf(
    "image/jpeg", "image/png", "image/webp",
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "text/plain",
)

private const val MAX_UPLOAD_BYTES = 50 * 1024 * 1024 // 50 MB (enforced client-side)

fun Route.vaultRoutes() {
    route("/vault") {
        post("/upload-init") {
            val auth = call.requireAuth()
            val req = call.receive<UploadInitRequest>()

            require(req.fileName.isNotBlank()) { "File name is required" }
            require(!req.fileName.contains("..") && !req.fileName.contains("/")) { "Invalid file name" }
            require(req.contentType in ALLOWED_CONTENT_TYPES) {
                "Unsupported file type: ${req.contentType}. Allowed: ${ALLOWED_CONTENT_TYPES.joinToString()}"
            }

            val documentId = UUID.randomUUID().toString()
            val storedName = "$documentId-${req.fileName.replace("/", "_")}"
            val path = "${auth.uid}/$storedName"
            val uploadUrl = StorageService.signedUploadUrl(path, req.contentType)

            AuditService.log(
                "vault.upload_init",
                auth.uid,
                documentId,
                mapOf("fileName" to req.fileName, "contentType" to req.contentType),
            )

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

            if (!path.startsWith("${auth.uid}/")) {
                throw SecurityException("Access denied")
            }

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
                call.respondRaw(mapOf(
                    "viewedAt" to now,
                    "expiresAt" to expiresAt,
                    "durationMinutes" to durationMinutes,
                    "remainingSeconds" to durationMinutes * 60,
                    "isExpired" to false,
                ))
            } else {
                val expiresAt = viewedAt + durationMinutes * 60L * 1000
                val remainingMs = expiresAt - now
                val remainingSeconds = if (remainingMs > 0) remainingMs / 1000 else 0L
                val isExpired = remainingMs <= 0
                call.respondRaw(mapOf(
                    "viewedAt" to viewedAt,
                    "expiresAt" to expiresAt,
                    "durationMinutes" to durationMinutes,
                    "remainingSeconds" to remainingSeconds,
                    "isExpired" to isExpired,
                ))
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
