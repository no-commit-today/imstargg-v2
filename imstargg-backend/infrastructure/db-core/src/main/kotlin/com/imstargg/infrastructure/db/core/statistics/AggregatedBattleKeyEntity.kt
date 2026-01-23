package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.infrastructure.db.core.BaseEntity
import com.imstargg.infrastructure.db.core.toKoreaLocalDate
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "aggregated_battle_key")
class AggregatedBattleKeyEntity private constructor(
    @Column(name = "battle_date", updatable = false, nullable = false)
    val battleDate: LocalDate,
    @Column(name = "battle_key", updatable = false, nullable = false)
    val battleKey: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aggregated_battle_key_id", nullable = false)
    val id: Long? = null

    companion object {
        fun from(statsBattle: StatsBattleEntity) =
            AggregatedBattleKeyEntity(
                battleDate = statsBattle.battleAt.toKoreaLocalDate(),
                battleKey = statsBattle.battleKey,
            )
    }
}
