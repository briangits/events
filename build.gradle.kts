import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Platforms
    alias(kt.plugins.multiplatform)
    alias(kt.plugins.multiplatform.android)
}

group = "io.github.briangits"
version = "0.0.1"

kotlin {
    jvm()

    js {
        browser()
        nodejs()
    }

    android {
        namespace = "$group"
        compileSdk = 37
        minSdk = 2

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

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
