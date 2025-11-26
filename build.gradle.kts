// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()  // Required for Hilt and other Android dependencies
        mavenCentral()
        maven { url = uri("https://jitpack.io") }// Required for other dependencies like Kotlin
    }
    /* dependencies {
         Android Gradle Plugin (AGP)
        classpath("com.android.tools.build:gradle:8.5.1")

         Hilt Gradle Plugin
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.44")
    } */
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.hilt.android) apply false
}

