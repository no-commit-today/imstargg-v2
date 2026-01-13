allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    implementation(project(":core:core-enum"))

    // Retry
    implementation("org.springframework.retry:spring-retry")

    // JPA
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("io.github.openfeign.querydsl:querydsl-jpa:${property("querydslVersion")}")
    api("io.github.openfeign.querydsl:querydsl-sql:${property("querydslVersion")}")
    api("io.github.openfeign.querydsl:querydsl-sql-json:${property("querydslVersion")}")
    ksp("io.github.openfeign.querydsl:querydsl-ksp-codegen:${property("querydslVersion")}")
    annotationProcessor("io.github.openfeign.querydsl:querydsl-apt:${property("querydslVersion")}:jakarta")

    // DB
    runtimeOnly("com.mysql:mysql-connector-j")

    // Test Fixtures
    testFixturesRuntimeOnly("com.h2database:h2")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
}
