package com.imstargg.core.config

import org.springframework.core.MethodParameter
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.util.AntPathMatcher
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import java.time.Duration

/**
 * 정적 성격 GET(브롤러·이벤트·브롤러 통계)의 **성공(2xx) 응답에만** Cache-Control 을 부여한다.
 * 서버 캐시 TTL(Caffeine 5분 / Redis statistics 30분)과 일치시킨다.
 * 에러 응답(4xx/5xx)에 붙이면 브라우저가 에러를 캐시해 복구 후에도 사용자에게 에러가 남으므로 제외.
 * 플레이어 데이터(프로필·전투·갱신·플레이어 통계)는 갱신 직후 최신성이 중요하므로 대상이 아니다.
 */
@RestControllerAdvice
class CacheControlResponseAdvice : ResponseBodyAdvice<Any> {
    private val pathMatcher = AntPathMatcher()

    private val shortCachePatterns =
        listOf("/v1/brawlers/**", "/v1/brawler/**", "/v1/events/**", "/v1/event/**")
    private val statisticsCachePatterns = listOf("/v1/statistics/brawler/**")

    private val shortCacheControl =
        CacheControl.maxAge(Duration.ofMinutes(5)).staleWhileRevalidate(Duration.ofMinutes(1)).headerValue
    private val statisticsCacheControl =
        CacheControl.maxAge(Duration.ofMinutes(30)).staleWhileRevalidate(Duration.ofMinutes(5)).headerValue

    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>,
    ): Boolean = true

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? {
        val servletRequest = (request as? ServletServerHttpRequest)?.servletRequest ?: return body
        val servletResponse = (response as? ServletServerHttpResponse)?.servletResponse ?: return body
        if (servletRequest.method != "GET") return body
        if (servletResponse.status !in 200..299) return body

        val path = servletRequest.requestURI
        val cacheControl =
            when {
                shortCachePatterns.any { pathMatcher.match(it, path) } -> shortCacheControl
                statisticsCachePatterns.any { pathMatcher.match(it, path) } -> statisticsCacheControl
                else -> null
            } ?: return body

        servletResponse.setHeader(HttpHeaders.CACHE_CONTROL, cacheControl)
        return body
    }
}
