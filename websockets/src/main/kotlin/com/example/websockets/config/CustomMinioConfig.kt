package com.example.websockets.config

import com.jlefebure.spring.boot.minio.MinioConfigurationProperties
import com.jlefebure.spring.boot.minio.MinioService
import io.minio.MinioClient
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.*

@Configuration
@ConfigurationProperties("custom.minio")
data class CustomMinioConfig (
    var accessKey: String = "",
    var secretKey: String = "",
    var bucket: String = "",
    var endpoint: String = "",
    var port: String = ""
) {
    fun url() : String {
        return "http://${endpoint}:${port}"
    }

    fun createMinioConfig(): MinioConfigurationProperties {
        val c = MinioConfigurationProperties()
        c.bucket = bucket
        c.url = url()
        c.accessKey = accessKey
        c.secretKey = secretKey
        c.isSecure = false
        return c
    }

    fun createMinioClient(): MinioClient {
        return MinioClient.builder()
            .endpoint(endpoint, port.toInt(), false)
            .credentials(accessKey, secretKey)
            .build()
    }

    fun createMinioService() : MinioService {
        return MinioService(createMinioClient(), createMinioConfig())
    }
}

