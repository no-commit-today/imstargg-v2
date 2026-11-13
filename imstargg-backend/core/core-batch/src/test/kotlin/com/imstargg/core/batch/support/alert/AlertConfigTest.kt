package com.imstargg.core.batch.support.alert

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.restclient.RestClientCustomizer
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
