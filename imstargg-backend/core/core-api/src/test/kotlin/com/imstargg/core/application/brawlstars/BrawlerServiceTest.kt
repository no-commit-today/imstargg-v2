package com.imstargg.core.application.brawlstars

import com.imstargg.CoreApiContextTest
import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import com.imstargg.infrastructure.db.core.brawlstars.brawler.BrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.CacheManager

class BrawlerServiceTest(
    private val sut: BrawlerService,
    private val cleanUp: CleanUp,
    private val brawlerJpaRepository: BrawlerJpaRepository,
    private val cacheManager: CacheManager,
) : CoreApiContextTest() {
    @BeforeEach
    fun setUp() {
        cleanUp.all()
        cacheManager.getCache(CacheNames.BRAWLER)?.clear()
    }

    @Test
    fun `getAll 은 저장된 모든 brawler 를 id 오름차순으로 반환한다`() {
        // given
        brawlerJpaRepository.save(brawler(id = 16_000_002L))
        brawlerJpaRepository.save(brawler(id = 16_000_000L))
        brawlerJpaRepository.save(brawler(id = 16_000_001L))

        // when
        val result = sut.getAll()

        // then
        assertThat(result)
            .extracting<Long> { it.id.value }
            .containsExactly(16_000_000L, 16_000_001L, 16_000_002L)
    }

    @Test
    fun `get 은 존재하는 id 면 brawler 를 반환하고, 미존재 id 면 NOT_FOUND 예외가 발생한다`() {
        // given
        brawlerJpaRepository.save(brawler(id = 16_000_000L))

        // when
        val found = sut.get(BrawlStarsId(16_000_000L))
        val ex =
            catchThrowableOfType(CoreException::class.java) {
                sut.get(BrawlStarsId(99_999_999L))
            }

        // then
        assertThat(found.id.value).isEqualTo(16_000_000L)
        assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
    }

    @Test
    fun `search 는 name 이 blank 면 전체 brawler 를 반환한다`() {
        // given
        brawlerJpaRepository.save(brawler(id = 16_000_000L))
        brawlerJpaRepository.save(brawler(id = 16_000_001L))

        // when
        val result = sut.search("   ")

        // then
        assertThat(result).hasSize(2)
    }

    @Test
    fun `getBrawlerWithItems 는 items 가 없을 때 빈 BrawlerItems 로 채워서 반환한다`() {
        // given
        brawlerJpaRepository.save(brawler(id = 16_000_000L))

        // when
        val result = sut.getBrawlerWithItems(BrawlStarsId(16_000_000L))

        // then
        assertThat(result.brawler.id.value).isEqualTo(16_000_000L)
        assertThat(result.items.gadgets).isEmpty()
        assertThat(result.items.gears).isEmpty()
        assertThat(result.items.starPowers).isEmpty()
        assertThat(result.items.hyperCharge).isNull()
        assertThat(result.items.buffies).isNull()
    }

    private fun brawler(id: Long) =
        BrawlerEntity(
            brawlStarsId = id,
            rarity = BrawlerRarity.STARTING_BRAWLER,
            role = BrawlerRole.DAMAGE_DEALER,
        )
}
