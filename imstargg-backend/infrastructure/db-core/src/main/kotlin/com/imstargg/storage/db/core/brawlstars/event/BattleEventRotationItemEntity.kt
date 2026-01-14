package com.imstargg.storage.db.core.brawlstars.event

import com.imstargg.storage.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(
    name = "battle_event_rotation_item",
    indexes = [Index(name = "ix_battle_event_rotation_item__1", columnList = "battle_event_rotation_id")]
)
class BattleEventRotationItemEntity(
    @Column(name = "battle_event_rotation_id", updatable = false, nullable = false)
    val battleEventRotationId: Long,

    @Column(name = "event_brawlstars_id", updatable = false, nullable = false)
    val eventBrawlStarsId: Long,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "modifiers", columnDefinition = "json", updatable = false, nullable = false)
    val modifiers: List<String>,

    @Column(name = "slot_id", updatable = false, nullable = false)
    val slotId: Int,

    @Column(name = "start_time", updatable = false, nullable = false)
    val startTime: OffsetDateTime,

    @Column(name = "end_time", updatable = false, nullable = false)
    val endTime: OffsetDateTime,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "battle_event_rotation_item_id")
    val id: Long? = null
}
