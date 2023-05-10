package com.example.websockets.controllers

import com.example.websockets.config.CustomMinioConfig
import com.jlefebure.spring.boot.minio.MinioException
import com.jlefebure.spring.boot.minio.MinioService
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.compress.utils.IOUtils
import org.springframework.core.io.InputStreamResource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.IOException
import java.io.InputStream
import java.net.URLConnection
import java.nio.file.Path

// Based of https://github.com/jlefebure/spring-boot-starter-minio example
@RestController
@RequestMapping("/files")
class MinioFilesController (
    config: CustomMinioConfig
) {
    private val minioService: MinioService = config.createMinioService()

    @GetMapping("/{object}")
    @Throws(MinioException::class, IOException::class)
    fun getObject(@PathVariable("object") obj: String, response: HttpServletResponse) {

        // TODO: CHECK IF REQUESTER CAN ACCESS THE FILE

        val inputStream: InputStream = minioService.get(Path.of(obj))

        // Set the content type and attachment header.
        response.addHeader("Content-disposition", "attachment;filename=$obj")
        response.contentType = URLConnection.guessContentTypeFromName(obj)

        // Copy the stream to the response's output stream.
        IOUtils.copy(inputStream, response.outputStream)
        response.flushBuffer()
    }
}