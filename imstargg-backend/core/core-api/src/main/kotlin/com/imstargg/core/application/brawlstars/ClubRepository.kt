package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.player.Club
import com.imstargg.core.domain.brawlstars.player.ClubMember
import com.imstargg.core.enums.ClubMemberRole
import com.imstargg.core.enums.ClubType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.infrastructure.db.core.brawlstars.player.ClubEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.ClubJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.ClubMemberEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.ClubMemberJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class ClubRepository(
    private val clubJpaRepository: ClubJpaRepository,
    private val clubMemberJpaRepository: ClubMemberJpaRepository,
) {
    fun find(tag: BrawlStarsTag): Club? {
        val clubEntity = clubJpaRepository.findByBrawlStarsTag(tag.value) ?: return null
        val memberEntities = clubMemberJpaRepository.findAllByClub(clubEntity)
        return toClub(clubEntity, memberEntities)
    }

    private fun toClub(
        entity: ClubEntity,
        members: List<ClubMemberEntity>,
    ): Club =
        Club(
            tag = BrawlStarsTag(entity.brawlStarsTag),
            name = entity.name,
            description = entity.description,
            type = EnumCodeType.findByCode<ClubType>(entity.type) ?: ClubType.UNKNOWN,
            badgeId = BrawlStarsId(entity.badgeBrawlStarsId),
            requiredTrophies = entity.requiredTrophies,
            trophies = entity.trophies,
            members = members.map { toClubMember(it) },
        )

    private fun toClubMember(entity: ClubMemberEntity): ClubMember =
        ClubMember(
            tag = BrawlStarsTag(entity.brawlStarsTag),
            name = entity.name,
            nameColor = entity.nameColor,
            role = EnumCodeType.findByCode<ClubMemberRole>(entity.role) ?: ClubMemberRole.UNKNOWN,
            trophies = entity.trophies,
            iconId = BrawlStarsId(entity.iconBrawlStarsId),
        )
}
