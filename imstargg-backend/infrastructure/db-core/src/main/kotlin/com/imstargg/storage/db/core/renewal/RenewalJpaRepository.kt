package com.imstargg.storage.db.core.renewal

import org.springframework.data.jpa.repository.JpaRepository

interface RenewalJpaRepository : JpaRepository<RenewalEntity, Long>
