package com.imstargg.infrastructure.db.core.brawlstars.event

import com.imstargg.infrastructure.db.core.BaseTimeEntity
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
    indexes = [Index(name = "ix_battle_event_rotation_item__battle_event_rotation_id", columnList = "battle_event_rotation_id")],
)
class BattleEventRotationItemEntity(
    @Column(name = "battle_event_rotation_id", updatable = false, nullable = false)
    val battleEventRotationId: Long,
    @Column(name = "event_brawlstars_id", updatable = false, nullable = false)
    val eventBrawlStarsId: Long,
    @Column(name = "event_mode_name", updatable = false)
    val eventModeName: String? = null,
    @Column(name = "event_mode_brawlstars_id", updatable = false)
    val eventModeBrawlStarsId: Long? = null,
    @Column(name = "event_map_name", updatable = false)
    val eventMapName: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "modifiers", updatable = false, nullable = false)
    val modifiers: List<String>,
    @Column(name = "slot_id", updatable = false, nullable = false)
    val slotId: Int,
    @Column(name = "start_time", updatable = false, nullable = false)
    val startTime: OffsetDateTime,
    @Column(name = "end_time", updatable = false, nullable = false)
    val endTime: OffsetDateTime,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "battle_event_rotation_item_id")
    val id: Long? = null
}
