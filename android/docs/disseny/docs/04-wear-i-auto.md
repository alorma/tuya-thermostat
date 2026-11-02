# 04 · Wear OS i Android Auto

Mateixa lògica i mateixa capa de dades que l'app de mòbil (mòdul compartit `:core` amb el client de Tuya, el model d'estat i les regles de pausa). Captures `16-wear-os.png` i `17-android-auto.png`.

## Wear OS

Mòdul `:wear`, **Compose for Wear OS amb Material 3 per a Wear** (`androidx.wear.compose:compose-material3`). No inventar components: fer servir els d'aquesta llibreria.

### App autònoma

- `com.google.android.wearable.standalone = true`: funciona sense el mòbil a prop (wifi o LTE del rellotge) cridant Tuya directament.
- Credencials: s'envien **una vegada** des de l'app del mòbil per la **Data Layer API** i es desen xifrades al rellotge. Sense credencials, pantalla «Falta la configuració» amb `EdgeButton` «Obre al mòbil» (obre la configuració al telèfon amb `RemoteActivityHelper`).
- Refresc: en obrir l'app i cada 60 s mentre és en primer pla; la tile i les complicacions, amb la freqüència del sistema i en cada ordre.

### Pantalla principal

| Element | Component Wear M3 |
|---|---|
| Contenidor | `AppScaffold` + `ScreenScaffold` amb `TimeText` |
| Anell de la vora en color d'accent de l'estat | `CircularProgressIndicator` a pantalla completa (progrés decoratiu = posició de la consigna dins el rang) |
| Xip d'estat | `Button` compacte (`CompactButton`) amb icona, colors d'accent del mood |
| Temperatura actual | `Text` Nunito 800, ~50 sp |
| Consigna | `IconButton` circulars −/+ i **corona** (`Modifier.rotaryScrollable` / `onRotaryScrollEvent`) a passos de 0,5 °C, amb el mateix debounce de 800 ms |
| Acció principal | `EdgeButton`: **Pausa** (en actiu) / **Activa** (en pausa) |
| Llista en lliscar | `TransformingLazyColumn`: títol «Mode» → `RadioButton`/`ToggleButton` Automàtic / Manual; «Temperatures ràpides» → tres `Button` circulars 20° 21° 23°; **Apaga** com a `Button` de text atenuat al final |

En pausa: paleta lila, consigna a recuperar amb vora discontínua i atenuada, `EdgeButton` «Activa».

### Colors al rellotge

Sempre fosc i fons `#000000`. De `android/Color.kt` es fan servir les paletes **fosques**: `accent`/`onAccent` per al xip, l'anell i les dreceres seleccionades; `hero`/`onHero` per a la targeta de la tile; `primaryContainer` per a l'`EdgeButton` «Pausa».

### Tile

ProtoLayout Material 3 (`androidx.wear.protolayout:protolayout-material3`): `primaryLayout` amb títol «Termòstat», contingut principal = targeta hero amb la temperatura i l'estat, i `EdgeButton` **Pausa/Activa** que envia l'ordre directament.

### Complicacions

`ComplicationDataSourceService` amb:
- `RANGED_VALUE`: temperatura actual dins el rang [min, max], icona flama/pausa, text «19°».
- `SHORT_TEXT`: «19,3°» amb icona d'estat.
- Tocar-les obre l'app.

## Android Auto

Mòdul `:car` amb la **Car App Library** (`androidx.car.app`), categoria **IoT** (`androidx.car.app.category.IOT`). Comprovar els requisits de qualitat de Google per a aquesta categoria abans de publicar.

- Només plantilles: **`GridTemplate`**. No hi ha interfície a mida.
- Títol de la plantilla: «19,3 °C · Escalfant» (temperatura + estat).
- Icona de capçalera i element principal amb `CarColor` personalitzat (accent del mood, clar i fosc).
- Elements de la graella:
  1. **Pausa** (`pause`) o, en pausa, **Activa** (`play_arrow`, text secundari «Torna a 20,0 °C»).
  2. **20 °C**, **21 °C**, **23 °C** (icones amb el número). La consigna actual porta el text secundari «Consigna».
- `ActionStrip`: **Apaga** (secundari).
- Tocar una temperatura en pausa surt de la pausa amb aquella consigna.
- Refresc en obrir la pantalla i després de cada ordre; `invalidate()` per redibuixar.
