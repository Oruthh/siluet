plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.4.10"
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

dokka {
    dokkaSourceSets.configureEach {
        includes.from("README.md")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.2")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(project.group.toString(), "siluet-core", project.version.toString())

    pom {
        name.set("Siluet Core")
        description.set("Core module of Siluet - Templating Engine for Kotlin")
        inceptionYear.set("2026")
        url.set("https://siluet.dev/")

        licenses {
            license {
                name.set("Mozilla Public License 2.0")
                url.set("https://www.mozilla.org/en-US/MPL/2.0/")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("Oruthh")
                name.set("Oruthh")
                url.set("https://github.com/Oruthh")
            }
        }

        scm {
            url.set("https://github.com/Oruthh/siluet")
            connection.set("scm:git:git://github.com/Oruthh/siluet.git")
            developerConnection.set("scm:git:ssh://git@github.com/Oruthh/siluet.git")
        }
    }
}