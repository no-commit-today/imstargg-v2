package com.imstargg.core.api.controller.v1.operation

import com.imstargg.core.api.controller.v1.response.operation.FeedbackCreateResponse
import com.imstargg.core.application.operation.FeedbackService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class FeedbackController(
    private val feedbackService: FeedbackService,
) {
    @PostMapping("/v1/feedback")
    fun create(
        @Valid @RequestBody request: FeedbackCreateRequest,
    ): ResponseEntity<FeedbackCreateResponse> {
        val feedback =
            feedbackService.submit(
                category = request.category,
                content = request.content,
                email = request.email,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(FeedbackCreateResponse.of(feedback))
    }
}
