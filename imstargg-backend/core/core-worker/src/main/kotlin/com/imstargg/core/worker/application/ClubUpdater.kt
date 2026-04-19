package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.response.ClubResponse
import com.imstargg.core.worker.domain.Club
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ClubUpdater(
    private val clubRepository: ClubRepository,
) {
    @Transactional
    fun update(clubResponse: ClubResponse) {
        val club =
            clubRepository.findWithLockByBrawlStarsTag(clubResponse.tag)
                ?: Club.create(clubResponse)
        club.update(clubResponse)
        clubRepository.save(club)
    }
}
