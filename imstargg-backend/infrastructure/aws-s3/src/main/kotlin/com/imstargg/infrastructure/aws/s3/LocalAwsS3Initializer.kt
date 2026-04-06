package com.imstargg.infrastructure.aws.s3

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException

@Component
@Profile("local")
class LocalAwsS3Initializer(
    private val s3Client: S3Client,
    private val properties: AwsS3Properties,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        try {
            s3Client.createBucket { it.bucket(properties.bucket) }
            log.info("S3 버킷 생성 완료: {}", properties.bucket)
        } catch (e: BucketAlreadyOwnedByYouException) {
            log.info("S3 버킷 이미 존재: {}", properties.bucket, e)
        }
    }
}
