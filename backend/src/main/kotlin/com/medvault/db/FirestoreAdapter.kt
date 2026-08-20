package com.medvault.db

import com.google.cloud.firestore.*
import com.medvault.config.FirebaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FirestoreAdapter {

    private fun db(): Firestore = FirebaseProvider.firestore()

    suspend fun getRaw(collection: String, documentId: String): Map<String, Any?>? =
        withContext(Dispatchers.IO) {
            val doc = db().collection(collection).document(documentId).get().get()
            if (!doc.exists()) return@withContext null
            doc.data
        }

    suspend inline fun <reified T> get(collection: String, documentId: String): T? =
        @Suppress("UNCHECKED_CAST")
        getRaw(collection, documentId) as? T

    suspend fun setRaw(collection: String, documentId: String, data: Map<String, Any?>) =
        withContext(Dispatchers.IO) {
            db().collection(collection).document(documentId).set(data, SetOptions.merge()).get()
        }

    suspend fun queryRaw(
        collection: String,
        filters: List<Pair<String, Any>>,
        limit: Int = 50,
    ): List<DocumentSnapshot> = withContext(Dispatchers.IO) {
        var q: Query = db().collection(collection)
        for ((field, value) in filters) {
            q = q.whereEqualTo(field, value)
        }
        q = q.limit(limit)
        q.get().get().documents
    }

    suspend inline fun <reified T> query(
        collection: String,
        filters: List<Pair<String, Any>>,
        limit: Int = 50,
    ): List<T> = @Suppress("UNCHECKED_CAST")
        queryRaw(collection, filters, limit).mapNotNull { it.data as? T }

    suspend fun <T> runTransaction(block: (Transaction) -> T): T =
        withContext(Dispatchers.IO) {
            db().runTransaction<T> { block(it) }.get()
        }

    suspend fun delete(collection: String, documentId: String) =
        withContext(Dispatchers.IO) {
            db().collection(collection).document(documentId).delete().get()
        }
}
