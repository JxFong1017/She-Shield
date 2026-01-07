plugins {
    // Use id() to match your project-level definitions
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.kapt")
    // Duplicate line removed here

}

android {
    namespace = "com.example.grpassignment"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.grpassignment"
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

    kotlinOptions {
        jvmTarget = "11"   // <-- ADD THIS
    }
}

dependencies {
    // 1. Core UI Libraries (Note: if libs.appcompat fails, use "androidx.appcompat:appcompat:1.7.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    implementation(libs.play.services.maps)

    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.recyclerview)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.github.MKergall:osmbonuspack:6.9.0")
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.firebase.storage)
    kapt("com.github.bumptech.glide:compiler:4.16.0")


    // 2. Firebase Configuration
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth")

    // 3. Google Sign-In (Removed)
    implementation(libs.play.services.location)
    implementation(libs.firebase.firestore)

    // 4. Testing Libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20230227")
}
