pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "geek-chat-server-v2"

// Composite build providing the geekchat.* convention plugins.
includeBuild("build-logic")

include(
    ":common",
    ":user",
    ":auth",
    ":room",
    ":chat",
    ":websocket",
    ":ai",
    ":app",
)
