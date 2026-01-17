package com.imstargg.infrastructure.db.core.renewal

import org.springframework.data.jpa.repository.JpaRepository

interface RenewalJpaRepository : JpaRepository<RenewalEntity, Long>
