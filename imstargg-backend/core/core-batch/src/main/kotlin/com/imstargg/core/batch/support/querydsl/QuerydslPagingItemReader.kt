package com.imstargg.core.batch.support.querydsl

import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.EntityTransaction
import org.springframework.batch.infrastructure.item.database.AbstractPagingItemReader
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.util.ClassUtils
import java.util.concurrent.CopyOnWriteArrayList

/**
 * https://github.com/jojoldu/spring-batch-querydsl
 */
open class QuerydslPagingItemReader<T : Any>(
    protected val entityManagerFactory: EntityManagerFactory,
    pageSize: Int,
    protected val queryFunction: (JPAQueryFactory) -> JPAQuery<T>,
) : AbstractPagingItemReader<T>() {
    protected val jpaPropertyMap: MutableMap<String, Any> = HashMap()
    protected lateinit var entityManager: EntityManager
    protected var transacted: Boolean = true

    init {
        setName(ClassUtils.getShortName(QuerydslPagingItemReader::class.java))
        setPageSize(pageSize)
    }

    override fun doOpen() {
        super.doOpen()
        entityManager = entityManagerFactory.createEntityManager(jpaPropertyMap)
            ?: throw DataAccessResourceFailureException("Unable to obtain an EntityManager")
    }

    override fun doReadPage() {
        val tx = getTxOrNull()

        val query =
            createQuery()
                .offset((page * pageSize).toLong())
                .limit(pageSize.toLong())

        initResults()
        fetchQuery(query, tx)
    }

    protected fun getTxOrNull(): EntityTransaction? {
        if (transacted) {
            val tx = entityManager.transaction
            tx.begin()
            entityManager.flush()
            entityManager.clear()
            return tx
        }
        return null
    }

    protected open fun createQuery(): JPAQuery<T> {
        val queryFactory = JPAQueryFactory(entityManager)
        return queryFunction(queryFactory)
    }

    protected fun initResults() {
        val r = results
        if (r.isNullOrEmpty()) {
            results = CopyOnWriteArrayList()
        } else {
            r.clear()
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun fetchQuery(
        query: JPAQuery<T>,
        tx: EntityTransaction?,
    ) {
        if (transacted) {
            (results as MutableList<T>).addAll(query.fetch())
            tx?.commit()
        } else {
            val queryResult = query.fetch()
            for (entity in queryResult) {
                entityManager.detach(entity)
                results?.add(entity)
            }
        }
    }

    override fun doClose() {
        entityManager.close()
        super.doClose()
    }
}
