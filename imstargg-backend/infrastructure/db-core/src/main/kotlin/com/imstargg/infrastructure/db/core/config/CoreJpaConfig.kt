package com.imstargg.infrastructure.db.core.config

import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = ["com.imstargg.infrastructure.db.core"])
@EnableJpaRepositories(basePackages = ["com.imstargg.infrastructure.db.core"])
internal class CoreJpaConfig
