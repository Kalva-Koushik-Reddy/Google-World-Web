plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.firebase.appdistribution)
}

android {
    namespace = "com.example.google_world_web"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.google_world_web"
        minSdk = 26

        targetSdk = 34


        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1

        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


    }
    signingConfigs {
        create("release") { // It's good practice to use create explicitly
            storeFile = file(System.getenv("SIGNING_KEYSTORE") ?: "release-key.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: ""
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release") // Correct assignment
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}
firebaseAppDistribution {
    serviceCredentialsFile = System.getenv("FIREBASE_CREDENTIALS")
    appId = System.getenv("FIREBASE_APP_ID")
    groups = "testers"  // Firebase tester group
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
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.firebase.storage)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.rules)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.database) // For Kotlin extensions (good for both)    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    implementation(libs.androidx.compose.foundation) // Assuming you add this to your libs.versions.toml
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
