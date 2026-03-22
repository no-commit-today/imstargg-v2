package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.infrastructure.db.core.statistics.battle.AggregatedResultBattleKeyEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AggregatedResultBattleKeyJpaRepository :
    JpaRepository<AggregatedResultBattleKeyEntity, Long>,
    AggregatedResultBattleKeyJpaRepositoryCustom
