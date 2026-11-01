# Termòstat · paquet per a Claude Code

App d'Android personal per controlar **un sol termòstat Wi‑Fi de calefacció** a través del núvol de Tuya. Sense onboarding, sense comptes, sense llistes de dispositius: s'obre i ja es veu el termòstat. Tots els textos en català.

## Què hi ha

| Ruta | Contingut |
|---|---|
| `docs/01-especificacio.md` | Comportament funcional: pantalles, estats, regles de negoci, refresc, errors, widgets i rajola |
| `docs/02-disseny-i-components.md` | Sistema de disseny **Caliu** i quin component de **Material 3 Expressive** (Compose) s'usa per a cada element |
| `docs/03-tuya-api.md` | Com parlar amb Tuya Cloud (signatura, endpoints, codis de dades) i què s'ha de verificar amb el dispositiu real |
| `android/Color.kt` | Paletes per estat (escalfant, en repòs, en pausa, apagat) en clar i fosc, mapejades a `ColorScheme` |
| `android/Type.kt` | Tipografia (Roboto Flex + Nunito per a les xifres) |
| `android/strings.xml` | Tots els textos de la interfície en català |
| `design/tokens.json` | Tokens del sistema Caliu (font de veritat dels colors, mides, radis) |
| `captures/` | 29 captures: totes les pantalles i estats en clar i fosc, configuració, widgets i rajola. Índex a `captures/INDEX.md` |

> Les captures s'han renderitzat amb Inter com a substitut tipogràfic i amb icones Material aproximades. A l'app la tipografia és **Roboto Flex** (interfície) i **Nunito** (xifres de temperatura), i les icones **Material Symbols Rounded**.

## Prompt suggerit per a Claude Code

```
Llegeix tot el contingut d'aquesta carpeta (README, docs/, android/, design/ i les captures de captures/).
Crea una app d'Android nativa en Kotlin amb Jetpack Compose i Material 3 Expressive
(MaterialExpressiveTheme, androidx.compose.material3 amb l'API expressiva) que implementi
exactament l'especificació de docs/01-especificacio.md i el disseny de docs/02-disseny-i-components.md.

Regles:
- Fes servir els components de Material 3 / M3 Expressive indicats a la taula de components.
  No inventis components propis si n'hi ha un d'M3E que ho resol; si un component no existeix
  a la versió de material3 que facis servir, digues-m'ho abans de substituir-lo.
- Colors: android/Color.kt. Textos: android/strings.xml (català). Tipografia: android/Type.kt.
- Abans d'escriure la capa de Tuya, segueix docs/03-tuya-api.md i deixa els codis de dades (DP)
  configurables: s'han de verificar amb el meu dispositiu.
- Arquitectura: una sola Activity, MVVM amb StateFlow, Ktor o Retrofit + OkHttp, DataStore,
  credencials xifrades amb Android Keystore, Glance per als widgets i TileService per a la rajola.
- Comença proposant l'estructura de mòduls i el pla; després implementa per fases
  (tema → pantalla principal amb dades falses → capa Tuya → configuració → widgets i rajola).
```
