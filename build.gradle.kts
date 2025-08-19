// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        // ✅ Add Firebase Google Services Gradle Plugin
        classpath("com.google.gms:google-services:4.4.1")
    }

    repositories {
        google()
        mavenCentral()
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}