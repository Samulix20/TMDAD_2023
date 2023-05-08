package com.example.websockets

import com.jlefebure.spring.boot.minio.MinioConfigurationProperties
import com.jlefebure.spring.boot.minio.MinioException
import com.jlefebure.spring.boot.minio.MinioService
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.compress.utils.IOUtils
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.InputStreamResource
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import java.io.IOException
import java.io.InputStream
import java.net.URLConnection
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@Configuration
@ConfigurationProperties("custom.minio")
data class CustomMinioConfig (
    var accessKey: String = "",
    var secretKey: String = "",
    var bucket: String = "",
    var endpoint: String = "",
    var port: String = ""
) {
    var url = "http://${endpoint}:${port}"

    fun createMinioConfig(): MinioConfigurationProperties {
        val c = MinioConfigurationProperties()
        c.bucket = bucket
        c.url = url
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

@Service
class CustomMinioService  (
    val config: CustomMinioConfig
){

    private val minioClient : MinioClient = config.createMinioClient()

    fun createPreSignedUrl(uuid: String): String {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(io.minio.http.Method.PUT)
                .bucket(config.bucket)
                .`object`(uuid)
                .expiry(2, TimeUnit.HOURS)
                .build()
        )
    }
}

// Based of https://github.com/jlefebure/spring-boot-starter-minio example
@RestController
@RequestMapping("/files")
class MinioFilesRestEndpoint (
    config: CustomMinioConfig
) {
    private val minioService: MinioService = config.createMinioService()

    @GetMapping("/{object}")
    @Throws(MinioException::class, IOException::class)
    fun getObject(@PathVariable("object") obj: String, response: HttpServletResponse) {

        // TODO: CHECK IF REQUESTER CAN ACCESS THE FILE

        val inputStream: InputStream = minioService.get(Path.of(obj))
        val inputStreamResource = InputStreamResource(inputStream)

        // Set the content type and attachment header.
        response.addHeader("Content-disposition", "attachment;filename=$obj")
        response.contentType = URLConnection.guessContentTypeFromName(obj)

        // Copy the stream to the response's output stream.
        IOUtils.copy(inputStream, response.outputStream)
        response.flushBuffer()
    }
}
