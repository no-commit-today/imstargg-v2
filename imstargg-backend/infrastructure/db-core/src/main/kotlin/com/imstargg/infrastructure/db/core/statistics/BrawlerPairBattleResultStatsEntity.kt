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
import jakarta.persistence.Index
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "brawler_pair_battle_result_stats",
    indexes = [
        Index(
            name = "ix_brawler_pair_battle_result_stats_2",
            columnList = "brawler_brawlstars_id, tier_range, start_battle_date, end_battle_date",
        ),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_brawler_pair_battle_result_stats__1",
            columnNames = [
                "event_brawlstars_id",
                "start_battle_date",
                "end_battle_date",
                "tier_range",
                "brawler_brawlstars_id",
                "pair_brawler_brawlstars_id",
            ],
        ),
    ],
)
class BrawlerPairBattleResultStatsEntity(
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
    @Column(name = "pair_brawler_brawlstars_id", updatable = false, nullable = false)
    val pairBrawlerBrawlStarsId: Long,
    @Embedded
    val resultCount: ResultCountEmbedded,
) : BaseTimeEntity() {
    init {
        require(brawlerBrawlStarsId != pairBrawlerBrawlStarsId) {
            "brawlerBrawlStarsId($brawlerBrawlStarsId)와 pairBrawlerBrawlStarsId($pairBrawlerBrawlStarsId)는 달라야 합니다."
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "brawlerPairBattleResultStatsSeq")
    @SequenceGenerator(
        name = "brawlerPairBattleResultStatsSeq",
        sequenceName = "brawler_pair_battle_result_stats_seq",
        allocationSize = 100,
    )
    @Column(name = "brawler_pair_battle_result_stats_id", nullable = false)
    val id: Long? = null
}
