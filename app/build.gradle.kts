plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.grpassignment"
    compileSdk {
        version = release(36)
    }

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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // 1. Google Sign-In (Resolves GoogleSignIn, GoogleSignInClient, etc. errors)
    implementation("com.google.android.gms:play-services-auth:21.3.0")
// 2. Material Design Components (Resolves TextInputEditText errors)
    implementation("com.google.android.material:material:1.11.0")
}