plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

// Make the Kotlin / Spring Boot / dependency-management Gradle plugins available to the
// precompiled convention plugins under src/main/kotlin (geekchat.*.gradle.kts).
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    implementation("org.jetbrains.kotlin:kotlin-allopen:2.3.21")
    implementation("org.jetbrains.kotlin:kotlin-noarg:2.3.21")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:4.1.1")
}
