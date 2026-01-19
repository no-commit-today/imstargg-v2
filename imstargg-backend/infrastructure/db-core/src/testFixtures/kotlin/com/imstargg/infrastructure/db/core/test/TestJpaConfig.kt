package com.imstargg.infrastructure.db.core.test

import com.imstargg.infrastructure.db.core.lock.LockName
import com.imstargg.infrastructure.db.core.lock.LockTemplate
import jakarta.persistence.EntityManagerFactory
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.jdbc.core.JdbcTemplate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

@TestConfiguration
@EnableJpaAuditing
class TestJpaConfig {
    @Bean
    fun testDataManager(
        jdbcTemplate: JdbcTemplate,
        entityManagerFactory: EntityManagerFactory,
    ): TestDataManager = TestDataManager(jdbcTemplate, entityManagerFactory)

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
