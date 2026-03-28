package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerTeamBattleResultStatsJpaRepository :
    JpaRepository<BrawlerTeamBattleResultStatsEntity, Long>,
    BrawlerTeamBattleResultStatsJpaRepositoryCustom
