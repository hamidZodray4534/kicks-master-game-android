#-------------------------------------------------
# 1. Project Specific
#-------------------------------------------------
# Preserve data models for Gson mapping
-keep class com.kicks.master.model.** { *; }
-keep class com.kicks.master.helper.model.** { *; }
# Preserve Javascript interfaces and Annotations
-keepattributes *Annotation*,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
#-------------------------------------------------
# 2. Gson & Retrofit & OkHttp
#-------------------------------------------------
# Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.examples.android.model.** { *; }
-keep class com.google.gson.internal.** { *; }
-keep class com.google.gson.internal.bind.** { *; }

# Retrofit
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

#-------------------------------------------------
# 3. Firebase & Google Play Services & Credentials
#-------------------------------------------------
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

#-------------------------------------------------
# 4. Ads Networks
#-------------------------------------------------
# Google Mobile Ads
-keep class com.google.android.gms.ads.** { *; }

# Unity Ads
-keep class com.unity3d.services.** { *; }
-keep class com.unity3d.ads.** { *; }
-dontwarn com.unity3d.services.**
-dontwarn com.unity3d.ads.**

# Facebook Audience Network
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**

# InMobi
-keep class com.inmobi.** { *; }
-dontwarn com.inmobi.**

# Vungle
-keep class com.vungle.warren.** { *; }
-dontwarn com.vungle.warren.**
-keep class com.vungle.ads.** { *; }
-dontwarn com.vungle.ads.**

# Pangle (PAG SDK)
-keep class com.bytedance.sdk.openadsdk.** { *; }
-dontwarn com.bytedance.sdk.openadsdk.**
-keep class com.bytedance.sdk.component.** { *; }
-dontwarn com.bytedance.sdk.component.**
-keep class com.pangle.global.** { *; }
-dontwarn com.pangle.**

# Fyber FairBid
-keep class com.fyber.** { *; }
-dontwarn com.fyber.**

# OneSignal
-keep class com.onesignal.** { *; }
-dontwarn com.onesignal.**

#-------------------------------------------------
# 5. Core Jetpack & Others
#-------------------------------------------------
-keep class androidx.lifecycle.** { *; }
-keep class androidx.security.** { *; }
-keep class androidx.multidex.** { *; }

# Preserve Line Numbers for Crashlytics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

#-------------------------------------------------
# 6. Kotlin & Coroutines
#-------------------------------------------------
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

#-------------------------------------------------
# 7. Project Specific API & Logic
#-------------------------------------------------
-keep class com.kicks.master.helper.apicall.** { *; }
-keep class com.kicks.master.utills.** { *; }
-keep class com.kicks.master.model.** { *; }
-keep class com.kicks.master.helper.model.** { *; }
-keep enum com.kicks.master.** { *; }