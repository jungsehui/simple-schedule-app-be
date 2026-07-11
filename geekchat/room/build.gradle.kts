plugins {
    id("geekchat.kotlin-library")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":user"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security") // @AuthenticationPrincipal
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-hateoas") // Phase 7: EntityModel + link assembler
}
