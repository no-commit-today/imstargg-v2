package com.imstargg.core.admin.application

import com.imstargg.CoreAdminContextTest
import com.imstargg.core.admin.request.BattleEventCreateRequest
import com.imstargg.core.admin.request.BattleEventModeCreateRequest
import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.MessageJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BattleEventServiceTest
    @Autowired
    constructor(
        private val sut: BattleEventService,
        private val battleEventJpaRepository: BattleEventJpaRepository,
        private val battleEventModeJpaRepository: BattleEventModeJpaRepository,
        private val messageJpaRepository: MessageJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreAdminContextTest() {
        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        @Test
        fun `이벤트 등록 시 맵 이름이 함께 저장된다`() {
            // given
            val request =
                BattleEventCreateRequest(
                    brawlStarsId = 15000000L,
                    modeId = null,
                    mapName = "Gem Fort",
                    mapNames = mapOf(Language.KOREAN to "젬 요새", Language.ENGLISH to "Gem Fort"),
                )

            // when
            sut.createEvent(request)

            // then
            assertThat(battleEventJpaRepository.findAll().toList().map { it.brawlStarsId }).contains(15000000L)
            val names =
                messageJpaRepository
                    .findAllByTypeAndBrawlStarsIdIn(MessageType.BATTLE_EVENT_NAME, listOf(15000000L))
                    .associate { it.lang to it.content }
            assertThat(names)
                .containsExactlyInAnyOrderEntriesOf(
                    mapOf(Language.KOREAN to "젬 요새", Language.ENGLISH to "Gem Fort"),
                )
        }

        @Test
        fun `이벤트 모드 등록 시 이름이 함께 저장된다`() {
            // given
            val request =
                BattleEventModeCreateRequest(
                    brawlStarsId = 48000000L,
                    brawlStarsName = "gemGrab",
                    names = mapOf(Language.KOREAN to "젬 그랩", Language.ENGLISH to "Gem Grab"),
                )

            // when
            sut.createMode(request)

            // then
            assertThat(battleEventModeJpaRepository.findByBrawlStarsId(48000000L)).isNotNull
            val names =
                messageJpaRepository
                    .findAllByTypeAndBrawlStarsIdIn(MessageType.BATTLE_EVENT_MODE_NAME, listOf(48000000L))
                    .associate { it.lang to it.content }
            assertThat(names)
                .containsExactlyInAnyOrderEntriesOf(
                    mapOf(Language.KOREAN to "젬 그랩", Language.ENGLISH to "Gem Grab"),
                )
        }

        @Test
        fun `모드 목록의 resultType 은 통계 수집 유형이다`() {
            // given
            battleEventModeJpaRepository.save(BattleEventModeEntity(brawlStarsId = 0L, brawlStarsName = "gemGrab"))
            battleEventModeJpaRepository.save(BattleEventModeEntity(brawlStarsId = 6L, brawlStarsName = "soloShowdown"))
            battleEventModeJpaRepository.save(BattleEventModeEntity(brawlStarsId = 24L, brawlStarsName = "duels"))

            // when
            val result = sut.findAllModes()

            // then
            assertThat(result.associate { it.brawlStarsId to it.resultType })
                .containsExactlyInAnyOrderEntriesOf(
                    mapOf(
                        0L to BattleEventModeResultType.RESULT,
                        6L to BattleEventModeResultType.RANK,
                        24L to null,
                    ),
                )
        }
    }
