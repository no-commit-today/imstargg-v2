package com.imstargg.storage.db.core.brawlstars.ranking

import com.imstargg.core.enums.Country
import com.imstargg.storage.db.core.BaseEntity
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

@Entity
@Table(
    name = "brawler_ranking",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_brawler_ranking__country_brawler_rank",
            columnNames = ["country", "brawler_brawlstars_id", "rank_value"],
        ),
    ],
)
class BrawlerRankingEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "country", columnDefinition = "varchar(25)", nullable = false, updatable = false)
    val country: Country,
    @Column(name = "brawler_brawlstars_id", nullable = false, updatable = false)
    val brawlerBrawlStarsId: Long,
    @Column(name = "rank_value", nullable = false, updatable = false)
    val rank: Int,
    player: RankingEntityPlayer,
    trophies: Int,
    rankChange: Int?,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_ranking_id")
    val id: Long? = null

    @Embedded
    var player: RankingEntityPlayer = player
        protected set

    @Column(name = "trophies", nullable = false)
    var trophies: Int = trophies
        protected set

    @Column(name = "rank_change")
    var rankChange: Int? = rankChange
        protected set
}
