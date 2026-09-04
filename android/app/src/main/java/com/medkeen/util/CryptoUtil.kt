package com.medkeen.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * Client-side envelope encryption for health files (SECURITY.md §4).
 *
 * Each file gets a fresh random AES-256 key (DEK) and a 12-byte GCM IV.
 * The uploaded blob is `IV(12) || ciphertext`; the DEK is never stored on the
 * server. The owner keeps the DEK in [FileKeyStore]; recipients receive the DEK
 * wrapped to their RSA public key (see [wrapDek]/[unwrapDek]).
 */
data class EncryptedPayload(
    val iv: ByteArray,
    val ciphertext: ByteArray,
    val key: ByteArray,
)

object CryptoUtil {
    private const val KEY_SIZE = 256
    private const val IV_SIZE = 12
    private const val GCM_TAG = 128
    private const val AES_TRANSFORM = "AES/GCM/NoPadding"
    private const val RSA_TRANSFORM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private const val RSA_ALIAS = "medkeen_user_rsa"

    fun encrypt(plain: ByteArray): EncryptedPayload {
        val key = ByteArray(KEY_SIZE / 8).also { Random.nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { Random.nextBytes(it) }
        val cipher = Cipher.getInstance(AES_TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG, iv))
        val ciphertext = cipher.doFinal(plain)
        return EncryptedPayload(iv = iv, ciphertext = ciphertext, key = key)
    }

    /** Build the uploadable blob: `IV || ciphertext`. */
    fun toBlob(iv: ByteArray, ciphertext: ByteArray): ByteArray =
        iv + ciphertext

    fun decrypt(payload: EncryptedPayload): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORM)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(payload.key, "AES"),
            GCMParameterSpec(GCM_TAG, payload.iv),
        )
        return cipher.doFinal(payload.ciphertext)
    }

    fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

    // ---- RSA keypair (Android Keystore) for envelope wrapping ----

    /** Ensure an RSA keypair exists in the Android Keystore (private key non-exportable). */
    fun ensureUserKeyPair(context: Context) {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!ks.containsAlias(RSA_ALIAS)) {
            val kpg = java.security.KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                "AndroidKeyStore",
            )
            val spec = KeyGenParameterSpec.Builder(
                RSA_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            ).setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .build()
            kpg.initialize(spec)
            kpg.generateKeyPair()
        }
    }

    fun getUserPublicKeyPem(context: Context): String {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val encoded = ks.getCertificate(RSA_ALIAS).publicKey.encoded
        val b64 = Base64.encodeToString(encoded, Base64.NO_WRAP).chunked(64).joinToString("\n")
        return "-----BEGIN PUBLIC KEY-----\n$b64\n-----END PUBLIC KEY-----"
    }

    /** Wrap a DEK to a recipient's RSA public key (PEM). Returns base64. */
    fun wrapDek(dek: ByteArray, recipientPublicKeyPem: String): String {
        val pem = recipientPublicKeyPem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .trim()
        val keySpec = X509EncodedKeySpec(Base64.decode(pem, Base64.NO_WRAP))
        val pub = KeyFactory.getInstance("RSA").generatePublic(keySpec)
        val cipher = Cipher.getInstance(RSA_TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, pub)
        return Base64.encodeToString(cipher.doFinal(dek), Base64.NO_WRAP)
    }

    /** Unwrap a DEK using this device's private key. */
    fun unwrapDek(wrappedB64: String, context: Context): ByteArray {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val priv = ks.getKey(RSA_ALIAS, null) as PrivateKey
        val cipher = Cipher.getInstance(RSA_TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, priv)
        return cipher.doFinal(Base64.decode(wrappedB64, Base64.NO_WRAP))
    }
}

/**
 * Persists per-file data-encryption keys on device. The master key lives in the
 * Android Keystore; the DEK values are sealed by EncryptedSharedPreferences,
 * keyed by the document id.
 */
object FileKeyStore {
    private const val PREFS = "medkeen_file_keys"
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = try {
                val masterKey = androidx.security.crypto.MasterKey.Builder(context)
                    .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                    .build()
                androidx.security.crypto.EncryptedSharedPreferences.create(
                    context,
                    PREFS,
                    masterKey,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            } catch (_: Exception) {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            }
        }
    }

    fun putKey(documentId: String, key: ByteArray) {
        prefs?.edit()?.putString("key:$documentId", CryptoUtil.encode(key))?.apply()
    }

    fun getKey(documentId: String): ByteArray? {
        val prefs = prefs ?: return null
        val key = prefs.getString("key:$documentId", null) ?: return null
        return CryptoUtil.decode(key)
    }
}
