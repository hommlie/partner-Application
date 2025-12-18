plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    kotlin("kapt")
    id("kotlin-parcelize")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
}

android {
    namespace = "com.hommlie.partner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hommlie.partner"
        minSdk = 26
        targetSdk = 35
        ndkVersion = "26.2.11394342"
        versionCode = 14
        versionName = "2.3"
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

    }

    buildTypes {
        release {
            isMinifyEnabled = true // <-- Set this to true for release build
            isShrinkResources = true
            isDebuggable = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            dependenciesInfo {
                includeInApk = false
                includeInBundle = false
            }
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


    implementation("com.prolificinteractive:material-calendarview:1.4.3")

//    implementation("com.kizitonwose.calendar:view:2.3.0")
//    implementation("com.applandeo:material-calendar-view:1.5.0")

    //CircelImageView
    implementation("com.makeramen:roundedimageview:2.3.0")

    //Image round
    implementation ("de.hdodenhof:circleimageview:2.2.0")

    implementation ("com.github.yalantis:ucrop:2.2.8")

    implementation ("com.github.dhaval2404:imagepicker:2.1")


    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation ("com.razorpay:checkout:1.6.33")
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")

    implementation("com.airbnb.android:lottie:6.4.0")

    implementation("com.github.gcacace:signature-pad:1.3.1")

    implementation("com.itextpdf:itext7-core:7.2.5") {
        exclude(group = "org.bouncycastle")
    }

    implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")
    implementation("org.slf4j:slf4j-nop:2.0.7")


}



