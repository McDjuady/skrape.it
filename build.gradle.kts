import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_SRC_DIR_KOTLIN
import kotlinx.kover.api.KoverTaskExtension

plugins {
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin")
    kotlin("multiplatform") apply false
    id("org.jetbrains.dokka") apply false
    id("org.jetbrains.kotlinx.kover")
    id("com.github.ben-manes.versions")
    id("se.patrikerdes.use-latest-versions")
    id("com.adarshr.test-logger")
    id("io.gitlab.arturbosch.detekt")
}

allprojects {
    val release_version: String by project
    version = release_version
    group = "it.skrape"

    repositories {
        mavenCentral()
    }

    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "se.patrikerdes.use-latest-versions")

    apply(plugin = "com.adarshr.test-logger")
    testlogger {
        setTheme("mocha-parallel")
        slowThreshold = 1000
        showStandardStreams = false
    }

    apply(plugin = "io.gitlab.arturbosch.detekt")
    detekt {
        toolVersion = "1.19.0"
        autoCorrect = true
        buildUponDefaultConfig = true
        source = files(DEFAULT_SRC_DIR_KOTLIN)
        config = files("$rootDir/detekt.yml")
    }
    val includeToPublishing = listOf(
        "assertions",
        "base-fetcher",
        "dsl",
        "http-fetcher",
        "async-fetcher",
        "browser-fetcher",
        "html-parser",
        "ktor-extension",
        "mock-mvc-extension",
        "skrapeit"
    )
    if (this.name in includeToPublishing) {
        apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "maven-publish")
        publishing {
            publications {
                create<MavenPublication>("mavenJava") {
                    artifactId = if (rootProject.name == project.name) rootProject.name else "${rootProject.name}-${project.name}"
                    //from(components["java"])
                    pom {
                        name.set("skrape{it}")
                        description.set("A Kotlin-based testing/scraping/parsing library providing the ability to analyze and extract data from HTML (server & client-side rendered). It places particular emphasis on ease of use and a high level of readability by providing an intuitive DSL. First and foremost it aims to be a testing lib, but it can also be used to scrape websites in a convenient fashion.")
                        url.set("https://docs.skrape.it")
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/licenses/MIT")
                            }
                        }
                        developers {
                            developer {
                                id.set("christian-draeger")
                                name.set("Christian Dräger")
                            }
                        }
                        scm {
                            connection.set("scm:git:git://github.com/skrapeit/skrape.it.git")
                            developerConnection.set("scm:git:ssh://github.com:skrapeit/skrape.it.git")
                            url.set("https://github.com/skrapeit/skrape.it/tree/master")
                        }
                    }
                }
            }
        }

        apply(plugin = "signing")
        signing {
            sign(publishing.publications["mavenJava"])

            val signingKeyId: String? by project
            val signingKey: String? by project
            val signingPassword: String? by project
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.multiplatform")

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
        jvm()

        sourceSets {
            val jvmTest by getting {
                dependencies {
                    implementation(Deps.jUnit)
                    implementation(Deps.strikt)
                    implementation(Deps.Mockk.mockk)
                    implementation(Deps.Mockk.dslJvm)
                }
            }
        }
    }
    tasks {
        withType<JavaCompile> {
            options.encoding = "UTF-8"
        }

        withType<Test> {
            shouldRunAfter(useLatestVersions)
            dependsOn(detekt)
            useJUnitPlatform()
            systemProperties = mapOf(
                "junit.jupiter.execution.parallel.enabled" to true,
                "junit.jupiter.execution.parallel.mode.default" to "concurrent",
                "junit.jupiter.execution.parallel.mode.classes.default" to "concurrent"
            )
        }

        withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {

            gradleReleaseChannel = "current"

            rejectVersionIf {
                val isFlaggedAsNonStable =
                    listOf("alpha", "beta", "RC", "rc", "dev", "M1", "M2", "M3").any { candidate.version.contains(it) }
                        .not()
                val isSemanticVersion = "^[0-9,.v-]+(-r)?$".toRegex().matches(candidate.version)
                (isFlaggedAsNonStable || isSemanticVersion).not()
            }
        }

        val updateDependencies by creating {
            dependsOn(useLatestVersions, named("allTests"))
        }
    }
}

tasks {
    withType<Test> {
        extensions.configure<KoverTaskExtension> {
            excludes = listOf("com.example.subpackage.*")
        }
        finalizedBy(koverReport, koverCollectReports)
    }
}

kover {
    coverageEngine.set(kotlinx.kover.api.CoverageEngine.INTELLIJ)
}

nexusPublishing {
    repositories {
        sonatype()
    }
}