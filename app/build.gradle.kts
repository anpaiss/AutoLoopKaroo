plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

import java.util.Properties

// Release signing comes from local.properties (never committed). A checkout
// without it must still configure, so tests and the manifest task can run.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.example.autoloopkaroo"
    compileSdk = 34

    signingConfigs {
        if (localProps.containsKey("signing.storeFile")) {
            create("release") {
                storeFile = file(localProps["signing.storeFile"] as String)
                storePassword = localProps["signing.storePassword"] as String
                keyAlias = localProps["signing.keyAlias"] as String
                keyPassword = localProps["signing.keyPassword"] as String
            }
        }
    }

    defaultConfig {
        applicationId = "com.example.autoloopkaroo"
        minSdk = 31
        targetSdk = 31
        versionCode = 9
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            val versionName = output.versionName.orNull ?: "unknown"
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)
                ?.outputFileName?.set("AutoLoopKaroo-${versionName}.apk")
        }
    }
}

// Karoo extension manifest: the JSON the Karoo fetches from MANIFEST_URL (see
// AndroidManifest.xml) to list the app in its Extension Library and offer
// updates. Written on every build to app/manifest.json (ignored); attach it to
// each GitHub release next to AutoLoopKaroo.apk. latestVersionCode must be the
// versionCode of that APK — it is what decides whether an update is offered.
// Schema: io.hammerhead.karooext.models.KarooAppManifest.
val generateManifest by tasks.registering {
    val out = layout.projectDirectory.file("manifest.json")
    val version = android.defaultConfig.versionName ?: "0"
    val code = android.defaultConfig.versionCode ?: 0
    val notes = System.getenv("RELEASE_NOTES").orEmpty()
    inputs.property("version", version); inputs.property("code", code); inputs.property("notes", notes)
    outputs.file(out)
    doLast {
        fun q(v: String) = "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
        // Screenshots live in docs/: github.com/user-attachments URLs answer 403
        // to anything that is not a browser, so the Karoo backend could not use them.
        val shots = listOf("config-1", "config-2", "field-on", "field-off")
            .joinToString(", ") { q("https://raw.githubusercontent.com/anpaiss/AutoLoopKaroo/master/docs/screenshot-$it.png") }
        out.asFile.writeText(
            """
            {
              "label": "Auto Loop Karoo",
              "packageName": "com.example.autoloopkaroo",
              "latestApkUrl": "https://github.com/anpaiss/AutoLoopKaroo/releases/latest/download/AutoLoopKaroo.apk",
              "latestVersion": ${q(version)},
              "latestVersionCode": $code,
              "iconUrl": "https://raw.githubusercontent.com/anpaiss/AutoLoopKaroo/master/icon_foreground.png",
              "developer": "Andrea Paissan",
              "description": ${q("Automatically scrolls through your ride data pages so you can focus on riding. Navigation-aware: switches to the map page before a turn and resumes scrolling after it. Per-page dwell time, skip pages, optional sound feedback.")},
              "releaseNotes": ${q(notes)},
              "screenshotUrls": [$shots],
              "tags": ["performance"]
            }
            """.trimIndent()
        )
    }
}
tasks.named("preBuild") { dependsOn(generateManifest) }

dependencies {
    implementation(libs.karoo.ext)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
