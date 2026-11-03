package com.imstargg.core.batch.support.coreapi

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

private val log = KotlinLogging.logger {}

/**
 * core-batch → core-api 내부 호출 클라이언트.
 *
 * 갱신 상태 머신(쿨다운·삭제·점검 검사)과 SQS 발행은 core-api 가 단일 진입점으로
 * 수행한다 — batch 가 renewal 파이프라인(renewal row·SQS)을 직접 조작하지 않기 위한 경계.
 */
@Component
class CoreApiClient(
    private val restClient: RestClient,
) {
    fun renewPlayer(tag: String): PlayerRenewalResult =
        try {
            restClient
                .post()
                .uri("/v1/players/{tag}/renewal", tag)
                .retrieve()
                .toBodilessEntity()
            PlayerRenewalResult.ACCEPTED
        } catch (ex: HttpStatusCodeException) {
            when (ex.statusCode.value()) {
                409 -> PlayerRenewalResult.ALREADY_IN_PROGRESS
                410 -> PlayerRenewalResult.PLAYER_DELETED
                503 -> PlayerRenewalResult.API_UNDER_MAINTENANCE
                else -> {
                    log.warn { "플레이어 갱신 요청 실패 - tag: $tag, status: ${ex.statusCode}" }
                    PlayerRenewalResult.FAILED
                }
            }
        } catch (ex: RestClientException) {
            log.warn(ex) { "플레이어 갱신 요청 실패 - tag: $tag" }
            PlayerRenewalResult.FAILED
        }
}
