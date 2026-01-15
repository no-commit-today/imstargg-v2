rootProject.name = "imstargg-backend"

include(
    "core:core-enum",
    "core:core-api",
    "core:core-batch",
    "core:core-worker",
    "infrastructure:db-core",
    "infrastructure:gcp-pubsub",
    "client:brawlstars-client",
    "support:logging",
    "support:monitoring"
)

pluginManagement {
    val kotlinVersion: String by settings
    val springBootVersion: String by settings
    val springDependencyManagementVersion: String by settings
    val ktlintVersion: String by settings
    val kspVersion: String by settings

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