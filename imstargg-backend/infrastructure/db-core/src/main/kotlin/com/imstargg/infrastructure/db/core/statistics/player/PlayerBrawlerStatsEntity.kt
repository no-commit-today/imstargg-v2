package com.imstargg.infrastructure.db.core.statistics.player

import com.imstargg.core.enums.BattleType
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "player_brawler_stats",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_player_brawler_stats",
            columnNames = ["player_brawlstars_tag", "battle_type", "start_battle_date"],
        ),
    ],
)
class PlayerBrawlerStatsEntity(
    @Column(name = "player_brawlstars_tag", updatable = false, nullable = false)
    val playerBrawlStarsTag: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "battle_type", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val battleType: BattleType,
    @Column(name = "start_battle_date", updatable = false, nullable = false)
    val startBattleDate: LocalDate,
    @Column(name = "end_battle_date", updatable = false, nullable = false)
    val endBattleDate: LocalDate,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_brawler_stats_id", nullable = false)
    val id: Long? = null
}
