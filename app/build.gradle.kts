plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-android")
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

// Top of build.gradle.kts
val major = 1
val minor = 11
val patch = 2
val build = 10

val type = 0 // 1=beta, 2=alpha else=production

val baseVersionName = "$major.$minor.$patch"

val versionCodeInt =
    (String.format("%02d", major) + String.format("%02d", minor) + String.format(
        "%02d",
        patch
    ) + String.format("%02d", build)).toInt()

val versionNameStr = when (type) {
    1 -> "$baseVersionName-beta build $build"
    2 -> "$baseVersionName-alpha build $build"
    else -> "$baseVersionName build $build"
}

val applicationName = when (type) {
    1 -> "app.wazabe.mlauncher.beta"
    2 -> "app.wazabe.mlauncher.alpha"
    else -> "app.wazabe.mlauncher"
}

android {
    namespace = "app.wazabe.mlauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = applicationName
        minSdk = 31
        targetSdk = 36
        versionCode = versionCodeInt
        versionName = versionNameStr
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            // applicationIdSuffix = ".dev"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_name", "Cascade Launcher Dev")
            resValue("string", "app_version", versionNameStr)
            resValue("string", "empty", "")
        }

        getByName("release") {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_name", "Cascade Launcher")
            resValue("string", "app_version", versionNameStr)
            resValue("string", "empty", "")
        }
    }

    applicationVariants.all {
        if (buildType.name == "release") {
            outputs.all {
                val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
                if (output?.outputFileName?.endsWith(".apk") == true) {
                    output.outputFileName =
                        "${defaultConfig.applicationId}_v${defaultConfig.versionName}-Signed.apk"
                }
            }
        }
        if (buildType.name == "debug") {
            outputs.all {
                val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
                if (output?.outputFileName?.endsWith(".apk") == true) {
                    output.outputFileName =
                        "${defaultConfig.applicationId}_v${defaultConfig.versionName}-Debug.apk"
                }
            }
        }
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
    }

    packaging {
        // Keep debug symbols for specific native libraries
        // found in /app/build/intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib
        jniLibs {
            keepDebugSymbols.add("libandroidx.graphics.path.so") // Ensure debug symbols are kept
        }
    }
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// KSP configuration
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    // Core libraries
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.recyclerview)
    implementation(libs.activity.ktx)
    implementation(libs.palette.ktx)
    implementation(libs.material)
    implementation(libs.viewpager2)
    implementation(libs.activity)
    implementation(libs.commons.text)

    // Android Lifecycle
    implementation(libs.lifecycle.extensions)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Navigation
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Work Manager
    implementation(libs.work.runtime.ktx)

    // UI Components
    implementation(libs.constraintlayout)
    implementation(libs.constraintlayout.compose)
    implementation(libs.activity.compose)

    // Jetpack Compose
    implementation(libs.compose.material) // Compose Material Design
    implementation(libs.compose.android) // Android
    implementation(libs.compose.animation) // Animations
    implementation(libs.compose.ui) // Core UI library
    implementation(libs.compose.foundation) // Foundation library
    implementation(libs.compose.ui.tooling) // UI tooling for previews

    // Biometric support
    implementation(libs.biometric.ktx)

    // Moshi
    implementation(libs.moshi)
    implementation(libs.moshi.ktx)
    implementation(libs.firebase.crashlytics)
    ksp(libs.moshi.codegen)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // AndroidX Test - Espresso
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.contrib)
    implementation(libs.espresso.idling.resource) // Idling resources for Espresso tests

    // Test rules and other testing dependencies
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    implementation(libs.test.core.ktx) // Test core utilities

    // Jetpack Compose Testing
    androidTestImplementation(libs.ui.test.junit4) // For createComposeRule
    debugImplementation(libs.ui.test.manifest) // Debug-only dependencies for Compose testing

    // Fragment testing
    debugImplementation(libs.fragment.testing)

    // Navigation testing
    androidTestImplementation(libs.navigation.testing)
}
