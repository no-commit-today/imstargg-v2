package com.imstargg.infrastructure.db.core.brawlstars.ranking

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(
    name = "player_ranking",
    indexes = [
        Index(name = "ix_player_ranking__country_collected_at", columnList = "country, collected_at"),
    ],
)
class PlayerRankingEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "country", columnDefinition = "varchar(25)", nullable = false, updatable = false)
    val country: Country,
    @Column(name = "collected_at", nullable = false, updatable = false)
    val collectedAt: OffsetDateTime,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_ranking_id")
    val id: Long? = null
}
