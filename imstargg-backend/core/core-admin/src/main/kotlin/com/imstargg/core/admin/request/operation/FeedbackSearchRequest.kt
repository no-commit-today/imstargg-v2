package com.imstargg.core.admin.request.operation

import com.imstargg.core.enums.operation.FeedbackCategory

data class FeedbackSearchRequest(
    val category: FeedbackCategory? = null,
    val checked: Boolean? = null,
    val page: Int = 0,
    val size: Int = 20,
) {
    fun normalizedSize(): Int = size.coerceIn(1, 100)
}
