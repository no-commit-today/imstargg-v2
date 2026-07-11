package com.imstargg.core.worker.application.club

import com.imstargg.CoreWorkerContextTest
import com.imstargg.client.brawlstars.response.ClubMemberResponse
import com.imstargg.client.brawlstars.response.ClubResponse
import com.imstargg.client.brawlstars.response.PlayerIconResponse
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.ClubJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.ClubMemberJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.ClubEntityFixture.Companion.aClubEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ClubUpdaterConcurrencyTest(
    private val sut: ClubUpdater,
    private val clubRepository: ClubJpaRepository,
    private val clubMemberRepository: ClubMemberJpaRepository,
    private val cleanUp: CleanUp,
) : CoreWorkerContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `동시에 같은 태그로 처음 update하면 재시도로 모두 성공하고 하나만 저장된다`() {
        // given
        val tag = "#CLUB1"
        val barrier = CyclicBarrier(2)
        val executor = Executors.newFixedThreadPool(2)

        // when
        val futures =
            (1..2).map {
                executor.submit<Exception?> {
                    barrier.await()
                    try {
                        sut.update(createClubResponse(tag))
                        null
                    } catch (e: Exception) {
                        e
                    }
                }
            }
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()

        // then
        assertThat(results).allMatch { it == null }
        assertThat(clubRepository.count()).isEqualTo(1)
    }

    @Test
    fun `동시에 같은 클럽을 update하면 모두 성공한다`() {
        // given
        val tag = "#CLUB2"
        clubRepository.save(aClubEntity().withBrawlStarsTag(tag).build())
        val barrier = CyclicBarrier(2)
        val executor = Executors.newFixedThreadPool(2)

        // when
        val futures =
            (1..2).map {
                executor.submit<Exception?> {
                    barrier.await()
                    try {
                        sut.update(createClubResponse(tag))
                        null
                    } catch (e: Exception) {
                        e
                    }
                }
            }
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()

        // then
        assertThat(results).allMatch { it == null }
        assertThat(clubRepository.count()).isEqualTo(1)
    }

    @Test
    fun `동시에 같은 클럽멤버를 update하면 중복 저장되지_않는다`() {
        // given
        val tag = "#CLUB3"
        clubRepository.save(aClubEntity().withBrawlStarsTag(tag).build())
        val members =
            listOf(
                createClubMemberResponse("#MEMBER1"),
                createClubMemberResponse("#MEMBER2"),
            )
        val barrier = CyclicBarrier(2)
        val executor = Executors.newFixedThreadPool(2)

        // when
        val futures =
            (1..2).map {
                executor.submit<Exception?> {
                    barrier.await()
                    try {
                        sut.update(createClubResponse(tag, members))
                        null
                    } catch (e: Exception) {
                        e
                    }
                }
            }
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()

        // then
        assertThat(results).allMatch { it == null }
        assertThat(clubMemberRepository.count()).isEqualTo(2)
    }

    private fun createClubResponse(
        tag: String,
        members: List<ClubMemberResponse> = emptyList(),
    ) = ClubResponse(
        tag = tag,
        name = "Test Club",
        description = "desc",
        type = "open",
        badgeId = 1L,
        requiredTrophies = 0,
        trophies = 100,
        members = members,
    )

    private fun createClubMemberResponse(tag: String) =
        ClubMemberResponse(
            tag = tag,
            name = "Member $tag",
            nameColor = "0xff0000ff",
            role = "member",
            trophies = 500,
            icon = PlayerIconResponse(id = 1L),
        )
}
