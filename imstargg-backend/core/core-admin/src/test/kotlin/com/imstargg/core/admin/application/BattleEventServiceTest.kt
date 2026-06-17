package com.imstargg.core.admin.application

import com.imstargg.CoreAdminContextTest
import com.imstargg.core.admin.request.BattleEventCreateRequest
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
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
    }
