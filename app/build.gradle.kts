import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  id("org.jetbrains.kotlin.android")
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.services)
  id("kotlin-kapt")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun buildConfigString(value: String): String {
    val escaped = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "\"$escaped\""
}

val debugGeminiApiKey = localProperties.getProperty("GEMINI_API_KEY")
    ?: System.getenv("GEMINI_API_KEY")
    ?: ""

android {
    namespace = "com.mithaq.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.mithaq.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 20
        versionName = "2.0"
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "IS_PRODUCTION", "false")
            buildConfigField("String", "GEMINI_API_KEY", buildConfigString(debugGeminiApiKey))
        }
        release {
            isMinifyEnabled = true   // Shrinks & obfuscates code — required for production
            isShrinkResources = true // Removes unused resources from the APK
            isDebuggable = false
            buildConfigField("boolean", "IS_PRODUCTION", "true")
            buildConfigField("String", "GEMINI_API_KEY", buildConfigString(""))
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-core:1.7.0")
  implementation("androidx.compose.material:material-icons-extended:1.7.0")
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Firebase
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.storage)
  implementation(libs.firebase.messaging)
  implementation(libs.kotlinx.coroutines.play.services)
  implementation(libs.play.services.auth)
  implementation(libs.play.services.mlkit.face.detection)
  implementation(libs.coil.compose)

  // Room Database
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  add("kapt", libs.androidx.room.compiler)

  // Biometric
  implementation(libs.androidx.biometric)

  // Location & Permissions
  implementation("com.google.android.gms:play-services-location:21.0.1")
  implementation("com.google.accompanist:accompanist-permissions:0.34.0")

  // WorkManager – background  // Background Sync
  implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Prayer Time calculation
    implementation("com.batoulapps.adhan:adhan:1.2.1")

  // Google Generative AI (Gemini) SDK
  implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
}
tasks.withType<org.jetbrains.kotlin.gradle.internal.KaptWithoutKotlincTask>().configureEach {
    if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
        val userHome = System.getProperty("user.home")
        val sqliteTmpDir = File(userHome, ".gemini/antigravity/sqlite_tmp")
        if (!sqliteTmpDir.exists()) {
            sqliteTmpDir.mkdirs()
        }
        kaptProcessJvmArgs.add("-Dorg.sqlite.tmpdir=${sqliteTmpDir.absolutePath}")
        kaptProcessJvmArgs.add("-Djava.io.tmpdir=${sqliteTmpDir.absolutePath}")
    }
}
