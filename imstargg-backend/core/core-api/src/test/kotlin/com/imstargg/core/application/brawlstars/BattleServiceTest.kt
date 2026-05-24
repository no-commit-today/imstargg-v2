package com.imstargg.core.application.brawlstars

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BattleServiceTest(
    private val sut: BattleService,
    private val cleanUp: CleanUp,
    private val playerJpaRepository: PlayerJpaRepository,
) : CoreApiContextTest() {
    @BeforeEach
    fun setUp() {
        cleanUp.all()
    }

    @Test
    fun `존재하지 않는 플레이어 태그면 NOT_FOUND 예외가 발생한다`() {
        // when
        val ex =
            catchThrowableOfType(CoreException::class.java) {
                sut.getBattles(BrawlStarsTag("#NONE"), OffsetLimit(offset = 0, limit = 20))
            }

        // then
        assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
    }

    @Test
    fun `존재하는 플레이어이고 전투가 없으면 빈 Slice 가 반환된다`() {
        // given
        val tag = BrawlStarsTag("#BSTOK")
        playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag(tag.value).build())

        // when
        val result = sut.getBattles(tag, OffsetLimit(offset = 0, limit = 20))

        // then
        assertThat(result.content).isEmpty()
        assertThat(result.hasNext).isFalse()
    }
}
