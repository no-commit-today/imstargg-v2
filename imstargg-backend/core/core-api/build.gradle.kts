version = "${property("coreApiVersion")}"

springBoot {
    buildInfo()
}

tasks.getByName("bootJar") {
    enabled = true
}

dependencies {
    implementation(project(":core:core-enum"))
    implementation(project(":infrastructure:db-core"))
    implementation(project(":infrastructure:redis-core"))
    implementation(project(":infrastructure:aws-sqs"))
    implementation(project(":support:monitoring"))
    implementation(project(":support:logging"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    implementation("com.github.ben-manes.caffeine:caffeine")

    // Test
    testImplementation(testFixtures(project(":infrastructure:db-core")))
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("io.micrometer:micrometer-core")
}
