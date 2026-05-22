package com.imstargg.core.admin.controller.operation

import com.imstargg.core.admin.application.operation.AdminFeedbackService
import com.imstargg.core.admin.request.operation.FeedbackCheckRequest
import com.imstargg.core.admin.request.operation.FeedbackSearchRequest
import com.imstargg.core.admin.response.operation.FeedbackListResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminFeedbackController(
    private val service: AdminFeedbackService,
) {
    @GetMapping("/feedbacks")
    fun list(
        @ModelAttribute request: FeedbackSearchRequest,
    ): ResponseEntity<FeedbackListResponse> {
        val page =
            service.search(
                rating = request.rating,
                checked = request.checked,
                page = request.normalizedPage(),
                size = request.normalizedSize(),
            )
        return ResponseEntity.ok(FeedbackListResponse.of(page))
    }

    @PutMapping("/feedbacks/{id}/check")
    fun updateCheck(
        @PathVariable id: Long,
        @RequestBody request: FeedbackCheckRequest,
    ): ResponseEntity<Unit> {
        service.updateCheck(id, request.checked)
        return ResponseEntity.noContent().build()
    }
}
