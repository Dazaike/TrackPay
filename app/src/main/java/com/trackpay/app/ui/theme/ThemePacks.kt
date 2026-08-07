package com.trackpay.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.trackpay.app.domain.model.ThemeIds

/**
 * Material 3 light/dark schemes for each catalog theme id.
 * Unknown ids fall back to Verdant (default).
 */
object ThemePacks {

    fun schemeFor(themeId: String, darkTheme: Boolean): ColorScheme {
        val pack = packs[themeId] ?: packs.getValue(ThemeIds.VERDANT)
        return if (darkTheme) pack.dark else pack.light
    }

    /** Primary seed color for list swatches (light-scheme primary). */
    fun swatchPrimary(themeId: String): Color {
        val pack = packs[themeId] ?: packs.getValue(ThemeIds.VERDANT)
        return pack.light.primary
    }

    private data class Pack(val light: ColorScheme, val dark: ColorScheme)

    private val packs: Map<String, Pack> = mapOf(
        ThemeIds.VERDANT to Pack(verdantLight, verdantDark),
        ThemeIds.CLASSIC_BLUE to Pack(classicBlueLight, classicBlueDark),
        ThemeIds.SUNSET to Pack(sunsetLight, sunsetDark),
        ThemeIds.BERRY to Pack(berryLight, berryDark),
        ThemeIds.LAGOON to Pack(lagoonLight, lagoonDark),
        ThemeIds.AMETHYST to Pack(amethystLight, amethystDark),
        ThemeIds.EMBER to Pack(emberLight, emberDark),
    )
}

// --- Verdant (default, free) ---

private val verdantDark = darkColorScheme(
    primary = VerdantGreen,
    onPrimary = VerdantOnPrimary,
    primaryContainer = VerdantPrimaryContainer,
    onPrimaryContainer = VerdantOnPrimaryContainer,
    secondary = VerdantGreenLight,
    onSecondary = VerdantOnPrimary,
    background = VerdantDarkBackground,
    onBackground = VerdantDarkOnSurface,
    surface = VerdantDarkSurface,
    onSurface = VerdantDarkOnSurface,
    surfaceVariant = VerdantDarkSurfaceContainer,
    onSurfaceVariant = VerdantDarkOnSurfaceVariant,
    surfaceContainer = VerdantDarkSurfaceContainer,
    surfaceContainerLow = VerdantDarkBackground,
    surfaceContainerHigh = VerdantDarkSurfaceContainer,
    error = VerdantError,
    onError = Color(0xFF690005),
    outline = VerdantDarkOutline,
)

private val verdantLight = lightColorScheme(
    primary = VerdantLightPrimary,
    onPrimary = VerdantLightOnPrimary,
    primaryContainer = VerdantLightPrimaryContainer,
    onPrimaryContainer = VerdantLightOnPrimaryContainer,
    secondary = VerdantGreen,
    onSecondary = VerdantLightOnPrimary,
    background = VerdantLightBackground,
    onBackground = VerdantLightOnSurface,
    surface = VerdantLightSurface,
    onSurface = VerdantLightOnSurface,
    surfaceVariant = Color(0xFFDCE5DE),
    onSurfaceVariant = VerdantLightOnSurfaceVariant,
    surfaceContainer = Color(0xFFE8F2EB),
    surfaceContainerLow = VerdantLightBackground,
    surfaceContainerHigh = Color(0xFFDCE5DE),
    error = VerdantLightError,
    onError = Color(0xFFFFFFFF),
    outline = VerdantLightOutline,
)

// --- Classic Blue (free) ---

private val classicBlueLight = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    surfaceContainer = Color(0xFFECF0F8),
    surfaceContainerLow = Color(0xFFF3F5FC),
    surfaceContainerHigh = Color(0xFFE6EAF2),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF73777F),
)

private val classicBlueDark = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    tertiary = Color(0xFFD6BEE4),
    onTertiary = Color(0xFF3B2948),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainerHigh = Color(0xFF272A2F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFF8D9199),
)

// --- Sunset ---

private val sunsetLight = lightColorScheme(
    primary = Color(0xFF9A4522),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF380D00),
    secondary = Color(0xFF77574C),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF6B5E2F),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF231A16),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF231A16),
    surfaceVariant = Color(0xFFF5DED6),
    onSurfaceVariant = Color(0xFF53433E),
    surfaceContainer = Color(0xFFFCEAE4),
    surfaceContainerLow = Color(0xFFFFF1EC),
    surfaceContainerHigh = Color(0xFFF6E4DE),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF85736D),
)

private val sunsetDark = darkColorScheme(
    primary = Color(0xFFFFB59A),
    onPrimary = Color(0xFF5B1A00),
    primaryContainer = Color(0xFF7B2E0C),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFE7BDB0),
    onSecondary = Color(0xFF442A21),
    tertiary = Color(0xFFD7C68E),
    onTertiary = Color(0xFF3A2F05),
    background = Color(0xFF1A110E),
    onBackground = Color(0xFFF1DFD8),
    surface = Color(0xFF1A110E),
    onSurface = Color(0xFFF1DFD8),
    surfaceVariant = Color(0xFF53433E),
    onSurfaceVariant = Color(0xFFD8C2BB),
    surfaceContainer = Color(0xFF271D19),
    surfaceContainerLow = Color(0xFF221814),
    surfaceContainerHigh = Color(0xFF322722),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFFA08D86),
)

// --- Berry ---

private val berryLight = lightColorScheme(
    primary = Color(0xFF9C254D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E1),
    onPrimaryContainer = Color(0xFF3F0015),
    secondary = Color(0xFF75565D),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF7B5733),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF8F8),
    onBackground = Color(0xFF22191B),
    surface = Color(0xFFFFF8F8),
    onSurface = Color(0xFF22191B),
    surfaceVariant = Color(0xFFF3DDE1),
    onSurfaceVariant = Color(0xFF514347),
    surfaceContainer = Color(0xFFFBEAED),
    surfaceContainerLow = Color(0xFFFFF0F2),
    surfaceContainerHigh = Color(0xFFF5E4E7),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF837377),
)

private val berryDark = darkColorScheme(
    primary = Color(0xFFFFB1C4),
    onPrimary = Color(0xFF610027),
    primaryContainer = Color(0xFF7E0637),
    onPrimaryContainer = Color(0xFFFFD9E1),
    secondary = Color(0xFFE4BDC4),
    onSecondary = Color(0xFF43292F),
    tertiary = Color(0xFFEDBD91),
    onTertiary = Color(0xFF472A0A),
    background = Color(0xFF191113),
    onBackground = Color(0xFFEFDEE1),
    surface = Color(0xFF191113),
    onSurface = Color(0xFFEFDEE1),
    surfaceVariant = Color(0xFF514347),
    onSurfaceVariant = Color(0xFFD6C2C6),
    surfaceContainer = Color(0xFF261D1F),
    surfaceContainerLow = Color(0xFF21181A),
    surfaceContainerHigh = Color(0xFF312729),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFF9E8C90),
)

// --- Lagoon ---

private val lagoonLight = lightColorScheme(
    primary = Color(0xFF006A6A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF6FF7F6),
    onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFF4A6363),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF4B607C),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF4FBFA),
    onBackground = Color(0xFF161D1D),
    surface = Color(0xFFF4FBFA),
    onSurface = Color(0xFF161D1D),
    surfaceVariant = Color(0xFFDAE5E4),
    onSurfaceVariant = Color(0xFF3F4948),
    surfaceContainer = Color(0xFFE8F2F1),
    surfaceContainerLow = Color(0xFFEEF7F6),
    surfaceContainerHigh = Color(0xFFE2ECEB),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF6F7979),
)

private val lagoonDark = darkColorScheme(
    primary = Color(0xFF4CDADA),
    onPrimary = Color(0xFF003737),
    primaryContainer = Color(0xFF004F4F),
    onPrimaryContainer = Color(0xFF6FF7F6),
    secondary = Color(0xFFB0CCCC),
    onSecondary = Color(0xFF1B3535),
    tertiary = Color(0xFFB3C8E8),
    onTertiary = Color(0xFF1C314B),
    background = Color(0xFF0E1515),
    onBackground = Color(0xFFDDE4E3),
    surface = Color(0xFF0E1515),
    onSurface = Color(0xFFDDE4E3),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBEC9C8),
    surfaceContainer = Color(0xFF1A2121),
    surfaceContainerLow = Color(0xFF161D1D),
    surfaceContainerHigh = Color(0xFF242B2B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFF889392),
)

// --- Amethyst ---

private val amethystLight = lightColorScheme(
    primary = Color(0xFF6B4FA2),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEBDDFF),
    onPrimaryContainer = Color(0xFF25005A),
    secondary = Color(0xFF635B70),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF7E525D),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1A22),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1A22),
    surfaceVariant = Color(0xFFE7E0EB),
    onSurfaceVariant = Color(0xFF49454E),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerLow = Color(0xFFF9F2FC),
    surfaceContainerHigh = Color(0xFFEDE6F1),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF7A757F),
)

private val amethystDark = darkColorScheme(
    primary = Color(0xFFD3BBFF),
    onPrimary = Color(0xFF3B1D70),
    primaryContainer = Color(0xFF533688),
    onPrimaryContainer = Color(0xFFEBDDFF),
    secondary = Color(0xFFCDC2DB),
    onSecondary = Color(0xFF342D40),
    tertiary = Color(0xFFF0B7C5),
    onTertiary = Color(0xFF4A2530),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE7E0E8),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE7E0E8),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCBC4CF),
    surfaceContainer = Color(0xFF211F24),
    surfaceContainerLow = Color(0xFF1D1A22),
    surfaceContainerHigh = Color(0xFF2B2930),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFF948F99),
)

// --- Ember ---

private val emberLight = lightColorScheme(
    primary = Color(0xFFB3261E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF410001),
    secondary = Color(0xFF775652),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF715B2E),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF231918),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF231918),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534341),
    surfaceContainer = Color(0xFFFCEAE7),
    surfaceContainerLow = Color(0xFFFFF0EE),
    surfaceContainerHigh = Color(0xFFF6E4E1),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF857370),
)

private val emberDark = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690004),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFE7BDB7),
    onSecondary = Color(0xFF442926),
    tertiary = Color(0xFFE0C38C),
    onTertiary = Color(0xFF3F2E04),
    background = Color(0xFF1A1110),
    onBackground = Color(0xFFF1DFDC),
    surface = Color(0xFF1A1110),
    onSurface = Color(0xFFF1DFDC),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BE),
    surfaceContainer = Color(0xFF271D1C),
    surfaceContainerLow = Color(0xFF221816),
    surfaceContainerHigh = Color(0xFF322726),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFFA08C89),
)
