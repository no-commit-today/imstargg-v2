package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.response.ClubResponse
import com.imstargg.core.worker.domain.Club
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ClubUpdater(
    private val clubRepository: ClubRepository,
) {
    /**
     * 신규 클럽 동시 INSERT race(유니크 충돌) 는 1회 재시도로 흡수한다. Spring Retry
     * 인터셉터가 트랜잭션 인터셉터 바깥에 적용되어 재시도마다 새 트랜잭션이 열리고,
     * 재시도 시점에는 row 가 존재해 SELECT FOR UPDATE 가 정상 동작한다.
     */
    @Retryable(retryFor = [DataIntegrityViolationException::class], maxAttempts = 2)
    @Transactional
    fun update(clubResponse: ClubResponse) {
        val club =
            clubRepository.findWithLockByBrawlStarsTag(clubResponse.tag)
                ?: Club.create(clubResponse)
        club.update(clubResponse)
        clubRepository.save(club)
    }
}
