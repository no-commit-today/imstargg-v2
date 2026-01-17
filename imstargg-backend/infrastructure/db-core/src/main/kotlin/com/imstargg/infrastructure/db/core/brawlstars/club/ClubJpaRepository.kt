package com.imstargg.infrastructure.db.core.brawlstars.club

import org.springframework.data.jpa.repository.JpaRepository

interface ClubJpaRepository : JpaRepository<ClubEntity, Long>
