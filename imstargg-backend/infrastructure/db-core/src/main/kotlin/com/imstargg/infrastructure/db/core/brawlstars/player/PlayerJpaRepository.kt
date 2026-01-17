package com.imstargg.infrastructure.db.core.brawlstars.player

import org.springframework.data.jpa.repository.JpaRepository

interface PlayerJpaRepository : JpaRepository<PlayerEntity, Long>
