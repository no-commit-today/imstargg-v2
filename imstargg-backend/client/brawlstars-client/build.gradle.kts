dependencies {
    api("org.springframework:spring-web")
    implementation(project(":support:http-client"))
    implementation(project(":support:logging"))
    implementation("org.springframework.boot:spring-boot-restclient")
    implementation("io.github.resilience4j:resilience4j-spring-boot4:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:${property("resilience4jVersion")}")
}
