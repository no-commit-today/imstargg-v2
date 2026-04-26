package com.imstargg.infrastructure.db.core.brawlstars.event

import com.imstargg.core.enums.BattleEventResultType
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
    resultType: BattleEventResultType = BattleEventResultType.RESULT,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "battle_event_mode_id")
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false)
    var resultType: BattleEventResultType = resultType
        protected set

    fun changeResultType(newType: BattleEventResultType) {
        this.resultType = newType
    }

    companion object {
        // API 응답에서 name이 null인 경우 사용
        const val UNKNOWN_NAME = "UNKNOWN"
    }
}
