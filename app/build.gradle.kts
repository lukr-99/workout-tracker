plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.lukr99.workout"
    compileSdk = 35
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.lukr99.workout"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema export — checked in under app/schemas/ (see 03-data-model.md "Migrations").
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }

    buildTypes {
        // No debug applicationId suffix: the frozen MAUI 1.0 app ships as
        // `com.lukr99.workouttracker`, so `com.lukr99.workout` already coexists with it, and the
        // tools/build-and-install.ps1 -Launch step targets the un-suffixed id.
        release {
            isMinifyEnabled = false
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

    // JVM unit tests (domain analytics/estimates + serialization round-trip)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented Room tests (DAO CRUD, filters, cascades, seed, round-trip)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
