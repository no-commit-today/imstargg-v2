package com.imstargg.core.admin.application.brawlstars

import com.imstargg.core.admin.response.brawlstars.BrawlStarsPatchListResponse
import com.imstargg.core.admin.response.brawlstars.BrawlStarsPatchResponse
import com.imstargg.infrastructure.db.core.brawlstars.patch.BrawlStarsPatchEntity
import com.imstargg.infrastructure.db.core.brawlstars.patch.BrawlStarsPatchRegistrationPolicy
import com.imstargg.infrastructure.db.core.brawlstars.patch.repository.BrawlStarsPatchJpaRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class AdminBrawlStarsPatchService(
    private val brawlStarsPatchJpaRepository: BrawlStarsPatchJpaRepository,
    private val clock: Clock,
) {
    fun findAll(): BrawlStarsPatchListResponse {
        val today = LocalDate.now(clock)
        return BrawlStarsPatchListResponse(
            today = today,
            earliestRegistrableDate = BrawlStarsPatchRegistrationPolicy.earliestRegistrableDate(today),
            patches =
                brawlStarsPatchJpaRepository
                    .findAllByOrderByPatchDateDesc()
                    .map(BrawlStarsPatchResponse::of),
        )
    }

    @Transactional
    fun register(
        patchDate: LocalDate,
        memo: String?,
    ): BrawlStarsPatchResponse {
        requireRegistrable(patchDate)
        requireNotExists(patchDate)
        val saved = brawlStarsPatchJpaRepository.save(BrawlStarsPatchEntity(patchDate, normalizeMemo(memo)))
        return BrawlStarsPatchResponse.of(saved)
    }

    /** 날짜가 그대로면 기한도 중복도 보지 않고 메모만 고친다. */
    @Transactional
    fun update(
        id: Long,
        patchDate: LocalDate,
        memo: String?,
    ) {
        val entity = getEntity(id)
        val normalizedMemo = normalizeMemo(memo)
        if (entity.patchDate != patchDate) {
            requireRegistrable(entity.patchDate)
            requireRegistrable(patchDate)
            requireNotExists(patchDate)
        }
        entity.update(patchDate, normalizedMemo)
    }

    @Transactional
    fun delete(id: Long) {
        val entity = getEntity(id)
        requireRegistrable(entity.patchDate)
        brawlStarsPatchJpaRepository.delete(entity)
    }

    private fun getEntity(id: Long): BrawlStarsPatchEntity =
        brawlStarsPatchJpaRepository
            .findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "BrawlStarsPatch not found: id=$id") }

    private fun requireRegistrable(patchDate: LocalDate) {
        val today = LocalDate.now(clock)
        if (!BrawlStarsPatchRegistrationPolicy.isRegistrable(today, patchDate)) {
            val earliest = BrawlStarsPatchRegistrationPolicy.earliestRegistrableDate(today)
            throw badRequest("$patchDate 는 다룰 수 없는 날짜입니다. $earliest 부터 오늘($today)까지의 패치일만 등록, 변경, 삭제할 수 있습니다.")
        }
    }

    private fun requireNotExists(patchDate: LocalDate) {
        if (brawlStarsPatchJpaRepository.existsByPatchDate(patchDate)) {
            throw badRequest("$patchDate 패치일이 이미 등록되어 있습니다.")
        }
    }

    private fun normalizeMemo(memo: String?): String? {
        val normalized = memo?.trim()?.takeIf { it.isNotEmpty() }
        if (normalized != null && normalized.length > MEMO_MAX_LENGTH) {
            throw badRequest("메모는 ${MEMO_MAX_LENGTH}자 이하로 적어 주세요.")
        }
        return normalized
    }

    private fun badRequest(message: String) = ResponseStatusException(HttpStatus.BAD_REQUEST, message)

    companion object {
        private const val MEMO_MAX_LENGTH = 255
    }
}
