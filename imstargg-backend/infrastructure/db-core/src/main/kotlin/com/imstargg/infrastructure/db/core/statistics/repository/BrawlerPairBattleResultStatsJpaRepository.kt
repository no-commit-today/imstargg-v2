package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.infrastructure.db.core.statistics.BrawlerPairBattleResultStatsEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerPairBattleResultStatsJpaRepository :
    JpaRepository<BrawlerPairBattleResultStatsEntity, Long>,
    BrawlerPairBattleResultStatsJpaRepositoryCustom
