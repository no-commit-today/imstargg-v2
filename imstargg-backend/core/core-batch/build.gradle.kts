tasks.getByName("bootJar") {
    enabled = true
}

dependencies {
    implementation(project(":core:core-enum"))
    implementation(project(":support:logging"))
    implementation(project(":infrastructure:db-core"))

    implementation("org.springframework.boot:spring-boot-starter-batch")

    // Test
    testImplementation("org.springframework.batch:spring-batch-test")
    testImplementation(testFixtures(project(":infrastructure:db-core")))
}
