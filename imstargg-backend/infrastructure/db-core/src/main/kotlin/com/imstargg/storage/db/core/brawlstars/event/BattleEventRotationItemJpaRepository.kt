package com.imstargg.storage.db.core.brawlstars.event

import org.springframework.data.jpa.repository.JpaRepository

interface BattleEventRotationItemJpaRepository : JpaRepository<BattleEventRotationItemEntity, Long>
