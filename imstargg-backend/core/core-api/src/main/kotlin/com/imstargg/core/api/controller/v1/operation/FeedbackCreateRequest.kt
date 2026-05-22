package com.imstargg.core.api.controller.v1.operation

import com.imstargg.core.enums.operation.FeedbackRating
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class FeedbackCreateRequest(
    @field:NotNull
    val rating: FeedbackRating?,
    @field:NotBlank
    @field:Size(max = 2000)
    val content: String,
)
