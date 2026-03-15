plugins {
    id("com.vaadin")
}

tasks.getByName("bootJar") { enabled = true }

vaadin {
    productionMode = false
}

dependencies {
    implementation(project(":core:core-enum"))
    implementation(project(":infrastructure:db-core"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.vaadin:vaadin-spring-boot-starter")
    developmentOnly("com.vaadin:vaadin-dev")

    implementation(platform("software.amazon.awssdk:bom:${property("awsVersion")}"))
    implementation("software.amazon.awssdk:s3")

    testImplementation(testFixtures(project(":infrastructure:db-core")))
}
