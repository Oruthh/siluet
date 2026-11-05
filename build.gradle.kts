import com.sun.net.httpserver.SimpleFileServer
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI

plugins {
    kotlin("jvm") version "2.4.10" apply false
    id("org.jetbrains.dokka") version "2.2.0"
    id("com.github.node-gradle.node") version "7.0.2"
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
        outputDirectory.set(rootProject.layout.projectDirectory.dir("build/docs/api"))
    }
    pluginsConfiguration.html {
        customAssets.from(file("docs/dokka/back-button.js"))
    }
}

node {
    version.set("20.12.0")
    download.set(true)
    nodeProjectDir.set(file("docs"))
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

val buildVitepress = tasks.register<com.github.gradle.node.npm.task.NpmTask>("buildVitepress") {
    dependsOn(tasks.named("npmInstall"))
    args.set(listOf("run", "docs:build"))
    inputs.dir("docs")
    outputs.dir(layout.buildDirectory.dir("docs/vitepress"))
}

val assembleSite = tasks.register<Sync>("assembleSite") {
    group = "documentation"
    description = "Assembles full documentation site with Dokka API references."

    dependsOn(tasks.named("dokkaGenerateHtml"), buildVitepress)

    into(layout.buildDirectory.dir("site"))

    from(layout.buildDirectory.dir("docs/vitepress"))

    from(layout.buildDirectory.dir("docs/api")) {
        into("api")
    }

    from(layout.projectDirectory.file("logo.svg")) {
        into("images")
    }
}

val previewSite = tasks.register("previewSite") {
    group = "documentation"
    description = "Builds and serves the documentation using JDK SimpleFileServer."
    dependsOn(assembleSite)

    doLast {
        val sitePath = layout.buildDirectory.dir("site").get().asFile.toPath()
        val port = 3080
        val address = InetSocketAddress(port)
        val server = SimpleFileServer.createFileServer(
            address,
            sitePath,
            SimpleFileServer.OutputLevel.INFO
        )
        server.start()

        // Shutdown hook as fallback if the JVM terminates abruptly
        val shutdownHook = Thread { server.stop(0) }
        Runtime.getRuntime().addShutdownHook(shutdownHook)

        try {
            val url = "http://localhost:$port"
            println("\nDocumentation server running at: $url")

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            }

            println("Press Enter in terminal or click Stop in IntelliJ to shut down...")

            // Wait loop responsive to both console input and IDE cancellation interrupts
            while (!Thread.currentThread().isInterrupted) {
                try {
                    if (System.`in`.available() > 0) {
                        System.`in`.read()
                        break
                    }
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        } finally {
            println("\nStopping documentation server...")
            server.stop(0)
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook)
            } catch (_: IllegalStateException) {
                // Ignore if JVM is already shutting down
            }
        }
    }
}