import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.tasks.testing.Test

plugins {
    base
    alias(libs.plugins.cyclonedx)
    alias(libs.plugins.gradle.versions)
    alias(libs.plugins.spotless)
}

group = "com.newlanderp"
version = "0.0.0-SNAPSHOT"

val javaLanguageVersion =
    providers.gradleProperty("javaVersion").map(String::toInt).map(JavaLanguageVersion::of)

subprojects {
    group = rootProject.group
    version = rootProject.version

    dependencyLocking {
        lockAllConfigurations()
        lockMode.set(org.gradle.api.artifacts.dsl.LockMode.STRICT)
    }

    pluginManager.withPlugin("java") {
        pluginManager.apply("checkstyle")

        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(javaLanguageVersion)
            withSourcesJar()
        }

        extensions.configure<CheckstyleExtension> {
            toolVersion = libs.versions.checkstyle.get()
            configFile = rootProject.file("config/checkstyle/checkstyle.xml")
            configProperties =
                mapOf(
                    "org.checkstyle.google.suppressionfilter.config" to
                        rootProject.file("config/checkstyle/suppressions.xml").absolutePath,
                )
            isShowViolations = true
            maxWarnings = 0
        }

        dependencies {
            add("testImplementation", platform(libs.junit.bom))
            add("testImplementation", libs.junit.jupiter)
            add("testRuntimeOnly", libs.junit.platform.launcher)
            add("testImplementation", libs.archunit.junit5)
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            testLogging {
                events("failed", "skipped")
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }

        tasks.withType<Checkstyle>().configureEach {
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }
    }
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "gradle/**/*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }

    format("repositoryText") {
        target(
            "*.md",
            "*.yml",
            "*.yaml",
            "*.json",
            "*.jsonc",
            "*.toml",
            ".editorconfig",
            ".gitattributes",
            ".gitignore",
            ".dockerignore",
            ".npmrc",
            ".nvmrc",
            ".node-version",
            ".java-version",
            "config/**/*.xml",
        )
        targetExclude(
            ".github/ISSUE_TEMPLATE/*.yml",
            "gradle/verification-metadata.xml",
            "node_modules/**",
            "pnpm-lock.yaml",
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.matching { it.name == "cyclonedxBom" }.configureEach {
    notCompatibleWithConfigurationCache("CycloneDX resolves Maven POM metadata during SBOM generation.")
}

tasks.matching { it.name == "dependencyUpdates" }.configureEach {
    notCompatibleWithConfigurationCache("Dependency update reports inspect resolved configurations at execution time.")
}

val architectureCheck by tasks.registering(Exec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Verifies required foundation files and approved repository boundaries."
    commandLine("node", "tools/architecture/verify.mjs")
}

val test by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs tests in all approved JVM subprojects."
    dependsOn(subprojects.map { project -> project.tasks.matching { it.name == "test" } })
}

tasks.named("check") {
    dependsOn(architectureCheck, test)
}

tasks.named("build") {
    dependsOn("check")
}
