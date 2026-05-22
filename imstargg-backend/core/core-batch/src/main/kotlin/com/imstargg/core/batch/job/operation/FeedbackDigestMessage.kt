package com.imstargg.core.batch.job.operation

import com.imstargg.core.enums.operation.FeedbackRating

object FeedbackDigestMessage {
    private val RATING_LABELS: Map<FeedbackRating, String> =
        mapOf(
            FeedbackRating.VERY_SATISFIED to "매우 만족",
            FeedbackRating.SATISFIED to "만족",
            FeedbackRating.NEUTRAL to "보통",
            FeedbackRating.DISSATISFIED to "불만족",
            FeedbackRating.VERY_DISSATISFIED to "매우 불만족",
        )

    fun build(countByRating: Map<FeedbackRating, Long>): FeedbackDigest? {
        val total = countByRating.values.sum()
        if (total == 0L) return null

        val lines = mutableListOf<String>()
        FeedbackRating.entries.forEach { rating ->
            val count = countByRating[rating] ?: 0L
            if (count > 0L) {
                val label = RATING_LABELS[rating] ?: rating.name
                lines += "• $label ${count}건"
            }
        }

        return FeedbackDigest(
            title = "📨 지난 24시간 피드백 ${total}건",
            content = lines.joinToString("\n"),
        )
    }
}
