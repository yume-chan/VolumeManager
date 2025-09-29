import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.google.devtools.ksp")
    id("android.aop")
}

androidAopConfig {
    // enabled is false, the aspect no longer works, the default is not written as true
    enabled = true
    debug = true

    // include does not set all scans by default. After setting, only the code of the set package name will be scanned.
//    include("moe.chensi.volume")

    // exclude is the package excluded during scanning
    // Can exclude kotlin related and improve speed
    exclude(
        "kotlin.jvm",
        "kotlin.internal",
        "kotlinx.coroutines.internal",
        "kotlinx.coroutines.android"
    )
    // Exclude the entity name of the package
    excludePackaging("license/NOTICE", "license/LICENSE.dom-software.txt", "license/LICENSE")

    // verifyLeafExtends Whether to turn on verification leaf inheritance, it is turned on by default. If type = MatchType.LEAF_EXTENDS of @AndroidAopMatchClassMethod is not set, it can be turned off.
    verifyLeafExtends = true
    //Disabled by default. Enabled after Build or Packaging, a cut information file will be generated in app/build/tmp/ (cutInfo.json, cutInfo.html)
    cutInfoJson = true
}

android {
    namespace = "moe.chensi.volume"
    compileSdk = 36

    defaultConfig {
        applicationId = "moe.chensi.volume"
        minSdk = 33
        targetSdk = 35
        versionCode = 9
        versionName = "0.3-beta.5"

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

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        compose = true
        aidl = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.hiddenapibypass)

    implementation(libs.androidx.datastore.core.android)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.joor)

    implementation(libs.androidaop.core)
    implementation(libs.androidx.appcompat)
    ksp(libs.androidaop.apt)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
