package com.medvault.services

import com.medvault.config.AppConfig
import io.minio.BucketExistsArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import java.util.concurrent.TimeUnit

object StorageService {

    private lateinit var client: MinioClient
    private lateinit var bucket: String

    fun init(cfg: AppConfig) {
        bucket = cfg.minioBucket
        client = MinioClient.builder()
            .endpoint(cfg.minioEndpoint)
            .credentials(cfg.minioAccessKey, cfg.minioSecretKey)
            .build()

        val exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
        }
    }

    fun signedDownloadUrl(path: String, ttlMinutes: Long = 15): String {
        return client.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .bucket(bucket)
                .`object`(path)
                .method(io.minio.http.Method.GET)
                .expiry(ttlMinutes.toInt(), TimeUnit.MINUTES)
                .build()
        )
    }

    fun signedUploadUrl(path: String, contentType: String? = null, ttlMinutes: Long = 15): String {
        val builder = GetPresignedObjectUrlArgs.builder()
            .bucket(bucket)
            .`object`(path)
            .method(io.minio.http.Method.PUT)
            .expiry(ttlMinutes.toInt(), TimeUnit.MINUTES)
        if (contentType != null) {
            builder.extraHeaders(mapOf("Content-Type" to contentType))
        }
        return client.getPresignedObjectUrl(builder.build())
    }

    fun deleteFile(path: String) {
        client.removeObject(RemoveObjectArgs.builder().bucket(bucket).`object`(path).build())
    }

    fun putObject(path: String, input: java.io.InputStream, size: Long, contentType: String) {
        client.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(path)
                .stream(input, size, -1)
                .contentType(contentType)
                .build()
        )
    }
}
