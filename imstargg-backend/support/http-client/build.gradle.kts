dependencies {
    // 호출 로깅 인터셉터(ApiLoggingInterceptor) — ClientHttpRequestInterceptor·HttpRequestValues 는 spring-web, 로그는 kotlin-logging
    implementation("org.springframework:spring-web")
    implementation(project(":support:logging"))
    // HttpServiceClientAutoConfiguration 이 spring.http.clients.* 를 @ImportHttpServices 그룹의 requestFactory 에 적용한다 — 없으면 조용히 무시된다
    implementation("org.springframework.boot:spring-boot-restclient")
    // ClientHttpRequestFactoryBuilderCustomizer — HttpClientConfig 가 HC5 자체 재시도를 끄고 커넥션 풀을 넓힌다
    implementation("org.springframework.boot:spring-boot-http-client")
    // Apache HttpClient 5 — read-timeout 이 전체 교환 데드라인(JDK)이 아니라 소켓 무활동 시간이 된다
    implementation("org.apache.httpcomponents.client5:httpclient5")
}
