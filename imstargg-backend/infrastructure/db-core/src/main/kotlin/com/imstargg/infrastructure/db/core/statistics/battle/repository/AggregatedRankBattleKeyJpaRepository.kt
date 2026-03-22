package com.imstargg.infrastructure.db.core.statistics.battle

import org.springframework.data.jpa.repository.JpaRepository

interface AggregatedRankBattleKeyJpaRepository :
    JpaRepository<AggregatedRankBattleKeyEntity, Long>,
    AggregatedRankBattleKeyJpaRepositoryCustom
