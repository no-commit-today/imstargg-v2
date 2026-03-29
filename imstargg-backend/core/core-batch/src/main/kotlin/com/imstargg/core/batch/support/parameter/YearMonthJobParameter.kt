package com.imstargg.core.batch.support.parameter

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.time.YearMonth

open class YearMonthJobParameter {
    private val log = LoggerFactory.getLogger(javaClass)

    var yearMonth: YearMonth? = null
        private set

    @Value("#{jobParameters['$KEY']}")
    fun setYearMonth(yearMonth: YearMonth?) {
        this.yearMonth = yearMonth
    }

    @PostConstruct
    fun init() {
        log.debug("JobParameter[$KEY]={}", yearMonth)
    }

    companion object {
        const val KEY = "yearMonth"
    }
}
