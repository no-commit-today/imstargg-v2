package com.imstargg.core.infrastructure.jpa

import com.imstargg.core.domain.Slice
import org.springframework.data.domain.Slice as SpringDataSlice

fun <T : Any> SpringDataSlice<T>.toSlice(): Slice<T> =
    Slice(
        content = content,
        hasNext = hasNext(),
    )
