# Caliu · pla de treball

App d'Android per controlar l'únic termòstat de casa (R11, Tuya Cloud). S'obre i ja es veu el termòstat: no hi ha
onboarding, comptes ni llista de dispositius.

- `disseny/` conté el paquet de Claude Design tal com va arribar: especificació, components, notes de Tuya, colors,
  tipografia, textos, tokens i 29 captures. **És la font de veritat del disseny.** No s'edita: si cal desviar-se'n,
  s'apunta a la secció «Diferències» d'aquí sota.
- `fases.md` recull les fases d'implementació, amb els passos i com es valida cadascuna.

## Abast

Està dins l'abast:

- La pantalla principal, amb tots els estats de les captures `01`–`11`.
- La pantalla de configuració (captures `12`–`14`).
- Dos widgets Glance (2×1 i 4×2) i una rajola de configuració ràpida (captura `15`).
- La distribució per Firebase App Distribution, que es farà en una fase posterior.

Queda fora de l'abast:

- Editar el programa setmanal. Només es mostra el programa d'avui i se'n pot canviar el tram actiu, cosa que crea un canvi
  temporal.
- Més d'un dispositiu, Google Play i Wear OS.

## Decisions

| Tema | Decisió |
|---|---|
| Paquet | `com.alorma.caliu`. La debug és `com.alorma.caliu.dev` i el paquet `cat.termostat` del handoff s'adapta a aquest. |
| Mòduls | `:app` (pantalles, ViewModels, dades, widgets, rajola) i `:designsystem` (tema Caliu, tipografia, icones i components visuals). Tots els `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` van a `:designsystem`. |
| UI | Compose amb Material 3 Expressive (`material3` 1.5.0-beta01). Totes les API que demana `disseny/docs/02` hi existeixen (vegeu la llista d'API comprovades més avall). |
| Arquitectura | Una sola Activity, MVVM amb `StateFlow`, Navigation 3 (principal i configuració), Koin, i cada pantalla amb una versió sense estat (`…Content`) per a les previews i els tests de captures. És el mateix que fan Converses i Caducity. |
| Xarxa | OkHttp amb kotlinx.serialization. La signatura HMAC va en un interceptor. No hi ha Retrofit: només són 5 endpoints. |
| Persistència | DataStore. Les credencials es xifren amb una clau AES/GCM d'Android Keystore (`EncryptedSharedPreferences` està obsolet). |
| Tema | Sense color dinàmic: la paleta depèn del *mood* (escalfant, en repòs, en pausa, apagat), segueix el mode clar o fosc del sistema i el canvi s'anima. |
| Tipografia | Roboto Flex (la mateixa `.ttf` de Converses) i Nunito, totes dues a `res/font` amb llicència OFL. No es fan servir Downloadable Fonts. |
| Icones | Material Symbols Rounded, importades com a vector drawables a `:designsystem` (unes 15). `material-icons-extended` són Material Icons, no Symbols. |
| Tests | Tests JVM per a la signatura, el decodificador del programa, la fórmula de la pausa, el càlcul del mood i els ViewModels. Tests de captures de Compose per a cada captura del handoff, en clar i fosc. |
| Termòstat simulat | `FakeThermostatRepository` (a `data/fake`) implementa `ThermostatRepository` en memòria, sense xarxa: segueix el programa, crea canvis temporals que caduquen al tram següent, apaga, i la temperatura s'acosta a la consigna a cada lectura. Amb `failure` es força qualsevol error. És el que fa servir l'app fins a la fase 3 i, després, la debug per defecte, perquè les proves no toquin mai la calefacció. |
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
| Programa genèric | `week_program_13_1` … `_7` en base64 `[dia, n, (h, min, temp×10 u16 BE)×n]`, amb 6 trams per dia, llegit per v2. | El decodificador es porta de `ScheduleSlot.decode`, amb tests. Cada tram acaba on comença el següent, i l'últim a les 24:00. |
| Escala a l'especificació | Valors enters ×10 (`temp_set` 220 = 22,0 °C). Els límits són `lower_temp` i `upper_temp`, per defecte 5–35. | Tot es configura a `ThermostatDpMap`. |
| `battery_percentage` | L'app de macOS el llegeix com a opcional; s'ha de comprovar si aquest R11 el té. | Si no hi és, el peu amaga la bateria. |
| Pausa | L'app de macOS ja fa la mateixa pausa: baixa la consigna i ho desa localment. | Es fa servir la fórmula del handoff i l'estat es desa a DataStore. Cal comprovar si el v2 té algun mode equivalent (`holiday`, `eco`…). |
| `temp_correction` | El valor és −20 a propòsit. | No s'hi toca mai. |
| Regió | EU | Es desa com a configuració, amb EU per defecte. |
| Grup connectat (Mode, dreceres) amb `ButtonGroup` | A la 1.5.0-beta01, `ButtonGroup` només té la variant amb desbordament | Un `Row` de `ToggleButton` amb `ButtonGroupDefaults.connected*ButtonShapes`, que és el patró oficial de grup connectat de selecció única. |
| Targeta hero de 134 dp | Les mètriques de Nunito fan la línia de 100 sp més alta | La targeta té l'alçada fixa i la xifra hi queda centrada. |
| Bateria horitzontal a les captures | Les `battery_*` de Material Symbols són verticals | Es fan servir les verticals que es van importar a la fase 0. |
| `moodColorScheme` no fixa `surfaceVariant`, `surfaceTint` ni `inverseSurface` | Es quedarien amb el lila de M3 | Es deriven del mood: `cardHigh`, `primary` i `onSurface`/`card` (Snackbar). |

## Decisions de l'usuari (2026-11-01)

- **Idiomes:** l'anglès és l'idioma per defecte (`values/`) i el català va a `values-ca/`. Es pot canviar l'idioma de
  l'app des d'Info de l'app (`locales_config` amb `en` i `ca`). Es treu l'espanyol de l'esquelet. Els textos catalans del
  handoff (`disseny/android/strings.xml`) són la referència per a `values-ca/`, i l'anglès es tradueix a partir d'aquests.
- **Credencials precarregades:** la debug omple la configuració des de `local.properties` en local i des dels GitHub
  Secrets al CI (`TUYA_ACCESS_ID`, `TUYA_ACCESS_SECRET`, `TUYA_REGION`, `TUYA_DEVICE_ID`), via `BuildConfig`. Si no n'hi
  ha, els camps queden buits. El repo és privat, però l'APK de debug porta aquests valors en clar: només s'han de posar a
  la debug, mai a la release.
- **Keystore de debug compartida** a `certs/debug.keystore`, com a Converses, perquè els APK del CI s'instal·lin a sobre.
- **Nom a la rajola i als widgets:** «Termòstat» en català i «Thermostat» en anglès. El launcher es diu «Caliu».
