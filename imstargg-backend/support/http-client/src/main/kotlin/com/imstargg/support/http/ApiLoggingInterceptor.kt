package com.imstargg.support.http

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.MethodParameter
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.service.invoker.HttpRequestValues
import java.lang.reflect.Method

/**
 * 외부 API 호출 로깅. 메서드·경로·쿼리·상태·소요 시간만 남기고 헤더와 본문은 남기지 않는다.
 * 가리는 일은 하지 않는다 — API 키를 붙이는 인터셉터(`BrawlStarsApiKeyInterceptor`)를 이 인터셉터보다 **뒤에** 등록해,
 * 키가 붙기 전 URI 만 이 인터셉터에 보이게 한다. 호출부가 헤더로 넘기는 값은 헤더를 남기지 않으므로 보이지 않는다.
 *
 * Feign 처럼 호출한 `@HttpExchange` 인터페이스(`XxxApi`)의 로거로 `[XxxApi#메서드]` 를 붙여 남긴다 — `logging.level.<XxxApi FQCN>` 으로
 * Api 마다 끄고 켤 수 있다. 인터셉터는 그룹의 RestClient 하나에 걸려 어느 Api 가 불렀는지 모르므로, [HttpRequestValues.Processor] 로서
 * 프록시가 요청을 만들 때 호출 메서드를 요청 속성에 실어 둔다. 그래서 그룹 설정에서 같은 인스턴스를 `requestInterceptor` 와
 * `httpRequestValuesProcessor` 양쪽에 등록한다 — 한쪽만 등록하면 첫 호출에서 [IllegalStateException] 으로 드러난다.
 *
 * 응답이 오면 상태와 관계없이 INFO 로 남긴다 — 4xx·5xx 도 호출부가 예외로 받아 처리하는 정상 흐름이다. 응답을 받지 못한 전송 예외는 WARN 으로 남기고 그대로 올린다.
 */
class ApiLoggingInterceptor :
    ClientHttpRequestInterceptor,
    HttpRequestValues.Processor {
    override fun process(
        method: Method,
        parameters: Array<MethodParameter>,
        arguments: Array<Any?>,
        builder: HttpRequestValues.Builder,
    ) {
        builder.addAttribute(API_METHOD_ATTRIBUTE, method)
    }

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val startedAt = System.nanoTime()
        val method =
            checkNotNull(request.attributes[API_METHOD_ATTRIBUTE] as Method?) {
                "ApiLoggingInterceptor 를 HttpServiceProxyFactory 의 httpRequestValuesProcessor 에도 등록해야 한다"
            }
        val log = KotlinLogging.logger(method.declaringClass.name)
        val tag = "${method.declaringClass.simpleName}#${method.name}"
        val uri = request.uri.rawPath + (request.uri.rawQuery?.let { "?$it" } ?: "")
        try {
            val response = execution.execute(request, body)
            log.info { "[$tag] ${request.method} $uri -> ${response.statusCode.value()} (${elapsedMillis(startedAt)}ms)" }
            return response
        } catch (ex: Exception) {
            log.warn { "[$tag] ${request.method} $uri -> ${ex.javaClass.simpleName} (${elapsedMillis(startedAt)}ms)" }
            throw ex
        }
    }

    private fun elapsedMillis(startedAtNanos: Long): Long = (System.nanoTime() - startedAtNanos) / 1_000_000

    companion object {
        private val API_METHOD_ATTRIBUTE = "${ApiLoggingInterceptor::class.java.name}.apiMethod"
    }
}
