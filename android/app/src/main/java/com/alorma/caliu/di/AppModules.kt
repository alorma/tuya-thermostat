package com.alorma.caliu.di

import com.alorma.caliu.data.fake.FakeThermostatRepository
import com.alorma.caliu.domain.ThermostatRepository
import com.alorma.caliu.ui.home.HomeViewModel
import java.time.Clock
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private val dataModule =
  module {
    single<Clock> { Clock.systemDefaultZone() }
    // Termòstat simulat: no fa cap crida a Tuya. El repositori real arriba a la fase 3 i a la debug només s'hi
    // connectarà si es demana explícitament
    single<ThermostatRepository> { FakeThermostatRepository(clock = get()) }
  }

private val uiModule =
  module {
    viewModelOf(::HomeViewModel)
  }

val appModules: List<Module> = listOf(dataModule, uiModule)
