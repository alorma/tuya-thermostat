package com.alorma.caliu.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.alorma.caliu.BuildConfig
import com.alorma.caliu.data.fake.FakeThermostatRepository
import com.alorma.caliu.data.settings.CredentialsStore
import com.alorma.caliu.data.settings.DataStoreCredentialsStore
import com.alorma.caliu.data.settings.DataStorePauseStore
import com.alorma.caliu.data.settings.KeystoreSecretCipher
import com.alorma.caliu.data.tuya.ConnectionTester
import com.alorma.caliu.data.tuya.TuyaApi
import com.alorma.caliu.data.tuya.TuyaConfigProvider
import com.alorma.caliu.data.tuya.TuyaConnectionTester
import com.alorma.caliu.data.tuya.TuyaErrors
import com.alorma.caliu.data.tuya.TuyaThermostatRepository
import com.alorma.caliu.data.tuya.preloadedTuyaConfig
import com.alorma.caliu.domain.PauseStore
import com.alorma.caliu.domain.ThermostatController
import com.alorma.caliu.domain.ThermostatRepository
import com.alorma.caliu.ui.home.HomeViewModel
import com.alorma.caliu.ui.settings.SettingsViewModel
import java.time.Clock
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private const val TIMEOUT_SECONDS = 15L

private val Context.credentialsDataStore: DataStore<Preferences> by preferencesDataStore(
  name = DataStoreCredentialsStore.FILE_NAME,
)

private val dataModule =
  module {
    single<Clock> { Clock.systemDefaultZone() }
    single {
      OkHttpClient
        .Builder()
        .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    }
    single<CredentialsStore> {
      DataStoreCredentialsStore(
        dataStore = androidContext().credentialsDataStore,
        cipher = KeystoreSecretCipher(),
        preloaded = preloadedTuyaConfig(),
      )
    }
    single<TuyaConfigProvider> { get<CredentialsStore>() }
    single { TuyaApi(client = get(), configProvider = get(), clock = get()) }
    single { TuyaErrors(androidContext()) }
    single<ConnectionTester> { TuyaConnectionTester(client = get(), clock = get(), errors = get()) }
    // La debug fa servir el termòstat simulat si no es posa `caliu.realThermostat=true` a local.properties, perquè
    // les proves no toquin la calefacció de casa. La release sempre parla amb Tuya
    single<ThermostatRepository> {
      if (BuildConfig.REAL_THERMOSTAT) {
        TuyaThermostatRepository(api = get(), errors = get(), clock = get())
      } else {
        FakeThermostatRepository(clock = get())
      }
    }
    single<PauseStore> { DataStorePauseStore(androidContext()) }
    single { ThermostatController(repository = get(), pauseStore = get()) }
  }

private val uiModule =
  module {
    viewModel { HomeViewModel(controller = get(), clock = get(), configSaved = get<CredentialsStore>().saved) }
    viewModel { SettingsViewModel(store = get(), tester = get()) }
  }

val appModules: List<Module> = listOf(dataModule, uiModule)
