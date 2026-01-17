package com.imstargg.core.domain.brawlstars.player

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.ClubType

data class Club(
    val tag: BrawlStarsTag,
    val name: String,
    val description: String,
    val type: ClubType?,
    val badgeId: BrawlStarsId,
    val requiredTrophies: Int,
    val trophies: Int,
    val members: List<ClubMember>,
)
