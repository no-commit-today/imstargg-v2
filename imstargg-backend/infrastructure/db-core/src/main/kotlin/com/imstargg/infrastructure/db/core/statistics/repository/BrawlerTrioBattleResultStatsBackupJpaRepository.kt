package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.infrastructure.db.core.statistics.BrawlerTrioBattleResultStatsBackupEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerTrioBattleResultStatsBackupJpaRepository :
    JpaRepository<BrawlerTrioBattleResultStatsBackupEntity, Long>,
    BrawlerTrioBattleResultStatsBackupJpaRepositoryCustom
