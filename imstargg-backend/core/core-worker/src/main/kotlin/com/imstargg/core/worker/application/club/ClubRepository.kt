package com.imstargg.core.worker.application.club

import com.imstargg.core.worker.domain.Club
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.ClubJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.ClubMemberJpaRepository
import org.springframework.stereotype.Component

@Component
class ClubRepository(
    private val clubJpaRepository: ClubJpaRepository,
    private val clubMemberJpaRepository: ClubMemberJpaRepository,
) {
    fun findWithLockByBrawlStarsTag(brawlStarsTag: String): Club? {
        val entity = clubJpaRepository.findWithLockByBrawlStarsTag(brawlStarsTag) ?: return null
        return Club(
            entity = entity,
            members = clubMemberJpaRepository.findAllByClub(entity),
        )
    }

    fun save(club: Club): Club {
        clubJpaRepository.save(club.entity)
        val existingMembers = clubMemberJpaRepository.findAllByClub(club.entity)
        if (existingMembers.isNotEmpty()) {
            clubMemberJpaRepository.deleteAllInBatch(existingMembers)
        }
        clubMemberJpaRepository.saveAll(club.members)
        return club
    }
}
