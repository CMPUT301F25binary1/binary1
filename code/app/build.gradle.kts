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
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    testImplementation("junit:junit:4.13.2")

    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")


    testImplementation("androidx.test:core:1.5.0")

    testImplementation("org.robolectric:robolectric:4.11.1")

    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")

    // Android instrumented tests
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("com.google.firebase:firebase-messaging:23.4.1")
    androidTestImplementation("org.mockito:mockito-android:5.10.0")
    androidTestImplementation("org.mockito:mockito-core:5.10.0")
    // Firebase + UI libs
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.navigation:navigation-fragment:2.7.6")
    implementation("androidx.navigation:navigation-ui:2.7.6")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") // for easy Bitmap
    implementation("com.google.firebase:firebase-firestore:25.1.1")
    implementation("com.google.firebase:firebase-storage:21.0.1")
    implementation("com.google.android.gms:play-services-tasks:18.0.2")
    implementation("com.google.firebase:firebase-functions:20.4.0")

}
