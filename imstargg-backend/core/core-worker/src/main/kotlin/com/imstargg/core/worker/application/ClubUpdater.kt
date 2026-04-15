package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.response.ClubResponse
import com.imstargg.infrastructure.db.core.brawlstars.player.ClubEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.ClubJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ClubUpdater(
    private val clubRepository: ClubJpaRepository,
    private val clubUpdateApplier: ClubUpdateApplier,
) {
    @Transactional
    fun update(clubResponse: ClubResponse) {
        val club =
            clubRepository.findWithLockByBrawlStarsTag(clubResponse.tag)
                ?: createClub(clubResponse)

        clubUpdateApplier.apply(club, clubResponse)
    }

    private fun createClub(response: ClubResponse): ClubEntity =
        ClubEntity(
            brawlStarsTag = response.tag,
            badgeBrawlStarsId = response.badgeId,
            name = response.name,
            description = response.description,
            type = response.type,
            requiredTrophies = response.requiredTrophies,
            trophies = response.trophies,
        )
}
