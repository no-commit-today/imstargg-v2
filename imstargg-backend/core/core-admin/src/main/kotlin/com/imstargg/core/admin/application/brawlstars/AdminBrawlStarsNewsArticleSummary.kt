package com.imstargg.core.admin.application.brawlstars

import com.imstargg.core.enums.BrawlStarsNewsArticleType
import java.time.Instant

/** 글 한 판의 요약. 본문(`raw_body`, `content`)은 담지 않는다. [seenAt] 은 그 판을 처음 저장한 시각이다. */
data class AdminBrawlStarsNewsArticleSummary(
    val id: Long,
    val articleKey: String,
    val linkUrl: String,
    val title: String,
    val type: BrawlStarsNewsArticleType,
    val publishedAt: Instant,
    val seenAt: Instant,
    val versionCount: Long,
)
