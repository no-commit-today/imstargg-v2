package com.imstargg.core.worker.consumer

import com.imstargg.core.worker.application.player.PlayerRenewalService
import com.imstargg.infrastructure.aws.sqs.message.PlayerRenewalMessage
import io.awspring.cloud.sqs.annotation.SqsListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class PlayerRenewalMessageConsumer(
    private val playerRenewalService: PlayerRenewalService,
) {
    @SqsListener($$"${app.aws.sqs.renewal.player-queue-name}")
    fun handle(message: PlayerRenewalMessage) {
        log.info { "[플레이어 갱신] 시작 tag=${message.tag}" }
        val startedAt = System.nanoTime()
        playerRenewalService.renew(message.tag)
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        log.info { "[플레이어 갱신] 완료 tag=${message.tag} elapsedMs=$elapsedMs" }
    }
}
