package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.response.ClubMemberResponse
import com.imstargg.client.brawlstars.response.ClubResponse
import com.imstargg.infrastructure.db.core.brawlstars.player.ClubEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.ClubJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.ClubMemberEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.ClubMemberJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ClubUpdateApplier(
    private val clubRepository: ClubJpaRepository,
    private val clubMemberRepository: ClubMemberJpaRepository,
) {
    @Transactional
    fun apply(
        club: ClubEntity,
        response: ClubResponse,
    ) {
        club.update(
            badgeBrawlStarsId = response.badgeId,
            name = response.name,
            description = response.description,
            type = response.type,
            requiredTrophies = response.requiredTrophies,
            trophies = response.trophies,
        )
        clubRepository.save(club)

        syncMembers(club, response.members)
    }

    private fun syncMembers(
        club: ClubEntity,
        memberResponses: List<ClubMemberResponse>,
    ) {
        val existingMembers =
            clubMemberRepository
                .findAllByClub(club)
                .associateBy { it.brawlStarsTag }

        val responseTags = memberResponses.map { it.tag }.toSet()

        val membersToDelete =
            existingMembers.values
                .filter { it.brawlStarsTag !in responseTags }
        clubMemberRepository.deleteAll(membersToDelete)

        for (response in memberResponses) {
            val existing = existingMembers[response.tag]
            if (existing != null) {
                existing.update(
                    name = response.name,
                    nameColor = response.nameColor,
                    role = response.role,
                    trophies = response.trophies,
                    iconBrawlStarsId = response.icon.id,
                )
            } else {
                clubMemberRepository.save(
                    ClubMemberEntity(
                        club = club,
                        brawlStarsTag = response.tag,
                        name = response.name,
                        nameColor = response.nameColor,
                        role = response.role,
                        trophies = response.trophies,
                        iconBrawlStarsId = response.icon.id,
                    ),
                )
            }
        }
    }
}
