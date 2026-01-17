package com.imstargg.core.domain.brawlstars.player

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.ClubMemberRole

data class ClubMember(
    val tag: BrawlStarsTag,
    val name: String,
    val nameColor: String?,
    val role: ClubMemberRole,
    val trophies: Int,
    val iconId: BrawlStarsId,
)
