package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.infrastructure.db.core.statistics.BrawlerPairBattleRankStatsEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerPairBattleRankStatsJpaRepository :
    JpaRepository<BrawlerPairBattleRankStatsEntity, Long>,
    BrawlerPairBattleRankStatsJpaRepositoryCustom
