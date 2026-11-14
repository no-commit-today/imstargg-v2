package com.imstargg.infrastructure.db.core.brawlstars.news

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

/**
 * 공식 블로그 글의 주소.
 *
 * [linkUrl] 은 절대 주소이고, [articleKey] 는 같은 글의 다른 판을 묶는 키다.
 * 공식 블로그는 글을 고쳐 다시 낼 때 슬러그 끝에 `-2`, `-3` 처럼 숫자를 붙이므로
 * 슬러그(마지막 구간) 끝의 숫자 한두 자리를 한 번만 떼어 키로 쓴다.
 * 연도처럼 네 자리 숫자는 떼지 않는다.
 */
@Embeddable
data class BrawlStarsNewsArticleLink(
    @Column(name = "link_url", updatable = false, nullable = false, length = 500)
    val linkUrl: String,
    @Column(name = "article_key", updatable = false, nullable = false, length = 255)
    val articleKey: String,
) {
    companion object {
        const val SITE_URL = "https://supercell.com"

        private val VERSION_SUFFIX = Regex("-\\d{1,2}$")

        /** 목록이 주는 경로(`/en/games/brawlstars/blog/{분류}/{슬러그}`, 끝 슬래시는 있어도 된다)로 만든다. */
        fun of(path: String): BrawlStarsNewsArticleLink {
            val trimmed = path.trimEnd('/')
            val slug = trimmed.substringAfterLast('/')
            return BrawlStarsNewsArticleLink(
                linkUrl = SITE_URL + trimmed,
                articleKey = slug.replace(VERSION_SUFFIX, ""),
            )
        }
    }
}
