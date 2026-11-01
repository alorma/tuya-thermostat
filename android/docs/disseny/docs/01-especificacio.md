# 01 · Especificació funcional

## Context

- Un sol termòstat de calefacció (no hi ha refrigeració). Es controla per **Tuya Cloud** (OpenAPI).
- Eina personal: no hi ha onboarding, comptes ni selecció de dispositiu. Si falta configuració, es mostra l'estat «Falta la configuració».
- Idioma únic: català. Temperatures amb coma decimal i un decimal (`20,4 °C`); a les dreceres, sense decimal (`21°`).
- Una sola pantalla principal, d'un cop d'ull, optimitzada per a una mà. Configuració com a segona pantalla.

## Model d'estat

Estat del termòstat que l'app manté (font: Tuya; vegeu `03-tuya-api.md`):

| Camp | Tipus | Notes |
|---|---|---|
| `power` | bool | Encès / apagat |
| `paused` | bool | Estat propi de l'app (vegeu Pausa) |
| `mode` | `AUTO` · `MANUAL` | |
| `currentTemp` | decimal | Temperatura actual. **Apagat no n'hi ha** (es mostra «—») |
| `setpoint` | decimal | Consigna, passos de 0,5 °C, dins `[min, max]` del dispositiu (normalment 5–35) |
| `heating` | bool | Si la caldera/vàlvula escalfa ara. Si el dispositiu no ho dona, derivar: `power && !paused && current < setpoint` |
| `override` | `{ until: LocalTime, setpoint }?` | Canvi temporal mentre està en AUTO |
| `schedule` | 6 trams del dia | `{ start, end, setpoint }` |
| `battery` | int % | |
| `lastUpdated` | instant | |

### Estat visual («mood»)

Determina la paleta de tota la pantalla (vegeu `02-disseny-i-components.md`):

| Condició | Mood | Xip d'estat |
|---|---|---|
| `!power` | `OFF` | `power_settings_new` · «Apagat» |
| `power && paused` | `PAUSE` | `pause_circle` · «En pausa» |
| `power && !paused && heating` | `HEAT` | `local_fire_department` · «Escalfant» |
| `power && !paused && !heating` | `CALM` | `pause` · «En repòs» |
| errors, càrrega, configuració | `OFF` (neutre) | — |

Els canvis de mood s'animen (transició de color, ~300 ms).

## Pantalla principal

Ordre vertical (captures `01`–`07`):

1. **Barra superior** sense títol d'app:
   - Esquerra: **xip d'estat** (color d'accent del mood, icona farcida, text, fletxa ▾/▴). És el control d'estat: en tocar-lo obre un menú (captura `02`):
     - **Activa** (`play_arrow`) · **Pausa** (`pause`) · separat i atenuat, **Apaga** (`power_settings_new`).
     - L'opció actual surt marcada. Sortir de la pausa (Activa o Apaga) restaura la consigna anterior.
   - Dreta: icona **Configuració** (`settings`) → pantalla de configuració.
2. **Temperatura actual** (títol de secció) · targeta hero amb la xifra gran (Nunito 100 sp) i «°C». Res més a la targeta.
3. **Mode** · selector connectat de dues opcions: **Automàtic** / **Manual**.
4. Segons el mode:
   - **Manual** (captura `03`):
     - **Consigna** · `−` · valor · `+` (botons de 72 dp). Pas 0,5 °C.
     - **Temperatures ràpides** · tres opcions connectades: `20°`, `21°`, `23°`. La que coincideix amb la consigna queda seleccionada.
   - **Automàtic** (captures `01`, `04`):
     - Bàner **«Temporal fins a les HH:MM»** amb icona `back_hand` i botó de text **Tornar al programa**, només si hi ha `override`.
     - **Programa d'avui** · 6 trams. Passats atenuats, futurs en color secundari, l'actiu ressaltat i més alt. El tram actiu porta `−`/`+` petits: canviar-hi la consigna crea un `override` fins al final del tram. El tram actiu es recalcula cada minut.
5. **Peu**: icona de bateria + «87 %» a l'esquerra; «Actualitzat a les HH:MM» a la dreta (o «Actualitzant…» mentre refresca).

Els títols de secció (Temperatura actual, Mode, Consigna, Temperatures ràpides, Programa d'avui) són petits i en color secundari. **No hi ha cap text sota les xifres.**

### Pausa

La pausa és l'acció habitual de l'usuari; apagar és secundari (apagat no es pot llegir la temperatura de casa, i l'app de Tuya no té pausa).

- **Pausa**: desa `{mode, setpoint}` actuals (persistent, DataStore), passa a MANUAL i posa la consigna per sota de la temperatura actual: `max(min, floor((current − 1) × 2) / 2)`.
- **Sortir de la pausa** (Activa): restaura el mode i la consigna desats.
- A la UI, el selector de Mode continua mostrant el mode desat (al qual es tornarà), encara que al dispositiu s'hagi forçat MANUAL. En AUTO es veu el programa amb el tram actiu desactivat (captura `05`); en MANUAL, la targeta de consigna mostra la consigna que es recuperarà, atenuada i amb vora discontínua. −/+ i dreceres desactivats en tots dos casos.
- *A decidir/verificar*: si el dispositiu té un mode propi equivalent («holiday», «eco», «off-timer»…), preferir-lo.

### Apagat

- Xifra «—», controls al 38 % d'opacitat i desactivats (captura `06`). El xip diu «Apagat»; des del menú es pot Activar o Pausar.

### Ordres i sincronització

- Refresc automàtic cada **60 s** amb l'app en primer pla, i **pull‑to‑refresh** (captura `07`).
- **Optimista**: la UI canvia a l'instant; si l'ordre falla, es reverteix i es mostra un Snackbar d'error.
- Tocs ràpids de −/+ s'agrupen amb **debounce de 800 ms** abans d'enviar-se.
- Valors fora de `[min, max]` no es permeten; botons desactivats als límits.

## Estats d'error i càrrega

| Estat | Contingut | Captura |
|---|---|---|
| Càrrega inicial | Skeleton amb la mateixa estructura (xip, hero, mode, programa) | `08` |
| Refresc | Indicador de pull‑to‑refresh i «Actualitzant…» al peu | `07` |
| Error de l'API | Icona `error`, títol en vermell «Error de l'API de Tuya», explicació (missatge de Tuya traduït, p. ex. «La prova de l'IoT Core ha caducat; renova-la a la consola de Tuya.»), botó **Obrir iot.tuya.com** i botó de text **Torna-ho a provar** | `09` |
| Sense connexió | Icona `wifi_off`, «Sense connexió a internet», «Comprova la wifi o les dades mòbils.», **Torna-ho a provar** | `10` |
| Falta configuració | Icona `key`, «Falta la configuració», «Connecta el teu compte de Tuya per veure el termòstat.», **Configura** → Configuració | `11` |

## Configuració

Captures `12`–`14`. Barra superior gran (`arrow_back`, títol «Configuració», botó **Desa**).

- Grup de camps connectats: **Access ID**, **Access Secret** (ocult, amb botó mostrar/amagar), **Regió** (selector connectat EU · US · CN · IN), **Device ID**.
- Botó tonal **Provar connexió** → mostra «Provant connexió…», després «Connexió correcta» (verd aigua) o «No s'ha pogut connectar» + causa (error).
- Les credencials es desen xifrades (Android Keystore). Desa torna a la pantalla principal i força un refresc.

## Widgets (Glance)

Captura `15`. Prenen el color hero del mood.

- **Petit 2×1**: icona d'estat en quadrat arrodonit + temperatura actual («20,4°»).
- **Mitjà 4×2**: temperatura actual + xip d'estat; −/+ amb la consigna; tram actiu del programa («08:00 – 14:00», icona `schedule`) quan és AUTO.
- Actualització: en cada refresc de l'app i periòdicament amb WorkManager (mínim 15 min). Tocar el widget obre l'app.

## Rajola de configuració ràpida (TileService)

- Títol «Termòstat», subtítol «20,4 °C · Escalfant».
- **Tocar-la alterna la pausa** (activa ⇄ en pausa), no l'encesa. Activa = `Tile.STATE_ACTIVE`; en pausa = `STATE_INACTIVE` amb subtítol «… · En pausa». Apagat: subtítol «Apagat».
