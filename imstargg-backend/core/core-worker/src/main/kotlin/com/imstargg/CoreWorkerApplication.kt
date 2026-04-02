package com.imstargg

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class CoreWorkerApplication

fun main(args: Array<String>) {
    runApplication<CoreWorkerApplication>(*args)
}
