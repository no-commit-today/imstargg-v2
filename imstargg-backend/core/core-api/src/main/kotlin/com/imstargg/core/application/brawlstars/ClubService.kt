package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.Slice
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.player.Club
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
        query: String,
        offsetLimit: OffsetLimit,
    ): Slice<Club> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            throw CoreException(ErrorType.INVALID_REQUEST, "query is blank")
        }
        return if (trimmed.startsWith("#")) {
            // Malformed tag → empty result (not 500). Tag detail page handles its own NOT_FOUND.
            if (!BrawlStarsTag.isValid(trimmed)) {
                return Slice(content = emptyList(), hasNext = false)
            }
            val club = clubRepository.searchByTag(BrawlStarsTag(trimmed))
            Slice(content = listOfNotNull(club), hasNext = false)
        } else {
            clubRepository.searchByName(trimmed, offsetLimit)
        }
    }
}
