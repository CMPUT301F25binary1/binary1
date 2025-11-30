plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.fairchance"
    compileSdk {
        version = release(36)
    }
    defaultConfig {
        applicationId = "com.example.fairchance"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.useJUnitPlatform() // allows Gradle to detect the test engine
        }
    }
}
dependencies {
    // --- ANDROIDX UI ---
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.cardview:cardview:1.0.0")

    // --- NAVIGATION ---
    implementation("androidx.navigation:navigation-fragment:2.7.6")
    implementation("androidx.navigation:navigation-ui:2.7.6")

    // --- FIREBASE via BOM (no explicit versions) ---
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-messaging")

    // --- GOOGLE PLAY SERVICES ---
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-tasks:18.0.2")

    // --- QR / IMAGING / UI HELPERS ---
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // --- UNIT TESTS ---
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")

    // --- ANDROID INSTRUMENTED TESTS ---
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("org.mockito:mockito-android:5.10.0")
    androidTestImplementation("androidx.fragment:fragment-testing:1.6.2")
    debugImplementation("androidx.fragment:fragment-testing:1.6.2")

}
