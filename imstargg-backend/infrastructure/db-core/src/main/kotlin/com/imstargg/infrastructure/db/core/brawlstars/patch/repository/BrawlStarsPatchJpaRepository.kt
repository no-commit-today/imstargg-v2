package com.imstargg.infrastructure.db.core.brawlstars.patch.repository

import com.imstargg.infrastructure.db.core.brawlstars.patch.BrawlStarsPatchEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface BrawlStarsPatchJpaRepository : JpaRepository<BrawlStarsPatchEntity, Long> {
    fun findAllByOrderByPatchDateDesc(): List<BrawlStarsPatchEntity>

    fun existsByPatchDate(patchDate: LocalDate): Boolean
}
