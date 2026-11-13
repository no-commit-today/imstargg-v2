package com.imstargg.core.admin.application.brawlstars

import com.imstargg.infrastructure.db.core.brawlstars.news.QBrawlStarsNewsArticleEntity.Companion.brawlStarsNewsArticleEntity
import com.querydsl.core.Tuple
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * 목록과 판 목록은 `raw_body`, `content` 를 읽지 않도록 필요한 컬럼만 투영해 읽는다.
 * core-admin 의 DB 응답 대기가 짧아 본문 컬럼을 읽는 목록은 시간 초과가 날 수 있다.
 */
@Repository
@Transactional(readOnly = true)
class AdminBrawlStarsNewsArticleRepository(
    entityManager: EntityManager,
) {
    private val queryFactory = JPAQueryFactory(entityManager)

    /** 글(주소 키)마다 id 가 가장 큰 판 하나이고, 가장 최근 판의 id 가 큰 것부터다. */
    fun findLatestVersions(): List<AdminBrawlStarsNewsArticleSummary> {
        val groups =
            queryFactory
                .select(brawlStarsNewsArticleEntity.id.max(), brawlStarsNewsArticleEntity.id.count())
                .from(brawlStarsNewsArticleEntity)
                .groupBy(brawlStarsNewsArticleEntity.link.articleKey)
                .fetch()
        if (groups.isEmpty()) {
            return emptyList()
        }
        val versionCounts =
            groups.associate {
                requireNotNull(it.get(0, Long::class.javaObjectType)) to requireNotNull(it.get(1, Long::class.javaObjectType))
            }
        return selectSummaries(brawlStarsNewsArticleEntity.id.`in`(versionCounts.keys))
            .sortedByDescending { it.id }
            .map { it.copy(versionCount = versionCounts.getValue(it.id)) }
    }

    /** id 오름차순이다. `versionCount` 는 채우지 않는다(0). */
    fun findVersions(articleKey: String): List<AdminBrawlStarsNewsArticleSummary> =
        selectSummaries(brawlStarsNewsArticleEntity.link.articleKey.eq(articleKey))
            .sortedBy { it.id }

    private fun selectSummaries(predicate: Predicate): List<AdminBrawlStarsNewsArticleSummary> =
        queryFactory
            .select(
                brawlStarsNewsArticleEntity.id,
                brawlStarsNewsArticleEntity.link.articleKey,
                brawlStarsNewsArticleEntity.link.linkUrl,
                brawlStarsNewsArticleEntity.title,
                brawlStarsNewsArticleEntity.type,
                brawlStarsNewsArticleEntity.publishedAt,
                brawlStarsNewsArticleEntity.createdAt,
            ).from(brawlStarsNewsArticleEntity)
            .where(predicate)
            .fetch()
            .map(::toSummary)

    private fun toSummary(tuple: Tuple): AdminBrawlStarsNewsArticleSummary =
        AdminBrawlStarsNewsArticleSummary(
            id = requireNotNull(tuple.get(brawlStarsNewsArticleEntity.id)),
            articleKey = requireNotNull(tuple.get(brawlStarsNewsArticleEntity.link.articleKey)),
            linkUrl = requireNotNull(tuple.get(brawlStarsNewsArticleEntity.link.linkUrl)),
            title = requireNotNull(tuple.get(brawlStarsNewsArticleEntity.title)),
            type = requireNotNull(tuple.get(brawlStarsNewsArticleEntity.type)),
            publishedAt = requireNotNull(tuple.get(brawlStarsNewsArticleEntity.publishedAt)),
            seenAt = requireNotNull(tuple.get(brawlStarsNewsArticleEntity.createdAt)),
            versionCount = 0,
        )
}
