plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // This applies the Firebase plugin to your app module
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.grpassignment"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.grpassignment"
        minSdk = 24
        targetSdk = 35
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
    packagingOptions {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.recyclerview)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // OSMDroid for OpenStreetMap
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Firebase Firestore
    implementation("com.google.firebase:firebase-firestore:26.0.2")

    // --- Add Glide for image loading ---
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    // --------------------------------

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
    // ... keep your existing build types and compile options ...
}

dependencies {
    // Direct library references to bypass version catalog issues
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // Firebase configuration
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
}
