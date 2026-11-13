package com.imstargg.core.batch.domain.news

import com.imstargg.core.enums.BrawlStarsNewsArticleType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class NewsArticleTypeClassifierTest {
    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "Release Notes November 2026|RELEASE_NOTE",
            "PATCH NOTES: Angels vs Demons|RELEASE_NOTE",
            "UPDATE 65: BUFFIES!|RELEASE_NOTE",
            "Brawl Stars x Duolingo|OTHER",
            "Balance Changes|OTHER",
            "Updates are coming|OTHER",
            // 릴리스 노트가 아니어도 "update + 숫자"면 잡힌다. 규칙이 넓다는 것을 남긴다.
            "Update 2 the rules|RELEASE_NOTE",
        ],
    )
    fun `제목으로 유형을 판정한다`(
        title: String,
        expected: BrawlStarsNewsArticleType,
    ) {
        assertThat(NewsArticleTypeClassifier.classify(title)).isEqualTo(expected)
    }
}
