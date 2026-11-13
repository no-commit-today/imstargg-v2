package com.imstargg.infrastructure.db.core.brawlstars.news.repository

import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleEntity
import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleLink

/**
 * 같은 주소의 가장 최근 행을 먼저 찾고, 없으면 같은 주소 키의 가장 최근 행이다.
 * 가장 최근은 id 가 가장 큰 행이다.
 */
fun BrawlStarsNewsArticleJpaRepository.findPrevious(
    link: BrawlStarsNewsArticleLink,
    beforeId: Long = Long.MAX_VALUE,
): BrawlStarsNewsArticleEntity? =
    findTopByLinkLinkUrlAndIdLessThanOrderByIdDesc(link.linkUrl, beforeId)
        ?: findTopByLinkArticleKeyAndIdLessThanOrderByIdDesc(link.articleKey, beforeId)
