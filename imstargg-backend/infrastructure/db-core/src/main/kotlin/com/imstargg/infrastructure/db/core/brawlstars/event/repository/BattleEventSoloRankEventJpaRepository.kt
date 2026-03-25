package com.imstargg.infrastructure.db.core.brawlstars.event.repository

import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventSoloRankEventEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BattleEventSoloRankEventJpaRepository : JpaRepository<BattleEventSoloRankEventEntity, Long> {
    fun findAllBySoloRankSeasonKey(soloRankSeasonKey: String): List<BattleEventSoloRankEventEntity>
}
