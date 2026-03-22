package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.MappedSuperclass
import java.time.LocalDate
import java.time.OffsetDateTime

@MappedSuperclass
abstract class BaseBattleRankStatsEntity(
    @Column(name = "event_brawlstars_id", updatable = false, nullable = false)
    val eventBrawlStarsId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "tier_range", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val tierRange: StatsTierRange,
    @Column(name = "date", updatable = false, nullable = false)
    val date: LocalDate,
    @Column(name = "start_at", updatable = false, nullable = false)
    val startAt: OffsetDateTime,
    @Column(name = "end_at", updatable = false, nullable = false)
    val endAt: OffsetDateTime,
) : BaseTimeEntity() {
    init {
        require(startAt.isBefore(endAt)) {
            "startAt($startAt)은 endAt($endAt)보다 이전이어야 합니다."
        }
    }
}
