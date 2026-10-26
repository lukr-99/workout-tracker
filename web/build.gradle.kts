import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// The browser edition of Ember: Compose Multiplatform compiled to WebAssembly.
//
// It compiles the app's own shared code (domain, settings, the Room-free part of data, and the
// Compose UI) straight from app/src/main, so the workout logic, the export format and the screens are
// the same code on both platforms. The web edition adds its own storage (IndexedDB), its own app
// graph and its versions of the platform files (ARCHITECTURE.md, "Shared code and platform files").
// Nothing here changes the Android build. See docs/adr/0001-web-edition.md.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val appSources = rootProject.file("app/src/main/java/com/lukr99/workout")

/**
 * Files and folders under app/src/main that the web edition does not compile: Room, Android-only
 * features (Run Mode, Health Connect, backup scheduling, the updater, wger sync, GPS) and the Android
 * side of every platform file. A platform file excluded here has a file of the same name and package
 * under web/src/wasmJsMain/kotlin.
 */
val androidOnly = listOf(
    // Room and the Android app graph.
    "data/AppContainer.kt",
    "data/Converters.kt",
    "data/EntryWithSets.kt",
    "data/RoomTransactionRunner.kt",
    "data/RoomWorkoutRepository.kt",
    "data/SessionWithEntries.kt",
    "data/TemplateWithExercises.kt",
    "data/WorkoutDao.kt",
    "data/WorkoutDb.kt",
    "data/WorkoutEntityMapping.kt",
    "data/run/RoomRunRepository.kt",
    "data/run/RunDao.kt",
    "data/migrations/",
    "Entity.kt",
    // Android-only features.
    "data/ShareCardStyle.kt",
    "data/WorkoutShareCardRenderer.kt",
    "data/backup/",
    "data/health/",
    "data/location/",
    "data/map/",
    "data/music/",
    "data/routing/",
    "data/sync/",
    "data/run/RunCues.kt",
    "data/run/ShareCardRenderer.kt",
    "data/images/ExercisePhotoStore.kt",
    "data/transfer/AndroidDocumentGateway.kt",
    "domain/run/GpxCodec.kt",
    "settings/DataStore",
    "settings/OrientationStore.kt",
    "ui/UpdatesState.kt",
    "ui/UpdatesViewModel.kt",
    "ui/android/",
    "ui/run/",
    "ui/components/LocalSpotify.kt",
    "update/",
    // Android's side of the platform files.
    "data/images/FreeExerciseImageIndexResource.kt",
    "data/images/LocalPhotoModel.kt",
    "data/images/StripAccents.kt",
    "domain/LocalDecimals.kt",
    "ui/PlatformApp.kt",
    "ui/PlatformBackHandler.kt",
    "ui/components/ExerciseImage.kt",
    "ui/components/FullScreenDialog.kt",
    "ui/components/LocalDateText.kt",
    "ui/components/MusicPlayback.kt",
    "ui/components/SystemAnimationsOff.kt",
    "ui/components/TestTagsAsResourceId.kt",
    "ui/screens/RememberDocumentActions.kt",
    "ui/screens/RememberExercisePhotoPicker.kt",
    "ui/screens/RememberWorkoutSharer.kt",
    "ui/settings/LocationPermissionRequest.kt",
    "ui/theme/BarlowFonts.kt",
)

// The app's typefaces, served beside the page; Android reads the same files from res/font.
val webFonts = tasks.register<Sync>("webFonts") {
    from(rootProject.file("app/src/main/res/font")) {
        include("*.ttf")
        into("fonts")
    }
    into(layout.buildDirectory.dir("generated/webFonts"))
}

// The bundled free exercise image index, served beside the page; Android reads the same file from
// its classpath resources. The page fetches it before the first screen (WebResources.kt).
val webData = tasks.register<Sync>("webData") {
    from(rootProject.file("app/src/main/resources")) {
        include("free_exercise_image_index.json")
    }
    into(layout.buildDirectory.dir("generated/webData"))
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "ember"
        browser {
            commonWebpackConfig {
                outputFileName = "ember.js"
            }
            testTask {
                // Headless browser tests need a downloaded browser; the shared logic is already
                // tested on the JVM by :app, and this edition's own tests run under Node.
                enabled = false
            }
        }
        nodejs {
            testTask {
                // Compose's Skia build fetches its .wasm, which Node cannot do from disk on its own.
                nodeJsArgs += listOf("--import", project.file("src/wasmJsTest/node/file-fetch.mjs").toURI().toString())
            }
        }
        binaries.executable()
    }

    sourceSets {
        val wasmJsMain by getting {
            // The shared sources, compiled from where they live in the app. They are platform code
            // here rather than common, because they call the platform files this module supplies, and
            // Kotlin 2 compiles a common source set apart from its platform.
            listOf("domain", "settings", "data", "ui").forEach { kotlin.srcDir(appSources.resolve(it)) }
            kotlin.exclude { element ->
                val path = element.file.invariantSeparatorsPath
                val shared = appSources.invariantSeparatorsPath + "/"
                path.startsWith(shared) && androidOnly.any { path.removePrefix(shared).let { rel -> rel.startsWith(it) || rel.endsWith(it) } }
            }
            resources.srcDir(webFonts)
            resources.srcDir(webData)
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.materialIconsExtended)
                implementation(libs.jb.lifecycle.viewmodel.compose)
                implementation(libs.jb.lifecycle.runtime.compose)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }
        val wasmJsTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

// The page shows the same version as the Android app.
val appVersionName: String = Regex("""versionName\s*=\s*"([^"]+)"""")
    .find(rootProject.file("app/build.gradle.kts").readText())
    ?.groupValues?.get(1)
    ?: error("versionName not found in app/build.gradle.kts")

val generateWebVersion by tasks.registering {
    val out = layout.buildDirectory.dir("generated/version/kotlin")
    inputs.property("versionName", appVersionName)
    outputs.dir(out)
    doLast {
        val file = out.get().file("com/lukr99/workout/web/WebVersion.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "package com.lukr99.workout.web\n\n" +
                "/** Generated from versionName in app/build.gradle.kts. */\n" +
                "internal const val WEB_VERSION_NAME = \"$appVersionName-web\"\n",
        )
    }
}
kotlin.sourceSets.named("wasmJsMain") { kotlin.srcDir(generateWebVersion) }

// The Kotlin plugin adds its own download repositories for Node.js, Yarn and Binaryen, which the
// settings forbid. With these cleared it resolves them through the repositories declared in
// settings.gradle.kts instead. Setting -Pember.web.systemYarn=true uses a Yarn already on PATH,
// for machines that cannot reach GitHub.
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().downloadBaseUrl = null
}
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
        downloadBaseUrl = null
        if (providers.gradleProperty("ember.web.systemYarn").orNull == "true") download = false
    }
}
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootExtension>().downloadBaseUrl = null
}
