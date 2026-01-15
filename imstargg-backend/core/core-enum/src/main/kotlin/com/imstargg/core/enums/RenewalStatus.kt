package com.imstargg.core.enums

enum class RenewalStatus(
    override val code: String,
) : EnumCodeType {
    PENDING("pending"),
    EXECUTING("executing"),
    COMPLETED("completed"),
    ;

    override val title: String
        get() = code
}
