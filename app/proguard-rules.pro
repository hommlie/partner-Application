# -------------------------
# Production ProGuard / R8 rules
# Tailored for: Gson, Retrofit, OkHttp, Hilt/Dagger, Glide, Firebase, Lottie, ZXing, UCrop, ImagePicker, Razorpay, WorkManager, Lifecycle, Coroutines, MPAndroidChart
# Package-specific keeps for your app: com.hommlie.partner
# -------------------------

# Keep generic type signatures (VERY important for Gson TypeToken / reflection)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod,Exceptions,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations

# Keep @Keep annotated classes/members
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# -------------------------
# Gson (JSON parsing) - preserve Gson internals and TypeToken subclasses
# -------------------------
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-dontwarn com.google.gson.**

# Keep model classes (adjust package if your models are elsewhere)
# Keep fields and methods so Gson reflection works
-keep class com.hommlie.partner.model.** { *; }
-keepclassmembers class com.hommlie.partner.model.** {
    <fields>;
    <init>(...);
}

# Also keep network / data classes often used with reflection/deserializers
-keep class com.hommlie.partner.network.** { *; }
-keep class com.hommlie.partner.data.** { *; }

# -------------------------
# Retrofit & OkHttp
# -------------------------
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep Retrofit service interfaces annotated with @retrofit2.http.* (so methods/annotations preserved)
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}

# -------------------------
# Hilt / Dagger
# -------------------------
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <fields>;
    @javax.inject.Inject <methods>;
}
-keep class * implements dagger.hilt.android.HiltAndroidApp { *; }
-keep class * extends dagger.hilt.internal.** { *; }
-dontwarn dagger.**
-dontwarn javax.inject.**

# Keep generated Hilt classes (narrowly)
-keep class dagger.hilt.internal.** { *; }
-keep class dagger.hilt.android.internal.managers.* { *; }

# -------------------------
# Glide
# -------------------------
# Keep Glide modules & generated API
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.AppGlideModule
-keep class com.bumptech.glide.** { *; }
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
-dontwarn com.bumptech.glide.**

# -------------------------
# Firebase / Play Services / Crashlytics / Analytics
# -------------------------
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

-keep class com.google.android.gms.internal.location.** { *; }
-dontwarn com.google.android.gms.internal.location.**


# Crashlytics mapping: don't strip mapping upload support (Gradle plugin uploads mapping automatically).
# (No specific keep required, but ensure mapping file is uploaded on release)

# -------------------------
# Lottie
# -------------------------
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# -------------------------
# UCrop, ImagePicker, ZXing, Razorpay, Calendar, RoundedImageView, CircleImageView, MPAndroidChart
# -------------------------
-keep class com.yalantis.ucrop.** { *; }
-keep class com.github.dhaval2404.imagepicker.** { *; }
-keep class com.journeyapps.** { *; }          # ZXing Android Embedded
-keep class com.google.zxing.** { *; }
-keep class com.razorpay.** { *; }
-keep class com.prolificinteractive.materialcalendarview.** { *; }
-keep class com.makeramen.** { *; }            # roundedimageview
-keep class de.hdodenhof.circleimageview.** { *; }
-keep class com.github.PhilJay.** { *; }       # MPAndroidChart
-dontwarn com.yalantis.ucrop.**
-dontwarn com.github.dhaval2404.imagepicker.**
-dontwarn com.journeyapps.**
-dontwarn com.google.zxing.**
-dontwarn com.razorpay.**
-dontwarn com.prolificinteractive.materialcalendarview.**
-dontwarn com.makeramen.**
-dontwarn de.hdodenhof.circleimageview.**
-dontwarn com.github.PhilJay.**

# -------------------------
# WorkManager, Lifecycle, ViewModel, LiveData, Room (if used)
# -------------------------
-keep class androidx.work.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.work.**
-dontwarn androidx.lifecycle.**

# Keep ViewModel constructors used by frameworks
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# -------------------------
# Kotlin & Coroutines
# -------------------------
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.jvm.internal.Intrinsics { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# -------------------------
# Keep enum methods (to avoid some issues with reflection)
# -------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# -------------------------
# Keep annotations / line info (optional)
# If you want to keep SourceFile & LineNumberTable uncomment these; for crash reporting you normally upload mapping.txt instead.
# -------------------------
#-keepattributes SourceFile,LineNumberTable

# -------------------------
# Additional safe defaults
# -------------------------
# Keep classes referenced via reflection by name
-keepnames class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep annotations and any runtime visible annotations
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations

# Keep classes annotated with @Parcelize (kotlinx) if used
-keepclassmembers class * {
    @kotlinx.parcelize.Parcelize *;
}

# Prevent some warnings from cluttering build logs (safe)
-dontnote
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# -------------------------
# Final: Application specific - be conservative for safety
# Keep app package important parts (models, network, db, ui glue)
# -------------------------
-keep class com.hommlie.partner.** { *; }

# -------------------------
# End of file
# -------------------------
