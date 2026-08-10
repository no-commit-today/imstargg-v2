package com.imstargg.core.application.brawlstars

import com.imstargg.CoreApiContextTest
import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.enums.Language
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventSoloRankEventEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventSoloRankEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.MessageJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.BattleEventEntityFixture.Companion.aBattleEventEntity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId

@Import(BattleEventServiceTest.FixedClockConfig::class)
class BattleEventServiceTest(
    private val sut: BattleEventService,
    private val cleanUp: CleanUp,
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventSoloRankEventJpaRepository: BattleEventSoloRankEventJpaRepository,
    private val messageJpaRepository: MessageJpaRepository,
    private val cacheManager: CacheManager,
) : CoreApiContextTest() {
    @TestConfiguration
    class FixedClockConfig {
        @Bean
        @Primary
        fun fixedClock(): Clock = Clock.fixed(NOW.toInstant(), ZoneId.of("Asia/Seoul"))
    }

    companion object {
        /** 경쟁전 시즌 `2-1`(2025-07-17 ~ 2025-08-20) 구간의 시각. */
        val NOW: OffsetDateTime = OffsetDateTime.parse("2025-08-01T12:00:00+09:00")
        const val CURRENT_SEASON_KEY = "2-1"
        const val PAST_SEASON_KEY = "1-2"
    }

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
    fun `search 는 맵 이름 메시지에 부분 일치(대소문자 무시)하는 이벤트를 반환한다`() {
        // given
        battleEventJpaRepository.save(
            BattleEventEntity(brawlStarsId = 15_000_777L, mode = null, mapName = "Hard Rock Mine"),
        )
        battleEventJpaRepository.save(
            BattleEventEntity(brawlStarsId = 15_000_778L, mode = null, mapName = "Snake Prairie"),
        )
        messageJpaRepository.save(
            MessageEntity(
                type = MessageType.BATTLE_EVENT_NAME,
                brawlStarsId = 15_000_777L,
                lang = Language.DEFAULT,
                content = "Hard Rock Mine",
            ),
        )
        messageJpaRepository.save(
            MessageEntity(
                type = MessageType.BATTLE_EVENT_NAME,
                brawlStarsId = 15_000_778L,
                lang = Language.DEFAULT,
                content = "Snake Prairie",
            ),
        )

        // when
        val result = sut.search("rock")

        // then
        assertThat(result).hasSize(1)
        assertThat(result.first().id.value).isEqualTo(15_000_777L)
    }

    @Test
    fun `search 는 map_name 컬럼이 null 인 이벤트를 이름 메시지가 있어도 노출하지 않는다`() {
        // given - map_name null + 이름 메시지 존재 (admin 의 mapName null 게이팅과 동일 규약)
        battleEventJpaRepository.save(
            BattleEventEntity(brawlStarsId = 15_000_779L, mode = null, mapName = null),
        )
        messageJpaRepository.save(
            MessageEntity(
                type = MessageType.BATTLE_EVENT_NAME,
                brawlStarsId = 15_000_779L,
                lang = Language.DEFAULT,
                content = "Hidden Cave",
            ),
        )

        // when
        val result = sut.search("cave")

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

    @Test
    fun `getSoloRankEvents 는 seasonKey 가 없으면 현재 시즌 이벤트를 반환한다`() {
        // given
        saveSoloRankEvent(seasonKey = CURRENT_SEASON_KEY, eventBrawlStarsId = 15_000_101L)
        saveSoloRankEvent(seasonKey = PAST_SEASON_KEY, eventBrawlStarsId = 15_000_102L)

        // when
        val result = sut.getSoloRankEvents()

        // then
        assertThat(result).hasSize(1)
        assertThat(result.first().id.value).isEqualTo(15_000_101L)
    }

    @Test
    fun `getSoloRankEvents 는 seasonKey 로 과거 시즌 이벤트를 반환한다`() {
        // given
        saveSoloRankEvent(seasonKey = CURRENT_SEASON_KEY, eventBrawlStarsId = 15_000_101L)
        saveSoloRankEvent(seasonKey = PAST_SEASON_KEY, eventBrawlStarsId = 15_000_102L)

        // when
        val result = sut.getSoloRankEvents(PAST_SEASON_KEY)

        // then
        assertThat(result).hasSize(1)
        assertThat(result.first().id.value).isEqualTo(15_000_102L)
    }

    @Test
    fun `getSoloRankEvents 는 아직 오지 않은 시즌의 seasonKey 면 INVALID_REQUEST 예외가 발생한다`() {
        // when
        val ex =
            catchThrowableOfType(CoreException::class.java) {
                sut.getSoloRankEvents("2-2")
            }

        // then
        assertThat(ex.errorType).isEqualTo(ErrorType.INVALID_REQUEST)
    }

    @Test
    fun `getSoloRankEvents 는 형식이 잘못된 seasonKey 면 INVALID_REQUEST 예외가 발생한다`() {
        // when
        val ex =
            catchThrowableOfType(CoreException::class.java) {
                sut.getSoloRankEvents("abc")
            }

        // then
        assertThat(ex.errorType).isEqualTo(ErrorType.INVALID_REQUEST)
    }

    @Test
    fun `getSoloRankEvents 는 시즌 번호가 비정상적으로 큰 seasonKey 도 즉시 INVALID_REQUEST 로 거부한다`() {
        // when - SoloRankSeason.ofKey 였다면 목표 key 까지 next() 를 반복해 사실상 무한 루프가 되는 값
        val ex =
            catchThrowableOfType(CoreException::class.java) {
                sut.getSoloRankEvents("999999999-1")
            }

        // then
        assertThat(ex.errorType).isEqualTo(ErrorType.INVALID_REQUEST)
    }

    private fun saveSoloRankEvent(
        seasonKey: String,
        eventBrawlStarsId: Long,
    ) {
        battleEventJpaRepository.save(
            aBattleEventEntity()
                .withBrawlStarsId(eventBrawlStarsId)
                .withMode(null)
                .build(),
        )
        battleEventSoloRankEventJpaRepository.save(
            BattleEventSoloRankEventEntity(
                soloRankSeasonKey = seasonKey,
                eventBrawlStarsId = eventBrawlStarsId,
            ),
        )
    }
}
