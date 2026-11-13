package com.imstargg.core.admin.response.brawlstars

import com.imstargg.core.enums.BrawlStarsNewsArticleType
import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleEntity
import java.time.Instant

data class BrawlStarsNewsArticleDetailResponse(
    val id: Long,
    val articleKey: String,
    val linkUrl: String,
    val title: String,
    val type: BrawlStarsNewsArticleType,
    val publishedAt: Instant,
    val seenAt: Instant,
    val content: String,
    val previousId: Long?,
    val previousSeenAt: Instant?,
    val previousContent: String?,
) {
    companion object {
        fun of(
            entity: BrawlStarsNewsArticleEntity,
            previous: BrawlStarsNewsArticleEntity?,
        ): BrawlStarsNewsArticleDetailResponse =
            BrawlStarsNewsArticleDetailResponse(
                id = requireNotNull(entity.id),
                articleKey = entity.link.articleKey,
                linkUrl = entity.link.linkUrl,
                title = entity.title,
                type = entity.type,
                publishedAt = entity.publishedAt,
                seenAt = requireNotNull(entity.createdAt),
                content = entity.content,
                previousId = previous?.id,
                previousSeenAt = previous?.createdAt,
                previousContent = previous?.content,
            )
    }
}
