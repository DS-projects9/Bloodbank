package com.medkeen.auth

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object RateLimiter {
    private data class Bucket(val count: AtomicInteger, val windowStart: Long)

    private val buckets = ConcurrentHashMap<String, Bucket>()

    /**
     * Returns true if the request is allowed, false if rate-limited.
     * @param key   identifier (e.g. IP address or email)
     * @param limit max requests per window
     * @param windowMs window duration in milliseconds
     */
    fun tryAcquire(key: String, limit: Int, windowMs: Long): Boolean {
        val now = System.currentTimeMillis()
        val bucket = buckets.compute(key) { _, existing ->
            if (existing != null && now - existing.windowStart < windowMs) {
                existing
            } else {
                Bucket(AtomicInteger(0), now)
            }
        }!!
        val current = bucket.count.incrementAndGet()
        return current <= limit
    }

    fun remaining(key: String, limit: Int, windowMs: Long): Int {
        val bucket = buckets[key] ?: return limit
        val elapsed = System.currentTimeMillis() - bucket.windowStart
        if (elapsed >= windowMs) return limit
        return (limit - bucket.count.get()).coerceAtLeast(0)
    }

    fun cleanup(maxAgeMs: Long = 600_000) {
        val now = System.currentTimeMillis()
        buckets.entries.removeIf { (_, bucket) -> now - bucket.windowStart > maxAgeMs }
    }
}
