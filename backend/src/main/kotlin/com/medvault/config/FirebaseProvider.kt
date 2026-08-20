package com.medvault.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.cloud.FirestoreClient
import com.google.firebase.cloud.StorageClient
import java.io.ByteArrayInputStream

object FirebaseProvider {
    private var initialized = false

    // In-memory fake service account used only for emulator mode. The key is a
    // throwaway RSA keypair — it is never used to sign real credentials; the
    // Firebase Admin SDK only needs a structurally valid service account to talk
    // to the local emulators.
    private const val FAKE_SERVICE_ACCOUNT = """
        {
            "type": "service_account",
            "project_id": "medvault-11c68",
            "private_key_id": "fake-key-id",
            "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC5C6H6cclp5HXl\nXGhfena2iqDP1l6XAgilgSNSeHSmqr5pWG75JAfNaSvwUGh8wSSIyQlhSX+vJObC\nk/Rf6ceYVWCxVeveL/LjNlF1dVv+aeAm339Nd7I9//RAShZL+eAi5JSRkAJTeaSI\naJ7/PMRZb3f15sySuq/lApbqvyjol0tFp8t0xDav6HHYw3Qcb1k25Aq1OkrKCovs\nLUsrMGfUGCn7I0blBDGIe/uMIXIds53XquJFLlxZdGIr+4YMK7OArPW0dZcz7wJ5\n6OHzS+iUZ/aEODhab0xFqZLe1ZoyKabJvuna/YxIrBJvXl8vrw4UN5vryOrUXhuI\naPChOap1AgMBAAECggEAVWXF8hOzNThnJFuaG34j7ShbAK/Y5W3d2auYRoYqp4Qi\n7fEO/dGf/C8uRPCG2BQ2rR3CQ3CtCYJJMMEhRWZZY7b7MbwKZ3bPbAZ0X3Wy592q\nwaXhVToJO021RjoqwhNmWhQNcIP6XtXDleRahEZiAzhLL2O9Q3X+mPyx81IXKgdm\nMw6CcQijdMegvO3EH8nng5j+nBKxLMgbImoUlLJAvb6ARpqNsiQw9QyHCaF1kvRF\nI29mrdHpd6wOPNrNI8qEljHTivdTdOWk6Q+vFlfT951I552TaBFeC7/foxUm6Kpx\nWWlEsHiORFM4jMjIPQZCYDoazOiqxpxfeKhPdseI/QKBgQD1VmdrjjqbJcbKis3M\nfWLnLjAL+m9SmeZOpiWhthBpgsuwKkuE6TsFFxoReWXAGQp4VqtEay3QTuFvsPID\nDKqV+mD9sasuqrRnELKy6OQSDBZ2mSo7hH5ISJsykdgVnVO6tjXGe2PKZklvFaM2\ndxCCIThlBPpGgW2/gNNbY11dgwKBgQDBFm0hDsI+9FtILdVqBcUfmvOtkNd4Edza\nWRKUsDCLtCic4dLNvAR27QgxaH8u2nN3OElQQh+zjBsYQdPFwjwe0EdmfM2nr4Up\nVTbTZf9ewHFAxH8AZvPD9tgXiMiD3QyeJceAagsaksROwliNA+VSLUcEH+tywVYZ\nPQD3pVGOpwKBgAE0lXl/z2Xnv5xLJku06gF8xpuodVeQjgMg6dprjJUYIWwSKSDU\nDIgv3t7rG8bf2J9SvbCuXiFB4fWshuPg8g2el09nyhGGoTBIcrhSUJyOFqYqhnyY\nYA523NxYvxkMFtc6AfWhsZHTAtVa49xmpVweUaqdZaVnCWUBewhd6p2FAoGAe9nb\ns+mYlPL+HiUwD+qVj3k/tmQCoYSrqe6aaX8+FE5CAqcjJU3lezb9G1wQwEfk6mLT\nO8S+OteyhXOr19yH3afxNH73FwkRm/qJyT1SeT9tJYMkh2iCEX+jCi11mIdQUZyg\nA/GBH9FmU7J7RDBshrJ+K9ohlyEGDfhAR5jl4F8CgYEAySYZUJWpSzUtdX/A9Ck1\njHK3/2ka9oFPy5DN8HvE/sKId10yBAUIeh7s2qQRLlUadrR0VWGrv2rTJiec+311\n9VtIovGBjHYAyMS6O73suQaYClU6y7hP81Ud/dCeTyZuH3jHzPmSsi3af86HnrxE\nO19WCVCHH5B7GgvsaBF9LWA=\n-----END PRIVATE KEY-----\n",
            "client_email": "fake@medvault-11c68.iam.gserviceaccount.com",
            "client_id": "000000000000",
            "auth_uri": "https://accounts.google.com/o/oauth2/auth",
            "token_uri": "https://oauth2.googleapis.com/token"
        }
    """

    fun initialize(projectId: String) {
        if (initialized) return

        val firestoreHost = System.getenv("FIRESTORE_EMULATOR_HOST")
        val isEmulator = firestoreHost != null

        if (isEmulator) {
            println("[FirebaseProvider] Using Firestore emulator at $firestoreHost")
        } else {
            println("[FirebaseProvider] Using production Firebase")
        }

        val credentials = try {
            GoogleCredentials.getApplicationDefault()
        } catch (e: Exception) {
            if (isEmulator) {
                println("[FirebaseProvider] No ADC found, using embedded fake credentials for emulator")
                GoogleCredentials.fromStream(
                    ByteArrayInputStream(FAKE_SERVICE_ACCOUNT.toByteArray(Charsets.UTF_8))
                )
            } else {
                throw IllegalStateException(
                    "No Google credentials found. In production set GOOGLE_APPLICATION_CREDENTIALS " +
                    "or rely on the Cloud Run service account (ADC).", e
                )
            }
        }

        val firebaseOptions = FirebaseOptions.builder()
            .setProjectId(projectId)
            .setCredentials(credentials)
            .build()

        FirebaseApp.initializeApp(firebaseOptions)
        initialized = true
        println("[FirebaseProvider] Firebase initialized for project: $projectId")
    }

    fun auth(): FirebaseAuth = FirebaseAuth.getInstance()

    fun firestore() = FirestoreClient.getFirestore()

    fun storage() = try {
        StorageClient.getInstance().bucket()
    } catch (e: Exception) {
        println("[FirebaseProvider] Storage not available: ${e.message}")
        throw e
    }
}
