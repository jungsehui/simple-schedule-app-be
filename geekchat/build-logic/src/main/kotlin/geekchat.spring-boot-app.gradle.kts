// Convention plugin for the bootable :app module — produces the bootJar that bundles
// the library module jars as nested jars. Only this module applies the Spring Boot plugin.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.kotlin.plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "com.geekchat"
version = "0.0.1-SNAPSHOT"

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

kotlin {
    compilerOptions { freeCompilerArgs.add("-Xjsr305=strict") }
}

// The Spring Boot plugin auto-imports the spring-boot-dependencies BOM via dependency-management.
dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:2.1.0")
    }
}

dependencies {
    "implementation"("tools.jackson.module:jackson-module-kotlin")
    "implementation"("org.jetbrains.kotlin:kotlin-reflect")
    "implementation"("org.springframework.modulith:spring-modulith-starter-core")

    "testImplementation"("org.springframework.boot:spring-boot-starter-test")
    "testImplementation"("org.springframework.boot:spring-boot-starter-webmvc-test")
    "testImplementation"("org.springframework.security:spring-security-test")
    "testImplementation"("org.springframework.modulith:spring-modulith-starter-test")
    "testImplementation"("io.mockk:mockk:1.13.12")
    "testRuntimeOnly"("com.h2database:h2")
}

tasks.withType<Test> { useJUnitPlatform() }
