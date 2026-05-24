package com.imstargg.core.admin.request.operation

import com.imstargg.core.enums.operation.FeedbackRating

data class FeedbackSearchRequest(
    val rating: FeedbackRating? = null,
    val checked: Boolean? = null,
    val page: Int = 0,
    val size: Int = 20,
) {
    fun normalizedPage(): Int = page.coerceAtLeast(0)

    fun normalizedSize(): Int = size.coerceIn(1, 100)
}
