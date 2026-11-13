package com.imstargg.core.batch.domain.news

import com.imstargg.core.enums.BrawlStarsNewsArticleType

/**
 * 공식 블로그 글의 제목으로 유형을 판정한다.
 *
 * 규칙이 넓어서 릴리스 노트가 아니어도 제목에 `update` 와 숫자가 나란히 있으면 릴리스 노트로 잡는다.
 */
object NewsArticleTypeClassifier {
    private val RELEASE_NOTE_TITLE =
        Regex("""release notes|patch notes|\bupdate \d+\b""", RegexOption.IGNORE_CASE)

    fun classify(title: String): BrawlStarsNewsArticleType =
        if (RELEASE_NOTE_TITLE.containsMatchIn(title)) {
            BrawlStarsNewsArticleType.RELEASE_NOTE
        } else {
            BrawlStarsNewsArticleType.OTHER
        }
}
