package com.imstargg.core.application.brawlstars

import com.imstargg.CoreApiContextTest
import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.BattleEventEntityFixture.Companion.aBattleEventEntity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.CacheManager

class BattleEventServiceTest(
    private val sut: BattleEventService,
    private val cleanUp: CleanUp,
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val cacheManager: CacheManager,
) : CoreApiContextTest() {
    @BeforeEach
    fun setUp() {
        cleanUp.all()
        cacheManager.getCache(CacheNames.BATTLE_EVENT)?.clear()
    }

    @Test
    fun `get 은 존재하는 id 면 BattleEvent 를 반환한다`() {
        // given
        battleEventJpaRepository.save(
            aBattleEventEntity()
                .withBrawlStarsId(15_000_001L)
                .withMode(null)
                .build(),
        )

        // when
        val result = sut.get(BrawlStarsId(15_000_001L))

        // then
        assertThat(result.id.value).isEqualTo(15_000_001L)
        assertThat(result.mode).isNull()
    }

    @Test
    fun `get 은 미존재 id 면 NOT_FOUND 예외가 발생한다`() {
        // when
        val ex =
            catchThrowableOfType(CoreException::class.java) {
                sut.get(BrawlStarsId(99_999_999L))
            }

        // then
        assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
    }

    @Test
    fun `search 는 매칭되는 BATTLE_EVENT_NAME 메시지가 없으면 빈 리스트를 반환한다`() {
        // when
        val result = sut.search("아무이름")

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `getCurrentRotationEvents 는 로테이션 데이터가 없으면 빈 리스트를 반환한다`() {
        // when
        val result = sut.getCurrentRotationEvents()

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `getSoloRankEvents 는 현재 시즌에 등록된 이벤트가 없으면 빈 리스트를 반환한다`() {
        // when
        val result = sut.getSoloRankEvents()

        // then
        assertThat(result).isEmpty()
    }
}
