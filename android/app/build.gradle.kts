import com.android.compose.screenshot.tasks.PreviewScreenshotValidationTask

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.compose.screenshot)
}

android {
  namespace = "com.alorma.caliu"
  compileSdk =
    libs.versions.android.compileSdk
      .get()
      .toInt()

  defaultConfig {
    applicationId = "com.alorma.caliu"
    minSdk =
      libs.versions.android.minSdk
        .get()
        .toInt()
    targetSdk =
      libs.versions.android.targetSdk
        .get()
        .toInt()
    versionCode = 1
    versionName = "0.1.0"
  }

  // Keystore de debug compartida (al repo): l'APK de debug sempre té la mateixa signatura,
  // es construeixi on es construeixi, i es pot instal·lar a sobre sense esborrar l'app
  signingConfigs {
    getByName("debug") {
      storeFile = rootProject.file("certs/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    debug {
      signingConfig = signingConfigs.getByName("debug")
      // Permet tenir instal·lades a la vegada la versió de debug i la de release
      applicationIdSuffix = ".dev"
    }
    release {
      optimization {
        enable = true
      }
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

// El plugin de captures no té DSL per al llindar des d'AGP 9; es posa a la tasca de validació
tasks.withType<PreviewScreenshotValidationTask>().configureEach {
  testEngineInput.threshold.set(0.01f)
}

dependencies {
  implementation(project(":designsystem"))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
  implementation(libs.androidx.datastore.preferences)
  implementation(platform(libs.koin.bom))
  implementation(libs.koin.android)
  implementation(libs.koin.compose)
  implementation(libs.koin.compose.viewmodel)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.android)
  implementation(platform(libs.okhttp.bom))
  implementation(libs.okhttp)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(libs.androidx.compose.ui.tooling.preview)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.okhttp.mockwebserver)
  screenshotTestImplementation(libs.compose.screenshot.validation)
  screenshotTestImplementation(platform(libs.androidx.compose.bom))
  screenshotTestImplementation(libs.androidx.compose.ui)
  screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
