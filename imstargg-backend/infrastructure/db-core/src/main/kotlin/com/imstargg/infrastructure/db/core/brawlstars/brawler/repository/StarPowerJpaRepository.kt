package com.imstargg.infrastructure.db.core.brawlstars.brawler

import org.springframework.data.jpa.repository.JpaRepository

interface StarPowerJpaRepository : JpaRepository<StarPowerEntity, Long>
