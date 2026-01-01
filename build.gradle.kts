// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Standard Android plugins using the 'id' syntax for better compatibility
    id("com.android.application") version "8.7.2" apply false
    id("com.android.library") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false

    // Firebase Google Services Plugin - using version 4.4.2 as requested
    id("com.google.gms.google-services") version "4.4.2" apply false
}