package com.imstargg.core.api.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    @GetMapping("/health")
    fun health(): ResponseEntity<Unit> = ResponseEntity.status(HttpStatus.OK).build()
}
