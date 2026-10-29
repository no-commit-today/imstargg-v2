package com.imstargg.infrastructure.db.core.brawlstars.event

import com.imstargg.infrastructure.db.core.BaseSoftDeleteEntity
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * 삭제(soft-delete)된 이벤트도 core-api 조회에서 거르지 않는다 —
 * 과거 전투·통계가 이벤트를 ID 로 참조해 맵 이름·모드를 표시해야 하기 때문이다.
 * 현재 삭제 표시를 읽는 곳은 없다(`BattleEventSyncItemWriter` 는 미등록·불일치만 감지).
 */
@Entity
@Table(
    name = "battle_event",
    indexes = [
        Index(name = "uk_battle_event__brawlstarsid", columnList = "brawlstars_id", unique = true),
    ],
)
class BattleEventEntity(
    @Column(name = "brawlstars_id", updatable = false, nullable = false)
    val brawlStarsId: Long,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "battle_event_mode_id",
        updatable = false,
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT),
    )
    val mode: BattleEventModeEntity?,
    @Column(name = "map_name", length = 105, updatable = false)
    val mapName: String?,
) : BaseSoftDeleteEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "battle_event_id")
    val id: Long? = null
}
