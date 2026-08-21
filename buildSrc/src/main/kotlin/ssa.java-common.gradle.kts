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

// ArchUnit 좌표는 여기 한 곳에서만 선언한다 (test + testFixtures가 같은 버전을 쓰도록)
val archUnitJUnit5 = "com.tngtech.archunit:archunit-junit5:1.3.0"

dependencies {
    "compileOnly"("org.projectlombok:lombok")
    "annotationProcessor"("org.projectlombok:lombok")
    "testCompileOnly"("org.projectlombok:lombok")
    "testAnnotationProcessor"("org.projectlombok:lombok")

    "testImplementation"("org.springframework.boot:spring-boot-starter-test")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")

    // 아키텍처 규칙 검증 (헥사고날 레이어·도메인 순수성 — docs/adr/0002)
    "testImplementation"(archUnitJUnit5)
}

// testFixtures를 쓰는 모듈(common)의 공용 테스트 헬퍼도 ArchUnit API를 참조한다
// (FreezeStoreIntegrity — freeze 스토어 고아 검사). 런타임은 소비 모듈의 test 클래스패스가 제공하므로
// compileOnly로 충분하고, 소비 모듈 클래스패스에 같은 좌표를 두 번 올리지 않는다.
plugins.withId("java-test-fixtures") {
    dependencies {
        "testFixturesCompileOnly"(archUnitJUnit5)
    }
}

// @Tag("slow")(성능/데드락 타이밍 테스트)는 기본 test/CI에서 제외. 실행: ./gradlew test -PincludeSlow
tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        if (!project.hasProperty("includeSlow")) {
            excludeTags("slow")
        }
    }
}
