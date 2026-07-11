package com.imstargg.core.batch.support.querydsl

import com.querydsl.core.types.dsl.NumberPath
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.infrastructure.item.ExecutionContext
import org.springframework.batch.infrastructure.item.ItemStreamException
import org.springframework.batch.infrastructure.item.ItemStreamReader

/**
 * 키셋(`id > lastId`) 페이징 ItemStreamReader.
 *
 * offset 페이징(AbstractPagingItemReader)은 뒤 페이지로 갈수록 스킵 비용이 누적되지만
 * 키셋은 페이지 위치와 무관하게 일정하다. 키셋 조건(`id > lastId` + `order by id asc` + `limit`)은
 * 리더가 직접 주입한다 — 호출자는 from/where 만 담은 base 쿼리를 제공하므로
 * 키셋 조건 누락 실수가 원천 차단된다.
 *
 * read() 가 마지막으로 반환한 항목의 id 를 ExecutionContext 에 저장해
 * 스텝 재시작 시 커밋된 지점 다음부터 이어 읽는다.
 *
 * `@StepScope` 빈으로 등록되어 CGLIB 스코프 프록시 대상이므로 open class 여야 한다.
 *
 * 계약:
 * - T 는 이미 영속화되어 id 가 할당된 엔티티여야 한다 (idOf 가 non-null id 를 반환).
 * - name 은 같은 잡/스텝 내에서 유일해야 한다 — ExecutionContext 키("{name}.lastReadId")가
 *   name 에만 의존하므로, 한 스텝에 같은 name 리더 둘을 등록하면 커서가 충돌한다.
 */
open class KeysetItemReader<T : Any>(
    name: String,
    private val entityManagerFactory: EntityManagerFactory,
    private val pageSize: Long,
    private val idPath: NumberPath<Long>,
    private val idOf: (T) -> Long,
    private val queryFunction: (JPAQueryFactory) -> JPAQuery<T>,
) : ItemStreamReader<T> {
    private val lastReadIdKey = "$name.lastReadId"

    private var entityManager: EntityManager? = null
    private var lastReadId = 0L
    private var lastFetchedId = 0L
    private var buffer: List<T> = emptyList()
    private var bufferIndex = 0
    private var exhausted = false

    override fun open(executionContext: ExecutionContext) {
        entityManager?.close() // 방어적 정리 — 정상 경로(open→close 짝)에서는 no-op
        entityManager = entityManagerFactory.createEntityManager()
        lastReadId = executionContext.getLong(lastReadIdKey, 0L)
        lastFetchedId = lastReadId
        buffer = emptyList()
        bufferIndex = 0
        exhausted = false
    }

    override fun read(): T? {
        if (bufferIndex >= buffer.size) {
            if (exhausted) return null
            fetchNextPage()
            if (buffer.isEmpty()) return null
        }
        val item = buffer[bufferIndex++]
        lastReadId = idOf(item)
        return item
    }

    private fun fetchNextPage() {
        val em = entityManager ?: throw ItemStreamException("리더가 open 되지 않았습니다.")
        em.clear()
        buffer =
            queryFunction(JPAQueryFactory(em))
                .where(idPath.gt(lastFetchedId))
                .orderBy(idPath.asc())
                .limit(pageSize)
                .fetch()
        bufferIndex = 0
        if (buffer.isEmpty()) {
            exhausted = true
        } else {
            lastFetchedId = idOf(buffer.last())
        }
    }

    override fun update(executionContext: ExecutionContext) {
        executionContext.putLong(lastReadIdKey, lastReadId)
    }

    override fun close() {
        entityManager?.close()
        entityManager = null
    }
}
