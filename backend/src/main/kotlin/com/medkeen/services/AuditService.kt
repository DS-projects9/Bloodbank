package com.medkeen.services

import com.medkeen.db.FirestoreAdapter

/**
 * Lightweight, append-only audit trail. Each entry is written once and never
 * updated/deleted (immutability required by the security spec). Used for auth,
 * consent, share grant/revoke and file-access events.
 */
object AuditService {
    suspend fun log(
        action: String,
        actorId: String?,
        resourceId: String? = null,
        details: Map<String, Any?> = emptyMap(),
    ) {
        runCatching {
            val entry = mutableMapOf<String, Any?>(
                "action" to action,
                "actorId" to actorId,
                "resourceId" to resourceId,
                "timestamp" to System.currentTimeMillis(),
            )
            entry.putAll(details)
            FirestoreAdapter.setRaw("audit_logs", FirestoreAdapter.newId(), entry)
        }
    }
}
