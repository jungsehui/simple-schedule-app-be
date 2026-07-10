// Root project: no sources. Module configuration lives in build-logic convention plugins
// (geekchat.kotlin-library, geekchat.spring-boot-app) applied by each subproject.

// One-time per clone: point git at the version-controlled .githooks dir so the pre-push gate
// (./gradlew build, mirroring CI) runs locally and blocks pushes of broken code.
//   ./gradlew installGitHooks
tasks.register<Exec>("installGitHooks") {
    group = "git hooks"
    description = "Set git core.hooksPath to .githooks (enables the pre-push CI mirror gate)."
    commandLine("git", "config", "core.hooksPath", ".githooks")
}
