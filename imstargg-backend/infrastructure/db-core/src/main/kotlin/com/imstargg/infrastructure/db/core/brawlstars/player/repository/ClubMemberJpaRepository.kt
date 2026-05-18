package com.imstargg.infrastructure.db.core.brawlstars.player.repository

import com.imstargg.infrastructure.db.core.brawlstars.player.ClubEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.ClubMemberEntity
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface ClubMemberJpaRepository : JpaRepository<ClubMemberEntity, Long> {
    fun findAllByClub(club: ClubEntity): List<ClubMemberEntity>

    @EntityGraph(attributePaths = ["club"])
    fun findAllByClubIn(clubs: Collection<ClubEntity>): List<ClubMemberEntity>
}
