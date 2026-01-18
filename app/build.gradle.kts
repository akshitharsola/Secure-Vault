// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Add kapt for Room
    id("kotlin-kapt")
}

android {
    namespace = "com.securevault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.securevault"
        minSdk = 24
        targetSdk = 35
        versionCode = 19
        versionName = "2.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val alias = System.getenv("SIGNING_KEY_ALIAS")?.trim()
            val keyPass = System.getenv("SIGNING_KEY_PASSWORD")?.trim()
            val storePass = System.getenv("SIGNING_STORE_PASSWORD")?.trim()

            if (alias != null && keyPass != null && storePass != null) {
                keyAlias = alias
                keyPassword = keyPass
                storeFile = file("keystore.jks")
                storePassword = storePass

                // Enable all signing versions for maximum compatibility
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true

                println("Signing config: alias=$alias, keystore=keystore.jks")
            } else {
                println("Missing signing environment variables")
            }
        }
    }
    
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            val hasSigningEnv = System.getenv("SIGNING_KEY_ALIAS") != null &&
                               System.getenv("SIGNING_KEY_PASSWORD") != null &&
                               System.getenv("SIGNING_STORE_PASSWORD") != null
            
            signingConfig = if (hasSigningEnv) {
                println("Using release signing config")
                signingConfigs.getByName("release")
            } else {
                println("No signing config - building unsigned APK")
                null
            }
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
        buildConfig = true
    }

    packaging {
        resources {
            // Exclude conflicting META-INF files from Bouncy Castle
            excludes += setOf(
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    // Core dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.fragment.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Media
    implementation(libs.androidx.media3.common.ktx)

    // Biometric
    implementation(libs.androidx.biometric)  // Removed biometric-ktx reference

    // Extended Material Icons
    implementation(libs.androidx.material.icons.extended)

    // Gson for JSON serialization/deserialization
    implementation(libs.gson)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // For file operations
    implementation(libs.androidx.documentfile)

    // For encryption
    implementation(libs.androidx.security.crypto)

    // Bouncy Castle for post-quantum cryptography
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")

    // For file picking
    implementation(libs.androidx.activity.compose.v182)

    implementation(libs.androidx.material3)
}

// Task to preserve ProGuard mappings for crash analysis
tasks.register("saveProguardMapping", Copy::class) {
    from(layout.buildDirectory.dir("outputs/mapping/release"))
    into(rootProject.layout.projectDirectory.dir("proguard-mappings/v${android.defaultConfig.versionName}"))
    include("mapping.txt")
    doFirst {
        println("Saving ProGuard mapping for version ${android.defaultConfig.versionName}")
    }
}

// Automatically save mapping after release build (using afterEvaluate to ensure task exists)
afterEvaluate {
    tasks.findByName("assembleRelease")?.finalizedBy("saveProguardMapping")
}