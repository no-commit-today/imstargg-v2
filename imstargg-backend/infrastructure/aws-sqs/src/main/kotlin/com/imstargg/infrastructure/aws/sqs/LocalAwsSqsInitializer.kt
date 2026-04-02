package com.imstargg.infrastructure.aws.sqs

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException

@Component
@Profile("local")
class LocalAwsSqsInitializer(
    private val sqsAsyncClient: SqsAsyncClient,
    private val properties: RenewalSqsProperties,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        createQueueIfNotExists(properties.playerQueueName)
        createQueueIfNotExists(properties.clubQueueName)
    }

    private fun createQueueIfNotExists(queueName: String) {
        try {
            sqsAsyncClient.createQueue { it.queueName(queueName) }.join()
            log.info("SQS 큐 생성 완료: {}", queueName)
        } catch (e: Exception) {
            if (e.cause is QueueNameExistsException) {
                log.info("SQS 큐 이미 존재: {}", queueName)
            } else {
                throw e
            }
        }
    }
}
