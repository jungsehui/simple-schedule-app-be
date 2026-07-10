plugins {
    id("geekchat.kotlin-library")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":auth"))
    implementation(project(":room"))
    implementation(project(":chat"))

    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-tx") // @TransactionalEventListener / TransactionPhase
}
