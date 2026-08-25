import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Platforms
    alias(kt.plugins.multiplatform)
    alias(kt.plugins.multiplatform.android)

    // Publishing
    alias(libutils.plugins.mavenPublish)
}

group = "io.github.briangits"
version = "0.0.2"

kotlin {
    jvm()

    android {
        namespace = "$group.events"
        compileSdk = 37
        minSdk = 21

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    js {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    macosArm64()

    linuxX64()
    linuxArm64()

    mingwX64()

    sourceSets {
        commonMain.dependencies {
            implementation(kotlinx.coroutines)
        }

        commonTest.dependencies {
            implementation(kt.test)
            implementation(kotlinx.coroutines.test)
        }
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(group.toString(), "events", version.toString())

    pom {
        name = "events"
        description = "An in-process event bus"
        inceptionYear = "2026"

        url = "https://github.com/briangits/events"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "http://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }

        developers {
            developer {
                id = "briangits"
                name = "Gits"
                url = "https://github.com/briangits"
            }
        }

        scm {
            url = "https://github.com/briangits/events"
            connection = "scm:git:git://github.com/briangits/events.git"
            developerConnection = "scm:git:git://github.com/briangits/events.git"
        }
    }
}
