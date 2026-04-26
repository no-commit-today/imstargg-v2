package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Embedded
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
    name = "brawler_trio_battle_result_stats",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_brawler_trio_battle_result_stats__1",
            columnNames = [
                "event_brawlstars_id",
                "start_battle_date",
                "end_battle_date",
                "tier_range",
                "brawler_brawlstars_id",
                "pair_brawler1_brawlstars_id",
                "pair_brawler2_brawlstars_id",
            ],
        ),
    ],
)
class BrawlerTrioBattleResultStatsEntity(
    @Column(name = "event_brawlstars_id", updatable = false, nullable = false)
    val eventBrawlStarsId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "tier_range", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val tierRange: StatsTierRange,
    @Column(name = "start_battle_date", updatable = false, nullable = false)
    val startBattleDate: LocalDate,
    @Column(name = "end_battle_date", updatable = false, nullable = false)
    val endBattleDate: LocalDate,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    @Column(name = "pair_brawler1_brawlstars_id", updatable = false, nullable = false)
    val pairBrawler1BrawlStarsId: Long,
    @Column(name = "pair_brawler2_brawlstars_id", updatable = false, nullable = false)
    val pairBrawler2BrawlStarsId: Long,
    @Embedded
    val resultCount: ResultCountEmbedded,
) : BaseTimeEntity() {
    init {
        require(pairBrawler1BrawlStarsId < pairBrawler2BrawlStarsId) {
            "pairBrawler1BrawlStarsId($pairBrawler1BrawlStarsId)는 pairBrawler2BrawlStarsId($pairBrawler2BrawlStarsId)보다 작아야 합니다."
        }
        require(brawlerBrawlStarsId != pairBrawler1BrawlStarsId && brawlerBrawlStarsId != pairBrawler2BrawlStarsId) {
            "기준 브롤러와 pair 브롤러는 달라야 합니다."
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_trio_battle_result_stats_id", nullable = false)
    val id: Long? = null
}
