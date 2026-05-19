package com.imstargg.core.batch.support.transaction

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Component
class JpaBatchTransactionRunner(
    txManager: PlatformTransactionManager,
    private val entityManager: EntityManager,
) {
    private val transactionTemplate =
        TransactionTemplate(txManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    fun <T : Any> run(action: (EntityManager, JPAQueryFactory) -> T): T =
        transactionTemplate.execute {
            action(entityManager, JPAQueryFactory(entityManager))
        } ?: error("JpaBatchTransactionRunner action returned null")
}
