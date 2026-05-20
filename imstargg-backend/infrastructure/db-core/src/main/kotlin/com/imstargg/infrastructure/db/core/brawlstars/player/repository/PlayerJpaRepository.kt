package com.imstargg.infrastructure.db.core.brawlstars.player.repository

import com.imstargg.core.enums.PlayerStatus
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface PlayerJpaRepository : JpaRepository<PlayerEntity, Long> {
    fun findByBrawlStarsTag(brawlStarsTag: String): PlayerEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockByBrawlStarsTag(brawlStarsTag: String): PlayerEntity?

    fun findTopByStatusOrderByIdDesc(status: PlayerStatus): PlayerEntity?

    fun findAllByStatusAndIdGreaterThanEqual(
        status: PlayerStatus,
        id: Long,
        pageable: Pageable,
    ): List<PlayerEntity>

    fun findAllByBrawlStarsTagStartingWithOrderByTrophiesDescBrawlStarsTagAsc(
        brawlStarsTag: String,
        pageable: Pageable,
    ): Slice<PlayerEntity>

    fun findAllByNameStartingWithOrderByTrophiesDescBrawlStarsTagAsc(
        name: String,
        pageable: Pageable,
    ): Slice<PlayerEntity>
}
