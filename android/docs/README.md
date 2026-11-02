# Caliu · pla de treball

App d'Android per controlar l'únic termòstat de casa (R11, Tuya Cloud). S'obre i ja es veu el termòstat: no hi ha
onboarding, comptes ni llista de dispositius.

- `disseny/` conté el paquet de Claude Design tal com va arribar: especificació, components, notes de Tuya, colors,
  tipografia, textos, tokens, icona i 40 captures. La versió actual és la del 2026-11-02, que hi
  afegeix el programa setmanal, Wear OS, Android Auto i la icona. **És la font de veritat del disseny.** No s'edita: si cal desviar-se'n,
  s'apunta a la secció «Diferències» d'aquí sota.
- `fases.md` recull les fases d'implementació, amb els passos i com es valida cadascuna.

## Abast

Està dins l'abast:

- La pantalla principal, amb tots els estats de les captures `01`–`11`.
- La pantalla de configuració (captures `12`–`14`).
- El programa setmanal (captures `19-programa-*`), des del botó «Edita» de la pantalla principal.
- La icona adaptativa (captura `18`).
- Dos widgets Glance (2×1 i 4×2) i una rajola de configuració ràpida (captura `15`).
- La distribució com a APK de debug des del CI de GitHub (artifact `caliu-debug`). No hi ha release ni botiga.

Queda fora de l'abast:

- Més d'un dispositiu, Google Play i Firebase App Distribution.
- Wear OS i Android Auto (captures `16` i `17`, `disseny/docs/04-wear-i-auto.md`), amb els mòduls `:core`, `:wear` i
  `:car` que hi proposa el handoff. Estan dissenyats però no s'han demanat.

## Decisions

| Tema | Decisió |
|---|---|
| Paquet | `com.alorma.caliu`. La debug és `com.alorma.caliu.dev` i el paquet `cat.termostat` del handoff s'adapta a aquest. |
| Mòduls | `:app` (pantalles, ViewModels, dades, widgets, rajola) i `:designsystem` (tema Caliu, tipografia, icones i components visuals). Tots els `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` van a `:designsystem`. |
| UI | Compose amb Material 3 Expressive (`material3` 1.5.0-beta01). Totes les API que demana `disseny/docs/02` hi existeixen (vegeu la llista d'API comprovades més avall). |
| Arquitectura | Una sola Activity, MVVM amb `StateFlow`, Navigation 3 (principal i configuració), Koin, i cada pantalla amb una versió sense estat (`…Content`) per a les previews i els tests de captures. És el mateix que fan Converses i Caducity. |
| Xarxa | Retrofit 3 sobre OkHttp, amb el convertidor de kotlinx.serialization (`JsonObject`, sense models). La signatura HMAC la fa `TuyaSigningInterceptor`, que rep les credencials i el token de cada petició com a `@Tag` de Retrofit i hi posa el host de la regió. El token es demana i es renova a `TuyaApi`. (Decisió de l'usuari del 2026-11-02: al principi era OkHttp sense Retrofit.) |
| Persistència | DataStore. Les credencials es desen com un sol valor xifrat amb una clau AES/GCM d'Android Keystore (`EncryptedSharedPreferences` està obsolet), i el fitxer s'exclou de les còpies de seguretat perquè la clau no es pot restaurar. |
| Tema | Sense color dinàmic: la paleta depèn del *mood* (escalfant, en repòs, en pausa, apagat), segueix el mode clar o fosc del sistema i el canvi s'anima. |
| Tipografia | Roboto Flex (la mateixa `.ttf` de Converses) i Nunito, totes dues a `res/font` amb llicència OFL. No es fan servir Downloadable Fonts. |
| Icones | Material Symbols Rounded, importades com a vector drawables a `:designsystem` (unes 15). `material-icons-extended` són Material Icons, no Symbols. |
| Tests | Tests JVM per a la signatura, el decodificador del programa, la fórmula de la pausa, el càlcul del mood i els ViewModels. Tests de captures de Compose per a cada captura del handoff, en clar i fosc. |
| Termòstat simulat | `FakeThermostatRepository` (a `data/fake`) implementa `ThermostatRepository` en memòria, sense xarxa: segueix el programa, crea canvis temporals que caduquen al tram següent, apaga, i la temperatura s'acosta a la consigna a cada lectura. Amb `failure` es força qualsevol error. La debug el fa servir per defecte, perquè les proves no toquin mai la calefacció: el repositori real (`TuyaThermostatRepository`) només s'hi connecta amb `caliu.realThermostat=true` a `local.properties` o `CALIU_REAL_THERMOSTAT=true` a l'entorn, que és el que fa el CI de `main` amb la variable del repo (el seu APK és el que es fa servir a casa). La release sempre fa servir el real. |
| Escriptura al dispositiu | Escriure afecta la calefacció de casa. Les proves contra el dispositiu real es demanen abans a l'usuari, com diu el `CLAUDE.md` de l'arrel. |

### API de Material 3 Expressive comprovades a la 1.5.0-beta01

`MaterialExpressiveTheme`, `ButtonGroup` i `ToggleButton` amb `ButtonGroupDefaults.connected*ButtonShapes`,
`LargeFlexibleTopAppBar`, `LoadingIndicator`, `PullToRefreshBox` amb `PullToRefreshDefaults.LoadingIndicator`,
`DropdownMenuPopup`, `DropdownMenuGroup`, el `DropdownMenuItem` expressiu, `SecureTextField` i `FilledTonalIconButton`.
Si mai en falta alguna, cal avisar abans de substituir-la, tal com demana el disseny.

## Diferències entre el handoff i el dispositiu real

El handoff (`disseny/docs/03-tuya-api.md`) descriu un termòstat Tuya genèric. Aquest R11 ja el coneixem per l'app de
macOS (`macOS/Sources/TuyaClient.swift`) i pel `CLAUDE.md` de l'arrel, i **mana el que sabem del dispositiu**:

| Handoff | Dispositiu real | Què fa Caliu |
|---|---|---|
| Mode `auto` / `manual` | Té `home` (manual), `auto` i `temporary`. A `/v1.0/.../commands` només s'accepta `auto`. | `MANUAL` ↔ `home` i `AUTO` ↔ `auto`. El mode s'escriu sempre per v2 (`shadow/properties/issue`). |
| Override «a gestionar» | Si es toca la consigna en `auto`, el dispositiu passa sol a `temporary` fins al tram següent. | `override` existeix quan el mode és `temporary`. «Fins a les HH:MM» és el final del tram actiu, calculat a partir del programa. «Tornar al programa» envia `mode = auto`. |
| `heating` per `work_state` o derivat | `work_state` (`heating`/`stop`) només surt per v2 (`shadow/properties`). | Es llegeix per v2, i només es deriva si no hi és. |
| Programa setmanal: 4 o 6 trams, dies agrupats? (a verificar segons el handoff) | Sempre 6 trams, i cada dia és un DP a part (`week_program_13_1` … `_7`). | 6 trams i selector de 7 dies. |
| Programa setmanal: el primer tram és a les 00:00 i no es mou | El primer tram té hora lliure (06:15 al programa real). | Decisió de l'usuari (2026-11-02): els 6 trams tenen l'hora editable, de 00:00 a 23:59, i cada hora va entre la de l'anterior i la del següent. De 00:00 al primer tram mana l'últim tram del dia anterior. |
| Programa: hora del primer tram | El programa real comença a les 06:15, no a les 00:00 (llegit el 2026-11-01). Els 7 dies tenen els mateixos trams. | De 00:00 al primer tram s'aplica la consigna de l'últim tram, que és el que continua del dia anterior. |
| Programa genèric | `week_program_13_1` … `_7` en base64 de 25 bytes `[dia, (h, min, temp×10 u16 BE)×6]`, **sense byte de recompte** (el 2026-11-01 en vam escriure un de més i va corrompre el programa), llegit per v2. | El decodificador es porta de `ScheduleSlot.decode`, amb tests. Cada tram acaba on comença el següent, i l'últim a les 24:00. |
| Escala a l'especificació | Valors enters ×10 (`temp_set` 220 = 22,0 °C), amb pas de 5 (0,5 °C). El model diu fins a 600, però `upper_temp` és 350 i `lower_temp` 50. | Tot es configura a `ThermostatDpMap`. |
| `battery_percentage` | Existeix (lectura del 2026-11-01: 100 %, només lectura, 0–100). | Es mostra al peu. Si mai no hi és, el peu amaga la bateria. |
| Pausa | L'app de macOS ja fa la mateixa pausa: baixa la consigna i ho desa localment. El model v2 de `mode` només té `home`, `auto` i `temporary`. Hi ha `dormant_switch` i `dormant_time_set`, que no sabem què fan, i `frost` (antigel). | Es fa servir la fórmula del handoff i l'estat es desa a DataStore. `dormant_*` no s'investiga sense permís, perquè caldria escriure-hi per provar-ho. |
| `temp_correction` | El valor és −20 a propòsit. | No s'hi toca mai. |
| Regió | EU | Es desa com a configuració, amb EU per defecte. |
| Grup connectat (Mode, dreceres) amb `ButtonGroup` | A la 1.5.0-beta01, `ButtonGroup` només té la variant amb desbordament | Un `Row` de `ToggleButton` amb `ButtonGroupDefaults.connected*ButtonShapes`, que és el patró oficial de grup connectat de selecció única. |
| Targeta hero de 134 dp | Les mètriques de Nunito fan la línia de 100 sp més alta | La targeta té l'alçada fixa i la xifra hi queda centrada. |
| Bateria horitzontal a les captures | Les `battery_*` de Material Symbols són verticals | Es fan servir les verticals que es van importar a la fase 0. |
| `moodColorScheme` no fixa `surfaceVariant`, `surfaceTint` ni `inverseSurface` | Es quedarien amb el lila de M3 | Es deriven del mood: `cardHigh`, `primary` i `onSurface`/`card` (Snackbar). |
| Xifres dels widgets en Nunito | Glance (RemoteViews) només pot fer servir les fonts del sistema | Els widgets fan servir sans-serif negreta. |

## Decisions de l'usuari (2026-11-01)

- **Idiomes:** l'anglès és l'idioma per defecte (`values/`) i el català va a `values-ca/`. Es pot canviar l'idioma de
  l'app des d'Info de l'app (`locales_config` amb `en` i `ca`). Es treu l'espanyol de l'esquelet. Els textos catalans del
  handoff (`disseny/android/strings.xml`) són la referència per a `values-ca/`, i l'anglès es tradueix a partir d'aquests.
- **Credencials precarregades:** només a la debug local, des de `local.properties` via `BuildConfig`. Al principi
  també les posava el CI des dels GitHub Secrets, però es va treure el 2026-11-02 perquè el repo passa a ser públic i
  els artifacts (l'APK, amb els valors en clar) es poden baixar. A l'APK del CI s'entren una vegada a Configuració.
- **Keystore de debug compartida** a `certs/debug.keystore`, com a Converses, perquè els APK del CI s'instal·lin a sobre.
- **Nom a la rajola i als widgets:** «Termòstat» en català i «Thermostat» en anglès. El launcher es diu «Caliu».
