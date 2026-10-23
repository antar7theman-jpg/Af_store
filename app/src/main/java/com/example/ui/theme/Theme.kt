package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material3.Typography

val Typography = Typography()

enum class AppThemeMode(val labelAr: String = "", val subtitleAr: String = "", val badgeEmoji: String = "") {
    SYSTEM("تلقائي النظام", "يتبع إعدادات جهازك", "⚙️"),
    LIGHT("الوضع الفاتح", "مظهر نهاري مشرق ونظيف", "☀️"),
    DARK("الوضع الليلي", "مريح للعين والإضاءة الخافتة", "🌙"),
    OLED("شاشات OLED الداكن", "أسود دامس موفر للبطارية", "🖤"),
    OLED_BLACK("شاشات OLED الداكن", "أسود دامس موفر للبطارية", "🖤")
}

enum class AppThemePalette(
    val labelAr: String = "",
    val subtitleAr: String = "",
    val primaryHex: Long = 0xFF10B981L,
    val secondaryHex: Long = 0xFF059669L
) {
    EMERALD_CLOUD("زمردي سحابي", "أخضر زمردي مع سماوي", 0xFF10B981L, 0xFF06B6D4L),
    EMERALD("زمردي كلاسيكي", "أخضر زمردي منعش", 0xFF10B981L, 0xFF059669L),
    INDIGO("نيلي عصري", "أزرق نيلي تقني", 0xFF6366F1L, 0xFF4F46E5L),
    VIOLET("بنفسجي ملكي", "بنفسجي هادئ", 0xFF8B5CF6L, 0xFF7C3AEDL),
    AMBER("كهرماني ذهبي", "برتقالي دافئ", 0xFFF59E0BL, 0xFFD97706L),
    CRIMSON("قرمزي جريء", "أحمر احترافي", 0xFFEF4444L, 0xFFDC2626L)
}

val OLED_BLACK = Color(0xFF000000)

data class AFStoreSemanticColors(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val cardBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val divider: Color,
    val profitGreen: Color = Color(0xFF10B981),
    val dangerRed: Color = Color(0xFFEF4444),
    val debtAmber: Color = Color(0xFFF59E0B),
    val infoBlue: Color = Color(0xFF0284C7),
    val cardElevated: Color = Color(0xFFFFFFFF),
    val cardBorder: Color = Color(0xFFE2E8F0),
    val cardSubtle: Color = Color(0xFFF1F5F9),
    val profitContainer: Color = Color(0xFF10B981).copy(alpha = 0.12f),
    val dangerContainer: Color = Color(0xFFEF4444).copy(alpha = 0.12f),
    val debtContainer: Color = Color(0xFFF59E0B).copy(alpha = 0.12f),
    val isDark: Boolean = false,
    val headerBorder: Color = Color(0xFFCBD5E1),
    val onHeaderContainer: Color = Color(0xFF0F172A),
    val onHeaderSubtitle: Color = Color(0xFF475569),
    val tooltipBg: Color = Color(0xFF1E293B),
    val tooltipTitle: Color = Color(0xFFFFFFFF),
    val tooltipSubtitle: Color = Color(0xFF94A3B8),
    val chartGridLine: Color = Color(0xFFE2E8F0),
    val chartCanvasBg: Color = Color(0xFFFFFFFF),
    val chartCanvasBorder: Color = Color(0xFFCBD5E1),
    val chartAxisLabel: Color = Color(0xFF64748B),
    val cartonPurple: Color = Color(0xFF8B5CF6),
    val cartonContainer: Color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
    val isOledBlack: Boolean = false,
    val headerContainer: Color = Color(0xFFF1F5F9)
)

val LocalAFStoreSemanticColors = staticCompositionLocalOf {
    AFStoreSemanticColors(
        primary = Color(0xFF10B981),
        background = Color(0xFFF8FAFC),
        surface = Color(0xFFFFFFFF),
        cardBg = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF64748B),
        success = Color(0xFF10B981),
        warning = Color(0xFFF59E0B),
        error = Color(0xFFEF4444),
        divider = Color(0xFFE2E8F0),
        isDark = false
    )
}

val DarkSemanticColors = AFStoreSemanticColors(
    primary = Color(0xFF34D399),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    cardBg = Color(0xFF1E293B),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    success = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
    error = Color(0xFFF87171),
    divider = Color(0xFF334155),
    profitGreen = Color(0xFF34D399),
    dangerRed = Color(0xFFF87171),
    debtAmber = Color(0xFFFBBF24),
    infoBlue = Color(0xFF38BDF8),
    cardElevated = Color(0xFF1E293B),
    cardBorder = Color(0xFF334155),
    cardSubtle = Color(0xFF0F172A),
    profitContainer = Color(0xFF34D399).copy(alpha = 0.15f),
    dangerContainer = Color(0xFFF87171).copy(alpha = 0.15f),
    debtContainer = Color(0xFFFBBF24).copy(alpha = 0.15f),
    isDark = true,
    headerBorder = Color(0xFF475569),
    onHeaderContainer = Color(0xFFF8FAFC),
    onHeaderSubtitle = Color(0xFF94A3B8),
    tooltipBg = Color(0xFF0F172A),
    tooltipTitle = Color(0xFFFFFFFF),
    tooltipSubtitle = Color(0xFFCBD5E1),
    chartGridLine = Color(0xFF334155),
    chartCanvasBg = Color(0xFF1E293B),
    chartCanvasBorder = Color(0xFF475569),
    chartAxisLabel = Color(0xFF94A3B8),
    cartonPurple = Color(0xFFA78BFA)
)

val OledSemanticColors = DarkSemanticColors.copy(
    background = OLED_BLACK,
    surface = OLED_BLACK,
    cardBg = OLED_BLACK,
    cardElevated = OLED_BLACK
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    secondary = Color(0xFF38BDF8),
    tertiary = Color(0xFFF472B6),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF10B981),
    secondary = Color(0xFF0284C7),
    tertiary = Color(0xFFDB2777),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun AFStoreTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    palette: AppThemePalette = AppThemePalette.EMERALD,
    highContrastNumbers: Boolean = false,
    isRtl: Boolean = true,
    darkTheme: Boolean = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.OLED, AppThemeMode.OLED_BLACK -> true
    },
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isActualDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.OLED, AppThemeMode.OLED_BLACK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isActualDark -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context)
        }
        isActualDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val semanticColors = when (themeMode) {
        AppThemeMode.OLED, AppThemeMode.OLED_BLACK -> OledSemanticColors
        AppThemeMode.DARK -> DarkSemanticColors
        AppThemeMode.LIGHT -> LocalAFStoreSemanticColors.current
        AppThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkSemanticColors else LocalAFStoreSemanticColors.current
    }

    CompositionLocalProvider(
        LocalAFStoreSemanticColors provides semanticColors,
        LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
