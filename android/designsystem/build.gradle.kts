plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.alorma.caliu.designsystem"
  compileSdk =
    libs.versions.android.compileSdk
      .get()
      .toInt()

  defaultConfig {
    minSdk =
      libs.versions.android.minSdk
        .get()
        .toInt()
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
}

dependencies {
  api(platform(libs.androidx.compose.bom))
  api(libs.androidx.compose.ui)
  api(libs.androidx.compose.foundation)
  api(libs.androidx.compose.material3)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(libs.androidx.compose.ui.tooling.preview)
}
