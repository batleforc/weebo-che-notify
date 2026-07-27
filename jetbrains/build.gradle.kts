plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "fr.batleforc"
// Défaut dans gradle.properties, surchargeable par la CI : -PpluginVersion=x.y.z (tag)
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Compilé contre la version minimale supportée (2026.1) pour garantir la compat API
        // Distribution unifiée : IntelliJ IDEA Community (IC) n'est plus publiée depuis 2025.3
        // useInstaller = false : résolution via le repo Maven IntelliJ, l'API
        // installers ne référence pas encore les versions 2026 depuis ce réseau
        intellijIdea("2026.1.4") { useInstaller = false }
        // Le terminal est bundlé dans tous les IDE JetBrains : requis pour l'action « shell »
        bundledPlugin("org.jetbrains.plugins.terminal")
        bundledModule("intellij.terminal.frontend")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "261"
            untilBuild = provider { null }
        }
    }
}

kotlin {
    jvmToolchain(21)
}

tasks {
    processResources {
        // Embarque le CLI partagé bin/ide-notify (source unique à la racine du repo)
        from(rootDir.parentFile.resolve("bin")) {
            into("bin")
        }
    }
}
