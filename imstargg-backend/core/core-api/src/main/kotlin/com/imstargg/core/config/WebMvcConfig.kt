package com.imstargg.core.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

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
}
