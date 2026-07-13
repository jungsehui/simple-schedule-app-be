plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

// 컨벤션 플러그인(ssa.*)이 버전 없이 org.springframework.boot / io.spring.dependency-management 를
// 적용할 수 있도록 해당 Gradle 플러그인을 빌드 클래스패스에 제공한다. (SSA는 Boot 3.4 라인)
dependencies {
    implementation("org.springframework.boot:spring-boot-gradle-plugin:3.4.3")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
}
