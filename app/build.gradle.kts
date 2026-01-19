plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    kotlin("kapt")
//    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
}

android {
    namespace = "com.hommlie.partner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hommlie.partner"
        minSdk = 26
        targetSdk = 36
        versionCode = 20
        versionName = "2.9"
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            ndk.debugSymbolLevel = "FULL"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            dependenciesInfo {
                includeInApk = false
                includeInBundle = false
            }
        }
        debug {
            isMinifyEnabled = false // <-- Set this to true for release build
            isShrinkResources = false
        }
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    ndkVersion = "29.0.14206865"
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

kapt {
    correctErrorTypes = true
}    //  for ignore the error or compile time of hilt

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.play.services.location)
    implementation(libs.firebase.database.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.common)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.play.services.maps)
    implementation(libs.google.maps.utils)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Retrofit & OkHttp
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.gson.converter)
    // For responsive UI
    implementation(libs.sdp)
    implementation(libs.ssp)
    // Hilt dependencies
    implementation(libs.hilt.android)  // androidx.hilt:hilt-android
    kapt(libs.hilt.compiler)    // androidx.hilt:hilt-compiler
    implementation(libs.hilt.work)
    kapt(libs.hilt.work.compiler)
//    implementation(libs.javapoet)
    // Coroutines dependencies
    implementation(libs.coroutines)
    // ViewModel and LiveData dependencies
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycleLiveDataKtx)
    implementation(libs.lifecycle.runtime.ktx)
    //Glide
    implementation(libs.glide)
    kapt(libs.glide.compiler)
    // Firebase BoM – manages all versions
    implementation(platform(libs.firebase.bom))
    // Firebase components
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.material.calendarview)
//    implementation("com.kizitonwose.calendar:view:2.3.0")
//    implementation("com.applandeo:material-calendar-view:1.5.0")
    //CircelImageView
    implementation(libs.roundedimageview)
    //Image round
    implementation (libs.circleimageview)
    implementation (libs.ucrop)
    implementation (libs.imagepicker)
    implementation (libs.mpandroidchart)
    implementation (libs.checkout)
    implementation (libs.zxing.android.embedded)
    implementation(libs.lottie)
    implementation(libs.signature.pad)
    implementation("com.itextpdf:itext7-core:7.2.5") {
        exclude(group = "org.bouncycastle")
    }
//    implementation(libs.android.pdf.viewer)
//    implementation("com.github.barteksc:android-pdf-viewer:2.8.2")
    implementation(libs.slf4j.nop)
}



