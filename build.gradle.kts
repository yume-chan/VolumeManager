// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false

    alias(libs.plugins.androidaop) apply false
    id("com.google.devtools.ksp") version "2.2.20-2.0.3" apply false
}

apply(plugin = "android.aop.clean")