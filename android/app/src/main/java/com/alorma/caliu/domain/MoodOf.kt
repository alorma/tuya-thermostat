package com.alorma.caliu.domain

import com.alorma.caliu.designsystem.theme.Mood

/** Estat visual de la pantalla (`disseny/docs/01-especificacio.md` § Estat visual). */
fun ThermostatState.mood(): Mood =
  when {
    !power -> Mood.OFF
    paused -> Mood.PAUSE
    heating -> Mood.HEAT
    else -> Mood.CALM
  }
