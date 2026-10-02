// Convention plugin for GeekChat library modules (feature modules; NOT bootable).
// Produces a plain jar. The :app module bundles these as nested jars in its bootJar.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.kotlin.plugin.jpa")
    id("io.spring.dependency-management")
}

group = "com.geekchat"
version = "0.0.1-SNAPSHOT"

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

kotlin {
    compilerOptions { freeCompilerArgs.add("-Xjsr305=strict") }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.1")
        mavenBom("org.springframework.modulith:spring-modulith-bom:2.1.1")
    }
}

dependencies {
    "implementation"("tools.jackson.module:jackson-module-kotlin")
    "implementation"("org.jetbrains.kotlin:kotlin-reflect")
    // Each module declares @ApplicationModule in package-info.java → needs Modulith core to compile.
    "implementation"("org.springframework.modulith:spring-modulith-starter-core")

    "testImplementation"("org.springframework.boot:spring-boot-starter-test")
    "testImplementation"("io.mockk:mockk:1.13.12")
    "testRuntimeOnly"("com.h2database:h2")
    // Library modules run JUnit Platform without the Spring Boot plugin. Supply a launcher aligned
    // with the boot-managed junit-platform version (via junit-bom) so it matches the engine on the
    // test classpath; otherwise Gradle's older bundled launcher is used and mismatches the engine
    // ("OutputDirectoryCreator not available; unaligned junit-platform-engine/launcher").
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> { useJUnitPlatform() }
