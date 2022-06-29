@file:Suppress("PropertyName")

plugins {
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(projects.baseFetcher)
                api(Deps.htmlUnit) {
                    exclude("org.eclipse.jetty.websocket") // avoid android crash; see #93
                }
                val log4jOverSlf4jVersion = "1.7.30"
                val logbackVersion = "1.2.3"
                api("ch.qos.logback:logback-classic:$logbackVersion")
                api("org.slf4j:log4j-over-slf4j:$log4jOverSlf4jVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(Deps.wireMock)
                implementation(projects.testUtils)
            }
        }
    }
}

