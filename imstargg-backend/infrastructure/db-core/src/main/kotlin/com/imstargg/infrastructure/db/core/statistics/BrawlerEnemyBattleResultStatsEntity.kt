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
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(
    name = "brawler_enemy_battle_result_stats",
    indexes = [
        Index(
            name = "ix_brawler_enemy_battle_result_stats_1",
            columnList = "event_brawlstars_id, tier_range, start_battle_date, end_battle_date",
        ),
        Index(
            name = "ix_brawler_enemy_battle_result_stats_2",
            columnList = "brawler_brawlstars_id, tier_range, start_battle_date, end_battle_date",
        ),
    ],
)
class BrawlerEnemyBattleResultStatsEntity(
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
    @Column(name = "enemy_brawler_brawlstars_id", updatable = false, nullable = false)
    val enemyBrawlerBrawlStarsId: Long,
    @Embedded
    val resultCount: ResultCountEmbedded,
) : BaseTimeEntity() {
    init {
        require(brawlerBrawlStarsId != enemyBrawlerBrawlStarsId) {
            "brawlerBrawlStarsId($brawlerBrawlStarsId)와 enemyBrawlerBrawlStarsId($enemyBrawlerBrawlStarsId)는 달라야 합니다."
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_enemy_battle_result_stats_id", nullable = false)
    val id: Long? = null
}
