package com.imstargg

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CoreWorkerApplication

fun main(args: Array<String>) {
    runApplication<CoreWorkerApplication>(*args)
}
