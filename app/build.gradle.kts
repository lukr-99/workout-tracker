import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.isFile) {
    keystorePropertiesFile.inputStream().use(keystoreProperties::load)
}
fun signingValue(property: String, environment: String): String? =
    keystoreProperties.getProperty(property)?.takeIf(String::isNotBlank)
        ?: System.getenv(environment)?.takeIf(String::isNotBlank)

val releaseStoreFile = signingValue("storeFile", "KEYSTORE_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "KEYSTORE_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "KEYSTORE_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "KEYSTORE_KEY_PASSWORD")
val releaseSigningReady = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { it != null }

android {
    namespace = "com.lukr99.workout"
    compileSdk = 35
    // Pin to build-tools present in the canonical SDK (LOCALAPPDATA has 34/35, not 36). Phase 3
    // was authored against an SDK that only exposed 36.0.0; 35.0.0 matches compileSdk and keeps
    // feature/app-rework buildable on the standard toolchain (see reference-build-toolchain).
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.lukr99.workout"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        // Native rework release line. The frozen MAUI proof-of-concept already used v1.0.0.
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema export — checked in under app/schemas/ (see 03-data-model.md "Migrations").
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }

    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                storeFile = rootProject.file(checkNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        // No debug applicationId suffix: the frozen MAUI 1.0 app ships as
        // `com.lukr99.workouttracker`, so `com.lukr99.workout` already coexists with it, and the
        // tools/build-and-install.ps1 -Launch step targets the un-suffixed id.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    // Ship the exported Room schemas as instrumented-test assets so MigrationTestHelper / schema
    // validation can read them on-device.
    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas", "src/test/resources")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)

    // Compose (BOM aligns all Compose artifacts)
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.compose.ui.tooling)

    // Room (persistence — entities/DAO/Db; compiled via KSP)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Settings (DataStore) + JSON export contract + coroutines
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.health.connect.client)
    implementation(libs.work.runtime.ktx)

    // JVM unit tests (domain analytics/estimates + serialization round-trip)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented Room tests (DAO CRUD, filters, cascades, seed, round-trip)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.work.testing)
}
