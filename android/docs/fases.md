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
  - refresc amb l'app en primer pla (`repeatOnLifecycle(STARTED)`) i pull-to-refresh. Al principi era cada 60 s; ara
    és cada 15 s (vegeu «Refresc més freqüent»);
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

- ☑ Selector de tema (Clar · Fosc · Sistema, per defecte Sistema), com a Caducity i Converses. Es desa a DataStore
  (`ThemePreferences`) i s'aplica amb `UiModeManager.setApplicationNightMode`, que el sistema recorda i que també
  segueixen les barres i els diàlegs. Els widgets no: van amb el tema del dispositiu, com la resta del launcher.

- ☑ `CredentialsStore`: DataStore amb el valor xifrat amb AES/GCM d'Android Keystore. Conté Access ID, Access Secret,
  regió (EU per defecte) i Device ID.
- ☑ `SettingsScreen`: `LargeFlexibleTopAppBar` amb `arrow_back` i el botó «Desa»; els camps `TextField` connectats; el
  secret amb `SecureTextField` i el botó de mostrar/amagar; la regió amb un `ButtonGroup` EU · US · CN · IN.
- ☑ «Provar connexió»: un `FilledTonalButton` que mostra un `LoadingIndicator` petit mentre prova i després el resultat,
  correcte o error amb la causa (captures `13` i `14`).
- ☑ «Desa» torna a la pantalla principal i força un refresc. Sense credencials es mostra l'estat «Falta la configuració»,
  amb el botó «Configura».
- ☑ Precarregar les credencials en debug: `BuildConfig` llegeix `TUYA_*` de les variables d'entorn (GitHub Secrets al CI) o
  de `local.properties`, igual que la funció `secret()` de Converses. Només a la debug. (Fet a la fase 3, per poder fer
  la lectura real.)
- ☑ ~~Passar els secrets `TUYA_*` al CI~~. Es va fer i després es va treure: el repo passa a ser públic i l'APK del CI
  no ha de portar credencials (2026-11-02).
- ☑ Tests de captures `12`–`14`, més la prova en curs (`12b`, sense captura al handoff).

**Notes (2026-11-02):**

- La capa de Tuya passa a Retrofit (`TuyaService` + `TuyaSigningInterceptor`), a petició de l'usuari. `TuyaApi` manté la
  mateixa interfície i els tests de la fase 3 passen igual.
- Tota la configuració es desa xifrada en un sol valor. Mentre no s'ha desat res, es fan servir les precarregades del
  `BuildConfig` (debug); un cop desat, mana el que s'ha desat, encara que sigui incomplet («Falta la configuració»).
- «Provar connexió» fa un `GET …/status` amb les credencials del formulari, sense desar-les. Com que només llegeix, es fa
  contra Tuya fins i tot amb el termòstat simulat de la debug.
- «Desa» avisa per `CredentialsStore.saved` i el `HomeViewModel` torna a llegir (també des d'un error o de «Falta la
  configuració»).
- Validat a l'emulador (2026-11-02, amb el termòstat simulat i les credencials reals, només lectura): «Provar connexió»
  correcta amb EU (la signatura amb Retrofit funciona contra Tuya) i error amb US (28841107, ara traduït com a «regió
  equivocada»). Després de desar i matar l'app, les credencials hi continuen, i `credentials.preferences_pb` (237 bytes)
  no conté cap dels tres valors en clar.
- Trobat i arreglat en provar-ho: el títol de 40 sp es partia quan la barra es plegava (ara va pel rol `displaySmall` i
  plegada fa servir `titleLarge`), i Android oferia desar l'Access Secret al gestor de contrasenyes (ara es cancel·la
  l'autocompletat en sortir de Configuració).
- Per mirar: `uiautomator dump` mostra el text de l'Access Secret al node d'accessibilitat del `SecureTextField`.

**Validació:** desar, matar l'app i tornar-la a obrir: les credencials s'han de mantenir i el secret no ha de sortir en
text pla al DataStore.

## Fase 6 · Widgets (Glance)

- ☑ Un `GlanceTheme` amb `ColorProviders` derivats de `moodPalette` (el color hero del mood).
- ☑ Widget petit 2×1: la icona d'estat dins un quadrat arrodonit i la temperatura actual.
- ☑ Widget mitjà 4×2: la temperatura, el xip d'estat, −/+ amb la consigna (amb `ActionCallback`) i el tram actiu quan
  està en automàtic.
- ☑ Actualització: en cada refresc de l'app, i amb WorkManager periòdic de 15 min. Tocar el widget obre l'app.

**Notes (2026-11-02):**

- Cada lectura de l'estat (la de l'app, la del refresc periòdic, la de la rajola o la del widget) passa per
  `ThermostatController`, que l'envia a un `ThermostatObserver`. El `WidgetUpdater` la desa a `SnapshotStore` (DataStore)
  i torna a pintar els widgets, que sempre es pinten amb aquest últim estat i no esperen la xarxa.
- WorkManager (15 min, amb xarxa) s'engega amb el primer widget i s'atura amb l'últim. Quan s'afegeix un widget també
  es fa una lectura immediata.
- −/+ del widget (`StepSetpointAction`): canvi optimista, envia `temp_set` sense debounce (cada toc és una ordre), torna
  enrere si falla i torna a llegir al cap de 2 s. Apagat o en pausa no fan res.
- `withTarget` i `steppedTarget` són ara al domini, compartits amb el `HomeViewModel`.
- Previews de Glance a `WidgetPreviews.kt`. No hi ha tests de captures: el plugin de Compose no pinta Glance.
- Pendent per a la prova final: afegir els dos widgets a l'emulador i comparar-los amb la captura `15` en clar i fosc.

**Validació:** comparar amb la captura `15` en clar i fosc, i comprovar que −/+ des del widget funciona (amb permís).

## Fase 7 · Rajola de configuració ràpida

- ☑ `TileService`: el títol és «Termòstat» («Thermostat» en anglès) i el subtítol, «20,4 °C · Escalfant», «… · En pausa» o «Apagat».
- ☑ Tocar-la alterna la pausa, no l'encesa. Activa correspon a `STATE_ACTIVE` i en pausa a `STATE_INACTIVE`.
- ☑ Fa servir la mateixa lògica de pausa que el ViewModel, extreta a un cas d'ús compartit.
- ☑ Oferir afegir la rajola des de l'app amb `StatusBarManager.requestAddTileService` (Android 13+), per exemple des de
  Configuració, perquè no s'hagi d'anar a editar el panell a mà.

**Notes (2026-11-02):**

- `ThermostatTileService` (no és una rajola activa: el sistema la consulta cada vegada que s'obre el panell). Primer es
  pinta amb l'estat de `SnapshotStore` i després es torna a llegir el termòstat.
- Tocar-la crida `ThermostatController.togglePause()`, la mateixa pausa que el menú d'estat. El canvi és optimista i es
  desfà si l'ordre falla. Corre a l'scope de l'app (tancar el panell no el talla) i es torna a llegir al cap de 2 s,
  quan el núvol ja l'ha registrat. Mentre el panell és obert la rajola escolta `SnapshotStore`, com els widgets.
- Mantenir-la premuda obre l'app (`ACTION_QS_TILE_PREFERENCES` a `MainActivity`). Els dos punts de toc de la rajola
  de Dades mòbils (icona i resta) són exclusius de les rajoles del sistema: `CustomTile` no activa mai
  `handlesSecondaryClick`.
- Apagat: `STATE_UNAVAILABLE` amb el subtítol «Apagat», perquè la rajola no encén el termòstat. Sense dades encara:
  `STATE_INACTIVE` i cap subtítol.
- A Configuració, el botó «Afegeix la rajola al panell ràpid» (només a Android 13+) crida `requestAddTileService`.
- Pendent per a la prova final: afegir la rajola, mirar-ne el subtítol en cada estat i alternar la pausa amb el
  termòstat simulat.

**Validació:** captura `15`, i alternar la pausa des de la rajola (amb permís).

## Fase 8 · Distribució (CI de GitHub)

Decisió de l'usuari (2026-11-02): no hi ha release, ni botiga, ni Firebase. Caliu es distribueix com l'APK de debug
que construeix el CI.

- ☑ `android-build-apk.yml`, a cada push a `main`: ktlint, tests JVM, validació de captures i `assembleDebug`. L'APK es
  puja com a artifact `caliu-debug`. **No porta credencials:** el repo és públic i qualsevol pot baixar els artifacts.
  S'entren una sola vegada a Configuració.
- ☑ Com que la keystore de debug és compartida (`certs/debug.keystore`), cada APK nou s'instal·la a sobre de l'anterior.
- ☑ Els tests i les captures també corren a les PR (`android-pr-build.yml`).

- ☑ L'APK del CI parla amb el termòstat **real** si la variable del repo `CALIU_REAL_THERMOSTAT` és `true` (ja posada). La debug local continua
  fent servir el simulat si no es posa `caliu.realThermostat=true` a `local.properties`. Les PR construeixen sense
  secrets ni termòstat real.

## Refresc més freqüent

- ☑ L'app de Tuya rep els canvis en temps real (MQTT/Pulsar del núvol), però el nostre projecte d'OpenAPI no ho té a
  l'abast d'una app. En lloc d'això, amb l'app en primer pla es llegeix l'estat cada 15 s
  (`HomeViewModel.REFRESH_INTERVAL`), i després de cada ordre cada 2 s fins que canvia `heating` (30 s com a màxim).
  Fet el 2026-11-02 (`e7ea9a8`).

## Fase 9 · Programa setmanal

Nova pantalla del handoff (2026-11-02): `disseny/docs/01` § Programa setmanal, `disseny/docs/02` (files «Programa · …»)
i les captures `19-programa-edit`, `-copy`, `-time` i `-copied`, en clar i fosc. Els textos nous són a
`disseny/android/strings.xml` (`schedule_*`, `weekday_short`, `weekday_long`).

Escriu el programa del termòstat real, que és el que fa anar la calefacció de casa. El 2026-11-01 un byte de més ja el
va corrompre: **cap escriptura al dispositiu real sense permís de l'usuari**, i després de desar cal mirar-lo a Smart Life.

- ☑ Pantalla principal: el botó de text «Edita» a la dreta del títol «Programa d'avui», només en mode Automàtic. Les
  captures `01`, `02` i `04`–`07` han canviat per això: cal tornar a generar-ne els tests de captures.
- ☑ `WeekProgramEncoder`: l'invers de `WeekProgramDecoder`, 25 bytes `[dia, (h, min, temp×10 u16 BE)×6]`, **sense byte de
  recompte**. Tests d'anada i tornada amb el valor real del dispositiu, i que la mida sigui sempre 25.
- ☑ `WeekProgramDecoder` torna els **6 trams reals** del dia, sense afegir-ne cap a les 00:00. Ara se'n posa un de
  virtual a les 00:00 (`WeekProgramDecoder.kt:38`) amb la consigna de l'últim tram **d'avui**. El tram de la
  matinada (de 00:00 al primer tram) l'ha de calcular el domini amb l'últim tram **del dia anterior** (dilluns
  agafa el de diumenge): a «Programa d'avui», al tram actiu, al widget, a la barra de 24 h i al termòstat
  simulat. Ara no es nota perquè els 7 dies són iguals. Tests amb dies diferents.
- ☑ `TuyaApi` / `ThermostatRepository`: llegir els 7 dies (`week_program_13_1` … `_7`, ja es llegeixen per al programa
  d'avui) i escriure'n un per `shadow/properties/issue`. Al `FakeThermostatRepository`, desar-lo en memòria (amb
  `failure` per provar l'error d'un dia).
- ☑ `ScheduleViewModel`: còpia de treball dels 7 dies; dia seleccionat (avui en obrir); dies modificats; −/+ a passos de
  0,5 °C dins `[min, max]`; hora de cada tram entre la de l'anterior i la del següent; copiar un dia a d'altres;
  desar només els modificats, una ordre per dia, i deixar marcat el que falli.
- ☑ Components a `:designsystem` amb previews: selector de dia (7 `ToggleButton` connectats, anell a avui, punt als
  modificats), barra de 24 h (segments proporcionals, 5 nivells de color de `heat-hero` a `heat`, marques 0·6·12·18·24)
  i la fila de tram (hora en píndola tonal, temperatura amb −/+).
- ☑ `ScheduleScreen`: `LargeFlexibleTopAppBar` amb «Desa» (com Configuració), `TimePickerDialog` 24 h,
  `ModalBottomSheet` «Copia el … a» (dies en selecció múltiple, dreceres Feiners · Cap de setmana · Tots), Snackbar
  «Copiat a N dies» i `AlertDialog` «Descartar els canvis?» en sortir amb canvis. Ruta nova a Navigation 3.
- ☑ Textos `schedule_*` a `values-ca/` i la traducció a `values/` (anglès).
- ☑ Tests del ViewModel i tests de captures `19-programa-*` (el full inferior i el selector d'hora, si layoutlib no els
  pinta, com a previews, igual que la `02`).
- ☐ Desar, amb permís, un canvi petit al dispositiu real, tornar-lo a llegir i comprovar-lo a Smart Life.

**Decidit** (el handoff ho deixa «a verificar amb el dispositiu»):

- Trams per dia: el format en té sempre **6**, no 4.
- Dies: el dispositiu desa **cada dia per separat** (7 DPs) i no agrupa feiners / cap de setmana, així que el selector
  mostra els 7 dies.
- Primer tram (decisió de l'usuari, 2026-11-02): es mostren els 6 trams del dispositiu i **tots tenen l'hora editable**,
  de 00:00 a 23:59 (el programa real comença a les 06:15). Cada hora ha d'anar entre la del tram anterior i la del
  següent. De 00:00 al primer tram s'aplica l'últim tram del dia anterior, i la barra de 24 h pinta aquest tros amb el
  color de l'últim tram del dia anterior. No hi ha cap tram fix a les 00:00 com al handoff, però un tram pot començar
  a les 00:00 si es vol.

**Notes (2026-11-02):**

- Domini: `ProgramEntry` (inici + consigna) i `WeekProgram` (els 7 dies). `daySchedule(today, previous)` fa els trams
  de 00:00 a 24:00 i, si el primer no comença a les 00:00, hi posa el tram de matinada amb l'últim tram del dia
  anterior. Ho fan servir el mapper de Tuya (que ara llegeix el programa d'avui i el d'ahir), el simulat, el widget i
  la barra de 24 h.
- `WeekProgramEncoder` comprova abans d'enviar res que hi hagi 6 trams en ordre estricte d'hora, el dia d'1 a 7 i la
  temperatura dins d'uint16. Torna a codificar exactament el valor real del dispositiu (test).
- `ThermostatRepository.weekProgram()` llegeix els 7 DPs en una sola crida de v2; `setDayProgram` n'escriu un per
  `shadow/properties/issue`. El simulat ho desa en memòria (`failingDays` per provar l'error d'un dia) i a la debug el
  cap de setmana té un programa diferent, perquè es vegi el tram de matinada del dissabte, el diumenge i el dilluns.
- `ScheduleViewModel` no deixa editar si algun dia no té 6 trams (estat «No es pot llegir el programa»). Desar envia
  els dies modificats de dilluns a diumenge; si n'ha desat algun, `ProgramChanges` avisa la principal, que torna a
  llegir. Mentre desa, el botó «Desa» mostra un `LoadingIndicator` i no es pot editar.
- La barra de 24 h acoloreix per trams fixos de temperatura (≤18 °C neutre, 19, 20, 21, ≥21,5 °C), perquè un valor
  tingui el mateix color tots els dies.
- «Edita» surt sempre en automàtic, també apagat o en pausa: el programa es pot preparar igualment.
- El full «Copia el … a» i el selector d'hora es pinten a mà a les captures (`19-programa-copy`, `-time`), perquè
  layoutlib no pinta les finestres. A l'app són `ModalBottomSheet` i `TimePickerDialog` amb `TimeInput`.
- Pendent per a la prova final: a l'emulador amb el simulat, editar hores i temperatures, copiar, desar (també amb un
  dia que falli), sortir amb canvis i comprovar que la principal mostra el programa nou. Després, **amb permís**, un
  canvi petit al dispositiu real comprovat a Smart Life.

**Validació:** tests JVM i de captures, ktlint, provar-ho a l'emulador amb el termòstat simulat i, amb permís, una
escriptura real comprovada a Smart Life.

## Fora de l'abast, de moment

El handoff nou també porta Wear OS (`disseny/docs/04-wear-i-auto.md`, captura `16`), Android Auto (captura `17`) i
l'estructura de mòduls `:core` / `:app` / `:wear` / `:car`. Segueixen fora de l'abast fins que l'usuari ho decideixi.
La icona (captura `18`, `disseny/android/icona/`) ja es va fer (`e277996`, amb la marca DEV a la debug a `94441fe`).
