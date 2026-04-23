package com.finance.manager.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ExtendedColors(
    val incomeGreen: Color,
    val incomeGreenLight: Color,
    val expenseRed: Color,
    val expenseRedLight: Color,
    val transferBlue: Color,
    val transferBlueLight: Color,
    val danger: Color,
    val warning: Color,
    val surfaceElevated: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        incomeGreen = IncomeGreen,
        incomeGreenLight = IncomeGreenLight,
        expenseRed = ExpenseRed,
        expenseRedLight = ExpenseRedLight,
        transferBlue = TransferBlue,
        transferBlueLight = TransferBlueLight,
        danger = Danger,
        warning = Warning,
        surfaceElevated = SurfaceElevated,
        onSurfaceVariant = OnSurfaceVariant,
        outline = Outline,
        outlineVariant = OutlineVariant,
    )
}

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    background = Background,
    onBackground = OnBackground,
    error = Error,
    onError = OnError,
    outline = Outline,
    outlineVariant = OutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
)

@Composable
fun FinanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        ExtendedColors(
            incomeGreen = IncomeGreenDark,
            incomeGreenLight = IncomeGreenLight,
            expenseRed = ExpenseRedDark,
            expenseRedLight = ExpenseRedLight,
            transferBlue = TransferBlueDark,
            transferBlueLight = TransferBlueLight,
            danger = Danger,
            warning = Warning,
            surfaceElevated = SurfaceElevated,
            onSurfaceVariant = OnSurfaceVariant,
            outline = Outline,
            outlineVariant = OutlineVariant,
        )
    } else {
        ExtendedColors(
            incomeGreen = IncomeGreen,
            incomeGreenLight = IncomeGreenLight,
            expenseRed = ExpenseRed,
            expenseRedLight = ExpenseRedLight,
            transferBlue = TransferBlue,
            transferBlueLight = TransferBlueLight,
            danger = Danger,
            warning = Warning,
            surfaceElevated = SurfaceElevated,
            onSurfaceVariant = OnSurfaceVariant,
            outline = Outline,
            outlineVariant = OutlineVariant,
        )
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

val MaterialTheme.extendedColors: ExtendedColors
    @Composable get() = LocalExtendedColors.current
