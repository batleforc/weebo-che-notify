plugins {
    // Provisionne automatiquement le JDK 21 demandé par la toolchain si absent
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "weebo-bridge-notify"
