plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.skogberglabs.polestar"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.skogberglabs.polestar"
        minSdk = 29 // Android 10
        targetSdk = 34
        versionCode = 33
        versionName = "1.20.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "VersionCode", "\"${versionCode}\"")
        buildConfigField("String", "VersionName", "\"${versionName}\"")
    }

    signingConfigs {
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
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    useLibrary("android.car")
    kotlin {
        jvmToolchain(11)
    }
}

val autoVersion = "1.4.0-beta02"

dependencies {
    implementation("androidx.car.app:app:$autoVersion")
    implementation("androidx.car.app:app-automotive:$autoVersion")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    val moshiVersion = "1.15.0"
    implementation("com.squareup.moshi:moshi:$moshiVersion")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.car.app:app-testing:$autoVersion")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
