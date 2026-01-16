package com.imstargg.storage.db.core.brawlstars.ranking

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class RankingEntityPlayer(
    @Column(name = "player_brawlstars_tag", length = 45, nullable = false)
    val brawlStarsTag: String,
    @Column(name = "player_name", length = 105, nullable = false)
    val name: String,
    @Column(name = "player_name_color", length = 45)
    val nameColor: String?,
    @Column(name = "player_icon_brawlstars_id", nullable = false)
    val iconBrawlStarsId: Long,
    @Column(name = "player_club_name", length = 105, nullable = false)
    val clubName: String,
)
