plugins {
    kotlin("jvm") version "2.4.10" apply false
}

val appVersion = "1.0.0"
val appGroup = "dev.siluet"

allprojects {
    group = appGroup
    version = appVersion
}

tasks.register("test") {
    group = "verification"
    description = "Runs tests for core and all plugin subprojects."

    val targetProjects = subprojects.filter { subproject ->
        subproject.path == ":core" || subproject.path.startsWith(":plugins:")
    }

    dependsOn(targetProjects.map { it.tasks.named("test") })
}