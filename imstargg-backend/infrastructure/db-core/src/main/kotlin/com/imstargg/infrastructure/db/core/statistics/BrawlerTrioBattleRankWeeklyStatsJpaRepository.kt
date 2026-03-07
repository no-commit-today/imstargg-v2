package com.imstargg.infrastructure.db.core.statistics

import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerTrioBattleRankWeeklyStatsJpaRepository :
    JpaRepository<BrawlerTrioBattleRankWeeklyStatsEntity, Long>,
    BrawlerTrioBattleRankWeeklyStatsJpaRepositoryCustom
