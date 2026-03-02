package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.MappedSuperclass
import java.time.LocalDate

@MappedSuperclass
abstract class BaseWeeklyBattleResultEntity(
    @Column(name = "event_brawlstars_id", updatable = false, nullable = false)
    val eventBrawlStarsId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "tier_range", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val tierRange: StatsTierRange,
    @Column(name = "start_date", updatable = false, nullable = false)
    val startDate: LocalDate,
    @Column(name = "end_date", updatable = false, nullable = false)
    val endDate: LocalDate,
    victoryCount: Long,
    defeatCount: Long,
    drawCount: Long,
) : BaseTimeEntity() {
    @Column(name = "victory_count", nullable = false)
    var victoryCount: Long = victoryCount
        protected set

    @Column(name = "defeat_count", nullable = false)
    var defeatCount: Long = defeatCount
        protected set

    @Column(name = "draw_count", nullable = false)
    var drawCount: Long = drawCount
        protected set
}
