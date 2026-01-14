package com.imstargg.core.enums

interface EnumCodeType {
    val code: String
    val title: String

    companion object {
        inline fun <reified T> fromCode(code: String): T where T : Enum<T>, T : EnumCodeType =
            findByCode<T>(code)
                ?: throw IllegalArgumentException("No [${T::class.java.name}] with code: $code")

        inline fun <reified T> findByCode(code: String): T? where T : Enum<T>, T : EnumCodeType = enumValues<T>().find { it.code == code }
    }
}
