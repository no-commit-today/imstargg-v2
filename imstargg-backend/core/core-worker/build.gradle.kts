tasks.getByName("bootJar") {
    enabled = true
}

tasks.getByName("jar") {
    enabled = false
}

dependencies {
    implementation(project(":core:core-enum"))
    implementation(project(":client:brawlstars-client"))
    implementation(project(":infrastructure:db-core"))
    implementation(project(":infrastructure:gcp-pubsub"))
    implementation(project(":support:logging"))
    implementation(project(":support:monitoring"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Test
    testImplementation(testFixtures(project(":infrastructure:db-core")))
}
