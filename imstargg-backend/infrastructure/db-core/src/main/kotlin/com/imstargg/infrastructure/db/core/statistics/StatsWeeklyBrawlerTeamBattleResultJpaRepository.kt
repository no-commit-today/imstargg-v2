package com.imstargg.infrastructure.db.core.statistics

import org.springframework.data.jpa.repository.JpaRepository

interface StatsWeeklyBrawlerTeamBattleResultJpaRepository :
    JpaRepository<StatsWeeklyBrawlerTeamBattleResultEntity, Long>,
    StatsWeeklyBrawlerTeamBattleResultJpaRepositoryCustom
