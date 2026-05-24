package com.imstargg.infrastructure.db.core.brawlstars.event

import com.imstargg.DbCoreContextTest
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import com.imstargg.infrastructure.db.core.test.fixture.BattleEventModeEntityFixture.Companion.aBattleEventModeEntity
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BattleEventModeEntityTest : DbCoreContextTest() {
    @Autowired
    private lateinit var repository: BattleEventModeJpaRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `playerCounts jsonb round-trip 보존`() {
        // given
        val sut = aBattleEventModeEntity().withPlayerCounts(listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)).build()

        // when
        val saved = repository.save(sut)
        repository.flush()
        entityManager.clear()
        val reloaded = repository.findById(saved.id!!).orElseThrow()

        // then
        assertThat(reloaded.playerCounts).containsExactly(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
    }

    @Test
    fun `updatePlayerCounts 빈 리스트는 거부`() {
        // given
        val sut = aBattleEventModeEntity().withPlayerCounts(listOf(3, 3)).build()

        // when
        val ex =
            catchThrowableOfType(IllegalArgumentException::class.java) {
                sut.updatePlayerCounts(emptyList())
            }

        // then
        assertThat(ex).hasMessageContaining("playerCounts")
    }

    @Test
    fun `updatePlayerCounts 0 이하 인원은 거부`() {
        // given
        val sut = aBattleEventModeEntity().withPlayerCounts(listOf(3, 3)).build()

        // when
        val ex =
            catchThrowableOfType(IllegalArgumentException::class.java) {
                sut.updatePlayerCounts(listOf(3, 0))
            }

        // then
        assertThat(ex).hasMessageContaining("playerCounts")
    }
}
