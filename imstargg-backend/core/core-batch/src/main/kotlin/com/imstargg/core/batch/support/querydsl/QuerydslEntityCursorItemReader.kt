package com.imstargg.core.batch.support.querydsl

import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory

/**
 * @author choyoungrae
 */
class QuerydslEntityCursorItemReader<T : Any>(
    entityManagerFactory: EntityManagerFactory,
    pageSize: Int,
    private val queryFunctionBuildFunction: (T?, JPAQueryFactory) -> JPAQuery<T>,
) : QuerydslPagingItemReader<T>(entityManagerFactory, pageSize, { throw UnsupportedOperationException() }) {
    private var lastItem: T? = null

    init {
        setName(javaClass.simpleName)
        isSaveState = false
        transacted = false
    }

    override fun doReadPage() {
        val tx = getTxOrNull()

        val query =
            createQuery()
                .limit(pageSize.toLong())

        initResults()
        fetchQuery(query, tx)
        resetLastItem()
    }

    override fun createQuery(): JPAQuery<T> {
        val queryFactory = JPAQueryFactory(entityManager)
        return queryFunctionBuildFunction(lastItem, queryFactory)
    }

    private fun resetLastItem() {
        if (isNotEmptyResults()) {
            lastItem = results?.last()
        }
    }

    private fun isNotEmptyResults(): Boolean {
        val r = results?.toList() ?: return false
        return r.isNotEmpty()
    }
}
