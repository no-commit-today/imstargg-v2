package com.imstargg.core.config

import com.imstargg.core.enums.Country
import com.imstargg.core.enums.EnumCodeType
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

/**
 * `@RequestParam country: Country` 바인딩 시 `code`(예: "kr") 또는 enum 이름(예: "KOREA") 모두 허용.
 * 매칭 실패하면 IllegalArgumentException 을 던져 Spring 이 400 으로 변환하도록 한다.
 */
@Component
class StringToCountryConverter : Converter<String, Country> {
    override fun convert(source: String): Country {
        val trimmed = source.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Country code must not be blank")
        }
        EnumCodeType.findByCode<Country>(trimmed.lowercase())?.let { return it }
        return runCatching { Country.valueOf(trimmed.uppercase()) }
            .getOrElse { throw IllegalArgumentException("Unknown Country: '$source'") }
    }
}
