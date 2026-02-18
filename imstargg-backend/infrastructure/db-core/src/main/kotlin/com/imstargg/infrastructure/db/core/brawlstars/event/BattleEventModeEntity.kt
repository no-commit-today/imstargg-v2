package com.imstargg.infrastructure.db.core.brawlstars.event

import com.imstargg.infrastructure.db.core.BaseTimeEntity
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageCode
import com.imstargg.infrastructure.db.core.brawlstars.MessageCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "battle_event_mode",
    indexes = [
        Index(name = "uk_battle_event_mode__brawlstarsid", columnList = "brawlstars_id", unique = true),
    ],
)
class BattleEventModeEntity(
    @Column(name = "brawlstars_id", updatable = false, nullable = false)
    val brawlStarsId: Long,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "battle_event_mode_id")
    val id: Long? = null

    fun nameMessageCode(): MessageCode = MessageCode.of("event", "mode", brawlStarsId, "name")

    fun iconImageCode(): BrawlStarsImageCode = BrawlStarsImageCode.of("event", "mode", brawlStarsId, "icon")
}
