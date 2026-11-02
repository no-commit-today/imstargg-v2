dependencies {
    api("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.apache.commons:commons-pool2")
    implementation(project(":core:core-enum"))
    implementation(project(":support:logging"))

    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
}
