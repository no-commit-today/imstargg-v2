package com.imstargg.core.admin.application

import com.imstargg.core.admin.config.ImageStorageProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.InputStream

@Service
@Profile("!local")
class S3ImageStorageService(
    private val s3Client: S3Client,
    private val properties: ImageStorageProperties,
) : ImageStorageService {
    override fun upload(
        key: String,
        inputStream: InputStream,
        contentType: String,
    ): String {
        val bytes = inputStream.readAllBytes()
        val request =
            PutObjectRequest
                .builder()
                .bucket(properties.bucket)
                .key(key)
                .contentType(contentType)
                .build()
        s3Client.putObject(request, RequestBody.fromBytes(bytes))
        return key
    }

    override fun delete(key: String) {
        val request =
            DeleteObjectRequest
                .builder()
                .bucket(properties.bucket)
                .key(key)
                .build()
        s3Client.deleteObject(request)
    }
}
