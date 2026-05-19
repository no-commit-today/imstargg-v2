package com.imstargg.infrastructure.db.core.test

import com.imstargg.infrastructure.db.core.lock.LockName
import com.imstargg.infrastructure.db.core.lock.LockTemplate
import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.jdbc.core.JdbcTemplate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

@TestConfiguration
@EnableJpaAuditing
class TestJpaConfig {
    /**
     * H2 테스트용 DataSource.
     *
     * application-test-db.yml 의 storage.datasource.core 설정을 사용한다.
     * INIT URL 파라미터를 통해 `CREATE DOMAIN JSONB AS JSON`을 실행하여
     * H2에서 PostgreSQL `jsonb` 컬럼 정의가 동작하도록 한다.
     *
     * @AutoConfigureTestDatabase(replace=NONE) 과 함께 사용하면
     * Spring Boot 자동 구성 embedded DataSource 대신 이 Bean 이 사용된다.
     */
    @Primary
    @Bean
    @ConfigurationProperties(prefix = "storage.datasource.core")
    fun coreDataSource(): HikariDataSource = DataSourceBuilder.create().type(HikariDataSource::class.java).build()!!

    @Bean
    fun cleanUp(
        jdbcTemplate: JdbcTemplate,
        entityManagerFactory: EntityManagerFactory,
    ): CleanUp = CleanUp(jdbcTemplate, entityManagerFactory)

    @Bean
    fun lockTemplate(): LockTemplate = LocalLockTemplate()
}

class LocalLockTemplate : LockTemplate {
    private val locks = ConcurrentHashMap<String, ReentrantLock>()

    override fun <T> execute(
        lockName: LockName,
        timeoutSeconds: Int,
        supplier: () -> T,
    ): T {
        val lock = locks.computeIfAbsent(lockName.value) { ReentrantLock() }
        val acquired = lock.tryLock(timeoutSeconds.toLong(), TimeUnit.SECONDS)
        if (!acquired) {
            throw IllegalStateException("락 획득 타임아웃: ${lockName.value}")
        }
        try {
            return supplier()
        } finally {
            lock.unlock()
        }
    }
}
