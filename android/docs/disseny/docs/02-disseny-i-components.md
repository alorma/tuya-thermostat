# 02 · Disseny i components (Caliu · Material 3 Expressive)

## Principis

- **El color diu l'estat.** Tota la pantalla canvia de paleta segons el mood: **escalfant** ambre, **en repòs** verd aigua (vol dir «ja és a la consigna», mai «fred»), **en pausa** lila grisós, **apagat/errors/configuració** neutre càlid.
- **Terracota per actuar** (`primary`): Desa, Configura, opció seleccionada.
- **Vermell només per a errors.**
- **Sense etiquetes supletòries.** Cap subtítol sota les xifres. L'única ajuda estructural és un títol petit damunt de cada secció.
- **Una mà, una pantalla.** Objectius tàctils ≥ 48 dp; controls principals a la meitat inferior.
- **Res de soroll.** Sense gradients; ombra només en menús i indicador de refresc.

## Tema

- `MaterialExpressiveTheme` amb el `ColorScheme` que retorna `moodColorScheme(mood, dark)` d'`android/Color.kt`. Animar el canvi de scheme (`animateColorAsState` per rol o un `ColorScheme` interpolat).
- Sense color dinàmic (Material You): la paleta depèn de l'estat, no del fons de pantalla. Segueix el tema clar/fosc del sistema.
- Mapatge de rols:

| Rol Caliu | Rol `ColorScheme` | Ús |
|---|---|---|
| fons del mood | `background`, `surface` | Fons de pantalla |
| targeta | `surfaceContainerLow` | Targetes de consigna, programa, configuració |
| botó no seleccionat | `surfaceContainerHigh` | Botons no seleccionats, −/+ |
| hero | `tertiaryContainer` / `onTertiaryContainer` | Targeta de temperatura, tram actiu, widgets |
| accent d'estat | `tertiary` / `onTertiary` | Xip d'estat, rajola activa |
| acció | `primary`, `primaryContainer` | Seleccions, Desa |
| error | `error`, `errorContainer` | Errors |

## Tipografia

`android/Type.kt`. **Roboto Flex** per a la interfície (títols a 800 amb eix d'amplada `wdth 118`), **Nunito 800** només per a xifres de temperatura, sempre amb `fontFeatureSettings = "tnum"`.

| Estil | Mida/alçada | Ús |
|---|---|---|
| `tempHero` | 100/100, Nunito 800, −3 sp | Temperatura actual |
| `tempValue` | 40/44, Nunito 800 | Consigna |
| `tempWidget` | 34/36, Nunito 800 | Widgets |
| `titleLarge` (pantalla) | 40/44, Roboto Flex 800 wdth 118 | «Configuració» |
| `stateTitle` | 26/30, Roboto Flex 800 wdth 118 | Títols d'error i estats buits |
| `sectionLabel` | 14/20, 700, `onSurfaceVariant` | Títols de secció |
| `bodyLarge` | 17/24 | Valors de camps |
| `body` | 15/21 | Explicacions |
| `labelLarge` | 16/22, 700 | Botons |
| `bodySmall` | 13/18 | Peu |

## Formes i espais

- Targetes grans (hero, consigna, errors): `RoundedCornerShape(32.dp)`. Targetes de llista: 24 dp.
- Grups connectats: 24 dp a l'exterior, 8 dp a l'interior, 2 dp de separació. L'opció seleccionada passa a forma de píndola (comportament per defecte de `ButtonGroup` expressiu).
- Marge lateral 16 dp; 12 dp entre seccions; títol de secció a 8 dp de la seva targeta i sagnat 8 dp.
- Icones: **Material Symbols Rounded**, farcides quan indiquen estat.

## Components: quin M3 / M3E s'usa a cada lloc

> Regla: no s'inventen components. Si alguna API no existeix a la versió de `material3` triada, avisar abans de substituir-la.

| Element | Component Compose | Notes |
|---|---|---|
| Barra superior principal | `TopAppBar` (sense títol) | `navigationIcon` buit; el xip d'estat va a l'slot `title`; `actions` = `IconButton(settings)` |
| Xip d'estat (control) | `Button` amb `ButtonDefaults.buttonColors(containerColor = tertiary)`, icona inicial i `KeyboardArrowDown/Up` final | Alçada 48 dp, forma píndola. Obre el menú |
| Menú d'estat | `DropdownMenu` + `DropdownMenuItem` (variant expressiva amb ítem seleccionat i grups si la versió la té: `DropdownMenuGroup`) | Activa · Pausa · (separador) Apaga en `onSurfaceVariant` |
| Targeta de temperatura | `Card` (`CardDefaults.cardColors(tertiaryContainer)`) | Només la xifra + «°C» |
| Títols de secció | `Text` amb `sectionLabel` | |
| Mode Automàtic/Manual | `ButtonGroup` connectat amb `ToggleButton` (selecció única) | `ButtonGroupDefaults.connected*ButtonShapes()` |
| −/+ de consigna | `FilledTonalIconButton` mida gran (`IconButtonDefaults` sizes/shapes expressives, forma quadrada arrodonida) | 72 dp; repetició en mantenir premut opcional |
| Temperatures ràpides | `ButtonGroup` connectat amb `ToggleButton` | 3 opcions, una seleccionada si coincideix |
| Bàner Temporal | `Card` (colors hero d'escalfar) + `TextButton("Tornar al programa")` | |
| Programa d'avui | `Card` amb `Column` de `ListItem` (o files compactes) | Tram actiu: `ListItem` amb `trailingContent` = dos `FilledTonalIconButton` petits + valor |
| Peu | `Row` de `Text` + `Icon` | |
| Pull‑to‑refresh | `PullToRefreshBox` amb `PullToRefreshDefaults.LoadingIndicator` (expressiu) | |
| Càrrega inicial | Skeleton amb `Box`es de la mateixa forma; opcionalment `LoadingIndicator` | Captura `08` |
| Errors / estat buit | `Card` + `Icon` en contenidor 64 dp + `Button` / `FilledTonalButton` / `TextButton` | |
| Snackbar d'error d'ordre | `SnackbarHost` | «No s'ha pogut aplicar el canvi» |
| Configuració · barra | `LargeFlexibleTopAppBar` amb `arrow_back` i `Button("Desa")` | |
| Camps | `TextField` (filled) dins targetes connectades; secret amb `SecureTextField` o `TextField` + `VisualTransformation` i `IconButton` visibilitat | |
| Regió | `ButtonGroup` connectat amb `ToggleButton` | EU · US · CN · IN |
| Provar connexió | `FilledTonalButton` amb icona; resultat en `Card` (èxit: hero de «calm»; error: `errorContainer`) | Durant la prova: `LoadingIndicator` petit |
| Programa · barra | `LargeFlexibleTopAppBar` amb `arrow_back` i `Button("Desa")` | Com Configuració |
| Programa · selector de dia | `ButtonGroup` connectat amb 7 `ToggleButton` | Avui: vora `outline`; modificat: punt `primary` (`BadgedBox`/`Badge` petit) |
| Programa · barra de 24 h | `Canvas`/`Row` de `Box`es amb pesos proporcionals | Element visual, no interactiu |
| Programa · trams | `Card` amb `ListItem`: hora en `FilledTonalButton` píndola, temperatura amb `FilledTonalIconButton` −/+ | |
| Programa · hora | `TimePickerDialog` amb `TimePicker` (`is24Hour = true`) | |
| Programa · copiar | `FilledTonalButton` → `ModalBottomSheet` amb `ToggleButton`s de dia (multiselecció), `FilterChip`/`AssistChip` per a les dreceres, `TextButton` + `Button` | |
| Programa · confirmació de sortida | `AlertDialog` | |
| Widgets | Jetpack **Glance** (`GlanceAppWidget`, `GlanceTheme` amb `ColorProviders` derivats d'`Color.kt`) | Glance no té M3E: fer servir els seus `Box/Row/Text/Image`, `CircleIconButton`/`SquareIconButton` |
| Rajola | `TileService` | `Tile.label`, `Tile.subtitle`, `Tile.state` |

## Captures de referència

Vegeu `captures/INDEX.md`. Les captures fan servir tipografia i icones substitutes; les mides, colors, espais i jerarquia sí que són els definitius.
