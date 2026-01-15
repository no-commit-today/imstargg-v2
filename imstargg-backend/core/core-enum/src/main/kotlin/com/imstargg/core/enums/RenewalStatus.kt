package com.imstargg.core.enums

enum class RenewalStatus(
    override val code: String,
) : EnumCodeType {
    PENDING("pending"),
    EXECUTING("executing"),
    COMPLETED("completed"),
    ;

    companion object {
        init {
            EnumCodeType.validateNoDuplicateCodes(RenewalStatus::class.java)
        }
    }
}
