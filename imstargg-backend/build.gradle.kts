plugins {
    kotlin("jvm")
    kotlin("plugin.spring") apply false
    kotlin("plugin.jpa") apply false
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
    id("org.jlleitschuh.gradle.ktlint") apply false
    id("com.google.devtools.ksp")
    id("com.vaadin") apply false
}

allprojects {
    group = "${property("projectGroup")}"
    version = "${property("applicationVersion")}"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "com.google.devtools.ksp")
    apply(plugin = "java-library")
    apply(plugin = "java-test-fixtures")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        filter {
            exclude { it.file.path.contains("/generated/") }
        }
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of("${property("javaVersion")}")
        }
    }


    configurations {
        compileOnly {
            extendsFrom(configurations.annotationProcessor.get())
        }
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
            mavenBom("io.sentry:sentry-bom:${property("sentryVersion")}")
            mavenBom("com.vaadin:vaadin-bom:${property("vaadinVersion")}")
            mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:${property("springCloudAwsVersion")}")
        }
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
        implementation("tools.jackson.module:jackson-module-kotlin")
        annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("com.ninja-squad:springmockk:${property("springMockkVersion")}")
        testImplementation("org.springframework.boot:spring-boot-testcontainers")
        testImplementation("org.testcontainers:testcontainers-junit-jupiter")
        testImplementation("org.testcontainers:testcontainers-postgresql")
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.getByName("bootJar") {
        enabled = false
    }

    tasks.getByName("jar") {
        enabled = true
    }

    kotlin {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform {
            excludeTags("develop")
        }
    }

    tasks.register<Test>("unitTest") {
        description = "Runs the unit tests."
        group = "verification"
        useJUnitPlatform {
            excludeTags("develop", "context")
        }
    }

    tasks.register<Test>("contextTest") {
        description = "Runs the context tests."
        group = "verification"
        useJUnitPlatform {
            includeTags("context")
        }
    }

    tasks.register<Test>("developTest") {
        description = "Runs the development tests."
        group = "verification"
        useJUnitPlatform {
            includeTags("develop")
        }
    }

}

