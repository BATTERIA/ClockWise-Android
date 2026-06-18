plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.batteria.clockwise"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.batteria.clockwise"
        minSdk = 26
        targetSdk = 34
        versionCode = 371
        versionName = "3.7.1"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    androidResources {
        // v3.7: don't try to gzip already-compressed Opus/Ogg voice clips.
        // Saves CPU at install time; assets are small enough that compression
        // gains are negligible.
        noCompress.add("ogg")
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    // v3.6.5: needed for Icons.Filled.VolumeUp on the new TTS speaker button.
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-android-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Lottie
    implementation("com.airbnb.android:lottie-compose:6.3.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}
