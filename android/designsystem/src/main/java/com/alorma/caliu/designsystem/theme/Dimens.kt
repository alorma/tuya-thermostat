package com.alorma.caliu.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Espais de `design/tokens.json`. */
object CaliuSpacing {
  // Entre botons d'un grup connectat i files del programa
  val joint = 2.dp

  // Icona-text dins de xips
  val xs = 4.dp

  // Entre botons independents; títol de secció a 8 dp de la targeta i sagnat 8 dp
  val s = 8.dp

  // Entre blocs de la pantalla principal
  val section = 12.dp

  // Marge lateral de pantalla i padding de targetes
  val screen = 16.dp

  // Marge del títol, separació entre grups de formulari
  val l = 20.dp

  // Padding de targetes d'estat i d'error
  val xl = 24.dp
}

/** Radis de `design/tokens.json`. */
object CaliuShapes {
  // Cantonades interiors d'un grup connectat
  val joint = RoundedCornerShape(8.dp)

  // Files del programa i tram actiu
  val chip = RoundedCornerShape(12.dp)

  // Botons −/+ grans, exteriors dels grups connectats, targetes de llista
  val control = RoundedCornerShape(24.dp)

  // Targeta de temperatura, consigna i targetes d'error
  val hero = RoundedCornerShape(32.dp)
}
