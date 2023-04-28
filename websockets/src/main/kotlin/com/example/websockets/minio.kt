package com.example.websockets

import com.jlefebure.spring.boot.minio.MinioConfigurationProperties
import com.jlefebure.spring.boot.minio.MinioException
import com.jlefebure.spring.boot.minio.MinioService
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.compress.utils.IOUtils
import org.springframework.core.io.InputStreamResource
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Method
import java.net.URLConnection
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.min

fun createMinioConfig(): MinioConfigurationProperties {
    val c = MinioConfigurationProperties()
    c.bucket = "tmdad"
    c.url = "http://localhost:9000"
    c.accessKey = "IJNC0Jsf08qpRao0"
    c.secretKey = "gWhhm4UYiVbIyGQCqOUTxG41R2R3cfvN"
    c.isSecure = false
    return c
}

@Service
class CustomMinioService  {
    private val accessKey = "IJNC0Jsf08qpRao0"
    private val secretKey = "gWhhm4UYiVbIyGQCqOUTxG41R2R3cfvN"

    private val minioClient : MinioClient = MinioClient.builder()
        .endpoint("localhost", 9000, false)
        .credentials(accessKey, secretKey)
        .build()

    /*
    writeUrl, readUrl
    */
    fun createPreSignedUrls(uuid: String): Pair<String, String> {

        val writeUrl = minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(io.minio.http.Method.PUT)
                .bucket("tmdad")
                .`object`(uuid)
                .expiry(2, TimeUnit.HOURS)
                .build())

        val readUrl = minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(io.minio.http.Method.GET)
                .bucket("tmdad")
                .`object`(uuid)
                .expiry(7, TimeUnit.DAYS)
                .build())

        return writeUrl to readUrl
    }
}

/*
@RestController
@RequestMapping("/files")
class TestController {
    private val minioClient : MinioClient = MinioClient.builder()
        .endpoint("localhost", 9000, false)
        .credentials("IJNC0Jsf08qpRao0", "gWhhm4UYiVbIyGQCqOUTxG41R2R3cfvN")
        .build()
    private val minioService: MinioService = MinioService(minioClient, createMinioConfig())
    @GetMapping("/")
    @Throws(MinioException::class)
    fun testMinio(): List<io.minio.messages.Item> {
        return minioService.list()
    }

    @GetMapping("/{object}")
    @Throws(MinioException::class, IOException::class)
    fun getObject(@PathVariable("object") obj: String, response: HttpServletResponse) {
        val inputStream: InputStream = minioService.get(Path.of(obj))
        val inputStreamResource = InputStreamResource(inputStream)

        // Set the content type and attachment header.
        response.addHeader("Content-disposition", "attachment;filename=$obj")
        response.contentType = URLConnection.guessContentTypeFromName(obj)

        // Copy the stream to the response's output stream.
        IOUtils.copy(inputStream, response.outputStream)
        response.flushBuffer()
    }

    @PostMapping("/")
    fun addObject(@RequestParam("file") file: MultipartFile) {
        val path = Path.of(file.originalFilename!!)
        try {
            minioService.upload(path, file.inputStream, file.contentType)
        } catch (e: MinioException) {
            throw IllegalStateException("The file cannot be upload on the internal storage. Please retry later", e)
        } catch (e: IOException) {
            throw IllegalStateException("The file cannot be read", e)
        }
    }
}
 */