package com.cambio.converter.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Palette {
    val Navy = Color(0xFF0B1020)
    val NavyDeep = Color(0xFF070B18)
    val NavyElevated = Color(0xFF141B34)
    val Mint = Color(0xFF4AE3A0)
    val MintDeep = Color(0xFF17B37A)
    val Cyan = Color(0xFF5FE3FF)
    val Violet = Color(0xFF7C6CFF)
    val Amber = Color(0xFFFFC46B)
    val TextPrimary = Color(0xFFF3F6FF)
    val TextSecondary = Color(0xFF96A0C4)
    val OnAccent = Color(0xFF04231A)
    val Hairline = Color(0x22FFFFFF)

    val ScreenBrush: Brush
        get() = Brush.verticalGradient(
            colors = listOf(Navy, NavyDeep, Color(0xFF0A0F1F))
        )

    val CardBrush: Brush
        get() = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.045f),
                Color.White.copy(alpha = 0.085f)
            )
        )

    val SurfaceBrush: Brush
        get() = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.075f), Color.White.copy(alpha = 0.028f))
        )

    val AccentBrush: Brush
        get() = Brush.linearGradient(colors = listOf(Cyan, Mint, MintDeep))

    val MintLabelBrush: Brush
        get() = Brush.horizontalGradient(colors = listOf(Mint, Cyan, Mint))

    val HairlineBrush: Brush
        get() = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.24f), Color.Transparent)
        )

    val TopEdgeBrush: Brush
        get() = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.40f), Color.Transparent)
        )

    val StaticBorderBrush: Brush
        get() = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.14f)
            )
        )

    /** Borda mais marcada, para o estado de carregamento (sem animação). */
    val StaticBorderBright: Brush
        get() = Brush.linearGradient(
            colors = listOf(
                Mint.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.20f),
                Cyan.copy(alpha = 0.45f)
            )
        )

    val LabelSmall: TextStyle
        get() = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp
        )

    val BodyMedium: TextStyle
        get() = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 13.sp
        )

    val Headline: TextStyle
        get() = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )

    val DisplayValue: TextStyle
        get() = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp
        )
}

private val AppColorScheme = darkColorScheme(
    primary = Palette.Mint,
    onPrimary = Color(0xFF04231A),
    primaryContainer = Palette.MintDeep,
    onPrimaryContainer = Color(0xFF04231A),
    secondary = Palette.Violet,
    onSecondary = Color.White,
    background = Palette.Navy,
    onBackground = Palette.TextPrimary,
    surface = Palette.NavyElevated,
    onSurface = Palette.TextPrimary,
    surfaceVariant = Palette.NavyElevated,
    onSurfaceVariant = Palette.TextSecondary,
    outline = Palette.Hairline,
    error = Color(0xFFFF7A7A),
    onError = Color(0xFF2B0A0A)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 38.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.6.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun CambioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
