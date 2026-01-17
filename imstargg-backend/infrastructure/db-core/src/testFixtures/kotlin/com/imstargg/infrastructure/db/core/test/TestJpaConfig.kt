package com.imstargg.infrastructure.db.core.test

import jakarta.persistence.EntityManagerFactory
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.jdbc.core.JdbcTemplate

@TestConfiguration
@EnableJpaAuditing
class TestJpaConfig {
    @Bean
    fun testDataManager(
        jdbcTemplate: JdbcTemplate,
        entityManagerFactory: EntityManagerFactory,
    ): TestDataManager = TestDataManager(jdbcTemplate, entityManagerFactory)
}
