import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("geekchat.spring-boot-app")
}

// :ai manages the Spring AI starter versions via its own BOM, but io.spring.dependency-management
// versions do NOT propagate across the project(":ai") boundary — so the transitive spring-ai
// starters arrive here without a version. Import the same BOM so :app's runtime/test classpath can
// resolve them. (See ai/build.gradle.kts.)
dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:2.0.1")
    }
}

dependencies {
    // Aggregate all feature modules — their library jars are bundled as nested jars in the bootJar.
    implementation(project(":common"))
    implementation(project(":user"))
    implementation(project(":auth"))
    implementation(project(":room"))
    implementation(project(":chat"))
    implementation(project(":websocket"))
    implementation(project(":ai"))

    // Runtime stack for the running application (also reach app via transitive module deps).
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-hateoas") // Phase 7: HAL auto-config for EntityModel

    runtimeOnly("org.postgresql:postgresql")
}

// Local dev convenience: `./gradlew :app:bootRun` (and IDE bootRun) default to the
// self-contained `local` profile (MySQL on 3310 + dev secret) so it runs without remembering
// a profile arg. Production runs the bootJar with JWT_SECRET + profile set via env, not bootRun.
// Override locally with: ./gradlew :app:bootRun --args='--spring.profiles.active=dev'
tasks.named<BootRun>("bootRun") {
    args("--spring.profiles.active=local")
}
