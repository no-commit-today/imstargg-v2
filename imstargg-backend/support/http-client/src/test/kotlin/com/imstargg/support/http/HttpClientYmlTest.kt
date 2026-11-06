package com.imstargg.support.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.HttpClientSettings
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientsProperties
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Duration

/**
 * 앱이 import 하는 `http-client.yml` 만 읽어 값이 Boot 의 HTTP 클라이언트 자동 구성에 닿는지 고정한다.
 */
class HttpClientYmlTest {
    private val runner =
        ApplicationContextRunner()
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withPropertyValues("spring.config.name=http-client")
            .withConfiguration(
                AutoConfigurations.of(HttpClientAutoConfiguration::class.java, ImperativeHttpClientAutoConfiguration::class.java),
            )

    @Test
    fun `요청 팩토리는 Apache HttpClient 5 로 고정한다`() {
        runner.run { context ->
            // when
            val factory = context.getBean(ImperativeHttpClientsProperties::class.java).factory
            val builder = context.getBean(ClientHttpRequestFactoryBuilder::class.java)

            // then
            assertThat(factory).isEqualTo(ImperativeHttpClientsProperties.Factory.HTTP_COMPONENTS)
            assertThat(builder).isInstanceOf(HttpComponentsClientHttpRequestFactoryBuilder::class.java)
        }
    }

    @Test
    fun `connect 타임아웃은 3초 read 타임아웃은 10초다`() {
        runner.run { context ->
            // when
            val settings = context.getBean(HttpClientSettings::class.java)

            // then
            assertThat(settings.connectTimeout()).isEqualTo(Duration.ofSeconds(3))
            assertThat(settings.readTimeout()).isEqualTo(Duration.ofSeconds(10))
        }
    }
}
