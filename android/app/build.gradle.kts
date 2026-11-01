plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
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

  buildTypes {
    debug {
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
}

dependencies {
  implementation(project(":designsystem"))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(libs.androidx.compose.ui.tooling.preview)
  testImplementation(libs.junit)
}
