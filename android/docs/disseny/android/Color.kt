package cat.termostat.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Caliu · paletes per estat del termòstat.
 * Font de veritat: design/tokens.json. Cada mood té la mateixa estructura de rols en clar i fosc.
 *
 *  HEAT  → escalfant (ambre)
 *  CALM  → en repòs, ja és a la consigna (verd aigua; mai significa «fred»)
 *  PAUSE → en pausa (lila grisós)
 *  OFF   → apagat, errors, càrrega i configuració (neutre càlid)
 */
enum class Mood { HEAT, CALM, PAUSE, OFF }

data class MoodPalette(
    val surface: Color,              // fons de pantalla
    val card: Color,                 // targetes (surfaceContainerLow)
    val cardHigh: Color,             // botons no seleccionats (surfaceContainerHigh)
    val containerHighest: Color,     // pista d'interruptor apagat (surfaceContainerHighest)
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val onSurfacePast: Color,        // trams passats del programa
    val outline: Color,
    val outlineVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val hero: Color,                 // targeta de temperatura (tertiaryContainer)
    val onHero: Color,               // onTertiaryContainer
    val heroVariant: Color,          // «°C» i etiquetes sobre hero
    val accent: Color,               // xip d'estat (tertiary)
    val onAccent: Color,             // onTertiary
    val dial: Color,                 // formes grans en color d'estat (3:1)
)

private val Light = mapOf(
    Mood.HEAT to MoodPalette(
        surface = Color(0xFFFFF5EE), card = Color(0xFFFFFBF8), cardHigh = Color(0xFFF6E6DA), containerHighest = Color(0xFFEBD6C8),
        onSurface = Color(0xFF241912), onSurfaceVariant = Color(0xFF574337), onSurfacePast = Color(0xFF7A6557),
        outline = Color(0xFF8A7162), outlineVariant = Color(0xFFDEC4B4),
        primary = Color(0xFF8F4C24), onPrimary = Color(0xFFFFFFFF), primaryContainer = Color(0xFFFFDBC9), onPrimaryContainer = Color(0xFF341100),
        hero = Color(0xFFFFDCC2), onHero = Color(0xFF3A1A00), heroVariant = Color(0xFF6A3300),
        accent = Color(0xFF9A4A00), onAccent = Color(0xFFFFFFFF), dial = Color(0xFFB85A00),
    ),
    Mood.CALM to MoodPalette(
        surface = Color(0xFFF1F7F5), card = Color(0xFFFAFDFC), cardHigh = Color(0xFFE1ECE9), containerHighest = Color(0xFFD4E2DE),
        onSurface = Color(0xFF161D1B), onSurfaceVariant = Color(0xFF3F4946), onSurfacePast = Color(0xFF66716E),
        outline = Color(0xFF6F7976), outlineVariant = Color(0xFFBEC9C5),
        primary = Color(0xFF2F6A60), onPrimary = Color(0xFFFFFFFF), primaryContainer = Color(0xFFB5EDE2), onPrimaryContainer = Color(0xFF00201C),
        hero = Color(0xFFCDE8E1), onHero = Color(0xFF0F2E29), heroVariant = Color(0xFF2C4C46),
        accent = Color(0xFF2F6A60), onAccent = Color(0xFFFFFFFF), dial = Color(0xFF2F6A60),
    ),
    Mood.PAUSE to MoodPalette(
        surface = Color(0xFFF5F3F8), card = Color(0xFFFDFBFF), cardHigh = Color(0xFFE9E6EF), containerHighest = Color(0xFFDCD9E3),
        onSurface = Color(0xFF1C1B21), onSurfaceVariant = Color(0xFF47464F), onSurfacePast = Color(0xFF6E6C77),
        outline = Color(0xFF787680), outlineVariant = Color(0xFFC9C5D0),
        primary = Color(0xFF5D5B71), onPrimary = Color(0xFFFFFFFF), primaryContainer = Color(0xFFE2DFF9), onPrimaryContainer = Color(0xFF1A182B),
        hero = Color(0xFFE4E1EC), onHero = Color(0xFF1C1B22), heroVariant = Color(0xFF47464F),
        accent = Color(0xFF5D5B71), onAccent = Color(0xFFFFFFFF), dial = Color(0xFF787680),
    ),
    Mood.OFF to MoodPalette(
        surface = Color(0xFFF6F3F1), card = Color(0xFFFFFBF9), cardHigh = Color(0xFFECE6E2), containerHighest = Color(0xFFE0D9D5),
        onSurface = Color(0xFF1F1B19), onSurfaceVariant = Color(0xFF504641), onSurfacePast = Color(0xFF756A64),
        outline = Color(0xFF82756F), outlineVariant = Color(0xFFD4C8C2),
        primary = Color(0xFF8F4C24), onPrimary = Color(0xFFFFFFFF), primaryContainer = Color(0xFFFFDBC9), onPrimaryContainer = Color(0xFF341100),
        hero = Color(0xFFECE6E2), onHero = Color(0xFF1F1B19), heroVariant = Color(0xFF504641),
        accent = Color(0xFFE0D9D5), onAccent = Color(0xFF504641), dial = Color(0xFF82756F),
    ),
)

private val Dark = mapOf(
    Mood.HEAT to MoodPalette(
        surface = Color(0xFF1A110B), card = Color(0xFF251A13), cardHigh = Color(0xFF33261D), containerHighest = Color(0xFF413228),
        onSurface = Color(0xFFF3DFD3), onSurfaceVariant = Color(0xFFDEC4B4), onSurfacePast = Color(0xFFA68D7E),
        outline = Color(0xFFA68D7E), outlineVariant = Color(0xFF574337),
        primary = Color(0xFFFFB68F), onPrimary = Color(0xFF552000), primaryContainer = Color(0xFF723513), onPrimaryContainer = Color(0xFFFFDBC9),
        hero = Color(0xFF5E2F00), onHero = Color(0xFFFFE8D8), heroVariant = Color(0xFFFFC9A3),
        accent = Color(0xFFFFB77C), onAccent = Color(0xFF4A2300), dial = Color(0xFFFFB77C),
    ),
    Mood.CALM to MoodPalette(
        surface = Color(0xFF0E1513), card = Color(0xFF171F1D), cardHigh = Color(0xFF222B29), containerHighest = Color(0xFF2D3634),
        onSurface = Color(0xFFDDE4E1), onSurfaceVariant = Color(0xFFBEC9C5), onSurfacePast = Color(0xFF889390),
        outline = Color(0xFF889390), outlineVariant = Color(0xFF3F4946),
        primary = Color(0xFF99D1C5), onPrimary = Color(0xFF00382F), primaryContainer = Color(0xFF135046), onPrimaryContainer = Color(0xFFB5EDE2),
        hero = Color(0xFF1B3B35), onHero = Color(0xFFD6F1EA), heroVariant = Color(0xFFA8D3C9),
        accent = Color(0xFF99D1C5), onAccent = Color(0xFF00201C), dial = Color(0xFF99D1C5),
    ),
    Mood.PAUSE to MoodPalette(
        surface = Color(0xFF131318), card = Color(0xFF1C1B22), cardHigh = Color(0xFF27262E), containerHighest = Color(0xFF32313A),
        onSurface = Color(0xFFE5E1E9), onSurfaceVariant = Color(0xFFC9C5D0), onSurfacePast = Color(0xFF928F9A),
        outline = Color(0xFF928F9A), outlineVariant = Color(0xFF47464F),
        primary = Color(0xFFC6C2DC), onPrimary = Color(0xFF2F2D41), primaryContainer = Color(0xFF454358), onPrimaryContainer = Color(0xFFE2DFF9),
        hero = Color(0xFF2D2B3A), onHero = Color(0xFFECE8F6), heroVariant = Color(0xFFC9C5D0),
        accent = Color(0xFFC6C2DC), onAccent = Color(0xFF2F2D41), dial = Color(0xFF928F9A),
    ),
    Mood.OFF to MoodPalette(
        surface = Color(0xFF151210), card = Color(0xFF201B19), cardHigh = Color(0xFF2B2522), containerHighest = Color(0xFF36302C),
        onSurface = Color(0xFFEBE0DB), onSurfaceVariant = Color(0xFFD4C8C2), onSurfacePast = Color(0xFF9D908A),
        outline = Color(0xFF9D908A), outlineVariant = Color(0xFF504641),
        primary = Color(0xFFFFB68F), onPrimary = Color(0xFF552000), primaryContainer = Color(0xFF723513), onPrimaryContainer = Color(0xFFFFDBC9),
        hero = Color(0xFF2B2522), onHero = Color(0xFFEBE0DB), heroVariant = Color(0xFFD4C8C2),
        accent = Color(0xFF36302C), onAccent = Color(0xFFD4C8C2), dial = Color(0xFF9D908A),
    ),
)

/** Colors fixos, iguals en tots els moods. */
object FixedColors {
    val errorLight = Color(0xFFBA1A1A); val onErrorLight = Color(0xFFFFFFFF)
    val errorContainerLight = Color(0xFFFFDAD6); val onErrorContainerLight = Color(0xFF93000A)
    val errorDark = Color(0xFFFFB4AB); val onErrorDark = Color(0xFF690005)
    val errorContainerDark = Color(0xFF93000A); val onErrorContainerDark = Color(0xFFFFDAD6)

    // Bàner «Temporal» (sempre colors d'escalfar)
    val temporaryLight = Color(0xFFFFDCC2); val onTemporaryLight = Color(0xFF6A3300)
    val temporaryDark = Color(0xFF6E3900); val onTemporaryDark = Color(0xFFFFDCC2)

    // Pista translúcida per a −/+ dins d'una targeta hero
    val trackLight = Color(0x12000000); val trackDark = Color(0x1FFFFFFF)
}

fun moodPalette(mood: Mood, dark: Boolean): MoodPalette = (if (dark) Dark else Light).getValue(mood)

fun moodColorScheme(mood: Mood, dark: Boolean): ColorScheme {
    val p = moodPalette(mood, dark)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.primary, onPrimary = p.onPrimary,
        primaryContainer = p.primaryContainer, onPrimaryContainer = p.onPrimaryContainer,
        secondary = p.primary, onSecondary = p.onPrimary,
        secondaryContainer = p.primaryContainer, onSecondaryContainer = p.onPrimaryContainer,
        tertiary = p.accent, onTertiary = p.onAccent,
        tertiaryContainer = p.hero, onTertiaryContainer = p.onHero,
        background = p.surface, onBackground = p.onSurface,
        surface = p.surface, onSurface = p.onSurface, onSurfaceVariant = p.onSurfaceVariant,
        surfaceContainerLowest = p.card, surfaceContainerLow = p.card, surfaceContainer = p.card,
        surfaceContainerHigh = p.cardHigh, surfaceContainerHighest = p.containerHighest,
        outline = p.outline, outlineVariant = p.outlineVariant,
        error = if (dark) FixedColors.errorDark else FixedColors.errorLight,
        onError = if (dark) FixedColors.onErrorDark else FixedColors.onErrorLight,
        errorContainer = if (dark) FixedColors.errorContainerDark else FixedColors.errorContainerLight,
        onErrorContainer = if (dark) FixedColors.onErrorContainerDark else FixedColors.onErrorContainerLight,
    )
}
