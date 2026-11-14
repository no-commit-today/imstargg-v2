package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.infrastructure.db.core.statistics.BrawlerPairBattleResultStatsBackupEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerPairBattleResultStatsBackupJpaRepository :
    JpaRepository<BrawlerPairBattleResultStatsBackupEntity, Long>,
    BrawlerPairBattleResultStatsBackupJpaRepositoryCustom
