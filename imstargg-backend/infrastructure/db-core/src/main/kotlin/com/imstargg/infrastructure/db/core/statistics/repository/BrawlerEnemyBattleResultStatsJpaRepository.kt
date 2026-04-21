package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultStatsEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerEnemyBattleResultStatsJpaRepository :
    JpaRepository<BrawlerEnemyBattleResultStatsEntity, Long>,
    BrawlerEnemyBattleResultStatsJpaRepositoryCustom
