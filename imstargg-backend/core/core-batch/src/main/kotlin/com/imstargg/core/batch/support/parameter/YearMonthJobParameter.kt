package com.imstargg.core.batch.support.parameter

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.time.YearMonth

open class YearMonthJobParameter {
    private val log = LoggerFactory.getLogger(javaClass)

    @set:Value("#{jobParameters['$KEY']}")
    open var yearMonth: YearMonth? = null

    @PostConstruct
    open fun init() {
        log.debug("JobParameter[$KEY]={}", yearMonth)
    }

    companion object {
        const val KEY = "yearMonth"
    }
}
