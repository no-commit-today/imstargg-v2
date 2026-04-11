package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTeamSize
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable

@Embeddable
data class BrawlerTeamEmbedded(
    @Column(name = "team_key", updatable = false, nullable = false)
    val teamKey: String,
    @Convert(converter = StatsTeamSizeConverter::class)
    @Column(name = "team_size", updatable = false, nullable = false)
    val teamSize: StatsTeamSize,
) {
    val brawlerBrawlStarsIds: List<Long>
        get() = teamKey.split(DELIMITER).map { it.toLong() }

    companion object {
        private const val DELIMITER = "_"

        fun of(brawlerIds: Collection<Long>): BrawlerTeamEmbedded {
            require(brawlerIds.size == brawlerIds.toSet().size) {
                "중복된 브롤러 ID가 존재합니다: $brawlerIds"
            }
            return BrawlerTeamEmbedded(
                teamKey = brawlerIds.sorted().joinToString(DELIMITER),
                teamSize = StatsTeamSize.fromValue(brawlerIds.size),
            )
        }
    }
}
