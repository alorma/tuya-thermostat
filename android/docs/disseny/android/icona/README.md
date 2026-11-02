# Icona de l'app

Icona adaptativa (108 × 108 dp, zona segura central de 66 dp):

- `ic_launcher_background.svg` · color pla `#FFDCC2` (heat-hero clar). A Android, millor com a `<color>` a `colors.xml`.
- `ic_launcher_foreground.svg` · arc de termòstat (pista `#F4B88E`, progrés i tirador `#9A4A00`) i flama `#9A4A00`.
- `ic_launcher_monochrome.svg` · capa per a icones temàtiques (Android 13+): la pista al 35 % d'opacitat.
- `play-store-512.svg` · icona de 512 px a sang per a la fitxa de Google Play (si mai cal). També en PNG: `play-store-512.png`.

Converteix els SVG a Vector Drawable amb Android Studio (File › New › Vector Asset) i declara-ho a `mipmap-anydpi-v26/ic_launcher.xml` amb `<background>`, `<foreground>` i `<monochrome>`.
Wear OS fa servir la mateixa icona dins de la màscara circular.
