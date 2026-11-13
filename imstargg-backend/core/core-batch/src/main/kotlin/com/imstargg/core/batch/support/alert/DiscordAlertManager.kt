package com.imstargg.core.batch.support.alert

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

private val log = KotlinLogging.logger {}

/**
 * Discord 웹훅으로 알림을 보낸다. 전송에 실패하면 예외를 던진다.
 *
 * - 상태 코드 오류(`RestClientResponseException`)는 받은 예외를 그대로 던진다. Discord 의 503 이
 *   `HttpServerErrorException.ServiceUnavailable` 로 나가야 `BrawlStarsApiTasklet` 이 점검으로 넘길 수 있다.
 * - 연결 오류(`ResourceAccessException`)의 메시지에는 요청 주소가 들어 있다. 웹훅 주소는 비밀이므로
 *   원인 예외를 잇지 않고 주소를 뺀 `IllegalStateException` 으로 바꿔 던진다.
 *
 * 실패하면 알림 문구 전문을 ERROR 로그에 남긴다. 어느 경우에도 웹훅 주소는 로그에 적지 않는다.
 */
internal class DiscordAlertManager(
    restClientBuilder: RestClient.Builder,
    private val webhookUrl: String,
) : AlertManager {
    private val restClient: RestClient = restClientBuilder.build()

    override fun alert(alertCommand: AlertCommand) {
        val request = DiscordWebhookRequest.from(alertCommand)
        try {
            restClient
                .post()
                .uri(webhookUrl)
                .body(request)
                .retrieve()
                .body(Void::class.java)
        } catch (ex: RestClientResponseException) {
            log.error(ex) { "Discord 알림 전송 실패 - 상태: ${ex.statusCode}, 알림 문구:\n${request.content}" }
            throw ex
        } catch (ex: ResourceAccessException) {
            val causeName = (ex.cause ?: ex).javaClass.simpleName
            log.error { "Discord 알림 전송 실패 - 원인: $causeName, 알림 문구:\n${request.content}" }
            throw IllegalStateException("Discord 알림 전송 실패: $causeName")
        }
    }
}
