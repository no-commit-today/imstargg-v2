package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.infrastructure.db.core.statistics.BrawlerTrioBattleResultStatsEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerTrioBattleResultStatsJpaRepository :
    JpaRepository<BrawlerTrioBattleResultStatsEntity, Long>,
    BrawlerTrioBattleResultStatsJpaRepositoryCustom
