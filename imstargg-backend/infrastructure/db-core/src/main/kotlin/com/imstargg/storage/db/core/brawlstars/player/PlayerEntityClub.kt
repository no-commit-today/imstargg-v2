package com.imstargg.storage.db.core.brawlstars.player

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class PlayerEntityClub(
    @Column(name = "club_brawlstars_tag", length = 45, nullable = false)
    val brawlStarsTag: String,
    @Column(name = "club_name", length = 105, nullable = false)
    val name: String,
)
