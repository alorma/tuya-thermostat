package com.alorma.caliu.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Avisa quan s'ha desat el programa setmanal, perquè la pantalla principal torni a llegir el programa d'avui. */
class ProgramChanges {
  private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  val saved: Flow<Unit> = _saved.asSharedFlow()

  fun notifySaved() {
    _saved.tryEmit(Unit)
  }
}
