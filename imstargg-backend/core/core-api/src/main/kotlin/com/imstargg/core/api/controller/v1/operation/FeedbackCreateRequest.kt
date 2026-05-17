package com.imstargg.core.api.controller.v1.operation

import com.imstargg.core.enums.operation.FeedbackCategory
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class FeedbackCreateRequest(
    @field:NotNull
    val category: FeedbackCategory?,
    @field:NotBlank
    @field:Size(max = 2000)
    val content: String?,
    @field:Email
    @field:Size(max = 254)
    val email: String?,
)
