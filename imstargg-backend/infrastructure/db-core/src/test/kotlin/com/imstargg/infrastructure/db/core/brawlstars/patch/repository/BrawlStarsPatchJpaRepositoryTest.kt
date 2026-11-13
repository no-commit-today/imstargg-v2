package com.imstargg.infrastructure.db.core.brawlstars.patch.repository

import com.imstargg.DbCoreContextTest
import com.imstargg.infrastructure.db.core.brawlstars.patch.BrawlStarsPatchEntity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate

class BrawlStarsPatchJpaRepositoryTest(
    private val repository: BrawlStarsPatchJpaRepository,
) : DbCoreContextTest() {

    @Test
    fun `같은 패치일을 두 번 저장하면 실패한다`() {
        repository.saveAndFlush(BrawlStarsPatchEntity(LocalDate.of(2026, 12, 1), null))

        assertThatThrownBy {
            repository.saveAndFlush(BrawlStarsPatchEntity(LocalDate.of(2026, 12, 1), "중복"))
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `패치일이 늦은 것부터 돌려준다`() {
        repository.saveAndFlush(BrawlStarsPatchEntity(LocalDate.of(2026, 12, 1), null))
        repository.saveAndFlush(BrawlStarsPatchEntity(LocalDate.of(2026, 12, 20), null))
        repository.saveAndFlush(BrawlStarsPatchEntity(LocalDate.of(2026, 12, 10), null))

        val dates = repository.findAllByOrderByPatchDateDesc().map { it.patchDate }

        assertThat(dates).containsExactly(
            LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 1),
        )
    }

    @Test
    fun `패치일과 메모를 고친다`() {
        val saved = repository.saveAndFlush(BrawlStarsPatchEntity(LocalDate.of(2026, 12, 1), null))

        saved.update(LocalDate.of(2026, 12, 2), "고침")
        repository.saveAndFlush(saved)

        val found = repository.findById(saved.id!!).get()
        assertThat(found.patchDate).isEqualTo(LocalDate.of(2026, 12, 2))
        assertThat(found.memo).isEqualTo("고침")
        assertThat(repository.existsByPatchDate(LocalDate.of(2026, 12, 2))).isTrue()
        assertThat(repository.existsByPatchDate(LocalDate.of(2026, 12, 1))).isFalse()
    }
}
