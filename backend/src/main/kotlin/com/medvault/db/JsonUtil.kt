package com.medvault.db

import kotlinx.serialization.json.*

/**
 * A Firestore-like document handle returned by [FirestoreAdapter] so that route
 * code can keep using `.id`, `.data`, `.exists()`, `.getString(...)` etc.
 */
data class Doc(val id: String, val data: Map<String, Any?>) {
    fun exists(): Boolean = true
    fun getString(key: String): String? = data[key] as? String
    fun getBoolean(key: String): Boolean? = data[key] as? Boolean
    fun getLong(key: String): Long? = (data[key] as? Number)?.toLong()
    fun getInt(key: String): Int? = (data[key] as? Number)?.toInt()
    fun getDouble(key: String): Double? = (data[key] as? Number)?.toDouble()
}

object JsonUtil {
    fun toJsonElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is String -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Map<*, *> -> JsonObject(value.mapKeys { it.key.toString() }.mapValues { toJsonElement(it.value) })
        is List<*> -> JsonArray(value.map { toJsonElement(it) })
        is JsonElement -> value
        else -> JsonPrimitive(value.toString())
    }

    fun fromJsonElement(element: JsonElement): Any? = when (element) {
        JsonNull -> null
        is JsonObject -> element.mapValues { fromJsonElement(it.value) }
        is JsonArray -> element.map { fromJsonElement(it) }
        is JsonPrimitive -> {
            if (element.isString) element.content
            else if (element.booleanOrNull != null) element.boolean
            else element.double
        }
    }

    fun encode(value: Any?): String = Json.encodeToString(JsonElement.serializer(), toJsonElement(value))
    fun decode(json: String): Any? = fromJsonElement(Json.parseToJsonElement(json))
}
