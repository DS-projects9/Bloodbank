package com.medvault.services

import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage.SignUrlOption
import com.medvault.config.FirebaseProvider
import java.util.concurrent.TimeUnit

object StorageService {

    private fun bucket() = FirebaseProvider.storage()

    fun signedDownloadUrl(path: String, ttlMinutes: Long = 15): String {
        val blob = bucket().get(path)
            ?: throw IllegalArgumentException("File not found: $path")

        val url = blob.signUrl(
            ttlMinutes,
            TimeUnit.MINUTES,
            SignUrlOption.withV4Signature()
        )
        return url.toString()
    }

    fun signedUploadUrl(path: String, contentType: String, ttlMinutes: Long = 15): String {
        val blobInfo = BlobInfo.newBuilder(bucket().name, path)
            .setContentType(contentType)
            .build()

        val url = bucket().storage.signUrl(
            blobInfo,
            ttlMinutes,
            TimeUnit.MINUTES,
            SignUrlOption.withV4Signature()
        )
        return url.toString()
    }

    fun deleteFile(path: String) {
        bucket().get(path)?.delete()
    }
}
