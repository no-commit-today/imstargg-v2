package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultStatsBackupEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerEnemyBattleResultStatsBackupJpaRepository :
    JpaRepository<BrawlerEnemyBattleResultStatsBackupEntity, Long>,
    BrawlerEnemyBattleResultStatsBackupJpaRepositoryCustom
