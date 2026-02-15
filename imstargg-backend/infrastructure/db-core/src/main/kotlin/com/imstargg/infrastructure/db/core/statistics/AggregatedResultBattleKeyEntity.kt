package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.infrastructure.db.core.BaseTimeEntity
import com.imstargg.infrastructure.db.core.toKoreaLocalDate
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(
    name = "aggregated_result_battle_key",
    indexes = [
        Index(
            name = "uk_aggregated_result_battle_key__battle_date_battle_key",
            columnList = "battle_date, battle_key",
            unique = true,
        ),
    ],
)
class AggregatedResultBattleKeyEntity private constructor(
    @Column(name = "battle_date", updatable = false, nullable = false)
    val battleDate: LocalDate,
    @Column(name = "battle_key", updatable = false, nullable = false)
    val battleKey: String,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aggregated_result_battle_key_id", nullable = false)
    val id: Long? = null

    fun matches(statsResultBattle: StatsResultBattleEntity): Boolean = this.battleKey == statsResultBattle.battleKey

    companion object {
        fun from(statsResultBattle: StatsResultBattleEntity) =
            AggregatedResultBattleKeyEntity(
                battleDate = statsResultBattle.battleAt.toKoreaLocalDate(),
                battleKey = statsResultBattle.battleKey,
            )
    }
}
