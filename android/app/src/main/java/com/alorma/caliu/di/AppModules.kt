package com.alorma.caliu.di

import com.alorma.caliu.BuildConfig
import com.alorma.caliu.data.fake.FakeThermostatRepository
import com.alorma.caliu.data.tuya.BuildConfigTuyaConfigProvider
import com.alorma.caliu.data.tuya.TuyaApi
import com.alorma.caliu.data.tuya.TuyaConfigProvider
import com.alorma.caliu.data.tuya.TuyaErrors
import com.alorma.caliu.data.tuya.TuyaThermostatRepository
import com.alorma.caliu.domain.ThermostatRepository
import com.alorma.caliu.ui.home.HomeViewModel
import java.time.Clock
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private const val TIMEOUT_SECONDS = 15L

private val dataModule =
  module {
    single<Clock> { Clock.systemDefaultZone() }
    single {
      OkHttpClient
        .Builder()
        .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    }
    single<TuyaConfigProvider> { BuildConfigTuyaConfigProvider() }
    single { TuyaApi(client = get(), configProvider = get(), clock = get()) }
    single { TuyaErrors(androidContext()) }
    // La debug fa servir el termòstat simulat si no es posa `caliu.realThermostat=true` a local.properties, perquè
    // les proves no toquin la calefacció de casa. La release sempre parla amb Tuya
    single<ThermostatRepository> {
      if (BuildConfig.REAL_THERMOSTAT) {
        TuyaThermostatRepository(api = get(), errors = get(), clock = get())
      } else {
        FakeThermostatRepository(clock = get())
      }
    }
  }

private val uiModule =
  module {
    viewModelOf(::HomeViewModel)
  }

val appModules: List<Module> = listOf(dataModule, uiModule)
