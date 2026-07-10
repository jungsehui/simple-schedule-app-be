plugins {
    id("geekchat.kotlin-library")
}

dependencies {
    // Base JPA entities (jakarta.persistence) + ChatErrorMapping/HealthController/ErrorResponse (spring-web).
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}
