package com.imstargg.infrastructure.db.core.brawlstars.ranking

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.BaseEntity
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
    name = "club_ranking",
    indexes = [
        Index(name = "uk_club_ranking__country_rank", columnList = "country, rank_value", unique = true),
    ],
)
class ClubRankingEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "country", columnDefinition = "varchar(25)", nullable = false, updatable = false)
    val country: Country,
    brawlStarsTag: String,
    name: String,
    badgeBrawlStarsId: Long,
    trophies: Int,
    rank: Int,
    memberCount: Int,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_ranking_id")
    val id: Long? = null

    @Column(name = "brawlstars_tag", length = 45, nullable = false)
    var brawlStarsTag: String = brawlStarsTag
        protected set

    @Column(name = "name", length = 45, nullable = false)
    var name: String = name
        protected set

    @Column(name = "badge_brawlstars_id", nullable = false)
    var badgeBrawlStarsId: Long = badgeBrawlStarsId
        protected set

    @Column(name = "trophies", nullable = false)
    var trophies: Int = trophies
        protected set

    @Column(name = "rank_value", nullable = false)
    var rank: Int = rank
        protected set

    @Column(name = "member_count", nullable = false)
    var memberCount: Int = memberCount
        protected set
}
