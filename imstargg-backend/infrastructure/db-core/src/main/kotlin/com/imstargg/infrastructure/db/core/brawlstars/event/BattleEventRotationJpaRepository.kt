package com.imstargg.infrastructure.db.core.brawlstars.event

import org.springframework.data.jpa.repository.JpaRepository

interface BattleEventRotationJpaRepository : JpaRepository<BattleEventRotationEntity, Long>
