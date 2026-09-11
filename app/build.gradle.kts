plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "lk.synergypharma.employee"
    compileSdk = 36

    defaultConfig {
        applicationId = "lk.synergypharma.employee"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        // Sinhala + English only. Keeps the APK small and stops half-translated
        // system languages leaking through.
        localeFilters += setOf("en", "si")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            // Debug talks to the recognition backend running on the dev machine.
            // 10.0.2.2 is the emulator's alias for the host; on a real phone use
            // the machine's LAN IP (same Wi-Fi). Flip USE_MOCK_DATA back to true
            // to demo without a server.
            buildConfigField("boolean", "USE_MOCK_DATA", "false")
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/api/v1/\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Flip to false in the release that ships against the real backend.
            buildConfigField("boolean", "USE_MOCK_DATA", "true")
            buildConfigField("String", "API_BASE_URL", "\"https://hr.synergypharma.lk/api/v1/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time on API 24 devices
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("META-INF/*.kotlin_module", "META-INF/DEPENDENCIES")
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation(libs.appcompat)
    implementation(libs.core)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.swiperefreshlayout)
    implementation(libs.activity)
    implementation(libs.fragment)
    implementation(libs.exifinterface)

    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)

    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    implementation(libs.biometric)
    implementation(libs.security.crypto)

    // Declared now so the API contract compiles. Only MockApiService is wired
    // until phase 9 — see data/repository/RepositoryProvider.java.
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
