plugins {
    // Update AGP from 8.7.2 to 8.9.1
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false

    // Update Kotlin to a version compatible with AGP 8.9.1 (e.g., 1.9.23 or 2.0.x)
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false

    // Firebase Google Services can remain at 4.4.2
    id("com.google.gms.google-services") version "4.4.2" apply false
}