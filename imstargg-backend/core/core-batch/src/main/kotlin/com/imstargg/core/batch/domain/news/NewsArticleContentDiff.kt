package com.imstargg.core.batch.domain.news

/**
 * 두 본문의 줄을 견줘 한쪽에만 있는 줄을 센다. 같은 줄이 여러 번 나오면 횟수의 차이만큼 센다.
 *
 * 줄은 `\n` 으로 나눈 것이고, 빈 문자열은 0줄이다.
 */
data class NewsArticleContentDiff(
    val addedLineCount: Int,
    val deletedLineCount: Int,
) {
    companion object {
        fun of(
            previousContent: String,
            content: String,
        ): NewsArticleContentDiff {
            val previousCounts = countLines(previousContent)
            val counts = countLines(content)
            val added = counts.entries.sumOf { (line, count) -> maxOf(0, count - (previousCounts[line] ?: 0)) }
            val removed = previousCounts.entries.sumOf { (line, count) -> maxOf(0, count - (counts[line] ?: 0)) }
            return NewsArticleContentDiff(addedLineCount = added, deletedLineCount = removed)
        }

        private fun countLines(content: String): Map<String, Int> =
            if (content.isEmpty()) {
                emptyMap()
            } else {
                content.split("\n").groupingBy { it }.eachCount()
            }
    }
}
