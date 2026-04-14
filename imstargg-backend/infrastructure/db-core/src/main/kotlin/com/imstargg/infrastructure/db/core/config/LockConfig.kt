package com.imstargg.infrastructure.db.core.config

import com.imstargg.infrastructure.db.core.lock.PostgresAdvisoryLockTemplate
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("db-lock")
class LockConfig {
    @Bean
    @ConfigurationProperties(prefix = "storage.datasource.lock")
    fun lockDataSource(): HikariDataSource = DataSourceBuilder.create().type(HikariDataSource::class.java).build()!!

    @Bean
    fun lockTemplate() = PostgresAdvisoryLockTemplate(lockDataSource())
}
