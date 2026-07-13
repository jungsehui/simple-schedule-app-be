// SSA 공통 컨벤션 (ADR-0003 Stage 1) — 구 루트 build.gradle의 subprojects {} 블록을 대체.
// Java 21 · Boot 3.4 · dependency-management · Lombok · 공통 테스트(starter-test/ArchUnit) ·
// @Tag("slow") 기본 제외. 라이브러리/앱 컨벤션(ssa.java-library / ssa.spring-boot-app)의 베이스.
import org.gradle.api.tasks.testing.Test

plugins {
    java
    `java-library`
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

// Lombok을 annotationProcessor로 쓰되 compileOnly로도 노출 (기존 관례 유지)
configurations.getByName("compileOnly").extendsFrom(configurations.getByName("annotationProcessor"))

dependencies {
    "compileOnly"("org.projectlombok:lombok")
    "annotationProcessor"("org.projectlombok:lombok")
    "testCompileOnly"("org.projectlombok:lombok")
    "testAnnotationProcessor"("org.projectlombok:lombok")

    "testImplementation"("org.springframework.boot:spring-boot-starter-test")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")

    // 아키텍처 규칙 검증 (헥사고날 레이어·도메인 순수성 — docs/adr/0002)
    "testImplementation"("com.tngtech.archunit:archunit-junit5:1.3.0")
}

// @Tag("slow")(성능/데드락 타이밍 테스트)는 기본 test/CI에서 제외. 실행: ./gradlew test -PincludeSlow
tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        if (!project.hasProperty("includeSlow")) {
            excludeTags("slow")
        }
    }
}
