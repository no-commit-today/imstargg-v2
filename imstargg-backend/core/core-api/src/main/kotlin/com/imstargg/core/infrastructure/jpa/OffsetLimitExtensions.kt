package com.imstargg.core.infrastructure.jpa

import com.imstargg.core.domain.OffsetLimit
import org.springframework.data.domain.Pageable

fun OffsetLimit.toPageable(): Pageable = OffsetBasedPageRequest(offset = offset, limit = limit)
