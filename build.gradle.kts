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
    description = "SSA(common/course/notification) + GeekChat(:app) 통합 빌드 — CI 단일 파이프라인 엔트리포인트"
    dependsOn(":common:build", ":course:build", ":notification:build")
    dependsOn(gradle.includedBuild("geekchat").task(":app:build"))
}
