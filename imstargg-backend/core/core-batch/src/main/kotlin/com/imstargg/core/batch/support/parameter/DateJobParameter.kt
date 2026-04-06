package com.imstargg.core.batch.support.parameter

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.time.LocalDate

open class DateJobParameter {
    private val log = LoggerFactory.getLogger(javaClass)

    @set:Value("#{jobParameters['$KEY']}")
    open var date: LocalDate? = null

    @PostConstruct
    open fun init() {
        log.debug("JobParameter[$KEY]={}", date)
    }

    companion object {
        const val KEY = "date"
    }
}
