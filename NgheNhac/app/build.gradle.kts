import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.nghenhac"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.nghenhac"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ── BuildConfig fields (loaded from local.properties) ──
        val localProperties = rootProject.file("local.properties")
        val props = Properties()
        if (localProperties.exists()) {
            props.load(FileInputStream(localProperties))
        }

        buildConfigField("String", "BASE_API_URL",
            "\"${props.getProperty("base.api.url", "http://10.0.2.2:8080")}\"")
        buildConfigField("String", "FIREBASE_DB_URL",
            "\"${props.getProperty("firebase.db.url", "")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "LOG_HTTP", "true")
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "LOG_HTTP", "false")
        }
    }

    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
    }
}

dependencies {
    // ── AndroidX Core ──
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)

    // ── Media3 (ExoPlayer) ──
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)
    implementation(libs.media3.datasource.okhttp)

    // ── MediaSessionCompat ──
    implementation(libs.androidx.media)

    // ── Room Database ──
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    androidTestImplementation(libs.room.testing)

    // ── Network ──
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // ── Image Loading ──
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // ── Firebase (BoM auto-manages versions) ──
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)

    // ── Navigation ──
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // ── Lifecycle ──
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.common.java8)

    // ── Background Work ──
    implementation(libs.work.runtime)

    // ── Security ──
    implementation(libs.security.crypto)

    // ── Palette (color extraction) ──
    implementation(libs.palette)

    // ── Splash Screen ──
    implementation(libs.splashscreen)

    // ── Testing ──
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}