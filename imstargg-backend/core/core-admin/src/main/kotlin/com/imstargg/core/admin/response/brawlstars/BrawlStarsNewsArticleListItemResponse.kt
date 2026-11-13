package com.imstargg.core.admin.response.brawlstars

import com.imstargg.core.admin.application.brawlstars.AdminBrawlStarsNewsArticleSummary
import com.imstargg.core.enums.BrawlStarsNewsArticleType
import java.time.Instant

data class BrawlStarsNewsArticleListItemResponse(
    val id: Long,
    val articleKey: String,
    val linkUrl: String,
    val title: String,
    val type: BrawlStarsNewsArticleType,
    val publishedAt: Instant,
    val seenAt: Instant,
    val versionCount: Long,
) {
    companion object {
        fun of(summary: AdminBrawlStarsNewsArticleSummary): BrawlStarsNewsArticleListItemResponse =
            BrawlStarsNewsArticleListItemResponse(
                id = summary.id,
                articleKey = summary.articleKey,
                linkUrl = summary.linkUrl,
                title = summary.title,
                type = summary.type,
                publishedAt = summary.publishedAt,
                seenAt = summary.seenAt,
                versionCount = summary.versionCount,
            )
    }
}
