pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    plugins {
        id("com.android.library") version "8.5.2"
        id("org.jetbrains.kotlin.multiplatform") version "2.4.10"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10"
        id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
        id("org.jetbrains.compose") version "1.7.3"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "aidos-tcr"
