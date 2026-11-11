plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val clerkPublishableKey = providers.gradleProperty("APOLO_CLERK_PUBLISHABLE_KEY")
    .orElse(providers.environmentVariable("APOLO_CLERK_PUBLISHABLE_KEY"))
    .orElse("pk_test_placeholder_for_ci")

android {
    namespace = "com.apolo.llmstudio"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.apolo.llmstudio"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "CLERK_PUBLISHABLE_KEY", "\"" + clerkPublishableKey.get() + "\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("com.clerk:clerk-android-ui:1.4.1")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
