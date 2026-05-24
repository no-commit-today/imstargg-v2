package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.Slice
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.player.Club
import com.imstargg.core.domain.brawlstars.player.ClubSearch
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import org.springframework.stereotype.Service

@Service
class ClubService(
    private val clubRepository: ClubRepository,
) {
    fun get(tag: BrawlStarsTag): Club =
        clubRepository.find(tag)
            ?: throw CoreException(ErrorType.NOT_FOUND, "tag=$tag")

    fun search(
        search: ClubSearch,
        offsetLimit: OffsetLimit,
    ): Slice<Club> =
        if (BrawlStarsTag.isValid(search.query)) {
            clubRepository.searchByTag(BrawlStarsTag(search.query), offsetLimit)
        } else {
            clubRepository.searchByName(search.query, offsetLimit)
        }
}
