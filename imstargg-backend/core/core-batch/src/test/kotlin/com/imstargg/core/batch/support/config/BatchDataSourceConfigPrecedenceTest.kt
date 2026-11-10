package com.imstargg.core.batch.support.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * 운영 jar 와 같은 설정 파일 구성에서 core-batch 의 DB 설정이 db-core 의 `application-db.yml` 을 이기는지 확인한다.
 *
 * 프로필 전용 파일(`application-db.yml`)은 `application.yml` 안의 `on-profile` 문서보다 우선한다. 그래서
 * core-batch 가 `application.yml` 의 prod 문서에 둔 `socketTimeout` 은 무시되고 운영 배치의 소켓 읽기 제한이
 * db-core 기본값 3초였다(2026-11-07 부터 카운터·2인 잡이 `SocketTimeoutException` 으로 실패). core-batch 는 그룹 끝의
 * `batch-db` 프로필 파일로 덮는다. 테스트 리소스의 `application.yml` 이 main 의 것을 가리므로, main 의 파일과
 * 의존 모듈의 설정 파일을 한 폴더에 모아 `spring.config.location` 으로 띄운다.
 */
class BatchDataSourceConfigPrecedenceTest {
    @TempDir
    private lateinit var configDir: Path

    @ParameterizedTest
    @ValueSource(strings = ["local", "local-dev", "dev", "prod"])
    fun `core-batch 의 DB 소켓 읽기 제한과 풀 크기가 db-core 기본값을 이긴다`(profile: String) {
        // given
        prepareProductionLikeConfigDir()

        // when
        val environment =
            SpringApplicationBuilder(EmptyConfig::class.java)
                .web(WebApplicationType.NONE)
                .run("--spring.config.location=file:$configDir/", "--spring.profiles.active=$profile")
                .use { it.environment }

        // then
        assertThat(environment.getProperty("storage.datasource.core.data-source-properties.socketTimeout"))
            .isEqualTo(SOCKET_TIMEOUT_SECONDS)
        assertThat(environment.getProperty("storage.datasource.core.maximum-pool-size")).isEqualTo("5")
    }

    private fun prepareProductionLikeConfigDir() {
        File(MAIN_RESOURCES_DIR).listFiles { file -> file.extension == "yml" }!!.forEach {
            Files.copy(it.toPath(), configDir.resolve(it.name))
        }
        DEPENDENCY_CONFIG_FILES.forEach { name ->
            val resources = javaClass.classLoader.getResources(name).toList()
            assertThat(resources).describedAs("클래스패스의 $name").hasSize(1)
            resources.single().openStream().use { Files.copy(it, configDir.resolve(name)) }
        }
    }

    /** 설정만 읽으면 되므로 빈이 없다. 컴포넌트 스캔에 잡히지 않도록 `@Configuration` 을 달지 않는다. */
    class EmptyConfig

    companion object {
        private const val SOCKET_TIMEOUT_SECONDS = "600"
        private const val MAIN_RESOURCES_DIR = "src/main/resources"
        private val DEPENDENCY_CONFIG_FILES =
            listOf("application-db.yml", "application-db-lock.yml", "logging.yml", "http-client.yml", "brawlstars-client.yml")
    }
}
