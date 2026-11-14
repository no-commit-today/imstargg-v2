package com.imstargg.core.batch.domain.news

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NewsArticleContentDiffTest {
    @Test
    fun `한쪽에만 있는 줄을 센다`() {
        val diff = NewsArticleContentDiff.of(previousContent = "a\nb\nc", content = "a\nc\nd\ne")

        assertThat(diff).isEqualTo(NewsArticleContentDiff(addedLineCount = 2, deletedLineCount = 1))
    }

    @Test
    fun `같은 줄이 여러 번이면 횟수의 차이만큼 센다`() {
        val diff = NewsArticleContentDiff.of(previousContent = "a\na", content = "a")

        assertThat(diff).isEqualTo(NewsArticleContentDiff(addedLineCount = 0, deletedLineCount = 1))
    }

    @Test
    fun `같으면 0 이다`() {
        val diff = NewsArticleContentDiff.of(previousContent = "a\nb", content = "a\nb")

        assertThat(diff).isEqualTo(NewsArticleContentDiff(addedLineCount = 0, deletedLineCount = 0))
    }

    @Test
    fun `빈 본문은 0줄로 본다`() {
        assertThat(NewsArticleContentDiff.of(previousContent = "", content = "a\nb"))
            .isEqualTo(NewsArticleContentDiff(addedLineCount = 2, deletedLineCount = 0))
        assertThat(NewsArticleContentDiff.of(previousContent = "a", content = ""))
            .isEqualTo(NewsArticleContentDiff(addedLineCount = 0, deletedLineCount = 1))
    }
}
