package cl.nikalbazu.diptico.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalMovimiento = staticCompositionLocalOf { true }

@Composable
internal fun duracionMovimiento(ms: Int): Int = if (LocalMovimiento.current) ms else 0
