package com.imstargg.core.admin.application

import com.imstargg.infrastructure.aws.s3.AwsS3Properties
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.InputStream

@Service
class S3ImageStorageService(
    private val s3Client: S3Client,
    private val properties: AwsS3Properties,
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
        val url = s3Client.utilities().getUrl { it.bucket(properties.bucket).key(key) }
        return url.toString()
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
