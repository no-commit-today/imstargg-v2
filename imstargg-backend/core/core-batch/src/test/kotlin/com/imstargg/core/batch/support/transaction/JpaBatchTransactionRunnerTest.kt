package com.imstargg.core.batch.support.transaction

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus

class JpaBatchTransactionRunnerTest {
    private val txManager = mockk<PlatformTransactionManager>()
    private val entityManager = mockk<EntityManager>()
    private val sut = JpaBatchTransactionRunner(txManager, entityManager)

    @Test
    fun `REQUIRES_NEW transaction 안에서 EntityManager 와 QueryFactory 를 제공한다`() {
        // given
        val status = mockk<TransactionStatus>(relaxed = true)
        every {
            txManager.getTransaction(
                match { it.propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW },
            )
        } returns status
        every { txManager.commit(status) } just Runs

        // when
        val result =
            sut.run { em, queryFactory ->
                assertThat(em).isSameAs(entityManager)
                assertThat(queryFactory).isNotNull()
                "done"
            }

        // then
        assertThat(result).isEqualTo("done")
        verify(exactly = 1) { txManager.commit(status) }
    }

    @Test
    fun `action 이 실패하면 transaction 을 rollback 하고 예외를 전파한다`() {
        // given
        val status = mockk<TransactionStatus>(relaxed = true)
        val failure = IllegalStateException("replace failed")
        every {
            txManager.getTransaction(
                match { it.propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW },
            )
        } returns status
        every { txManager.rollback(status) } just Runs

        // when, then
        assertThatThrownBy {
            sut.run { _, _ -> throw failure }
        }.isSameAs(failure)
        verify(exactly = 1) { txManager.rollback(status) }
    }
}
