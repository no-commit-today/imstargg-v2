package com.imstargg.core.batch.support.parameter

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import java.time.LocalDate

private val log = KotlinLogging.logger {}

open class DateJobParameter {
    @set:Value("#{jobParameters['$KEY']}")
    open var date: LocalDate? = null

    @PostConstruct
    open fun init() {
        log.debug { "JobParameter[$KEY]=$date" }
    }

    companion object {
        const val KEY = "date"
    }
}
