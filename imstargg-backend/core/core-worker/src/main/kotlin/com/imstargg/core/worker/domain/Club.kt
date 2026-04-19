package com.imstargg.core.worker.domain

import com.imstargg.client.brawlstars.response.ClubResponse
import com.imstargg.infrastructure.db.core.brawlstars.player.ClubEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.ClubMemberEntity

class Club(
    val entity: ClubEntity,
    members: List<ClubMemberEntity> = emptyList(),
) {
    private val _members: MutableList<ClubMemberEntity> = members.toMutableList()
    val members: List<ClubMemberEntity> get() = _members

    fun update(response: ClubResponse) {
        entity.update(
            badgeBrawlStarsId = response.badgeId,
            name = response.name,
            description = response.description,
            type = response.type,
            requiredTrophies = response.requiredTrophies,
            trophies = response.trophies,
        )
        _members.clear()
        response.members.mapTo(_members) { memberResponse ->
            ClubMemberEntity(
                club = entity,
                brawlStarsTag = memberResponse.tag,
                name = memberResponse.name,
                nameColor = memberResponse.nameColor,
                role = memberResponse.role,
                trophies = memberResponse.trophies,
                iconBrawlStarsId = memberResponse.icon.id,
            )
        }
    }

    companion object {
        fun create(response: ClubResponse): Club =
            Club(
                entity =
                    ClubEntity(
                        brawlStarsTag = response.tag,
                        badgeBrawlStarsId = response.badgeId,
                        name = response.name,
                        description = response.description,
                        type = response.type,
                        requiredTrophies = response.requiredTrophies,
                        trophies = response.trophies,
                    ),
            )
    }
}
