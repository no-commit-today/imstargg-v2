version = "${property("coreAdminVersion")}"

tasks.getByName("bootJar") { enabled = true }

dependencies {
    implementation(project(":core:core-enum"))
    implementation(project(":infrastructure:db-core"))

    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation(project(":infrastructure:aws-s3"))

    testImplementation(testFixtures(project(":infrastructure:db-core")))
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
}
