package com.imstargg.core.batch.support.querydsl

import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.util.ClassUtils

/**
 * https://github.com/jojoldu/spring-batch-querydsl
 */
class QuerydslZeroPagingItemReader<T : Any>(
    entityManagerFactory: EntityManagerFactory,
    pageSize: Int,
    queryFunction: (JPAQueryFactory) -> JPAQuery<T>,
) : QuerydslPagingItemReader<T>(entityManagerFactory, pageSize, queryFunction) {
    init {
        setName(ClassUtils.getShortName(QuerydslZeroPagingItemReader::class.java))
        transacted = true
    }

    override fun doReadPage() {
        val tx = getTxOrNull()

        val query =
            createQuery()
                .offset(0)
                .limit(pageSize.toLong())

        initResults()
        fetchQuery(query, tx)
    }
}
