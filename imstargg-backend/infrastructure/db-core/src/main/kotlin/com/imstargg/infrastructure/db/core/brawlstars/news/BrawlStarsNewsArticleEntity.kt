package com.imstargg.infrastructure.db.core.brawlstars.news

import com.imstargg.core.enums.BrawlStarsNewsArticleType
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 공식 블로그 글의 한 판.
 *
 * 한 행이 한 판이고 `type`·`content` 말고는 바꾸지 않는다.
 * 같은 글이 고쳐져 다시 나오면 새 행을 쌓는다. 행이 적어 인덱스와 유니크를 두지 않는다.
 */
@Entity
@Table(name = "brawlstars_news_article")
class BrawlStarsNewsArticleEntity(
    link: BrawlStarsNewsArticleLink,
    title: String,
    publishedAt: Instant,
    type: BrawlStarsNewsArticleType,
    rawBody: String,
    content: String,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawlstars_news_article_id")
    val id: Long? = null

    @Embedded
    val link: BrawlStarsNewsArticleLink = link

    @Column(name = "title", updatable = false, nullable = false, length = 255)
    val title: String = title

    @Column(name = "published_at", updatable = false, nullable = false)
    val publishedAt: Instant = publishedAt

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 25)
    var type: BrawlStarsNewsArticleType = type
        protected set

    @Column(name = "raw_body", updatable = false, nullable = false, columnDefinition = "text")
    val rawBody: String = rawBody

    @Column(name = "content", nullable = false, columnDefinition = "text")
    var content: String = content
        protected set

    fun updateType(type: BrawlStarsNewsArticleType) {
        this.type = type
    }

    fun updateContent(content: String) {
        this.content = content
    }
}
