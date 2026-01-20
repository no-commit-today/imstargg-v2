package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.response.ClubResponse
import com.imstargg.infrastructure.db.core.brawlstars.player.ClubEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.ClubJpaRepository
import com.imstargg.infrastructure.db.core.lock.LockName
import com.imstargg.infrastructure.db.core.lock.LockTemplate
import org.springframework.stereotype.Component

@Component
class ClubUpdater(
    private val lockTemplate: LockTemplate,
    private val clubRepository: ClubJpaRepository,
    private val clubUpdateApplier: ClubUpdateApplier,
) {
    companion object {
        private const val LOCK_TIMEOUT_SECONDS = 3
    }

    fun update(clubResponse: ClubResponse) {
        lockTemplate.execute(
            LockName.createClubLockName(clubResponse.tag),
            LOCK_TIMEOUT_SECONDS,
        ) {
            val club =
                clubRepository.findByBrawlStarsTag(clubResponse.tag)
                    ?: createClub(clubResponse)

            clubUpdateApplier.apply(club, clubResponse)
        }
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
