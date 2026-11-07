package com.imstargg.core.batch.support.parameter

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import java.time.YearMonth

private val log = KotlinLogging.logger {}

open class YearMonthJobParameter {
    @set:Value("#{jobParameters['$KEY']}")
    open var yearMonth: YearMonth? = null

    @PostConstruct
    open fun init() {
        log.debug { "JobParameter[$KEY]=$yearMonth" }
    }

    companion object {
        const val KEY = "yearMonth"
    }
}
