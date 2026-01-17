package com.imstargg.infrastructure.db.core.brawlstars.player

import org.springframework.data.jpa.repository.JpaRepository

interface ClubMemberJpaRepository : JpaRepository<ClubMemberEntity, Long> {
    fun findAllByClub(club: ClubEntity): List<ClubMemberEntity>
}
