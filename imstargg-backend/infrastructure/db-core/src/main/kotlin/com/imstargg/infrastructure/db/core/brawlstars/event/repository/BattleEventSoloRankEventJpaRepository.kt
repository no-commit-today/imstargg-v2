package com.imstargg.infrastructure.db.core.brawlstars.event

import org.springframework.data.jpa.repository.JpaRepository

interface BattleEventSoloRankEventJpaRepository : JpaRepository<BattleEventSoloRankEventEntity, Long> {
    fun findBySoloRankSeasonKey(soloRankSeasonKey: String): List<BattleEventSoloRankEventEntity>
}
