package com.imstargg.client.brawlstars

import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.TestPropertySource

@Tag("develop")
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestPropertySource(
    properties = [
        "app.client.brawlstars.url=https://api.brawlstars.com",
    ],
)
abstract class DevelopTest
