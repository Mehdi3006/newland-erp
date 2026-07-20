import org.gradle.api.initialization.resolve.RepositoriesMode

if (gradle.startParameter.taskNames.contains("dependencyUpdates")) {
    gradle.startParameter.isParallelProjectExecutionEnabled = false
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "newland-erp"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

include("apps:backend")
