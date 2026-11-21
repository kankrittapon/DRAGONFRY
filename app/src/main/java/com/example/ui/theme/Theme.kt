package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DragonfyreDarkColorScheme = darkColorScheme(
    primary = DragonGold,
    onPrimary = OnDragonGold,
    primaryContainer = DragonGoldDark,
    onPrimaryContainer = ParchmentIvory,
    secondary = ArcaneAmethyst,
    onSecondary = ParchmentIvory,
    secondaryContainer = ArcaneAmethystDark,
    onSecondaryContainer = ParchmentIvory,
    tertiary = EmberCrimson,
    onTertiary = ParchmentIvory,
    tertiaryContainer = EmberCrimsonDark,
    onTertiaryContainer = ParchmentIvory,
    background = ObsidianVoid,
    onBackground = ParchmentIvory,
    surface = ObsidianSurface,
    onSurface = ParchmentIvory,
    surfaceVariant = SlateIron,
    onSurfaceVariant = AshSilver,
    surfaceContainerLowest = ObsidianVoid,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = SlateIron,
    surfaceContainerHigh = SlateElevated,
    outline = MetallicBorder,
    outlineVariant = MutedSteel,
    error = EmberGlow,
    onError = ObsidianVoid
)

val DragonfyreShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun DragonfyreTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DragonfyreDarkColorScheme,
        typography = DragonfyreTypography,
        shapes = DragonfyreShapes,
        content = content
    )
}
