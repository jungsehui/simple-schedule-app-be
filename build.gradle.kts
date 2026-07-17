// 루트는 조립만 담당 — 공통 빌드 설정은 buildSrc의 ssa.* 컨벤션 플러그인으로 이동했다. (ADR-0003 Stage 1)
// GeekChat(Boot 4.1)은 settings.gradle.kts의 includeBuild("geekchat")로 격리·통합된다.

tasks.register<Exec>("installGitHooks") {
    description = "git core.hooksPath를 .githooks로 설정해 pre-push CI 패리티 게이트를 활성화"
    commandLine("git", "config", "core.hooksPath", ".githooks")
}

// 단일 명령 통합 빌드: SSA 배포 모듈(common/course/notification) + GeekChat(:app)을 한 번에.
// CI가 이 태스크 하나로 전 스택을 빌드·테스트한다. (playground/ngrinder는 부하 테스트 전용이라 제외)
tasks.register("buildAll") {
    group = "build"
    description = "SSA(:app = course+notification 단일 JVM) + GeekChat(:app) 통합 빌드 — CI 단일 파이프라인 엔트리포인트"
    // :app:build가 course/notification/common을 컴파일하지만 그들의 test는 안 돌리므로 명시적으로 포함.
    // common은 testFixtures만 소비되어 :common:test가 빠져 있었다 — ArchUnit 헥사고날 래칫이
    // common에 있으므로, 빠지면 도메인 순수성 위반이 CI를 초록으로 통과한다 (ADR-0004).
    dependsOn(":common:build", ":course:build", ":notification:build", ":app:build")
    dependsOn(gradle.includedBuild("geekchat").task(":app:build"))
}
