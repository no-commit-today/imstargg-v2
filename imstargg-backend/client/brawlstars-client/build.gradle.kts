dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    // openfeign 5.0.x 가 Boot http-converter 자동 구성 타입(ClientHttpMessageConvertersCustomizer)을 참조하는데
    // Boot 4.1 에서 이 모듈이 전이 의존으로 들어오지 않아 feignHttpMessageConverters 빈 생성이 실패한다.
    implementation("org.springframework.boot:spring-boot-http-converter")
    implementation("io.github.openfeign:feign-hc5")
    implementation("io.github.openfeign:feign-micrometer")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
