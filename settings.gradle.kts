pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "simple-schedule-app"

// GeekChat(Kotlin/Boot 4.1)은 Boot Gradle 플러그인 버전이 SSA(3.4)와 달라 하나의 빌드로 합칠 수 없다.
// 컴포지트(includeBuild)로 격리하여 단일 명령(./gradlew buildAll)에서 함께 빌드한다. (ADR-0003 Stage 1)
// Boot 버전 통일(Stage 3) 이후 진짜 단일 빌드/단일 bootJar(Stage 4)로 합류 예정.
includeBuild("geekchat")

// INFRA (공유 기술 커널)
include("common")

// BOUNDED CONTEXTS (라이브러리 — 자체 bootJar 없음, :app이 조합)
include("course")
include("notification")

// BOOT (유일한 실행 모듈 — 단일 JVM, ADR-0003 Stage 2)
include("app")

// TOOLS (부하/성능 테스트 전용 — CI·배포 대상 아님, 자체 build.gradle 유지)
include("playground")
include("ngrinder")
