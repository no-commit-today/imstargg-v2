package com.imstargg.client.brawlstars

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import feign.RequestInterceptor
import feign.codec.Decoder
import org.springframework.beans.factory.ObjectFactory
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder
import org.springframework.cloud.openfeign.support.SpringDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter

@Configuration
@EnableConfigurationProperties(BrawlStarsClientProperties::class)
internal class BrawlStarsApiConfig {
    @Bean
    fun brawlStarsApiKeyInterceptor(properties: BrawlStarsClientProperties): RequestInterceptor = BrawlStarsApiKeyInterceptor(properties.apiKeys)

    @Bean
    fun brawlStarsFeignDecoder(): Decoder {
        val objectMapper =
            ObjectMapper()
                .registerModule(JavaTimeModule())
                .registerModule(KotlinModule.Builder().build())
        val converter = MappingJackson2HttpMessageConverter(objectMapper)
        val messageConverters: ObjectFactory<HttpMessageConverters> =
            ObjectFactory {
                HttpMessageConverters(converter)
            }
        return ResponseEntityDecoder(SpringDecoder(messageConverters))
    }
}
