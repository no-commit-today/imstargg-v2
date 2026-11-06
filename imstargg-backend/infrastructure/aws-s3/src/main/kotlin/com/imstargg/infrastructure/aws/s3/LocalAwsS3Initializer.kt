package com.imstargg.infrastructure.aws.s3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException

private val log = KotlinLogging.logger {}

@Component
@Profile("local")
class LocalAwsS3Initializer(
    private val s3Client: S3Client,
    private val properties: AwsS3Properties,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        try {
            s3Client.createBucket { it.bucket(properties.bucket) }
            log.info { "S3 버킷 생성 완료: ${properties.bucket}" }
        } catch (_: BucketAlreadyOwnedByYouException) {
            log.info { "S3 버킷 이미 존재: ${properties.bucket}" }
        }
    }
}
