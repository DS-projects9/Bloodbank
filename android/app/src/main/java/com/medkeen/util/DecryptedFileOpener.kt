package com.medkeen.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads an (encrypted) file from a presigned URL, decrypts it with the
 * provided DEK, and exposes it to an external viewer via a FileProvider URI.
 * Used when client-side encryption is enabled.
 */
object DecryptedFileOpener {
    private const val IV_SIZE = 12

    private fun downloadBytes(url: String): ByteArray? = try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 30_000
        conn.readTimeout = 30_000
        if (conn.responseCode !in 200..299) null else conn.inputStream.use { it.readBytes() }
    } catch (_: Exception) {
        null
    }

    /**
     * @return a content URI for the decrypted file, or null if decryption fails.
     */
    fun decryptToUri(context: Context, url: String, key: ByteArray, fileName: String): Uri? {
        val blob = downloadBytes(url) ?: return null
        if (blob.size <= IV_SIZE) return null
        val iv = blob.copyOfRange(0, IV_SIZE)
        val ciphertext = blob.copyOfRange(IV_SIZE, blob.size)
        val plain = try {
            CryptoUtil.decrypt(EncryptedPayload(iv, ciphertext, key))
        } catch (_: Exception) {
            return null
        }
        return writeTemp(context, fileName, plain)
    }

    private fun writeTemp(context: Context, fileName: String, bytes: ByteArray): Uri? = try {
        val dir = File(context.cacheDir, "decrypted").also { it.mkdirs() }
        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val file = File(dir, "${System.currentTimeMillis()}_$safeName")
        file.writeBytes(bytes)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (_: Exception) {
        null
    }

    /**
     * Resolves the URI to open for a shared document. When client-side encryption
     * is enabled and the document carries a wrapped key for this device, the
     * ciphertext is downloaded, decrypted, and exposed via a FileProvider content
     * URI. Otherwise the presigned URL is returned directly.
     */
    fun openUriForShared(
        context: Context,
        url: String,
        wrappedKeys: Map<String, String>,
        fileName: String,
    ): Uri {
        if (!com.medkeen.BuildConfig.ENABLE_CLIENT_ENCRYPTION || wrappedKeys.isEmpty()) {
            return url.toUri()
        }
        val wrapped = wrappedKeys[fileName] ?: wrappedKeys.values.firstOrNull()
            ?: return url.toUri()
        val key = runCatching { CryptoUtil.unwrapDek(wrapped, context) }.getOrNull()
            ?: return url.toUri()
        return decryptToUri(context, url, key, fileName) ?: url.toUri()
    }
}
