rootProject.name = "imstargg-backend"

include(
    "core:core-enum",
    "core:core-api",
    "core:core-admin",
    "core:core-batch",
    "core:core-worker",
    "infrastructure:db-core",
    "infrastructure:redis-core",
    "infrastructure:aws-sqs",
    "infrastructure:aws-s3",
    "client:brawlstars-client",
    "support:http-client",
    "support:logging",
    "support:monitoring"
)

pluginManagement {
    val kotlinVersion = providers.gradleProperty("kotlinVersion").get()
    val springBootVersion = providers.gradleProperty("springBootVersion").get()
    val springDependencyManagementVersion = providers.gradleProperty("springDependencyManagementVersion").get()
    val ktlintVersion = providers.gradleProperty("ktlintVersion").get()
    val kspVersion = providers.gradleProperty("kspVersion").get()

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.spring") version kotlinVersion
        kotlin("plugin.jpa") version kotlinVersion
        id("org.springframework.boot") version springBootVersion
        id("io.spring.dependency-management") version springDependencyManagementVersion
        id("org.jlleitschuh.gradle.ktlint") version ktlintVersion
        id("com.google.devtools.ksp") version kspVersion
    }
}