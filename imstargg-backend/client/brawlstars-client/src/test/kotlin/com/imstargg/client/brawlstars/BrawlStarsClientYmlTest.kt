package com.imstargg.client.brawlstars

import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.springboot.ratelimiter.autoconfigure.RateLimiterAutoConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.logging.LogLevel
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * 앱이 import 하는 `brawlstars-client.yml` 만 읽어 값이 제한기·프로퍼티에 닿는지 고정한다.
 * 이 모듈의 테스트 `application.yml` 은 제한기 값을 덮으므로 `spring.config.name` 으로 그 파일을 읽지 않는다.
 */
class BrawlStarsClientYmlTest {
    private val runner =
        ApplicationContextRunner()
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withPropertyValues("spring.config.name=brawlstars-client")
            .withConfiguration(AutoConfigurations.of(RateLimiterAutoConfiguration::class.java))
            .withUserConfiguration(PropertiesConfig::class.java)

    @Test
    fun `기본 프로필 제한기는 1초에 1회 허가하고 허가를 600초까지 기다린다`() {
        runner.run { context ->
            // when
            val config =
                context
                    .getBean(RateLimiterRegistry::class.java)
                    .rateLimiter(BrawlStarsClientConfig.RATE_LIMITER_NAME)
                    .rateLimiterConfig

            // then
            assertThat(config.limitForPeriod).isEqualTo(1)
            assertThat(config.limitRefreshPeriod).isEqualTo(Duration.ofSeconds(1))
            assertThat(config.timeoutDuration).isEqualTo(Duration.ofSeconds(600))
        }
    }

    @Test
    fun `배치 동시 실행 상한은 5 다`() {
        runner.run { context ->
            // when
            val properties = context.getBean(BrawlStarsClientProperties::class.java)

            // then
            assertThat(properties.batchConcurrency).isEqualTo(5)
        }
    }

    @Test
    fun `prod 프로필 제한기는 1초에 10회 허가하고 대기 상한 600초는 그대로다`() {
        runner.withPropertyValues("spring.profiles.active=prod").run { context ->
            // when
            val config =
                context
                    .getBean(RateLimiterRegistry::class.java)
                    .rateLimiter(BrawlStarsClientConfig.RATE_LIMITER_NAME)
                    .rateLimiterConfig

            // then
            assertThat(config.limitForPeriod).isEqualTo(10)
            assertThat(config.limitRefreshPeriod).isEqualTo(Duration.ofSeconds(1))
            assertThat(config.timeoutDuration).isEqualTo(Duration.ofSeconds(600))
        }
    }

    @Test
    fun `prod 프로필은 BrawlStarsApi 호출 로그를 WARN 으로 올린다`() {
        runner.withPropertyValues("spring.profiles.active=prod").run { context ->
            // when — LoggingApplicationListener 가 읽는 방식(logging.level 맵)으로 읽는다. 로거 이름은 ApiLoggingInterceptor 가 쓰는 Api FQCN 이다
            val levels =
                Binder
                    .get(context.environment)
                    .bind("logging.level", Bindable.mapOf(String::class.java, LogLevel::class.java))
                    .get()

            // then
            assertThat(levels[BrawlStarsApi::class.java.name]).isEqualTo(LogLevel.WARN)
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(BrawlStarsClientProperties::class)
    class PropertiesConfig
}
