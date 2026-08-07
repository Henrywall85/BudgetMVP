package com.henry.budgetmvp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BudgetColorScheme = lightColorScheme(
    background = BudgetBackground,
    surface = BudgetSurface,
    primary = BudgetPrimary,
    primaryContainer = BudgetPrimaryContainer,
    onPrimaryContainer = BudgetOnPrimaryContainer,
    surfaceVariant = BudgetSurfaceVariant,
    onSurfaceVariant = BudgetOnSurfaceVariant,
    onSurface = BudgetOnSurface,
    outline = BudgetOutline
)

@Composable
fun BudgetAppTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BudgetColorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = BudgetColorScheme,
        typography = Typography,
        content = content
    )
}
