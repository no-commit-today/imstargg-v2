package com.imstargg

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class CoreAdminApplication

fun main(args: Array<String>) {
    runApplication<CoreAdminApplication>(*args)
}
