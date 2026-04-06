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
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(
    name = "brawler_daily_ranking",
    indexes = [
        Index(
            name = "uk_brawler_daily_ranking__country_brawler_date",
            columnList = "country, brawler_brawlstars_id, date",
            unique = true,
        ),
    ],
)
class BrawlerDailyRankingEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "country", columnDefinition = "varchar(25)", nullable = false, updatable = false)
    val country: Country,
    @Column(name = "brawler_brawlstars_id", nullable = false, updatable = false)
    val brawlerBrawlStarsId: Long,
    @Column(name = "date", nullable = false, updatable = false)
    val date: LocalDate,
    @Column(name = "start_time", nullable = false, updatable = false)
    val startTime: OffsetDateTime,
    @Column(name = "end_time", nullable = false, updatable = false)
    val endTime: OffsetDateTime,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_daily_ranking_id")
    val id: Long? = null
}
