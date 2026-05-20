package com.imstargg.infrastructure.db.core.brawlstars.player.repository

import com.imstargg.infrastructure.db.core.brawlstars.player.ClubEntity
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface ClubJpaRepository : JpaRepository<ClubEntity, Long> {
    fun findByBrawlStarsTag(brawlStarsTag: String): ClubEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockByBrawlStarsTag(brawlStarsTag: String): ClubEntity?

    fun findAllByBrawlStarsTagStartingWithOrderByTrophiesDescBrawlStarsTagAsc(
        brawlStarsTag: String,
        pageable: Pageable,
    ): Slice<ClubEntity>

    fun findAllByNameStartingWithOrderByTrophiesDescBrawlStarsTagAsc(
        name: String,
        pageable: Pageable,
    ): Slice<ClubEntity>
}
