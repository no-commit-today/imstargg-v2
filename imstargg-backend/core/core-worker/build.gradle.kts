tasks.getByName("bootJar") {
    enabled = true
}

dependencies {
    implementation(project(":core:core-enum"))
    implementation(project(":client:brawlstars-client"))
    implementation(project(":infrastructure:db-core"))
    implementation(project(":infrastructure:redis-core"))
    implementation(project(":support:logging"))
    implementation(project(":support:monitoring"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.retry:spring-retry")

    // Test
    testImplementation(testFixtures(project(":infrastructure:db-core")))
}
