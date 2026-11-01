import com.android.compose.screenshot.tasks.PreviewScreenshotValidationTask

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.compose.screenshot)
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

  experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

// El plugin de captures no té DSL per al llindar des d'AGP 9; es posa a la tasca de validació
tasks.withType<PreviewScreenshotValidationTask>().configureEach {
  testEngineInput.threshold.set(0.01f)
}

dependencies {
  api(platform(libs.androidx.compose.bom))
  api(libs.androidx.compose.ui)
  api(libs.androidx.compose.foundation)
  api(libs.androidx.compose.material3)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(libs.androidx.compose.ui.tooling.preview)
  testImplementation(libs.junit)
  screenshotTestImplementation(libs.compose.screenshot.validation)
  screenshotTestImplementation(platform(libs.androidx.compose.bom))
  screenshotTestImplementation(libs.androidx.compose.ui)
  screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
