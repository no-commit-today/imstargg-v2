package com.imstargg.core.worker.consumer

import com.imstargg.core.worker.application.ClubRenewalService
import com.imstargg.infrastructure.aws.sqs.message.ClubRenewalMessage
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ClubRenewalMessageConsumer(
    private val clubRenewalService: ClubRenewalService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @SqsListener($$"${app.aws.sqs.renewal.club-queue-name}")
    fun handle(message: ClubRenewalMessage) {
        log.info("[클럽 갱신] 시작 tag={}", message.tag)
        clubRenewalService.renew(message.tag)
        log.info("[클럽 갱신] 완료 tag={}", message.tag)
    }
}
