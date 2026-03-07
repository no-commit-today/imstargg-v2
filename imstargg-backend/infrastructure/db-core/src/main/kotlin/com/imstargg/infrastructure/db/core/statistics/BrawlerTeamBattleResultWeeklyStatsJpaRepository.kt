package com.imstargg.infrastructure.db.core.statistics

import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerTeamBattleResultWeeklyStatsJpaRepository :
    JpaRepository<BrawlerTeamBattleResultWeeklyStatsEntity, Long>,
    BrawlerTeamBattleResultWeeklyStatsJpaRepositoryCustom
