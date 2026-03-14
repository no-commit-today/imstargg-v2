package com.imstargg.core.batch.support.parameter

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.time.LocalDate

class DateJobParameter {
    private val log = LoggerFactory.getLogger(javaClass)

    var date: LocalDate? = null
        private set

    @Value("#{jobParameters['$KEY']}")
    fun setDate(date: LocalDate?) {
        this.date = date
    }

    @PostConstruct
    fun init() {
        log.debug("JobParameter[$KEY]={}", date)
    }

    companion object {
        const val KEY = "date"
    }
}
