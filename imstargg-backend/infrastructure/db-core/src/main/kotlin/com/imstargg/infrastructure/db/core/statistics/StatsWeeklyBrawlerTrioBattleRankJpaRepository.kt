package com.imstargg.infrastructure.db.core.statistics

import org.springframework.data.jpa.repository.JpaRepository

interface StatsWeeklyBrawlerTrioBattleRankJpaRepository :
    JpaRepository<StatsWeeklyBrawlerTrioBattleRankEntity, Long>,
    StatsWeeklyBrawlerTrioBattleRankJpaRepositoryCustom
