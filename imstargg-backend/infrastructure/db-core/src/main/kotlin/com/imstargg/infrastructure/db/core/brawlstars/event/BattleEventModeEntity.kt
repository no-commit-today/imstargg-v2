package com.imstargg.infrastructure.db.core.brawlstars.event

import com.imstargg.infrastructure.db.core.BaseTimeEntity
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
    @Column(name = "brawlstars_name", updatable = false, nullable = false)
    val brawlStarsName: String,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "battle_event_mode_id")
    val id: Long? = null

    companion object {
        // API 응답에서 name이 null인 경우 사용
        const val UNKNOWN_NAME = "UNKNOWN"
    }
}
