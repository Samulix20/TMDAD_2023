package com.example.websockets.services

import com.example.websockets.config.CustomMinioConfig
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class CustomMinioService  (
    val config: CustomMinioConfig
){

    private val minioClient : MinioClient = config.createMinioClient()

    fun createPreSignedUrl(uuid: String): String {
        var url = minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(io.minio.http.Method.PUT)
                .bucket(config.bucket)
                .`object`(uuid)
                .expiry(2, TimeUnit.HOURS)
                .build()
        )
        return url.removePrefix(config.url())
    }
}