package com.imstargg.core.batch.support.alert

import com.imstargg.support.http.HttpClientConfig
import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration
import org.springframework.boot.restclient.RestClientCustomizer
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

class AlertConfigTest {
    private val runner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration::class.java))
            .withUserConfiguration(AlertConfig::class.java, PropertiesConfig::class.java)

    @Test
    fun `설정이 없으면 아무것도 보내지 않는 발송기가 뜬다`() {
        runner.run { context ->
            assertThat(context.getBean(AlertManager::class.java)).isInstanceOf(NoOpsAlertManager::class.java)
        }
    }

    @Test
    fun `Discord 설정이면 Discord 발송기가 뜬다`() {
        runner.withPropertyValues(*DISCORD_PROPERTIES).run { context ->
            assertThat(context.getBean(AlertManager::class.java)).isInstanceOf(DiscordAlertManager::class.java)
        }
    }

    @Test
    fun `Discord 발송기는 컨텍스트의 RestClient 빌더를 쓴다`() {
        runner
            .withUserConfiguration(CountingCustomizerConfig::class.java)
            .withPropertyValues(*DISCORD_PROPERTIES)
            .run { context ->
                // when
                context.getBean(AlertManager::class.java)

                // then
                assertThat(context.getBean(AtomicInteger::class.java).get()).isPositive()
            }
    }

    @Test
    fun `Discord 발송기에는 공통 HTTP 설정의 응답 타임아웃이 걸린다`() {
        // given
        val server =
            HttpServer.create(InetSocketAddress("localhost", 0), 0).apply {
                createContext("/") { exchange ->
                    Thread.sleep(2000)
                    runCatching {
                        exchange.sendResponseHeaders(204, -1)
                        exchange.close()
                    }
                }
                start()
            }
        try {
            ApplicationContextRunner()
                .withConfiguration(
                    AutoConfigurations.of(
                        RestClientAutoConfiguration::class.java,
                        HttpClientAutoConfiguration::class.java,
                        ImperativeHttpClientAutoConfiguration::class.java,
                    ),
                ).withUserConfiguration(HttpClientConfig::class.java, AlertConfig::class.java, PropertiesConfig::class.java)
                .withPropertyValues(
                    "spring.http.clients.imperative.factory=http-components",
                    "spring.http.clients.connect-timeout=3s",
                    "spring.http.clients.read-timeout=300ms",
                    "app.alert.type=DISCORD",
                    "app.alert.discord.webhook-url=http://localhost:${server.address.port}/hook",
                ).run { context ->
                    val sut = context.getBean(AlertManager::class.java)
                    val command =
                        AlertCommand
                            .builder()
                            .warn()
                            .title("제목")
                            .build()

                    // when
                    val start = System.nanoTime()
                    assertThatThrownBy { sut.alert(command) }.isInstanceOf(IllegalStateException::class.java)
                    val elapsedMillis = (System.nanoTime() - start) / 1_000_000

                    // then
                    assertThat(elapsedMillis).isLessThan(1500)
                }
        } finally {
            server.stop(0)
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DiscordAlertProperties::class)
    class PropertiesConfig

    @Configuration(proxyBeanMethods = false)
    class CountingCustomizerConfig {
        @Bean
        fun customizerCalls(): AtomicInteger = AtomicInteger()

        @Bean
        fun countingCustomizer(customizerCalls: AtomicInteger): RestClientCustomizer =
            RestClientCustomizer { customizerCalls.incrementAndGet() }
    }

    companion object {
        private val DISCORD_PROPERTIES =
            arrayOf(
                "app.alert.type=DISCORD",
                "app.alert.discord.webhook-url=https://discord.test/api/webhooks/1/secret-token",
            )
    }
}
