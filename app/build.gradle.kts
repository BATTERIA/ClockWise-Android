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
        versionCode = 450
        versionName = "4.5.0"
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

    // v4.0: Robolectric needs the unit test runner to run Android-aware
    // tests on the JVM, so we don't need a connected device/emulator to
    // verify the quiz + clock screens.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                // v4.4: forward Roborazzi system properties from the Gradle
                // command line into the test JVM. Without this `-Droborazzi.
                // test.record=true` is silently dropped and no PNG ever
                // gets written. Default verifyAndRecord so missing baselines
                // are captured automatically.
                test.systemProperties["roborazzi.test.record"] =
                    System.getProperty("roborazzi.test.record", "true")
                test.systemProperties["roborazzi.test.verify"] =
                    System.getProperty("roborazzi.test.verify", "false")
                // Robolectric graphics mode is scoped per-test via
                // @GraphicsMode(NATIVE) on screenshot tests; we leave it
                // alone here so the existing display-based tests keep using
                // the (faster) LEGACY mode they were written against.
            }
        }
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

    // v4.0 — testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    // v4.4 — screenshot tests (JVM-only via Robolectric Native Graphics).
    // No emulator/KVM required: Roborazzi renders Compose to PNG on the JVM
    // so we can self-verify visual fixes when the dev host has no AVD.
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.20.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.20.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.20.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
