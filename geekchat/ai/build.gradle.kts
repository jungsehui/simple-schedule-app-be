plugins {
    id("geekchat.kotlin-library")
}

dependencyManagement {
    imports {
        // Spring AI 2.0.0 targets Spring Boot 4.1.
        mavenBom("org.springframework.ai:spring-ai-bom:2.0.0")
    }
}

dependencies {
    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security") // @PreAuthorize admin gate

    // Provider-agnostic: code uses Spring AI ChatClient/ChatModel. All providers wired;
    // select the active one via `spring.ai.model.chat` (anthropic | openai | ollama).
    implementation("org.springframework.ai:spring-ai-starter-model-anthropic")
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("org.springframework.ai:spring-ai-starter-model-ollama")
}
