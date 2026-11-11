package app.echo.messages.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Brand: calm indigo for the app, warm coral reserved for one meaning only: "this was deleted".
private val Light = lightColorScheme(
    primary = Color(0xFF4B3BD6), onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E0FF), onPrimaryContainer = Color(0xFF16006B),
    secondary = Color(0xFF5D5B72), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E0F9), onSecondaryContainer = Color(0xFF1A182C),
    tertiary = Color(0xFFC8372D), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE1DC), onTertiaryContainer = Color(0xFF410100),
    error = Color(0xFFBA1A1A), onError = Color.White,
    background = Color(0xFFFBFAFF), onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBFAFF), onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE4E1EC), onSurfaceVariant = Color(0xFF47464F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F2FC),
    surfaceContainer = Color(0xFFEFECF7),
    surfaceContainerHigh = Color(0xFFE9E6F1),
    surfaceContainerHighest = Color(0xFFE3E1EB),
    outline = Color(0xFF787680), outlineVariant = Color(0xFFC8C5D0),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFC7C0FF), onPrimary = Color(0xFF26178F),
    primaryContainer = Color(0xFF3B2DB8), onPrimaryContainer = Color(0xFFE5E0FF),
    secondary = Color(0xFFC7C4DD), onSecondary = Color(0xFF2F2D42),
    secondaryContainer = Color(0xFF45435A), onSecondaryContainer = Color(0xFFE3E0F9),
    tertiary = Color(0xFFFF8C7F), onTertiary = Color(0xFF690004),
    tertiaryContainer = Color(0xFF5E1611), onTertiaryContainer = Color(0xFFFFDAD5),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    background = Color(0xFF0E0D14), onBackground = Color(0xFFE5E1EC),
    surface = Color(0xFF0E0D14), onSurface = Color(0xFFE5E1EC),
    surfaceVariant = Color(0xFF47464F), onSurfaceVariant = Color(0xFFC8C5D0),
    surfaceContainerLowest = Color(0xFF09080E),
    surfaceContainerLow = Color(0xFF16151D),
    surfaceContainer = Color(0xFF1B1A22),
    surfaceContainerHigh = Color(0xFF25242D),
    surfaceContainerHighest = Color(0xFF302F38),
    outline = Color(0xFF928F9A), outlineVariant = Color(0xFF47464F),
)

private val Base = Typography()
private val Type = Typography(
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(lineHeight = 24.sp),
)

private val Shape = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun EchoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = Type,
        shapes = Shape,
        content = content,
    )
}

/** Deterministic, accessible avatar colours (white text passes 4.5:1 on all of them). */
val AvatarPalette = listOf(
    Color(0xFF5B4BDB), Color(0xFF0B7A75), Color(0xFFB4441F), Color(0xFF8E3FA8),
    Color(0xFF2E6BC6), Color(0xFF6B6B12), Color(0xFFB0306A), Color(0xFF3D7A2E),
)
