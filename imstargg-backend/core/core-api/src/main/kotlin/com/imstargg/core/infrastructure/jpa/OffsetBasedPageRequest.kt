package com.imstargg.core.infrastructure.jpa

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

/**
 * offset 을 그대로 보존하는 Pageable.
 * `PageRequest.of(offset / limit, limit)` 는 offset 이 limit 의 배수가 아니면 위치가 어긋난다.
 */
class OffsetBasedPageRequest(
    private val offset: Long,
    private val limit: Int,
) : Pageable {
    init {
        require(offset >= 0) { "offset must not be negative" }
        require(limit >= 1) { "limit must not be less than one" }
    }

    constructor(offset: Int, limit: Int) : this(offset.toLong(), limit)

    override fun getPageNumber(): Int = (offset / limit).toInt()

    override fun getPageSize(): Int = limit

    override fun getOffset(): Long = offset

    override fun getSort(): Sort = Sort.unsorted()

    override fun next(): Pageable = OffsetBasedPageRequest(offset + limit, limit)

    override fun previousOrFirst(): Pageable = if (hasPrevious()) OffsetBasedPageRequest(offset - limit, limit) else first()

    override fun first(): Pageable = OffsetBasedPageRequest(0L, limit)

    override fun withPage(pageNumber: Int): Pageable = OffsetBasedPageRequest(pageNumber.toLong() * limit, limit)

    override fun hasPrevious(): Boolean = offset >= limit
}
