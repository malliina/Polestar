import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
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
            exec {
                commandLine("git", "add", "version.code")
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

val autoVersion = "1.4.0"

dependencies {
    implementation("androidx.car.app:app:$autoVersion")
    implementation("androidx.car.app:app-automotive:$autoVersion")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.datastore:datastore-preferences:1.1.3")
    implementation("com.jakewharton.timber:timber:5.0.1")
    val playServicesVersion = "21.3.0"
    implementation("com.google.android.gms:play-services-location:$playServicesVersion")
    implementation("com.google.android.gms:play-services-auth:$playServicesVersion")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    val moshiVersion = "1.15.2"
    implementation("com.squareup.moshi:moshi:$moshiVersion")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.car.app:app-testing:$autoVersion")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("com.google.assistant.appactions:testing:1.0.0")
}
