package com.imstargg.core.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.WebContentInterceptor
import java.time.Duration

@Configuration
class WebMvcConfig(
    private val cors: CorsProperties,
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        if (cors.allowedOrigins.isEmpty()) return
        registry
            .addMapping("/v1/**")
            .allowedOrigins(*cors.allowedOrigins.toTypedArray())
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowCredentials(false)
            .maxAge(3600)
    }

    /**
     * 정적 성격 GET(브롤러·이벤트·브롤러 통계)에만 Cache-Control 부여.
     * 서버 캐시 TTL(Caffeine 5분 / Redis statistics 30분)과 일치시킨다.
     * 플레이어 데이터(프로필·전투·갱신·플레이어 통계)는 갱신 직후 최신성이 중요하므로 제외.
     */
    override fun addInterceptors(registry: InterceptorRegistry) {
        val cacheControlInterceptor =
            WebContentInterceptor().apply {
                addCacheMapping(
                    CacheControl.maxAge(Duration.ofMinutes(5)).staleWhileRevalidate(Duration.ofMinutes(1)),
                    "/v1/brawlers/**",
                    "/v1/brawler/**",
                    "/v1/events/**",
                    "/v1/event/**",
                )
                addCacheMapping(
                    CacheControl.maxAge(Duration.ofMinutes(30)).staleWhileRevalidate(Duration.ofMinutes(5)),
                    "/v1/statistics/brawler/**",
                )
            }
        registry
            .addInterceptor(cacheControlInterceptor)
            .addPathPatterns(
                "/v1/brawlers/**",
                "/v1/brawler/**",
                "/v1/events/**",
                "/v1/event/**",
                "/v1/statistics/brawler/**",
            )
    }
}
