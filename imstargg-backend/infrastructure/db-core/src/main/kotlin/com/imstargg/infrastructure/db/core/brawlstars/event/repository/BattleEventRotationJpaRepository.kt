package com.imstargg.infrastructure.db.core.brawlstars.event.repository

import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface BattleEventRotationJpaRepository : JpaRepository<BattleEventRotationEntity, Long> {
    fun findTopByOrderByCollectedAtDesc(): BattleEventRotationEntity?

    /** `collectedAt` 이 [start] 이상 [end] 미만인 로테이션을 찾는다. */
    fun findAllByCollectedAtGreaterThanEqualAndCollectedAtLessThan(
        start: Instant,
        end: Instant,
    ): List<BattleEventRotationEntity>
}
