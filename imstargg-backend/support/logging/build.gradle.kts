dependencies {
    // 로깅 파사드 — 로그를 남기는 모듈은 이 모듈에 의존해 KotlinLogging 을 쓴다(버전은 gradle.properties)
    api("io.github.oshai:kotlin-logging-jvm:${property("kotlinLoggingVersion")}")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
//    implementation("io.sentry:sentry-spring-boot-4-starter")
}
