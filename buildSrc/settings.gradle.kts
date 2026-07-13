// SSA 컨벤션 플러그인 빌드 (ADR-0003 Stage 1). buildSrc는 루트 빌드에만 자동 적용되며
// includeBuild("geekchat")로 격리된 GeekChat 빌드에는 영향을 주지 않는다.
dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "buildSrc"
