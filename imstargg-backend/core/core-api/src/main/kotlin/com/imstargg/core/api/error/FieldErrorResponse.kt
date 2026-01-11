package com.imstargg.core.api.error

data class FieldErrorResponse(
    val field: String?,
    val value: Any?,
    val reason: String?,
)
