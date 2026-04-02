package com.imstargg

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import kotlin.system.exitProcess

@SpringBootApplication
@ConfigurationPropertiesScan
class CoreBatchApplication

fun main(args: Array<String>) {
    val context = runApplication<CoreBatchApplication>(*args)
    val exitCode = SpringApplication.exit(context)
    exitProcess(exitCode)
}
