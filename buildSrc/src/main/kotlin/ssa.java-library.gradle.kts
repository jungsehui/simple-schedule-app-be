// 라이브러리 모듈 컨벤션 (ADR-0003 Stage 1) — 실행 파일이 아니라 다른 모듈에 링크되는 jar를 생성.
// bootJar를 끄고 일반 jar를 켠다. (예: common)
plugins {
    id("ssa.java-common")
}

tasks.named("bootJar") { enabled = false }
tasks.named("jar") { enabled = true }
