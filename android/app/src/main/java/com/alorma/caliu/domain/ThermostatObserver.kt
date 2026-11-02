package com.alorma.caliu.domain

/**
 * Rep cada estat llegit del termòstat, vingui d'on vingui (pantalla principal, widgets, rajola, refresc periòdic).
 * Ha de tornar de seguida: la feina llarga es fa en una altra corutina.
 */
fun interface ThermostatObserver {
  fun onState(state: ThermostatState)
}
