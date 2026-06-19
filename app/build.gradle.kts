plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.kicks.master"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kicks.master"
        minSdk = 26
        targetSdk = 35
        versionCode = 18
        versionName = "1.8.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://kicksmasteradmin.bonixgames.com/api/\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"https://kicksmasteradmin.bonixgames.com/api/\"")
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src/main/assets", "assets")
            }
        }
    }
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += "**/*.so"
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.webkit:webkit:1.12.1")

    // Google Sign-In / Credentials
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Google Play Services
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")
    // Install Referrer
    implementation("com.android.installreferrer:installreferrer:2.2")

    // Firebase (BOM manages all versions)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")

    // Retrofit + Gson
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // SSP/SDP
    implementation("com.intuit.sdp:sdp-android:1.0.6")
    implementation("com.intuit.ssp:ssp-android:1.0.6")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // encrypt data
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Ads
    implementation("com.google.android.gms:play-services-ads:24.0.0")
    implementation("com.unity3d.ads:unity-ads:4.12.2")
    implementation("com.facebook.android:audience-network-sdk:6.18.0")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("com.inmobi.monetization:inmobi-ads-kotlin:10.8.0")
    implementation("com.google.ads.mediation:facebook:6.17.0.0")
    implementation("com.vungle:vungle-ads:7.4.2")
    implementation("com.pangle.global:pag-sdk:7.9.0.9")
    // Fyber FairBid SDK
    implementation("com.fyber:fairbid-sdk:3.62.0")
    // cloudx SDK
    implementation("io.cloudx:sdk:4.1.1")

    // Adapters for ad networks
    implementation("io.cloudx:adapter-digitalturbine:8.4.5.0")     // Digital Turbine Marketplace SDK 8.4.5
    implementation("io.cloudx:adapter-googlewaterfall:25.1.0.0")   // Google Mobile Ads SDK 25.1.0
    implementation("io.cloudx:adapter-inmobi:11.2.0.0")            // InMobi SDK 11.2.0
    implementation("io.cloudx:adapter-magnite:1.0.0.0")            // Magnite Ads SDK 1.0.0
    implementation("io.cloudx:adapter-meta:6.21.0.0")              // Meta Audience Network 6.21.0
    implementation("io.cloudx:adapter-moloco:4.8.0.0")             // Moloco SDK 4.8.0
    implementation("io.cloudx:adapter-unityads:4.17.0.0")          // Unity Ads SDK 4.17.0
    implementation("io.cloudx:adapter-vungle:7.7.3.0")

    // OneSignal
    implementation("com.onesignal:OneSignal:5.1.6")

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.multidex:multidex:2.0.1")
}