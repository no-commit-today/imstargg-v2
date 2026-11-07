package com.imstargg

import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.TestPropertySource

/**
 * 실제 키로 브롤스타즈 API 를 부르는 수동 테스트 베이스. 하위 클래스마다
 * `@EnabledIfEnvironmentVariable(named = "BRAWLSTARS_API_KEYS", matches = ".+")` 를 단다 — JUnit 조건 애노테이션은
 * `@Inherited` 가 아니라 여기 달면 하위 클래스에 적용되지 않는다. 키가 없을 때 컨텍스트 로드 실패 대신 건너뜀으로 끝난다.
 */
@Tag("develop")
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestPropertySource(
    properties = [
        "app.client.brawlstars.url=https://api.brawlstars.com",
    ],
)
abstract class BrawlStarsClientDevelopTest
