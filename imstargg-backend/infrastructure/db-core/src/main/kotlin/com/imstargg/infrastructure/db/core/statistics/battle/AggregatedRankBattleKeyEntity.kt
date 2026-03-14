package com.imstargg.infrastructure.db.core.statistics.battle

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
    name = "aggregated_rank_battle_key",
    indexes = [
        Index(
            name = "uk_aggregated_rank_battle_key__date_battlekey_player",
            columnList = "battle_date, battle_key, player_brawlstars_tag",
            unique = true,
        ),
    ],
)
class AggregatedRankBattleKeyEntity private constructor(
    @Column(name = "battle_date", updatable = false, nullable = false)
    val battleDate: LocalDate,
    @Column(name = "player_brawlstars_tag", updatable = false, nullable = false)
    val playerBrawlStarsTag: String,
    @Column(name = "battle_key", updatable = false, nullable = false)
    val battleKey: String,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aggregated_rank_battle_key_id", nullable = false)
    val id: Long? = null

    fun matches(statsRankBattle: StatsRankBattleEntity): Boolean =
        this.battleKey == statsRankBattle.battleKey &&
            this.playerBrawlStarsTag == statsRankBattle.playerBrawlStarsTag

    companion object {
        fun from(statsRankBattle: StatsRankBattleEntity) =
            AggregatedRankBattleKeyEntity(
                battleDate = statsRankBattle.battleAt.toKoreaLocalDate(),
                playerBrawlStarsTag = statsRankBattle.playerBrawlStarsTag,
                battleKey = statsRankBattle.battleKey,
            )
    }
}
