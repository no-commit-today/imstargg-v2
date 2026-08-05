package com.imstargg.core.worker.consumer

import com.imstargg.core.worker.application.player.PlayerRenewalService
import com.imstargg.infrastructure.aws.sqs.message.PlayerRenewalMessage
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PlayerRenewalMessageConsumer(
    private val playerRenewalService: PlayerRenewalService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @SqsListener($$"${app.aws.sqs.renewal.player-queue-name}")
    fun handle(message: PlayerRenewalMessage) {
        log.info("[플레이어 갱신] 시작 tag={}", message.tag)
        val startedAt = System.nanoTime()
        playerRenewalService.renew(message.tag)
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        log.info("[플레이어 갱신] 완료 tag={} elapsedMs={}", message.tag, elapsedMs)
    }
}
