package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.infrastructure.db.core.statistics.battle.AggregatedRankBattleKeyEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AggregatedRankBattleKeyJpaRepository :
    JpaRepository<AggregatedRankBattleKeyEntity, Long>,
    AggregatedRankBattleKeyJpaRepositoryCustom
