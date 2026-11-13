package com.imstargg.core.admin.response.brawlstars

import com.imstargg.core.admin.application.brawlstars.AdminBrawlStarsNewsArticleSummary
import java.time.Instant

data class BrawlStarsNewsArticleVersionResponse(
    val id: Long,
    val linkUrl: String,
    val title: String,
    val publishedAt: Instant,
    val seenAt: Instant,
) {
    companion object {
        fun of(summary: AdminBrawlStarsNewsArticleSummary): BrawlStarsNewsArticleVersionResponse =
            BrawlStarsNewsArticleVersionResponse(
                id = summary.id,
                linkUrl = summary.linkUrl,
                title = summary.title,
                publishedAt = summary.publishedAt,
                seenAt = summary.seenAt,
            )
    }
}
