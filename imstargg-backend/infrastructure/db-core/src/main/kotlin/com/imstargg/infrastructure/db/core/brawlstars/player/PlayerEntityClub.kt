package com.imstargg.infrastructure.db.core.brawlstars.player

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class PlayerEntityClub(
    @Column(name = "club_brawlstars_tag", length = 45)
    val brawlStarsTag: String,
    @Column(name = "club_name", length = 105)
    val name: String,
)
