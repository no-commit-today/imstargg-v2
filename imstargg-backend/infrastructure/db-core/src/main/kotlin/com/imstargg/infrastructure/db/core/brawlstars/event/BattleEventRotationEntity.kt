package com.imstargg.infrastructure.db.core.brawlstars.event

import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(
    name = "battle_event_rotation",
    indexes = [Index(name = "ix_battle_event_rotation__collected_at", columnList = "collected_at")],
)
class BattleEventRotationEntity(
    @Column(name = "collected_at", updatable = false, nullable = false)
    val collectedAt: OffsetDateTime,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "battle_event_rotation_id")
    val id: Long? = null
}
