import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("geekchat.spring-boot-app")
}

dependencies {
    // Aggregate all feature modules — their library jars are bundled as nested jars in the bootJar.
    implementation(project(":common"))
    implementation(project(":user"))
    implementation(project(":auth"))
    implementation(project(":room"))
    implementation(project(":chat"))
    implementation(project(":websocket"))

    // Runtime stack for the running application (also reach app via transitive module deps).
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    runtimeOnly("com.mysql:mysql-connector-j")
}

// Local dev convenience: `./gradlew :app:bootRun` (and IDE bootRun) default to the
// self-contained `local` profile (MySQL on 3310 + dev secret) so it runs without remembering
// a profile arg. Production runs the bootJar with JWT_SECRET + profile set via env, not bootRun.
// Override locally with: ./gradlew :app:bootRun --args='--spring.profiles.active=dev'
tasks.named<BootRun>("bootRun") {
    args("--spring.profiles.active=local")
}
