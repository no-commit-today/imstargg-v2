package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleRankStatsEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerTeamBattleRankStatsJpaRepository :
    JpaRepository<BrawlerTeamBattleRankStatsEntity, Long>,
    BrawlerTeamBattleRankStatsJpaRepositoryCustom
