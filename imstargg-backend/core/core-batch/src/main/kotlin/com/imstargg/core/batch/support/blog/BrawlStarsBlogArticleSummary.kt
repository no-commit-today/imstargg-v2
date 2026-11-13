package com.imstargg.core.batch.support.blog

import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleLink
import java.time.Instant

/** 공식 블로그 목록의 글 한 건. */
data class BrawlStarsBlogArticleSummary(
    val title: String,
    val link: BrawlStarsNewsArticleLink,
    val publishedAt: Instant,
)
