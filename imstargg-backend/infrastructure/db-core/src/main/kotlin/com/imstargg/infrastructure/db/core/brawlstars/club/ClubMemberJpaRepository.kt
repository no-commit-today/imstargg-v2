package com.imstargg.infrastructure.db.core.brawlstars.club

import org.springframework.data.jpa.repository.JpaRepository

interface ClubMemberJpaRepository : JpaRepository<ClubMemberEntity, Long>
