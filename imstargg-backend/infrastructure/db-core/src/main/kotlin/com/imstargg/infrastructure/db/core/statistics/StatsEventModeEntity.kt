package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsEventModeType
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * 통계를 수집하는 이벤트 모드. 행이 있으면 수집하고 없으면 수집하지 않는다.
 */
@Entity
@Table(
    name = "stats_event_mode",
    indexes = [
        Index(
            name = "uk_stats_event_mode__event_mode_brawlstars_id",
            columnList = "event_mode_brawlstars_id",
            unique = true,
        ),
    ],
)
class StatsEventModeEntity(
    @Column(name = "event_mode_brawlstars_id", updatable = false, nullable = false)
    val eventModeBrawlStarsId: Long,
    type: StatsEventModeType,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_event_mode_id", nullable = false)
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 25, nullable = false)
    var type: StatsEventModeType = type
        protected set

    fun updateType(type: StatsEventModeType) {
        this.type = type
    }
}
