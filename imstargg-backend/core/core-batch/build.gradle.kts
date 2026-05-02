version = "${property("coreBatchVersion")}"

tasks.getByName("bootJar") {
    enabled = true
}

dependencies {
    implementation(project(":core:core-enum"))
    implementation(project(":support:logging"))
    implementation(project(":infrastructure:db-core"))
    implementation(project(":client:brawlstars-client"))

    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework:spring-web")

    // Test
    testImplementation("io.github.openfeign:feign-core")
    testImplementation("org.springframework.batch:spring-batch-test")
    testImplementation(testFixtures(project(":infrastructure:db-core")))
}
