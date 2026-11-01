package com.alorma.caliu

import android.app.Application
import com.alorma.caliu.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CaliuApp : Application() {
  override fun onCreate() {
    super.onCreate()
    startKoin {
      androidContext(this@CaliuApp)
      modules(appModules)
    }
  }
}
