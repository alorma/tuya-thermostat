import com.android.compose.screenshot.tasks.PreviewScreenshotValidationTask
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.compose.screenshot)
}

// Credencials de Tuya per a la debug: de `local.properties` en local i de les variables d'entorn (GitHub Secrets) al CI.
// Si no n'hi ha, queden buides i l'app mostra «Falta la configuració»
val localProperties =
  Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
  }

fun secret(name: String): String = System.getenv(name) ?: localProperties.getProperty(name).orEmpty()

fun String.quoted() = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

// La debug fa servir el termòstat simulat si no es demana explícitament el real, perquè les proves no toquin la
// calefacció de casa. Es demana amb `caliu.realThermostat=true` a `local.properties`
val realThermostatInDebug = localProperties.getProperty("caliu.realThermostat").toBoolean()

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

  buildFeatures {
    buildConfig = true
  }

  buildTypes {
    debug {
      signingConfig = signingConfigs.getByName("debug")
      buildConfigField("boolean", "REAL_THERMOSTAT", realThermostatInDebug.toString())
      buildConfigField("String", "TUYA_ACCESS_ID", secret("TUYA_ACCESS_ID").quoted())
      buildConfigField("String", "TUYA_ACCESS_SECRET", secret("TUYA_ACCESS_SECRET").quoted())
      buildConfigField("String", "TUYA_REGION", secret("TUYA_REGION").quoted())
      buildConfigField("String", "TUYA_DEVICE_ID", secret("TUYA_DEVICE_ID").quoted())
      // Permet tenir instal·lades a la vegada la versió de debug i la de release
      applicationIdSuffix = ".dev"
    }
    release {
      // La release sempre parla amb Tuya i mai porta credencials al BuildConfig
      buildConfigField("boolean", "REAL_THERMOSTAT", "true")
      buildConfigField("String", "TUYA_ACCESS_ID", "\"\"")
      buildConfigField("String", "TUYA_ACCESS_SECRET", "\"\"")
      buildConfigField("String", "TUYA_REGION", "\"\"")
      buildConfigField("String", "TUYA_DEVICE_ID", "\"\"")
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
