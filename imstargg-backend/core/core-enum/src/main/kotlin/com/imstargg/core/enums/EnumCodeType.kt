package com.imstargg.core.enums

interface EnumCodeType {
    val code: String

    companion object {
        inline fun <reified T> fromCode(code: String): T where T : Enum<T>, T : EnumCodeType =
            findByCode<T>(code)
                ?: throw IllegalArgumentException("No [${T::class.java.name}] with code: $code")

        inline fun <reified T> findByCode(code: String): T? where T : Enum<T>, T : EnumCodeType = enumValues<T>().find { it.code == code }

        /**
         * 주어진 enum 클래스 내에서 코드 중복을 검증합니다.
         *
         * @param enumClass 검증할 enum 클래스
         * @throws IllegalStateException 중복된 코드가 발견된 경우
         */
        fun <T> validateNoDuplicateCodes(enumClass: Class<T>) where T : Enum<T>, T : EnumCodeType {
            val constants = enumClass.enumConstants

            val codeToEnums = constants.groupBy { it.code }

            val duplicates =
                codeToEnums.entries
                    .filter { it.value.size > 1 }
                    .map { entry ->
                        "code '${entry.key}' is used by: ${entry.value.joinToString(", ") { it.name }}"
                    }

            check(duplicates.isEmpty()) { "[${enumClass.simpleName}] has duplicate codes: ${duplicates.joinToString("; ")}" }
        }
    }
}
