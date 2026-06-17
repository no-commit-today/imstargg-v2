package com.imstargg.core.admin.application

import com.imstargg.CoreAdminContextTest
import com.imstargg.core.admin.request.BattleEventCreateRequest
import com.imstargg.core.admin.request.BattleEventModeCreateRequest
import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.MessageJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
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
        fun `이벤트 모드 등록 시 이름_인원수_색상이 함께 저장된다`() {
            // given
            val request =
                BattleEventModeCreateRequest(
                    brawlStarsId = 48000000L,
                    brawlStarsName = "gemGrab",
                    resultType = BattleEventModeResultType.RESULT,
                    names = mapOf(Language.KOREAN to "젬 그랩", Language.ENGLISH to "Gem Grab"),
                    playerCounts = listOf(3, 3),
                    color = "#1A2B3C",
                )

            // when
            sut.createMode(request)

            // then
            val mode = battleEventModeJpaRepository.findByBrawlStarsId(48000000L)
            assertThat(mode).isNotNull
            assertThat(mode!!.playerCounts).containsExactly(3, 3)
            assertThat(mode.color).isEqualTo("#1A2B3C")
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
        fun `이벤트 모드 등록 시 잘못된 색상이면 예외가 발생한다`() {
            // given
            val request =
                BattleEventModeCreateRequest(
                    brawlStarsId = 48000001L,
                    brawlStarsName = "bounty",
                    color = "not-a-color",
                )

            // when
            val thrown =
                catchThrowableOfType(IllegalArgumentException::class.java) { sut.createMode(request) }

            // then
            assertThat(thrown).isNotNull
        }
    }
