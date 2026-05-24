package com.imstargg.core.application.statistics

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.GameSlot
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class GameSlotRepositoryTest(
    private val sut: GameSlotRepository,
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventModeJpaRepository: BattleEventModeJpaRepository,
    private val cleanUp: CleanUp,
) : CoreApiContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `BattleEvent 의 mode playerCounts 를 GameSlot 으로 반환`() {
        // given
        val eventId = 15000001L
        val mode =
            battleEventModeJpaRepository.save(
                BattleEventModeEntity(
                    brawlStarsId = 48000001L,
                    brawlStarsName = "gemGrab",
                    playerCounts = listOf(3, 3),
                ),
            )
        battleEventJpaRepository.save(
            BattleEventEntity(brawlStarsId = eventId, mode = mode, mapName = "Hard Rock Mine"),
        )

        // when
        val result = sut.find(BrawlStarsId(eventId))

        // then
        assertThat(result.playerCounts).isEqualTo(listOf(3, 3))
    }

    @Test
    fun `BattleEvent 가 없으면 default GameSlot 반환`() {
        // given - 아무것도 저장 안 함

        // when
        val result = sut.find(BrawlStarsId(99999999L))

        // then
        assertThat(result).isEqualTo(GameSlot.default())
    }

    @Test
    fun `BattleEvent mode 가 null 이면 default GameSlot 반환`() {
        // given
        val eventId = 15000002L
        battleEventJpaRepository.save(
            BattleEventEntity(brawlStarsId = eventId, mode = null, mapName = "Some Map"),
        )

        // when
        val result = sut.find(BrawlStarsId(eventId))

        // then
        assertThat(result).isEqualTo(GameSlot.default())
    }

    @Test
    fun `findAllByEventIds 는 distinct 처리 후 각 eventId 에 대한 GameSlot 묶음 반환`() {
        // given
        val modeA =
            battleEventModeJpaRepository.save(
                BattleEventModeEntity(
                    brawlStarsId = 48000010L,
                    brawlStarsName = "gemGrab",
                    playerCounts = listOf(3, 3),
                ),
            )
        val modeB =
            battleEventModeJpaRepository.save(
                BattleEventModeEntity(
                    brawlStarsId = 48000011L,
                    brawlStarsName = "soloShowdown",
                    playerCounts = List(10) { 1 },
                ),
            )
        battleEventJpaRepository.save(BattleEventEntity(brawlStarsId = 100L, mode = modeA, mapName = "A"))
        battleEventJpaRepository.save(BattleEventEntity(brawlStarsId = 200L, mode = modeB, mapName = "B"))

        // when - 중복 + 미존재 eventId 섞임
        val result =
            sut.findAllByEventIds(
                listOf(BrawlStarsId(100L), BrawlStarsId(200L), BrawlStarsId(100L), BrawlStarsId(300L)),
            )

        // then
        assertThat(result[BrawlStarsId(100L)].playerCounts).isEqualTo(listOf(3, 3))
        assertThat(result[BrawlStarsId(200L)].playerCounts).isEqualTo(List(10) { 1 })
        assertThat(result[BrawlStarsId(300L)]).isEqualTo(GameSlot.default()) // missing 도 default 반환
    }
}
