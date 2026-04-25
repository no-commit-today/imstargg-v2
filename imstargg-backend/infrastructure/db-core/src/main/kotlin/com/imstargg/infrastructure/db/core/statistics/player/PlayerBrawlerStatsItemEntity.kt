package com.imstargg.infrastructure.db.core.statistics.player

import com.imstargg.core.enums.BattleResult
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import com.imstargg.infrastructure.db.core.statistics.ResultCountEmbedded
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "player_brawler_stats_item",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_player_brawler_stats_item",
            columnNames = ["player_brawler_stats_id", "brawler_brawlstars_id"],
        ),
    ],
)
class PlayerBrawlerStatsItemEntity(
    @Column(name = "player_brawler_stats_id", updatable = false, nullable = false)
    val playerBrawlerStatsId: Long,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    resultCount: ResultCountEmbedded,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_brawler_stats_item_id", nullable = false)
    val id: Long? = null

    @Embedded
    var resultCount: ResultCountEmbedded = resultCount
        protected set

    fun accumulate(result: BattleResult) {
        resultCount = resultCount.increment(result)
    }

    companion object {
        fun zero(
            playerBrawlerStatsId: Long,
            brawlerBrawlStarsId: Long,
        ): PlayerBrawlerStatsItemEntity =
            PlayerBrawlerStatsItemEntity(
                playerBrawlerStatsId = playerBrawlerStatsId,
                brawlerBrawlStarsId = brawlerBrawlStarsId,
                resultCount = ResultCountEmbedded.create(0, 0, 0),
            )
    }
}
