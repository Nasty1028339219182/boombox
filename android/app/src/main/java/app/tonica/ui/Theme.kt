package app.tonica.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Teal = Color(0xFF4FD8D4)
private val OnTeal = Color(0xFF003736)
private val TealContainer = Color(0xFF00504E)
private val OnTealContainer = Color(0xFF9FF4F0)
private val Surface = Color(0xFF0E1414)
private val OnSurface = Color(0xFFF5F7F6)
private val SurfaceContainer = Color(0xFF1A2121)
private val SurfaceHigh = Color(0xFF242B2B)
private val SurfaceHighest = Color(0xFF2F3636)
private val SecondaryContainer = Color(0xFF334B49)
private val OnSecondaryContainer = Color(0xFFCDE8E5)
private val Error = Color(0xFFFFB4AB)
private val ErrorContainer = Color(0xFF93000A)
private val OnErrorContainer = Color(0xFFFFDAD6)

private val scheme = darkColorScheme(
    primary = Teal,
    onPrimary = OnTeal,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = Color(0xFFB1CCC9),
    onSecondary = Color(0xFF1C3533),
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Color(0xFFB3C8EA),
    onTertiary = Color(0xFF1C314B),
    background = Surface,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = Color(0xFFC5D0CE),
    surfaceContainerLowest = Color(0xFF090F0F),
    surfaceContainerLow = Color(0xFF161D1D),
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHighest,
    error = Error,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    outline = Color(0xFF889392),
    outlineVariant = Color(0xFF3E4948),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun TonicaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        shapes = shapes,
        content = content,
    )
}
