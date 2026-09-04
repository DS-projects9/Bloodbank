package com.medkeen.utils

import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.json.*

fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is Map<*, *> -> JsonObject(
        this.mapNotNull { (k, v) -> (k?.toString())?.let { it to v.toJsonElement() } }.toMap()
    )
    is List<*> -> JsonArray(this.map { it.toJsonElement() })
    is Array<*> -> JsonArray(this.map { it.toJsonElement() })
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Enum<*> -> JsonPrimitive(this.name)
    else -> JsonPrimitive(this.toString())
}

suspend inline fun ApplicationCall.respondRaw(data: Any?) {
    respond(com.medkeen.models.success(data.toJsonElement()))
}
