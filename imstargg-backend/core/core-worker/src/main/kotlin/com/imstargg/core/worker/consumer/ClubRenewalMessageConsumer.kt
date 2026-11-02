package com.imstargg.core.worker.consumer

import com.imstargg.core.worker.application.club.ClubRenewalService
import com.imstargg.infrastructure.aws.sqs.message.ClubRenewalMessage
import io.awspring.cloud.sqs.annotation.SqsListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class ClubRenewalMessageConsumer(
    private val clubRenewalService: ClubRenewalService,
) {
    @SqsListener($$"${app.aws.sqs.renewal.club-queue-name}")
    fun handle(message: ClubRenewalMessage) {
        log.info { "[클럽 갱신] 시작 tag=${message.tag}" }
        clubRenewalService.renew(message.tag)
        log.info { "[클럽 갱신] 완료 tag=${message.tag}" }
    }
}
