package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.v1.response.RenewalResponse
import com.imstargg.core.application.renewal.RenewalService
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.OffsetDateTime

@RestController
class RenewalController(
    private val renewalService: RenewalService,
    private val clock: Clock,
) {
    @GetMapping("/v1/players/{tag}/renewal")
    fun getPlayerRenewal(
        @PathVariable tag: String,
    ): ResponseEntity<RenewalResponse> {
        val renewal = renewalService.getPlayerRenewal(BrawlStarsTag(tag))
        val now = OffsetDateTime.now(clock)
        return ResponseEntity.ok(RenewalResponse.of(renewal, progressing = !renewal.canRenew(now)))
    }

    @GetMapping("/v1/clubs/{tag}/renewal")
    fun getClubRenewal(
        @PathVariable tag: String,
    ): ResponseEntity<RenewalResponse> {
        val renewal = renewalService.getClubRenewal(BrawlStarsTag(tag))
        val now = OffsetDateTime.now(clock)
        return ResponseEntity.ok(RenewalResponse.of(renewal, progressing = !renewal.canRenew(now)))
    }

    @PostMapping("/v1/players/{tag}/renewal")
    fun renewPlayer(
        @PathVariable tag: String,
    ): ResponseEntity<RenewalResponse> {
        val renewal = renewalService.renewPlayer(BrawlStarsTag(tag))
        return ResponseEntity.ok(RenewalResponse.of(renewal, progressing = true))
    }

    @PostMapping("/v1/clubs/{tag}/renewal")
    fun renewClub(
        @PathVariable tag: String,
    ): ResponseEntity<RenewalResponse> {
        val renewal = renewalService.renewClub(BrawlStarsTag(tag))
        return ResponseEntity.ok(RenewalResponse.of(renewal, progressing = true))
    }
}
