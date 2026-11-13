package com.imstargg.core.admin.application.brawlstars

import com.imstargg.core.admin.response.brawlstars.BrawlStarsNewsArticleDetailResponse
import com.imstargg.core.admin.response.brawlstars.BrawlStarsNewsArticleListItemResponse
import com.imstargg.core.admin.response.brawlstars.BrawlStarsNewsArticleVersionResponse
import com.imstargg.core.enums.BrawlStarsNewsArticleType
import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleEntity
import com.imstargg.infrastructure.db.core.brawlstars.news.repository.BrawlStarsNewsArticleJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.news.repository.findPrevious
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@Transactional(readOnly = true)
class AdminBrawlStarsNewsArticleService(
    private val brawlStarsNewsArticleJpaRepository: BrawlStarsNewsArticleJpaRepository,
    private val adminBrawlStarsNewsArticleRepository: AdminBrawlStarsNewsArticleRepository,
) {
    fun findAll(): List<BrawlStarsNewsArticleListItemResponse> =
        adminBrawlStarsNewsArticleRepository
            .findLatestVersions()
            .map(BrawlStarsNewsArticleListItemResponse::of)

    fun getDetail(id: Long): BrawlStarsNewsArticleDetailResponse {
        val entity = getEntity(id)
        val previous = brawlStarsNewsArticleJpaRepository.findPrevious(entity.link, beforeId = id)
        return BrawlStarsNewsArticleDetailResponse.of(entity = entity, previous = previous)
    }

    fun findVersions(id: Long): List<BrawlStarsNewsArticleVersionResponse> =
        adminBrawlStarsNewsArticleRepository
            .findVersions(getEntity(id).link.articleKey)
            .map(BrawlStarsNewsArticleVersionResponse::of)

    /** 같은 주소 키의 모든 판에 적용한다. 유형은 글의 성질이라 판마다 다르면 안 된다. */
    @Transactional
    fun changeType(
        id: Long,
        type: BrawlStarsNewsArticleType,
    ) {
        val entity = getEntity(id)
        brawlStarsNewsArticleJpaRepository
            .findAllByLinkArticleKeyOrderByIdAsc(entity.link.articleKey)
            .forEach { it.changeType(type) }
    }

    private fun getEntity(id: Long): BrawlStarsNewsArticleEntity =
        brawlStarsNewsArticleJpaRepository
            .findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "BrawlStarsNewsArticle not found: id=$id") }
}
