import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

val versionFilename = "version.code"

fun Project.execToString(spec: ExecSpec.() -> Unit): String =
    ByteArrayOutputStream().use { outputStream ->
        exec {
            spec()
            standardOutput = outputStream
        }
        outputStream.toString().trim()
    }

android {
    namespace = "com.skogberglabs.polestar"
    compileSdk = 35
    val code = file(versionFilename).readText().trim().toIntOrNull() ?: 1

    fun makeVersion(c: Int): String = "1.22.$c"

    tasks.register("release") {
        group = "build"
        description = "Releases a new version to Google Play Store for internal testing."
        notCompatibleWithConfigurationCache("Not supported.")
        var nextCode = 1
        doFirst {
            val porcelain =
                execToString {
                    commandLine("git", "status", "--porcelain")
                }
            if (porcelain.isNotBlank()) {
                throw Exception("Git status is not empty.")
            }
            logger.warn("Incrementing version...")
            val file = File("automotive/version.code")
            if (!file.exists()) {
                file.createNewFile()
                file.writeText("$nextCode")
                logger.warn("Created $file with initial version code of $nextCode.")
            } else {
                nextCode = file.readText().trim().toInt() + 1
                file.writeText("$nextCode")
                logger.warn("Updated version code to $nextCode.")
            }
        }
        doLast {
            val latestTag =
                execToString {
                    commandLine("git", "describe", "--abbrev=0", "--tags")
                }
            // Commit messages since the latest tag
            val changelog =
                execToString {
                    commandLine("git", "log", "--pretty=- %s", "$latestTag..")
                }
            val changelogPath = "fastlane/metadata/android/en-US/changelogs/$nextCode.txt"
            val changelogFile = File(changelogPath)
            changelogFile.writeText(changelog)
            logger.warn("Wrote $changelogFile.")
            exec {
                commandLine("git", "add", "version.code", "../$changelogPath")
            }
            exec {
                commandLine("git", "commit", "-m", "Incrementing version code to $nextCode.")
            }
            val ver = makeVersion(nextCode)
            val tag = "v$ver"
            exec {
                commandLine("git", "tag", tag)
            }
            exec {
                commandLine("git", "push", "origin", "master")
            }
            exec {
                commandLine("git", "push", "origin", "tag", tag)
            }
            logger.warn("Pushed $tag.")
        }
    }

    defaultConfig {
        applicationId = "com.skogberglabs.polestar"
        minSdk = 29 // Android 10
        targetSdk = 35
        versionCode = code
        versionName = makeVersion(code)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "VersionCode", "\"${versionCode}\"")
        buildConfigField("String", "VersionName", "\"${versionName}\"")
    }

    signingConfigs {
        getByName("debug") {
            if (System.getenv("CI") == "true") {
                storeFile = rootProject.file("keystore.jks")
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            } else {
                val RELEASE_STORE_FILE: String by project
                storeFile = file(RELEASE_STORE_FILE)
                val RELEASE_STORE_PASSWORD: String by project
                storePassword = RELEASE_STORE_PASSWORD
                val RELEASE_KEY_ALIAS: String by project
                keyAlias = RELEASE_KEY_ALIAS
                val RELEASE_KEY_PASSWORD: String by project
                keyPassword = RELEASE_KEY_PASSWORD
            }
        }
        create("release") {
            if (System.getenv("CI") == "true") {
                storeFile = rootProject.file("keystore.jks")
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            } else {
                val RELEASE_STORE_FILE: String by project
                storeFile = file(RELEASE_STORE_FILE)
                val RELEASE_STORE_PASSWORD: String by project
                storePassword = RELEASE_STORE_PASSWORD
                val RELEASE_KEY_ALIAS: String by project
                keyAlias = RELEASE_KEY_ALIAS
                val RELEASE_KEY_PASSWORD: String by project
                keyPassword = RELEASE_KEY_PASSWORD
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    useLibrary("android.car")
    buildFeatures {
        buildConfig = true
    }
    kotlin {
        jvmToolchain(11)
    }
}

dependencies {
    implementation(libs.androidx.app)
    implementation(libs.androidx.app.automotive)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.googleid)
    implementation(libs.play.services.location)
    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.timber)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.app.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.testing)
}
