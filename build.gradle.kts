plugins {
    kotlin("jvm") version "2.4.10" apply false
    id("org.jetbrains.dokka") version "2.2.0"
}

repositories {
    mavenCentral()
}

val appVersion = "0.0.1"
val appGroup = "dev.siluet"

dependencies {
    dokka(project(":core"))
}

dokka {
    moduleName.set("Siluet")

    dokkaPublications.html {
        outputDirectory.set(rootProject.layout.projectDirectory.dir("docs/api"))
    }
}

allprojects {
    group = appGroup
    version = appVersion
}

subprojects {
    val isDocumentable = path == ":core"
    if (isDocumentable) {
        apply(plugin = "org.jetbrains.dokka")

        extensions.configure<org.jetbrains.dokka.gradle.DokkaExtension> {
            dokkaSourceSets.configureEach {
                jdkVersion.set(21)

                documentedVisibilities.set(
                    setOf(
                        org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Public,
                        org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Protected
                    )
                )

                perPackageOption {
                    matchingRegex.set(".*\\.internal.*")
                    suppress.set(true)
                }
            }
        }
    }
}

tasks.register("test") {
    group = "verification"
    description = "Runs tests for core and all plugin subprojects."

    val targetProjects = subprojects.filter { subproject ->
        subproject.path == ":core" || subproject.path.startsWith(":plugins:")
    }

    dependsOn(targetProjects.map { it.tasks.named("test") })
}