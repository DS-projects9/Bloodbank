package com.medvault.db

import com.medvault.db.JsonUtil.decode
import com.medvault.db.JsonUtil.encode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.util.UUID

object FirestoreAdapter {

    fun newId(): String = UUID.randomUUID().toString()

    // ----------------------------------------------------------------- queries

    suspend fun getRaw(collection: String, documentId: String): Map<String, Any?>? =
        withContext(Dispatchers.IO) { Database.connection().use { readDoc(it, collection, documentId)?.data } }

    suspend inline fun <reified T> get(collection: String, documentId: String): T? =
        @Suppress("UNCHECKED_CAST") getRaw(collection, documentId) as? T

    suspend fun setRaw(collection: String, documentId: String, data: Map<String, Any?>) =
        withContext(Dispatchers.IO) { Database.connection().use { writeDoc(it, collection, documentId, data, merge = true) } }

    suspend fun queryRaw(
        collection: String,
        filters: List<Pair<String, Any>>,
        limit: Int = 50,
    ): List<Doc> = withContext(Dispatchers.IO) {
        val sql = StringBuilder("SELECT id, data FROM documents WHERE collection = ?")
        val params = mutableListOf<Any?>(collection)
        for ((field, value) in filters) {
            sql.append(" AND data->>? = ?")
            params.add(field)
            params.add(value.toString())
        }
        sql.append(" LIMIT ?")
        params.add(limit)
        Database.connection().use { conn ->
            conn.prepareStatement(sql.toString()).use { ps ->
                params.forEachIndexed { i, p -> ps.setObject(i + 1, p) }
                ps.executeQuery().use { rs ->
                    val out = mutableListOf<Doc>()
                    while (rs.next()) {
                        out.add(Doc(rs.getString("id"), decode(rs.getString("data")) as Map<String, Any?>))
                    }
                    out
                }
            }
        }
    }

    suspend inline fun <reified T> query(
        collection: String,
        filters: List<Pair<String, Any>>,
        limit: Int = 50,
    ): List<T> = @Suppress("UNCHECKED_CAST") queryRaw(collection, filters, limit).mapNotNull { it.data as? T }

    suspend fun delete(collection: String, documentId: String) =
        withContext(Dispatchers.IO) {
            Database.connection().use { conn ->
                conn.prepareStatement("DELETE FROM documents WHERE collection = ? AND id = ?").use { ps ->
                    ps.setString(1, collection)
                    ps.setString(2, documentId)
                    ps.executeUpdate()
                }
            }
        }

    // ------------------------------------------------------------ transactions

    suspend fun <T> runTransaction(block: (Txn) -> T): T = withContext(Dispatchers.IO) {
        val conn = Database.connection()
        val prev = conn.autoCommit
        conn.autoCommit = false
        try {
            val result = block(TxnImpl(conn))
            conn.commit()
            result
        } catch (e: Exception) {
            try { conn.rollback() } catch (_: Exception) { }
            throw e
        } finally {
            try { conn.autoCommit = prev } catch (_: Exception) { }
            try { conn.close() } catch (_: Exception) { }
        }
    }

    fun batch(): Batch = BatchImpl

    // --------------------------------------------------------------- internals

    private fun readDoc(conn: Connection, collection: String, id: String): Doc? {
        conn.prepareStatement("SELECT data FROM documents WHERE collection = ? AND id = ?").use { ps ->
            ps.setString(1, collection)
            ps.setString(2, id)
            ps.executeQuery().use { rs ->
                if (!rs.next()) return null
                return Doc(id, decode(rs.getString("data")) as Map<String, Any?>)
            }
        }
    }

    private fun writeDoc(conn: Connection, collection: String, id: String, data: Map<String, Any?>, merge: Boolean) {
        val sql = if (merge)
            "INSERT INTO documents(collection, id, data) VALUES (?, ?, ?::jsonb) " +
                "ON CONFLICT (collection, id) DO UPDATE SET data = documents.data || EXCLUDED.data"
        else
            "INSERT INTO documents(collection, id, data) VALUES (?, ?, ?::jsonb) " +
                "ON CONFLICT (collection, id) DO UPDATE SET data = EXCLUDED.data"
        conn.prepareStatement(sql).use { ps ->
            ps.setString(1, collection)
            ps.setString(2, id)
            ps.setString(3, encode(data))
            ps.executeUpdate()
        }
    }

    private fun deepApply(base: MutableMap<String, Any?>, patch: Map<String, Any?>): MutableMap<String, Any?> {
        for ((key, value) in patch) {
            if (key.contains('.')) {
                val head = key.substringBefore('.')
                val rest = key.substringAfter('.')
                val nested = (base[head] as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()
                @Suppress("UNCHECKED_CAST")
                deepApply(nested as MutableMap<String, Any?>, mapOf(rest to value))
                base[head] = nested
            } else {
                base[key] = value
            }
        }
        return base
    }

    interface Txn {
        fun get(collection: String, id: String): Doc?
        fun set(collection: String, id: String, data: Map<String, Any?>)
        fun update(collection: String, id: String, patch: Map<String, Any?>)
    }

    private class TxnImpl(val conn: Connection) : Txn {
        override fun get(collection: String, id: String): Doc? = readDoc(conn, collection, id)
        override fun set(collection: String, id: String, data: Map<String, Any?>) =
            writeDoc(conn, collection, id, data, merge = true)
        override fun update(collection: String, id: String, patch: Map<String, Any?>) {
            val current = readDoc(conn, collection, id)?.data?.toMutableMap() ?: mutableMapOf()
            writeDoc(conn, collection, id, deepApply(current, patch), merge = true)
        }
    }

    interface Batch {
        suspend fun set(collection: String, id: String, data: Map<String, Any?>)
        suspend fun commit()
    }

    private object BatchImpl : Batch {
        override suspend fun set(collection: String, id: String, data: Map<String, Any?>) =
            FirestoreAdapter.setRaw(collection, id, data)
        override suspend fun commit() = Unit
    }
}
