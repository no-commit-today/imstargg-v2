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
        allowPublicRead()
    }

    /**
     * 로컬 이미지 URL(`app.aws.cloudfront.baseUrl`)을 브라우저가 서명 없이 읽도록 객체 공개 읽기를 연다.
     * AWS 와 같이 새 버킷은 공개 정책을 막고 있어 그 차단부터 푼다.
     * 에뮬레이터는 상태를 메모리에만 두므로 버킷이 이미 있어도 매번 다시 건다.
     */
    private fun allowPublicRead() {
        val bucket = properties.bucket
        s3Client.putPublicAccessBlock {
            it.bucket(bucket).publicAccessBlockConfiguration { config ->
                config
                    .blockPublicAcls(true)
                    .ignorePublicAcls(true)
                    .blockPublicPolicy(false)
                    .restrictPublicBuckets(false)
            }
        }
        s3Client.putBucketPolicy {
            it.bucket(bucket).policy(
                """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": "s3:GetObject",
                      "Resource": "arn:aws:s3:::$bucket/*"
                    }
                  ]
                }
                """.trimIndent(),
            )
        }
        log.info { "S3 버킷 공개 읽기 설정 완료: $bucket" }
    }
}
