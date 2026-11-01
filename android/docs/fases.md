# Caliu · fases

Es treballa a passos petits i cada fase es valida abans de començar la següent. Les referències a captures apunten a
`disseny/captures/`.

Llegenda: ☐ pendent · ◐ en curs · ☑ fet

## Fase 0 · Fonaments

- ☑ Textos: posar `disseny/android/strings.xml` a `values-ca/` i la traducció anglesa a `values/` (per defecte), amb
  `app_name` = «Caliu». Treure `values-es` i deixar `locales_config` amb `en` i `ca`.
- ☑ Keystore de debug compartida a `certs/debug.keystore`, i fer-la servir al `signingConfig` de debug.
- ☑ Fonts a `designsystem/src/main/res/font`: `roboto_flex.ttf` (es copia de Converses) i Nunito variable (de Google
  Fonts, amb llicència OFL).
- ☑ Icones Material Symbols Rounded com a vector drawables a `:designsystem`:
  `local_fire_department`, `pause`, `pause_circle`, `play_arrow`, `power_settings_new`, `settings`, `arrow_back`,
  `keyboard_arrow_down/up`, `remove`, `add`, `back_hand`, `battery_*`, `schedule`, `error`, `wifi_off`, `key`,
  `visibility/visibility_off` i `check_circle`. N'hi ha de farcides i de no farcides.
- ☑ Dependències al catàleg: Koin, Navigation 3, DataStore, kotlinx.serialization, OkHttp, coroutines i el plugin de
  captures. Abans d'afegir-les, cal mirar la versió estable a la documentació oficial. El plugin de captures només
  té alphas (0.0.1-alpha16, la mateixa que Converses).
- ☑ Paquets: `data/tuya`, `data/settings`, `domain`, `ui/home`, `ui/settings`, `ui/navigation`, `di`, `widget` i `tile`.

**Validació:** `./gradlew :app:assembleDebug ktlintCheck`.

## Fase 1 · Tema Caliu (`:designsystem`)

- ☑ Portar `disseny/android/Color.kt` a `theme/Color.kt`: `Mood`, `MoodPalette`, les paletes clara i fosca,
  `FixedColors` i `moodColorScheme`. També cal exposar els rols que no són de M3 (`onSurfacePast`, `heroVariant`, `dial`,
  `temporary`, `track`) per un `CompositionLocal` (`LocalCaliuColors`).
- ☑ `CaliuTheme(mood, darkTheme)`: `MaterialExpressiveTheme` amb `MotionScheme.expressive()`. El canvi de mood s'anima
  interpolant cada rol del `ColorScheme` en uns 300 ms. Es treu el color dinàmic de l'esquelet.
- ☑ Portar `disseny/android/Type.kt` amb les fonts reals: `CaliuType`, més un `Typography` de M3 amb Roboto Flex.
- ☑ Formes i espais: targetes de 32 i 24 dp, grups connectats (24/8 dp, separació de 2 dp), marge de 16 dp, 12 dp entre
  seccions i títols de secció amb 8 dp de separació i de sagnat.
- ☑ `CaliuPreviewTheme(mood)` i una galeria de previews: els 4 moods, en clar i fosc.

**Validació:** comparar la galeria amb `design/tokens.json` i amb les captures `01`, `03`, `05` i `06`.

## Fase 2 · Pantalla principal amb dades falses

Es fa sense xarxa. Un `FakeThermostatRepository` simula el termòstat en memòria (vegeu «Termòstat simulat» al
`README.md`), i les previews fan servir estats fixos (`HomePreviewData`).

- ☑ Model de domini: `ThermostatState` (els camps de `disseny/docs/01` § Model d'estat), `ScheduleSlot`, `Override`,
  `Mode` i `mood()`. Tests de `mood()`.
- ☑ `HomeUiState`: `Loading` · `Content(state, refreshing)` · `ApiError(message, openConsole)` · `Offline` ·
  `MissingConfig`.
- ☑ Components a `:designsystem`, cadascun amb previews:
  - `StatusChip`: un `Button` tertiary que obre un `DropdownMenuPopup` amb `DropdownMenuGroup` i ítems seleccionables.
  - `HeroTemperature`: un `Card` tertiaryContainer amb la xifra de 100 sp i «°C».
  - `SectionLabel`.
  - `ModeSelector`: un `ButtonGroup` amb `ToggleButton` connectats.
  - `SetpointControl`: botons `FilledTonalIconButton` de 72 dp i el valor. Té l'estat de pausa (atenuat, amb vora
    discontínua).
  - `QuickTemperatures`: un `ButtonGroup` amb 20°, 21° i 23°.
  - `TemporaryBanner`.
  - `TodaySchedule`: files compactes; el tram actiu porta −/+ petits.
  - `HomeFooter`: bateria i hora d'actualització.
  - `StatePanel`: per als errors i els estats buits.
  - `HomeSkeleton`.
- ☑ `HomeScreen` / `HomeContent`: un `Scaffold` amb un `TopAppBar` sense títol, el xip a l'slot del títol i la icona de
  configuració a les accions, dins un `PullToRefreshBox` amb un `LoadingIndicator` expressiu. Té un `SnackbarHost`.
- ☑ Estats Apagat i Pausa: controls desactivats (al 38 % d'opacitat quan està apagat); en pausa, el mode i la consigna
  que es recuperaran.
- ☑ Format: coma decimal i un decimal (`20,4 °C`), dreceres sense decimal (`21°`) i «—» quan està apagat.
- ☑ Tests de captures, un per a cada captura de la `01` a la `11`, en clar i fosc, més la pausa en manual (que no té
  captura). La `02` es queda només com a preview: layoutlib no pinta els popups.
- ☑ `HomeViewModel` mínim: llegeix l'estat en entrar, en estirar i en tornar-ho a provar. La resta d'accions arriben a
  la fase 4.

**Validació:** posar els tests de captures al costat de `disseny/captures/` (tenint en compte que les captures del
handoff fan servir Inter i icones aproximades).

## Fase 3 · Capa Tuya

- ☑ `TuyaSigner`: HMAC-SHA256 tal com diu la documentació oficial («Sign requests for Cloud authorization»). Té un test
  unitari amb l'exemple de la documentació i es contrasta amb `TuyaClient.swift`.
- ☑ `TuyaApi` sobre OkHttp:
  - token: obtenir-lo, guardar-lo i renovar-lo abans que caduqui;
  - `GET /v1.0/devices/{id}/status`;
  - `POST /v1.0/devices/{id}/commands` per a `switch` i `temp_set`;
  - `GET /v2.0/cloud/thing/{id}/shadow/properties` per a `work_state`, `mode` i `week_program_13_*`;
  - `POST …/shadow/properties/issue` per a `mode`.
- ☑ `ThermostatDpMap`: els codes, l'escala (×10) i els límits per defecte, tots en un sol lloc.
- ☑ `WeekProgramDecoder`: es porta de `ScheduleSlot.decode`. Té tests amb el valor base64 real del dispositiu i amb
  valors construïts amb el format.
- ☑ `TuyaError`: es tradueixen els errors de Tuya al català com a l'app de macOS (`28841002`, la prova d'IoT Core
  caducada, va a la consola), i els errors de xarxa passen a `Offline`.
- ☑ `TuyaThermostatRepository`: ajunta v1 i v2 en un `ThermostatState`, amb el mapatge de mode `home`/`auto`/`temporary`
  del `README.md`.
- ☑ Interruptor per triar el repositori: la debug fa servir el simulat per defecte i només connecta amb Tuya si es
  demana explícitament (`caliu.realThermostat=true` a `local.properties`). La release sempre fa servir el real.
- ☑ Comprovar amb el dispositiu, **només lectura** (2026-11-01, amb un script de GET): `battery_percentage` existeix
  (100); `mode` per v2 només admet `home`, `auto` i `temporary`, i no hi ha cap mode de vacances o eco. Resultats a la
  secció «Diferències» del `README.md`.

**Validació:** tests JVM, i una lectura real de l'estat des d'una pantalla de debug o un test manual.

## Fase 4 · Lògica de la pantalla principal

- ☑ `HomeViewModel`:
  - refresc cada 60 s amb l'app en primer pla (`repeatOnLifecycle(STARTED)`) i pull-to-refresh;
  - ordres optimistes que es reverteixen si fallen, amb el Snackbar «No s'ha pogut aplicar el canvi»;
  - debounce de 800 ms per a −/+;
  - límits `[min, max]`, amb els botons desactivats als extrems.
- ☑ Pausa i activa: es desa `{mode, setpoint}` a DataStore, es passa a `home` i la consigna queda a
  `max(min, floor((current − 1) × 2) / 2)`. En sortir de la pausa, es restaura el que s'havia desat. Apaga i Activa
  envien `switch`.
- ☑ Automàtic: el tram actiu es recalcula cada minut. Tocar −/+ al tram actiu envia `temp_set`, i el dispositiu passa a
  `temporary`. «Tornar al programa» envia `mode = auto`.
- ☑ Tests del ViewModel amb un repositori fals i `MainDispatcherRule`.
- ☐ **Abans de provar cap escriptura al dispositiu real, cal demanar-ho a l'usuari.**

**Traspàs (2026-11-01, sessió aturada pel límit):**

- Fet i amb tests (66 tests JVM verds, ktlint i `assembleDebug` correctes): `ThermostatController` (`domain/`), el cas
  d'ús compartit amb la rajola (pausa, activa, apaga, mode i consigna); `PauseStore` amb `DataStorePauseStore`; el
  `HomeViewModel` amb ordres optimistes, debounce de 800 ms, refresc cada 60 s amb `repeatOnLifecycle(STARTED)`,
  resincronització 2 s després de cada ordre i l'esdeveniment `CommandFailed` per al Snackbar.
- Com l'app de macOS, la pausa desa la consigna baixa que posa i, si el dispositiu ja no la té (algú l'ha canviada des
  de fora), la pausa s'abandona.
- Pendent: provar a mà a l'emulador amb el termòstat **fals** (la debug el fa servir per defecte) cada acció del menú i
  dels controls. S'havia instal·lat l'APK però no s'havia començat la prova. Després, amb permís de l'usuari, provar
  les escriptures al dispositiu real (`caliu.realThermostat=true` a `local.properties`).

**Validació:** tests JVM, i provar a mà, amb permís, cada acció del menú i dels controls.

## Fase 5 · Configuració

- ☐ `CredentialsStore`: DataStore amb el valor xifrat amb AES/GCM d'Android Keystore. Conté Access ID, Access Secret,
  regió (EU per defecte) i Device ID.
- ☐ `SettingsScreen`: `LargeFlexibleTopAppBar` amb `arrow_back` i el botó «Desa»; els camps `TextField` connectats; el
  secret amb `SecureTextField` i el botó de mostrar/amagar; la regió amb un `ButtonGroup` EU · US · CN · IN.
- ☐ «Provar connexió»: un `FilledTonalButton` que mostra un `LoadingIndicator` petit mentre prova i després el resultat,
  correcte o error amb la causa (captures `13` i `14`).
- ☐ «Desa» torna a la pantalla principal i força un refresc. Sense credencials es mostra l'estat «Falta la configuració»,
  amb el botó «Configura».
- ☑ Precarregar les credencials en debug: `BuildConfig` llegeix `TUYA_*` de les variables d'entorn (GitHub Secrets al CI) o
  de `local.properties`, igual que la funció `secret()` de Converses. Només a la debug. (Fet a la fase 3, per poder fer
  la lectura real.)
- ☐ Passar els secrets `TUYA_*` com a `env` al pas de build de `android-build-apk.yml`.
- ☐ Tests de captures `12`–`14`.

**Validació:** desar, matar l'app i tornar-la a obrir: les credencials s'han de mantenir i el secret no ha de sortir en
text pla al DataStore.

## Fase 6 · Widgets (Glance)

- ☐ Un `GlanceTheme` amb `ColorProviders` derivats de `moodPalette` (el color hero del mood).
- ☐ Widget petit 2×1: la icona d'estat dins un quadrat arrodonit i la temperatura actual.
- ☐ Widget mitjà 4×2: la temperatura, el xip d'estat, −/+ amb la consigna (amb `ActionCallback`) i el tram actiu quan
  està en automàtic.
- ☐ Actualització: en cada refresc de l'app, i amb WorkManager periòdic de 15 min. Tocar el widget obre l'app.

**Validació:** comparar amb la captura `15` en clar i fosc, i comprovar que −/+ des del widget funciona (amb permís).

## Fase 7 · Rajola de configuració ràpida

- ☐ `TileService`: el títol és «Termòstat» («Thermostat» en anglès) i el subtítol, «20,4 °C · Escalfant», «… · En pausa» o «Apagat».
- ☐ Tocar-la alterna la pausa, no l'encesa. Activa correspon a `STATE_ACTIVE` i en pausa a `STATE_INACTIVE`.
- ☐ Fa servir la mateixa lògica de pausa que el ViewModel, extreta a un cas d'ús compartit.
- ☐ Oferir afegir la rajola des de l'app amb `StatusBarManager.requestAddTileService` (Android 13+), per exemple des de
  Configuració, perquè no s'hagi d'anar a editar el panell a mà.

**Validació:** captura `15`, i alternar la pausa des de la rajola (amb permís).

## Fase 8 · Distribució (Firebase App Distribution)

Es concretarà més endavant.

- ☐ Keystore de release fora del repo, amb els secrets al CI.
- ☐ Un workflow manual que construeix la release i la puja a App Distribution.
- ☐ Afegir `testDebugUnitTest` i la validació de captures al CI quan hi hagi tests.

## Pendent: refresc més freqüent

L'app de Tuya rep els canvis en temps real (MQTT/Pulsar del núvol), però el nostre projecte d'OpenAPI no ho té a l'abast d'una app. En lloc d'això, mentre l'app sigui en primer pla, consultarà l'estat cada pocs segons (ara és cada 60 s, `HomeViewModel.REFRESH_INTERVAL`) per veure els canvis fets des de fora (botons físics, Smart Life). Cal vigilar la quota de crides de l'OpenAPI abans de triar l'interval.
