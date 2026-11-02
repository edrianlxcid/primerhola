package com.edrian.galeriaswipe.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Rojo = Color(0xFFFF4F6D)
val Verde = Color(0xFF2ECC71)

private val Colores = darkColorScheme(
    primary = Rojo,
    onPrimary = Color.White,
    secondary = Verde,
    error = Color(0xFFFF5252),
    background = Color(0xFF111114),
    surface = Color(0xFF111114),
    surfaceContainer = Color(0xFF1C1C21),
)

@Composable
fun TemaGaleria(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colores, content = contenido)
}
