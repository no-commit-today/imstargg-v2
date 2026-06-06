package com.imstargg.core.admin.application.brawlstars

import com.imstargg.core.admin.response.brawlstars.BrawlStarsApiSpecDetailResponse
import com.imstargg.core.admin.response.brawlstars.BrawlStarsApiSpecListItemResponse
import com.imstargg.infrastructure.db.core.brawlstars.system.repository.BrawlStarsApiSpecJpaRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@Transactional(readOnly = true)
class AdminBrawlStarsApiSpecService(
    private val brawlStarsApiSpecJpaRepository: BrawlStarsApiSpecJpaRepository,
) {
    fun findAll(): List<BrawlStarsApiSpecListItemResponse> =
        brawlStarsApiSpecJpaRepository
            .findAllByOrderByCollectedAtDesc()
            .map(BrawlStarsApiSpecListItemResponse::of)

    fun getDetail(id: Long): BrawlStarsApiSpecDetailResponse {
        val entity =
            brawlStarsApiSpecJpaRepository
                .findById(id)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "BrawlStarsApiSpec not found: id=$id") }
        val previous = brawlStarsApiSpecJpaRepository.findTopByCollectedAtLessThanOrderByCollectedAtDesc(entity.collectedAt)
        return BrawlStarsApiSpecDetailResponse.of(entity = entity, previous = previous)
    }
}
