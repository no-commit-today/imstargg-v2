package com.imstargg.core.batch.job.operation

import com.imstargg.core.enums.operation.FeedbackCategory

object FeedbackDigestMessage {
    fun build(
        countByCategory: Map<FeedbackCategory, Long>,
        emailCount: Long,
    ): FeedbackDigest? {
        val total = countByCategory.values.sum()
        if (total == 0L) return null

        val lines = mutableListOf<String>()
        FeedbackCategory.entries.forEach { category ->
            val count = countByCategory[category] ?: 0L
            if (count > 0L) {
                lines += "• ${category.name} ${count}건"
            }
        }
        if (emailCount > 0L) {
            lines += "✉️ 이메일 응답 요청 ${emailCount}건"
        }

        return FeedbackDigest(
            title = "📨 지난 24시간 피드백 ${total}건",
            content = lines.joinToString("\n"),
        )
    }
}
