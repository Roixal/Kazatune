plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.kazatune"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kazatune"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.2"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/*.md"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "META-INF/*.kotlin_module"
        }
    }
}

dependencies {
    // Skim Music Backend Engine (Modules from core)
    implementation(projects.domain)
    implementation(projects.data)
    implementation(projects.common)
    implementation(projects.media3)
    implementation(projects.media3Ui)
    implementation(projects.crashlyticsEmpty)
    implementation(projects.castEmpty)
    implementation(projects.lastfmEmpty)

    // Android & Compose UI
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.compose.material3.lib)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewmodelCompose)

    // Networking
    implementation(libs.okhttp3.okhttp)

    // Testing
    testImplementation(libs.junit)

    // Image loading (Spotify canvas fallback & album art)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Dependency Injection (Koin)
    implementation(libs.koin.android)

    // Analytics (Privacy-friendly telemetry)
    implementation("com.github.aptabase:aptabase-kotlin:0.1.0")
    implementation(libs.koin.androidx.compose)

    // Coroutines
    implementation(libs.coroutines.android)

    // Glance Home Screen Widgets
    implementation(libs.work.runtime.ktx)
    implementation(libs.glance)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
}
