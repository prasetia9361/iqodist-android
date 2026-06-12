// App-level build.gradle.kts
import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace   = "com.iqodist"
    compileSdk  = 35

    defaultConfig {
        applicationId           = "com.iqodist"
        minSdk                  = 24
        targetSdk               = 35
        versionCode             = 1
        versionName             = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val localProps = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localProps.load(localPropsFile.inputStream())
        }
        buildConfigField(
            "String",
            "BASE_URL",
            "\"${localProps.getProperty("BASE_URL", "https://apiexample.com/")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled  = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        compose     = true
        buildConfig = true
    }
}

dependencies {

    // ── CORE ──────────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)

    // ── COMPOSE ───────────────────────────────────────────────────────────
    // Bundle: UI + Material3 + ViewModel + Navigation + Hilt-Navigation
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // ── ROOM ──────────────────────────────────────────────────────────────
    // Bundle: room-runtime + room-ktx
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)

    // ── HILT ──────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // ── NETWORK ───────────────────────────────────────────────────────────
    // Bundle: Retrofit + OkHttp + Serialization converter
    implementation(libs.bundles.network)

    // ── COROUTINES ────────────────────────────────────────────────────────
    implementation(libs.bundles.coroutines)

    // ── DATASTORE ─────────────────────────────────────────────────────────
    implementation(libs.androidx.datastore.preferences)

    // ── WORKMANAGER ───────────────────────────────────────────────────────
    // Bundle: work-runtime-ktx + hilt-work
    implementation(libs.bundles.workmanager)
    ksp(libs.hilt.work.compiler)

    // ── COIL ──────────────────────────────────────────────────────────────
    // Bundle: coil-compose + coil-network-okhttp
    implementation(libs.bundles.coil)

    // ── LOGGING ───────────────────────────────────────────────────────────
    implementation(libs.timber)

    // ── TESTING ───────────────────────────────────────────────────────────
    testImplementation(libs.bundles.testing.unit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.testing.android)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    //QR Scanner
    implementation(libs.bundles.camera)
    implementation(libs.accompanist.permissions)
}