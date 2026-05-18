package com.imstargg.infrastructure.db.core.brawlstars.player.repository

import com.imstargg.infrastructure.db.core.brawlstars.player.ClubEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ClubJpaRepository : JpaRepository<ClubEntity, Long> {
    fun findByBrawlStarsTag(brawlStarsTag: String): ClubEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockByBrawlStarsTag(brawlStarsTag: String): ClubEntity?

    /**
     * Prefix search on `lower(name)` — caller passes `"<lowercased input>%"`.
     * Native query to keep the SQL shape stable and guarantee the
     * `ix_club__lower_name` (`lower(name) varchar_pattern_ops`) index is hit.
     */
    @Query(
        nativeQuery = true,
        value = """
            select * from club
            where lower(name) like :pattern
            order by trophies desc, brawlstars_tag asc
            offset :offset limit :limit
        """,
    )
    fun findAllByLowerNamePrefix(
        @Param("pattern") pattern: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int,
    ): List<ClubEntity>
}
