plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

configurations.all {
    resolutionStrategy {
        cacheChangingModulesFor(0, "seconds")
    }
}

kotlin {
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation("it.skrape:skrapeit:0-SNAPSHOT") {
                    isChanging = true
                }
            }
        }
    }
}
