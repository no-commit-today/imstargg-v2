package com.imstargg.infrastructure.db.core.brawlstars.news.repository

import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlStarsNewsArticleJpaRepository : JpaRepository<BrawlStarsNewsArticleEntity, Long> {
    fun findTopByLinkLinkUrlAndIdLessThanOrderByIdDesc(
        linkUrl: String,
        id: Long,
    ): BrawlStarsNewsArticleEntity?

    fun findTopByLinkArticleKeyAndIdLessThanOrderByIdDesc(
        articleKey: String,
        id: Long,
    ): BrawlStarsNewsArticleEntity?

    fun findAllByLinkArticleKeyOrderByIdAsc(articleKey: String): List<BrawlStarsNewsArticleEntity>
}
