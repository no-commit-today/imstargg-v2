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
    name = "club_daily_ranking",
    indexes = [
        Index(
            name = "uk_club_daily_ranking__country_date",
            columnList = "country, date",
            unique = true,
        ),
    ],
)
class ClubDailyRankingEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "country", columnDefinition = "varchar(25)", nullable = false, updatable = false)
    val country: Country,
    @Column(name = "date", nullable = false, updatable = false)
    val date: LocalDate,
    @Column(name = "start_time", nullable = false, updatable = false)
    val startTime: OffsetDateTime,
    @Column(name = "end_time", nullable = false, updatable = false)
    val endTime: OffsetDateTime,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_daily_ranking_id")
    val id: Long? = null
}
